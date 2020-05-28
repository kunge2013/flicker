/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.*;
import com.cratos.platf.game.battle.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import java.util.stream.Collectors;
import org.redkale.service.RetResult;

import static com.cratos.game.skywar.Skywars.*;
import static org.redkale.util.Utility.ofMap;

/**
 *
 * @author zhangjx
 */
public class SkywarTable extends BattleGameTable<SkywarRound, SkywarPlayer, SkywarEnemyRecord, SkywarEnemyKind> {

    protected SkywarPokers pokers;

    public SkywarTable() {
    }

    @Override
    protected boolean isBossKindType(short kindtype) {
        return kindtype == 5;
    }

    protected int[] poker3Cards() {
        SkywarPokers sp = pokers;
        if (sp == null || sp.remaining() < 10) {
            sp = new SkywarPokers();
            pokers = sp;
        }
        return new int[]{sp.pollCard(), sp.pollCard(), sp.pollCard()};
    }

    public RetResult<SkywarKillBean> killEnemy(SkywarService service, final SkywarAccount account, SkywarPlayer player, SkywarKillBean bean) {
        final Logger logger = service.logger();
        bean.setWincoin(0);
        if (bean.getEnemyids() == null) return new RetResult<>(bean);
        final List<Integer> killedEnemyids = new ArrayList<>();
        final List<SkywarEnemyRecord> specialEnemys = new ArrayList<>();
        long killedWinCoins = 0;
        long killedWinDiamonds = 0;
        long killedWinCoupons = 0;
        int hitcount = 0;
        int bosscount = 0;
        if (bean.innerDantouKillPropid > 0) { //全屏攻击
            bean.setEnemyids(enemyRecordMap.values().stream().mapToInt(r -> r.getEnemyid()).toArray());
        }
        final int shotfactor = account.getFactor(player);
        for (int enemyid : bean.getEnemyids()) {
            final SkywarEnemyRecord enemy = findEnemyRecord(enemyid);
            if (enemy == null) continue;
            final BattleKindCounter tableKindCounter = findKindCounter(enemy.getKindid());
            final BattleKindCounter playerKindCounter = player.findKindCounter(enemy.getKindid());
            final int onecoin = enemy.getKind().getKindcoin() * shotfactor;
            final int onediamond = enemy.getKind().getKinddiamond() * shotfactor;
            final int onecoupon = enemy.getKind().getKindcoupon() * shotfactor;
            synchronized (enemy) {
                if (enemy.isDead()) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("敌机(id:" + enemy.getEnemyid() + ",w:" + enemy.getKind().getWidth() + ",h:" + enemy.getKind().getHeight() + ")已死过期");
                    }
                    continue;
                }
                logger.finest("敌机存活时长: 玩家" + player.getUserid() + " " + enemy.getLivingms() + ", 敌机轨迹:" + enemy.getLineid());
                tableKindCounter.increShoted();
                playerKindCounter.increShoted();

                String debugstr = null;
                if (logger.isLoggable(Level.FINEST)) {
                    float x = bean.getPointx();
                    float y = bean.getPointy();
                    GamePoint point = enemy.currGamePoint();
                    if (point != null) {
                        double distance = Math.sqrt(Math.abs((point.x - x) * (point.x - x) + (point.y - y) * (point.y - y)));
                        debugstr = ("敌机(id:" + enemy.getEnemyid()
                            + ")当前位置: (" + point.x + "," + point.y
                            + "), 撞击点位置: (" + x + "," + y + "), 两点距离: " + ((int) (distance * 10000)) / 10000.0
                            + ", 宽高:" + enemy.getKind().getWidth() + ":" + enemy.getKind().getHeight() + ", 击中:" + (distance <= enemy.getKind().getWidth()));
                    } else {
                        debugstr = ("敌机(id:" + enemy.getEnemyid()
                            + ")当前位置: (不存在), 撞击点位置: (" + x + "," + y + "), 两点距离: 无"
                            + ", 宽高:" + enemy.getKind().getWidth() + ":" + enemy.getKind().getHeight() + ", 击中:" + false);
                    }
                }
                if (bean.innerDantouKillPropid < 1 && !enemy.isRange(logger, bean.getPointx(), bean.getPointy())) {
                    tableKindCounter.increToofar();
                    playerKindCounter.increToofar();
                    if (debugstr != null) logger.finest(debugstr);
                    continue;
                }
                hitcount++;
                final int hitrate0 = enemy.getKind().getHitrate(); //敌机的命中率
                int hitrate = hitrate0;
                if (bean.innerDantouKillPropid > 0) hitrate += 100;
                //if (bean.isShotlock()) hitrate += service.confShotLockHitRate;
                //if (bean.isShotthrough()) hitrate += service.confShotThroughHitRate;
                int epopeerate = 0;

                //if (service.isInCoinStage(this.roomlevel)) hitrate += service.confInCoinStageHitRate;
                //if (service.isOutCoinStage(this.roomlevel)) hitrate += service.confOutCoinStageHitRate;
                //if (hpindex > 0) hitrate += hpindex * 10; //0-4的倍率命中率依次增加0%、1%、2%、3%
                if (service.breakLimitLosCoin(roomlevel, killedWinCoins + onecoin)) hitrate = 0; //奖池不够，不命中

                int nowrate = service.gameRandom().nextInt(1000); //千分制
                if (debugstr != null) logger.finest(debugstr + ", 玩家" + player.getUserid() + " 命中率: (hpindex:0," + hitrate0 + "," + hitrate + (epopeerate > 0 ? (",称号加持:" + epopeerate) : "") + "), 当前命中值: " + nowrate + ", 命中: " + (nowrate < hitrate));
                if (nowrate < hitrate) { //命中
                    enemy.getCurrhps()[0].decrementAndGet(); //敌机掉血                        
                    tableKindCounter.increHitted();
                    playerKindCounter.increHitted();
                } else {
                    tableKindCounter.increMissed();
                    playerKindCounter.increMissed();
                }

                if (enemy.isDead()) { //鱼死
                    service.broadcastHrollAnnouncement(player, enemy, onecoin);
                    killedWinCoins += onecoin;
                    killedWinDiamonds += onediamond;
                    killedWinCoupons += onecoupon;
                    tableKindCounter.increKilled();
                    playerKindCounter.increKilled();
                    removeEnemyRecord(enemyid);
                    killedEnemyids.add(enemyid);
                    if (isBossKindType(enemy.getKind().getKindtype())) bosscount++;
                    if (enemy.getKind().getKindcoin() >= 50) account.incrCurrAwardScore(1);
                    if (enemy.getKind().getKindtype() == Skywars.KIND_TYPE_SPECIAL) {
                        specialEnemys.add(enemy);
                    }
                }
            }
        }
        for (SkywarEnemyRecord enemy : specialEnemys) {
            specialDeadEnemy(enemy, shotfactor, service, account, player, bean);
        }
        bean.setKilledEnemyids(killedEnemyids);
        bean.setWincoin(killedWinCoins);
        bean.setWindiamond(killedWinDiamonds);
        bean.setWincoupons(killedWinCoupons);
        long now = System.currentTimeMillis();
        if (!isSport() && !killedEnemyids.isEmpty()) service.updateGameMissionKillEnemy(account.getUserid(), killedEnemyids.size(), now);
        if (!isSport() && hitcount > 0) service.updateGameMissionShotCount(account.getUserid(), hitcount, now);
        if (!isSport() && bosscount > 0) service.updateGameMissionKillBoss(account.getUserid(), bosscount, now);
        return new RetResult<>(bean);
    }

    protected void specialDeadEnemy(final SkywarEnemyRecord deadEnemy, final int shotfactor, SkywarService service, final SkywarAccount account, SkywarPlayer player, SkywarKillBean bean) {
        final int userid = account.getUserid();
        final int kindid = deadEnemy.getKind().getKindid();
        switch (kindid) {
            case 801: //雷电
            {
                List<SkywarEnemyRecord> list = enemyRecordMap.values().stream().filter(r -> r.getFactor() > 0 && r.getFactor() < 50)
                    .sorted((SkywarEnemyRecord r1, SkywarEnemyRecord r2) -> {
                        if (r1.getEnemygroupid() > 0 && r2.getEnemygroupid() < 1) return -1;
                        if (r1.getEnemygroupid() < 1 && r2.getEnemygroupid() > 0) return 1;
                        double distance1 = deadEnemy.distance(r1);
                        double distance2 = deadEnemy.distance(r2);
                        return distance1 > distance2 ? 1 : -1;
                    }).collect(Collectors.toList());
                if (list.isEmpty()) return;
                SkywarEnemyRecord one = list.get(0);
                List<SkywarEnemyRecord> maybes = new ArrayList<>();
                maybes.add(one);
                if (one.getEnemygroupid() > 0) {
                    for (SkywarEnemyRecord item : list) {  //最近的同组巡逻机
                        if (item == one) continue;
                        if (item.getEnemygroupid() == one.getEnemygroupid()) maybes.add(item);
                    }
                }
                long wincoin = 0;
                List<Integer> enemyids = new ArrayList<>();
                for (SkywarEnemyRecord enemy : maybes) {
                    final int onecoin = enemy.getKind().getKindcoin() * shotfactor;
                    if (service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin + onecoin)) continue;
                    wincoin += onecoin;
                    enemyids.add(enemy.getEnemyid());
                    removeEnemyRecord(enemy.getEnemyid());
                }
                if (enemyids.isEmpty()) return;
                bean.innerSpecialWinCoins += wincoin;
                bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("killedids", enemyids, "wincoin", wincoin));
                break;
            }
            case 802: //小行星
            {
                List<SkywarEnemyRecord> list = enemyRecordMap.values().stream().filter(r -> r.getFactor() > 0 && r.getFactor() < 50)
                    .sorted((SkywarEnemyRecord r1, SkywarEnemyRecord r2) -> {
                        double distance1 = deadEnemy.distance(r1);
                        double distance2 = deadEnemy.distance(r2);
                        return distance1 > distance2 ? 1 : -1;
                    }).collect(Collectors.toList());
                if (list.isEmpty()) return;
                List<SkywarEnemyRecord> maybes = new ArrayList<>();
                for (SkywarEnemyRecord item : list) { //2-40级最近最多3个敌机
                    if (deadEnemy.distance(item) <= 500 && maybes.size() < 3) maybes.add(item);
                }
                long wincoin = 0;
                List<Integer> enemyids = new ArrayList<>();
                for (SkywarEnemyRecord enemy : maybes) {
                    final int onecoin = enemy.getKind().getKindcoin() * shotfactor;
                    if (service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin + onecoin)) continue;
                    wincoin += onecoin;
                    enemyids.add(enemy.getEnemyid());
                    removeEnemyRecord(enemy.getEnemyid());
                }
                if (enemyids.isEmpty()) return;
                bean.innerSpecialWinCoins += wincoin;
                bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("killedids", enemyids, "wincoin", wincoin));
                break;
            }
            case 803: //核爆之星
            {
                List<SkywarEnemyRecord> list = enemyRecordMap.values().stream().filter(r -> r.getFactor() > 0 && r.getFactor() < 50)
                    .sorted((SkywarEnemyRecord r1, SkywarEnemyRecord r2) -> {
                        double distance1 = deadEnemy.distance(r1);
                        double distance2 = deadEnemy.distance(r2);
                        return distance1 > distance2 ? 1 : -1;
                    }).collect(Collectors.toList());
                if (list.isEmpty()) return;
                List<SkywarEnemyRecord> maybes = new ArrayList<>();
                for (SkywarEnemyRecord item : list) { //2-40级最近最多5个敌机
                    if (deadEnemy.distance(item) <= 500 && maybes.size() < 5) maybes.add(item);
                }
                long wincoin = 0;
                List<Integer> enemyids = new ArrayList<>();
                for (SkywarEnemyRecord enemy : maybes) {
                    final int onecoin = enemy.getKind().getKindcoin() * shotfactor;
                    if (service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin + onecoin)) continue;
                    wincoin += onecoin;
                    enemyids.add(enemy.getEnemyid());
                    removeEnemyRecord(enemy.getEnemyid());
                }
                if (enemyids.isEmpty()) return;
                bean.innerSpecialWinCoins += wincoin;
                bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("killedids", enemyids, "wincoin", wincoin));
                break;
            }
            case 804: //激光炮
            {
                bean.innerJiguangable = true;
                bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("jiguang", true));
                break;
            }
            case 805: //数码飞船
            {
                int rand = service.gameRandom().nextInt(1000);
                long wincoin = rand * shotfactor;
                if (service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    rand = service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + shotfactor) ? 0 : 1;
                    wincoin = rand * shotfactor;
                }
                bean.innerSpecialWinCoins += wincoin;
                bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("rand", new DecimalFormat("000").format(rand), "wincoin", wincoin));
                break;
            }
            case 806: //幸运UFO
            {
                int coinindex = PROP_UFO_COIN_WEIGHTS[service.gameRandom().nextInt(PROP_UFO_COIN_WEIGHTS.length)];
                int basecoin = PROP_UFO_COIN_ITEMS[coinindex];
                int factorindex = PROP_UFO_FACTOR_WEIGHTS[service.gameRandom().nextInt(PROP_UFO_FACTOR_WEIGHTS.length)];
                float factor = PROP_UFO_FACTOR_ITEMS[factorindex];
                long wincoin = (long) (basecoin * factor * shotfactor);
                if (!service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    bean.innerSpecialWinCoins += wincoin;
                    bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("coinitems", PROP_UFO_COIN_ITEMS, "factoritems", PROP_UFO_FACTOR_ITEMS, "basecoin", basecoin, "factor", factor, "wincoin", wincoin));
                }
                break;
            }
            case 807: //暴击舰
            {
                int rand = service.gameRandom().nextInt(200);
                long wincoin = rand * shotfactor;
                int trys = 0;
                while (trys < 50 && service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    rand = service.gameRandom().nextInt(200);
                    wincoin = rand * shotfactor;
                    trys++;
                }
                if (trys >= 50) {
                    rand = 1;
                    wincoin = rand * shotfactor;
                }
                if (!service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    bean.innerSpecialWinCoins += wincoin;
                    bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("items", new int[]{25, 9, 20, 18, 12, 16, 36, 40}, "wincoin", wincoin));
                }
                break;
            }
            case 808: //小丑飞艇
                //只得金币
                break;
            case 809: //幸运火箭
            {
                int rand = service.gameRandom().nextInt(100);
                long wincoupon = rand * shotfactor;
                bean.innerSpecialWinCoupons += wincoupon;
                bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("rand", new DecimalFormat("00").format(rand), "factor", shotfactor, "wincoupon", wincoupon));
                break;
            }
            case 810: //海盗舰鲨
            {
                int[] cards = poker3Cards();
                int cardtype = Skywars.getCardType(cards[0], cards[1], cards[2]);
                int cardfactor = Skywars.CARDTYPE_FACTORS[cardtype];
                int factor = service.gameRandom().nextInt(10) <= 7 ? 1 : 2;
                long wincoin = cardfactor * shotfactor * factor;
                int trys = 0;
                while (trys < 50 && service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    cards = poker3Cards();
                    cardtype = Skywars.getCardType(cards[0], cards[1], cards[2]);
                    cardfactor = Skywars.CARDTYPE_FACTORS[cardtype];
                    factor = service.gameRandom().nextInt(10) <= 7 ? 1 : 2;
                    wincoin = cardfactor * shotfactor * factor;
                    trys++;
                }
                if (trys >= 50) break;
                if (!service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    bean.innerSpecialWinCoins += wincoin;
                    bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("cards", cards, "cardtype", cardtype, "factor", factor, "wincoin", wincoin));
                }
                break;
            }
            case 811: //海盗鲸鲨
            {
                int[] cards = poker3Cards();
                int cardtype = Skywars.getCardType(cards[0], cards[1], cards[2]);
                int cardfactor = Skywars.CARDTYPE_FACTORS[cardtype];
                int factor = service.gameRandom().nextInt(10) <= 7 ? 1 : 2;
                long wincoin = cardfactor * shotfactor * factor;
                int trys = 0;
                while (trys < 50 && service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    cards = poker3Cards();
                    cardtype = Skywars.getCardType(cards[0], cards[1], cards[2]);
                    cardfactor = Skywars.CARDTYPE_FACTORS[cardtype];
                    factor = service.gameRandom().nextInt(10) <= 7 ? 1 : 2;
                    wincoin = cardfactor * shotfactor * factor;
                    trys++;
                }
                if (trys >= 50) break;
                if (!service.breakLimitLosCoin(roomlevel, bean.getWincoin() + bean.innerSpecialWinCoins + wincoin)) {
                    bean.innerSpecialWinCoins += wincoin;
                    bean.addSpecialResult(deadEnemy.getEnemyid(), ofMap("cards", cards, "cardtype", cardtype, "factor", factor, "wincoin", wincoin));
                }
                break;
            }
            case 812: //海盗船长
                break;
            case 813: //解码卫星
                break;
            case 814: //远古宝船
                break;
        }
    }

    @Override
    public SkywarEnemyRecord createEnemyRecord(BattleCoinGameService service, SkywarEnemyKind kind, BattleEnemyLine line, int lifetimesx, AtomicInteger seqno) {
        return new SkywarEnemyRecord(kind, line, lifetimesx, seqno);
    }

    @Override
    protected BattleEnemyArmy createEnemyArmy(BattleCoinGameService service) {
        return new SkywarEnemyArmy(service, this);
    }

    @Override  //会定期重复调用
    public void initConfig(BattleCoinGameService service) {
        if (this.players == null) {
            this.maxPlayerCount = (this.paramBean != null && this.paramBean.getMaxPlayerCount() == 1) ? 1 : 3;
            this.players = new SkywarPlayer[this.maxPlayerCount];
        }
    }

    @Override
    public boolean start(BattleCoinGameService service) {
        if (!super.start(service)) return false;
        return true;
    }

    @Override
    public boolean stop(BattleCoinGameService service) {
        if (!super.stop(service)) return false;
        return true;
    }

}
