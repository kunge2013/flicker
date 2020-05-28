/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.game.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.service.RetResult;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 * @param <R> BattleGameRound
 * @param <P> BattleGamePlayer
 * @param <E> BattleEnemyRecord
 * @param <K> BattleEnemyKind
 */
public abstract class BattleGameTable<R extends BattleGameRound, P extends BattleGamePlayer, E extends BattleEnemyRecord, K extends BattleEnemyKind> extends GameTable<P> {

    @Comment("金币底注，为0表示非金币场")
    protected int baseBetCoin;

    @Column(comment = "状态")
    protected short tableStatus = TABLE_STATUS_READYING;

    @Column(comment = "最大玩家数")
    protected int maxPlayerCount;

    @Column(nullable = false, comment = "扩展选项")
    protected Map<String, String> extmap;

    @Column(comment = "手续费")
    protected AtomicLong taxcoin = new AtomicLong();

    @Column(comment = "系统所获金币，负数表示系统亏钱")
    protected AtomicLong oswincoins = new AtomicLong();

    //-----------------非数据库字段------------------- 
    @Transient //是否正处于boss要加入的阶段
    protected boolean bosscoming;

    @Transient //射击频率每秒
    protected int shotrate = 0;

    @Transient
    protected final AtomicInteger seq = new AtomicInteger(1000);

    @Transient
    protected GameTableBean paramBean;

    @Transient
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //屏幕中的敌人, key为enemyid
    protected ConcurrentHashMap<Integer, E> enemyRecordMap = new ConcurrentHashMap<>();

    @Transient
    protected Map<Integer, BattleKindCounter> kindCounterMap;

    @Transient
    protected BattleEnemyArmy currEnemyArmy;

    @Transient
    protected BattleSportRoom sportRoom;

    //会定期重复调用
    protected void initConfig(BattleCoinGameService service) {
        this.shotrate = service.confShotRate;
    }

    @Override //玩家加入
    public RetResult<P> addPlayer(P player, GameService service) {
        RetResult<P> rs = super.addPlayer(player, service);
        if (rs.isSuccess() && this.kindCounterMap != null && player.kindCounterMap != null) {
            this.kindCounterMap.forEach((kindid, counter) -> player.kindCounterMap.put(kindid, new BattleKindCounter()));
        }
        return rs;
    }

    //是否比赛场
    public boolean isSport() {
        return this.roomlevel > 100;
    }

    protected abstract boolean isBossKindType(short kindtype);

    protected boolean containsBoss() {
        if (bosscoming) return true;
        return !enemyRecordMap.isEmpty() && enemyRecordMap.values().stream().anyMatch(e -> isBossKindType(e.kind.getKindtype()));
    }

    //创建敌人对象
    public abstract E createEnemyRecord(BattleCoinGameService service, K kind, BattleEnemyLine line, int lifetimesx, AtomicInteger seq);

    //创建敌人队伍
    protected abstract BattleEnemyArmy createEnemyArmy(BattleCoinGameService service);

