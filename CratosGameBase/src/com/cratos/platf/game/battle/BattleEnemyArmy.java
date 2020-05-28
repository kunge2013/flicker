/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseBean;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.persistence.Transient;
import org.redkale.util.ConstructorParameters;

/**
 *
 * @author zhangjx
 */
public class BattleEnemyArmy extends BaseBean {

    protected int armyid;

    //创建时间
    protected long createtime;

    //关闭时间点， < 1表示无需关闭
    protected long endtime;

    //是否关闭
    protected boolean closed;

    //敌机潮存活的时间， < 1表示无限
    protected int lifeSeconds;

    protected final LinkedBlockingQueue<List<BattleEnemyRecord>> queue = new LinkedBlockingQueue<>(30);

    protected AtomicInteger seq;

    @Transient
    protected BattleCoinGameService service;

    @Transient
    protected BattleGameTable table;

    @Transient
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient
    protected SecureRandom random;

    @ConstructorParameters({"service", "table"})
    public BattleEnemyArmy(BattleCoinGameService service, BattleGameTable table) {
        this.service = service;
        this.table = table;
        this.seq = table.seq;
        this.random = service.gameRandom();
    }

    //比赛场需要重载此方法
    protected LinkedHashMap<Short, Integer> getEnemyKindTypeToCreateRates() {
        LinkedHashMap<Short, Integer> map = (LinkedHashMap) service.enemyKindTypeToSportCreateRates.get(table.getRoomlevel());
        return map == null ? service.enemyKindTypeToNormalCreateRates : map;
    }

    //比赛场需要重载此方法
    protected LinkedHashMap<Short, Integer> getEnemyKindTypeToCreateCounts() {
        LinkedHashMap<Short, Integer> map = (LinkedHashMap) service.enemyKindTypeToSportCreateCounts.get(table.getRoomlevel());
        return map == null ? service.enemyKindTypeToNormalCreateCounts : map;
    }

    protected void initScheduledEnemy() {
        this.scheduler = table.scheduler;
        final Set<Integer> lineSet = new HashSet<>();
        final int roomlevel = table.getRoomlevel();
        scheduler.scheduleAtFixedRate(() -> {  //生产敌机
            try {
                if (table.enemyRecordMap.size() > service.confNormalKindScreenLimit) return;
                if (lineSet.size() > 10) lineSet.clear();
                final List<BattleEnemyRecord> enemys = new ArrayList<>();
                final LinkedHashMap<Short, Integer> kindTypeCreateRates = getEnemyKindTypeToCreateRates();
                final LinkedHashMap<Short, Integer> kindTypeCreateCounts = getEnemyKindTypeToCreateCounts();
                kindTypeCreateRates.forEach((kindtype, kindrate) -> {
                    if (random.nextInt(100) > kindrate) return;
                    int count = kindTypeCreateCounts.getOrDefault(kindtype, 0);
                    if (count < 1) return;
                    if (table.isBossKindType(kindtype) && table.containsBoss()) return;
                    int num = 0;
                    List<BattleEnemyRecord> subenemys = new ArrayList<>();
                    while (num < count) {
                        BattleEnemyKind kind = service.randomEnemyKind(roomlevel, kindtype);
                        if (kind == null) return; //场次不能有该kindtype
                        if (kind.getNewnum() < 1) continue;
                        int knum = kind.getNewnum();
                        if (table.isBossKindType(kindtype)) {
                            knum = 1;
                            table.bosscoming = true;
                        }
                        for (int i = 0; i < knum; i++) {
                            BattleEnemyRecord enemy = table.createEnemyRecord(service, kind, service.randomEnemyLine(roomlevel, kindtype, lineSet), 0, seq);
                            enemys.add(enemy);
                            subenemys.add(enemy);
                        }
                        num += knum;
                    }
                    if (!subenemys.isEmpty()) afterCreateEnemyRecord(subenemys);
                });
                if (!enemys.isEmpty()) queue.add(enemys); //list为空会被执行close
            } catch (Throwable e) {
                service.logger().log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    protected void afterCreateEnemyRecord(List<BattleEnemyRecord> enemys) {
    }

    //立即增加指定类型的敌机
    public List<BattleEnemyRecord> createEnemyRecord(final int count, short... kindtypes) {
        final int roomlevel = table.getRoomlevel();
        final Set<Integer> lineSet = new HashSet<>();
        final List<BattleEnemyRecord> subenemys = new ArrayList<>();
        int num = 0;
        int trys = 0;
        while (num < count) {
            short kindtype = kindtypes.length == 1 ? kindtypes[0] : kindtypes[random.nextInt(kindtypes.length)];
            BattleEnemyKind kind = service.randomEnemyKind(roomlevel, kindtype);
            if (kind == null) { //场次不能有该kindtype
                if (trys++ > 100) break;
                continue;
            }
            BattleEnemyRecord enemy = table.createEnemyRecord(service, kind, service.randomEnemyLine(roomlevel, kindtype, lineSet), 0, seq);
            subenemys.add(enemy);
            ++num;
        }
        afterCreateEnemyRecord(subenemys);
        return subenemys;
    }

    //阻塞式生产敌机对象
    public List<BattleEnemyRecord> pollEnemys() {
        if (scheduler == null) {
            synchronized (this) {
                if (scheduler == null) {
                    initScheduledEnemy();
                }
            }
        }
        try {
            List<BattleEnemyRecord> rs = queue.take();
            if (rs == null || rs.isEmpty()) close();
            return rs;
        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        this.closed = true;
        queue.add(new ArrayList<>()); //用于Table判断Army关闭
    }

    public int getArmyid() {
        return armyid;
    }
}
