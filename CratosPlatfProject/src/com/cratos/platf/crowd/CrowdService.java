/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.crowd;

import com.cratos.platf.base.*;
import static com.cratos.platf.crowd.CrowdApplyDayRecord.*;
import com.cratos.platf.info.*;
import com.cratos.platf.letter.*;
import com.cratos.platf.order.GoodsItem;
import com.cratos.platf.user.UserService;
import com.cratos.platf.util.ShuffleRandom;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.persistence.Transient;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class CrowdService extends BaseService {

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected DictService dictService;

    @Resource
    protected LetterService letterService;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient
    private CrowdPoolRecord yesterdayPoolRecord;

    @Transient
    private CrowdPoolRecord todayPoolRecord;

    @Override
    public void init(AnyValue conf) {
        checkPoolRecord(System.currentTimeMillis());
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                applyRobotCoin();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, this.getClass().getSimpleName() + " applyRobotCoin error", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
        long now = System.currentTimeMillis();
        long mid = Utility.midnight(now);
        long kaitime = mid + todayPoolRecord.getDakaendmills();
        long delay = kaitime >= now ? (kaitime - now) : (24 * 60 * 60 * 1000L + kaitime - now);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                kaiJiang();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, this.getClass().getSimpleName() + " kaiJiang error", e);
            }
        }, delay, 24 * 60 * 60 * 1000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
        CrowdPoolRecord data = this.todayPoolRecord;
        if (data != null) source.update(data);
    }

    protected void kaiJiang() {
        int yesterday = Utility.yesterday();
        CrowdPoolRecord yesterPoolRecord = source.find(CrowdPoolRecord.class, yesterday);
        if (yesterPoolRecord == null || yesterPoolRecord.getDakacoins() < 1) return;
        Set<Integer> userids = source.queryColumnSet("userid", CrowdApplyDayRecord.class, FilterNode.create("intday", yesterday).and("status", CROWD_APPLY_STATUS_DONE));
        for (int userid : userids) {
            List<CrowdApplyDayRecord> records = source.queryList(CrowdApplyDayRecord.class, FilterNode.create("intday", yesterday).and("status", CROWD_APPLY_STATUS_DONE).and("userid", userid));
            if (records.isEmpty()) continue;
            long applycoins = 0;
            for (CrowdApplyDayRecord r : records) {
                applycoins += r.getApplycoins();
            }
            int wincoins = (int) (((applycoins + 0.0) / yesterPoolRecord.getDakacoins()) * yesterPoolRecord.getHumancoins());
            LetterRecord letter = new LetterRecord();
            letter.setUserid(userid);
            letter.setLettertype(LetterRecord.LETTER_TYPE_GIFT);
            letter.setTitle(letterService.bundleResourceValue("crowd.daka.title"));
            letter.setContent(letterService.bundleResourceValue("crowd.daka.content", records.size(), wincoins));
            letter.setGoodsitems(new GoodsItem[]{GoodsItem.createCoin(wincoins)});
            letter.setModule("crowddaka");
            letter.setRemark("crowid=" + yesterPoolRecord.getIntday());
            letterService.createLetterRecord(letter);
        }
    }

    public CrowdPoolRecord getTodayPoolRecord() {
        return this.todayPoolRecord;
    }

    public int getApplyCount(int userid, int intday) {
        FilterNode node = FilterNode.create("userid", userid).and("intday", intday);
        int count = source.getNumberResult(CrowdApplyDayRecord.class, FilterFunc.COUNT, 0, null, node).intValue();
        return count;
    }

    public List<Short> queryApplyStatus(int userid, int intday) {
        FilterNode node = FilterNode.create("userid", userid).and("intday", intday);
        List<Short> statuses = source.queryColumnList("status", CrowdApplyDayRecord.class, node);
        return statuses;
    }

    //玩家打卡
    public RetResult daka(int userid) {
        int yesterday = Utility.yesterday();
        FilterNode node = FilterNode.create("userid", userid).and("intday", yesterday);
        long coins = source.getNumberResult(CrowdApplyDayRecord.class, FilterFunc.SUM, 0L, "applycoins", node).longValue();
        if (coins < 1) return RetResult.success();
        long now = System.currentTimeMillis();
        if (now < Utility.midnight(now) + yesterdayPoolRecord.getDakastartmills()) return RetCodes.retResult(RetCodes.RET_CROWD_DAKA_TIME_ILLEGAL);
        if (now > Utility.midnight(now) + yesterdayPoolRecord.getDakaendmills()) return RetCodes.retResult(RetCodes.RET_CROWD_DAKA_TIME_ILLEGAL);
        source.updateColumn(CrowdPoolRecord.class, FilterNode.create("intday", yesterday), ColumnValue.inc("dakacoins", coins));
        source.updateColumn(CrowdApplyDayRecord.class, node, ColumnValue.mov("status", CROWD_APPLY_STATUS_DONE));
        return RetResult.success();
    }

    //玩家报名
    public RetResult apply(int userid) {
        long now = System.currentTimeMillis();
        long mid = Utility.midnight(now);
        int today = Utility.yyyyMMdd(now);
        FilterNode node = FilterNode.create("userid", userid).and("intday", today);
        final CrowdPoolRecord todaydata = checkPoolRecord(now);
        if (now >= mid + todaydata.getDakastartmills()) return RetCodes.retResult(RetCodes.RET_CROWD_APPLY_TIME_ILLEGAL);
        synchronized (this) {
            int count = source.getNumberResult(CrowdApplyDayRecord.class, FilterFunc.COUNT, 0, null, node).intValue();
            if (count >= todaydata.getApplylimit()) return RetCodes.retResult(RetCodes.RET_CROWD_APPLY_LIMIT_ILLEGAL);
            UserInfo user = userService.findUserInfo(userid);
            long coin = todaydata.getApplystartcoins() + count * todaydata.getApplyincrecoins();
            if (user.getCoins() < coin) return RetCodes.retResult(RetCodes.RET_USER_COINS_NOTENOUGH);
            userService.decrePlatfUserCoins(userid, coin, now, "crowapply", "全民打卡报名");
            CrowdApplyDayRecord apply = new CrowdApplyDayRecord();
            apply.setApplyindex(count + 1);
            apply.setCreatetime(now);
            apply.setIntday(today);
            apply.setUserid(userid);
            apply.setApplycoins(now);
            apply.setApplycoins(coin);
            apply.setStatus(CROWD_APPLY_STATUS_UNDO);
            apply.setCrowdapplyrecordid(today + "-" + userid + "-" + apply.getApplyindex());
            todaydata.increHumanCoins(apply.getApplycoins());
            source.insert(apply);
        }
        return RetResult.success();
    }

    private void applyRobotCoin() {
        CrowdPoolRecord todaydata = this.todayPoolRecord;
        if (todaydata == null) return;
        if (random.nextInt(3) < 1) return;
        if (System.currentTimeMillis() >= Utility.midnight() + todaydata.getDakastartmills()) return;
        long coin = todaydata.getApplystartcoins() + random.nextInt(todaydata.getApplylimit()) * todaydata.getApplyincrecoins();
        todaydata.increRobotCoins(coin);
    }

    private CrowdPoolRecord checkPoolRecord(long now) {
        int intday = Utility.yyyyMMdd(now);
        CrowdPoolRecord todaydata = this.todayPoolRecord;
        if (todaydata == null) {
            synchronized (this) {
                if (todayPoolRecord == null) {
                    todayPoolRecord = source.find(CrowdPoolRecord.class, intday);
                    if (todayPoolRecord == null) {  //虚构一条记录
                        todayPoolRecord = createCrowdPoolRecord(now);
                        source.insert(todayPoolRecord);
                    }
                    int yesterday = Utility.yyyyMMdd(now - 24 * 60 * 60 * 1000L);
                    CrowdPoolRecord yesterRecord = source.find(CrowdPoolRecord.class, yesterday);
                    if (yesterRecord == null) { //虚构
                        yesterRecord = createCrowdPoolRecord(now - 24 * 60 * 60 * 1000L);
                        long coins = yesterRecord.getApplystartcoins() * (random.nextInt(3000) + 2345);
                        yesterRecord.increRobotCoins(coins);
                        source.insert(yesterRecord);
                    }
                    this.yesterdayPoolRecord = yesterRecord;
                    todayPoolRecord.setYesterdaypoolcoins(yesterRecord.getPoolcoins());
                }
            }
        } else if (todaydata.getIntday() != intday) {
            synchronized (this) {
                if (todayPoolRecord.getIntday() != intday) {
                    source.update(todayPoolRecord);
                    long yesterPoolCoins = todayPoolRecord.getPoolcoins();
                    this.yesterdayPoolRecord = todayPoolRecord;
                    this.todayPoolRecord = createCrowdPoolRecord(now);
                    todayPoolRecord.setYesterdaypoolcoins(yesterPoolCoins);
                }
            }
        }
        return this.todayPoolRecord;
    }

    private CrowdPoolRecord createCrowdPoolRecord(long time) {
        CrowdPoolRecord data = new CrowdPoolRecord();
        data.setIntday(Utility.yyyyMMdd(time));
        data.setApplylimit(dictService.findDictValue(DictInfo.PLATF_CROWD_APPLYLIMIT, 10));
        data.setApplyincrecoins(dictService.findDictValue(DictInfo.PLATF_CROWD_APPLYINCRECOIN, 1000L));
        data.setApplystartcoins(dictService.findDictValue(DictInfo.PLATF_CROWD_APPLYSTARTCOIN, 20000L));
        data.setCreatetime(time);
        data.setCrowdstarttime(dictService.findDictValue(DictInfo.PLATF_CROWD_STARTTIME, 0L));
        data.setCrowdendtime(dictService.findDictValue(DictInfo.PLATF_CROWD_ENDTIME, 0L));
        data.setDakastartmills(dictService.findDictValue(DictInfo.PLATF_CROWD_DAKASTART_MILLS, 68400000L));
        data.setDakaendmills(dictService.findDictValue(DictInfo.PLATF_CROWD_DAKAEND_MILLS, 77400000L));
        return data;
    }
}