    //开始
    public boolean start(BattleCoinGameService service) {
        if (scheduler != null) return false;
        this.currEnemyArmy = createEnemyArmy(service);
        this.scheduler = new ScheduledThreadPoolExecutor(4, (Runnable r) -> {
            final Thread t = new Thread(r, getClass().getSimpleName() + "-Scheduler-Thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.schedule(() -> {
            while (scheduler != null) {
                final List<BattleEnemyRecord> enemys = currEnemyArmy.pollEnemys();
                if (enemys == null || enemys.isEmpty()) {
                    if (service.logger().isLoggable(Level.FINEST)) service.logger().finest("no enemy create on army-" + currEnemyArmy.getArmyid());
                    try {
                        final Object lock = scheduler;
                        if (lock != null) {
                            synchronized (lock) {
                                lock.wait(10 * 1000);
                            }
                        }
                    } catch (Exception e) {
                    }
                    continue;
                }
                Consumer<List<BattleEnemyRecord>> runner = (List<BattleEnemyRecord> es) -> {
                    for (BattleEnemyRecord record : es) {
                        BattleKindCounter counter = findKindCounter(record.getKindid());
                        if (counter != null) counter.increCount();
                        enemyRecordMap.put(record.getEnemyid(), (E) record.resetCreatetime());
                    }
                    service.sendEnemyRecord(onlinePlayers(), es);
                };
                BattleEnemyRecord boss = null;
                List<BattleEnemyRecord> others = enemys;
                if (bosscoming) {
                    others = new ArrayList<>();
                    for (BattleEnemyRecord r : enemys) {
                        if (boss == null && isBossKindType(r.kind.getKindtype())) {
                            boss = r;
                        } else {
                            others.add(r);
                        }
                    }
                    if (boss != null) {
                        service.sendBossComing(onlinePlayers(), boss);
                        final List<BattleEnemyRecord> bosslist = Utility.ofList(boss);
                        scheduler.schedule(() -> {
                            runner.accept(bosslist);
                            bosscoming = false;
                        }, 3000, TimeUnit.MILLISECONDS);
                        runner.accept(others);
                    } else {
                        bosscoming = false;
                        runner.accept(others);
                    }
                } else {
                    runner.accept(others);
                }
            }
        }, 0, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> { //定时移除过期的鱼
            try {
                if (seq.get() > 20_0000) seq.set(0); //超过21万就再乘以1万就越界了
                //logger.log(Level.FINEST, tableno + " 当前的序号ID=" + seq.get());
                //过期超过三秒的移除
                enemyRecordMap.values().stream().filter(f -> f.isDead()).forEach(f -> {
                    enemyRecordMap.remove(f.getEnemyid());
                });
            } catch (Throwable t) {
                service.logger().log(Level.SEVERE, this.getClass().getSimpleName() + ".remove expired enemy error", t);
            }
        }, 1500, 200, TimeUnit.MILLISECONDS);
        return true;
    }

    //关闭
    public boolean stop(BattleCoinGameService service) {
        if (scheduler == null) return false;
        scheduler.shutdownNow();
        scheduler = null; //必须赋值为null， 供while判断
        if (this.currEnemyArmy != null) {
            this.currEnemyArmy.close();
            this.currEnemyArmy = null;
        }
        return true;
    }

    //立即增加指定类型的敌机
    public RetResult joinEnemyRecordImmediately(BattleCoinGameService service, int count, short... kindtypes) {
        if (this.currEnemyArmy == null) return RetResult.success();
        final List<BattleEnemyRecord> enemys = this.currEnemyArmy.createEnemyRecord(count, kindtypes);
        scheduler.schedule(() -> {
            for (BattleEnemyRecord record : enemys) {
                BattleKindCounter counter = findKindCounter(record.getKindid());
                if (counter != null) counter.increCount();
                enemyRecordMap.put(record.getEnemyid(), (E) record);
            }
            service.sendEnemyRecord2(onlinePlayers(), enemys);
        }, 500, TimeUnit.MILLISECONDS);
        return RetResult.success();
    }

    protected static void await(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
        }
    }

    public void paramBean(GameTableBean paramBean) {
        this.paramBean = paramBean;
    }

    public void increOswincoins(long coin) {
        this.oswincoins.addAndGet(coin);
    }

    public void increTaxcoin(long coin) {
        this.taxcoin.addAndGet(coin);
    }

    public BattleKindCounter findKindCounter(int kindid) {
        return this.kindCounterMap == null ? null : this.kindCounterMap.get(kindid);
    }

    public E findEnemyRecord(int enemyid) {
        return enemyRecordMap.get(enemyid);
    }

    public E removeEnemyRecord(int enemyid) {
        return enemyRecordMap.remove(enemyid);
    }

    public Collection<E> getEnemys() {
        return enemyRecordMap.values();
    }

    public int getBaseBetCoin() {
        return baseBetCoin;
    }

    public void setBaseBetCoin(int baseBetCoin) {
        this.baseBetCoin = baseBetCoin;
    }

    public short getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(short tableStatus) {
        this.tableStatus = tableStatus;
    }

    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public Map<String, String> getExtmap() {
        return extmap;
    }

    public void setExtmap(Map<String, String> extmap) {
        this.extmap = extmap;
    }

    public int getShotrate() {
        return shotrate;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public AtomicLong getTaxcoin() {
        return taxcoin;
    }

    public void setTaxcoin(AtomicLong taxcoin) {
        this.taxcoin = taxcoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public AtomicLong getOswincoins() {
        return oswincoins;
    }

    public void setOswincoins(AtomicLong oswincoins) {
        this.oswincoins = oswincoins;
    }

    public void setShotrate(int shotrate) {
        this.shotrate = shotrate;
    }

}
