/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.mission;

import com.cratos.platf.base.*;
import com.cratos.platf.liveness.LivenessService;
import static com.cratos.platf.mission.MissionInfo.*;
import static com.cratos.platf.mission.MissionRecord.*;
import com.cratos.platf.order.*;
import com.cratos.platf.user.*;
import com.cratos.platf.util.*;
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
public class MissionService extends BaseService {

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    public static final Flipper MYRECORD_FLIPPER = new Flipper(1000, "missionstatus ASC,display ASC");

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected GoodsService goodsService;

    @Resource
    protected LivenessService livenessService;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //新手任务 
    protected List<MissionInfo> onceMissions;

    @Transient //活跃任务 
    protected List<MissionInfo> dayMissions;

    @Transient //当天日期
    protected int currDay = Utility.today();

    @Transient //已完成活跃任务类型的任务组合 userid+intday+missiontype+missionobjid
    protected Set<String> currDayReaches = new CopyOnWriteArraySet<>();

    @Transient //已完成新手任务类型的任务userid
    protected Set<Integer> currOnceReaches = new CopyOnWriteArraySet<>();

    @Transient //MissionUpdateEntry 队列
    protected final QueueTask<MissionUpdateEntry> missionQueue = new QueueTask<>(5);

    @Override
    public void init(AnyValue conf) {
        reloadConfig();
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        missionQueue.init(logger, (queue, entry) -> {
            runMissionUpdateEntry(entry);
        });
        final long seconds = 1 * 60 * 1000L;
        final long delay = seconds - System.currentTimeMillis() % seconds; //每分钟执行
        scheduler.scheduleAtFixedRate(() -> {
            try {
                reloadConfig();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, MissionService.class.getSimpleName() + " scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
        missionQueue.destroy();
    }

    protected void runMissionUpdateEntry(MissionUpdateEntry entry) {
        if (entry.reachcount < 1) {
            logger.log(Level.SEVERE, "runMissionUpdateEntry " + entry + " error, reachcount less 1");
            return;
        }
        long now = System.currentTimeMillis();
        final int intday = Utility.yyyyMMdd(entry.reachtime);
        final String daykey = entry.userid + "-" + intday + "-" + entry.missiontype + "-" + entry.reachobjid;
        if (intday != this.currDay) {
            synchronized (this) {
                final int today = Utility.today();
                if (today != this.currDay) {
                    this.currDay = today;
                    this.currDayReaches = new CopyOnWriteArraySet<>();
                }
            }
        }
        if (!currOnceReaches.contains(entry.userid)) {
            FilterNode node = FilterNode.create("userid", entry.userid).and("missionstatus", MISSION_STATUS_DOING);
            List<MissionOnceRecord> records = source.queryList(MissionOnceRecord.class, node);
            if (records == null || records.isEmpty()) {
                currOnceReaches.add(entry.userid);
            } else {
                boolean onceAllreached = true;
                for (MissionOnceRecord item : records) {
                    if (entry.incrable) {
                        item.increCurrcount(entry.reachcount);
                    } else {
                        item.setCurrcount(entry.reachcount);
                    }
                    if (item.isReached()) {
                        item.setCurrcount(item.getReachcount());
                        item.setMissionstatus(MISSION_STATUS_REACH);
                        item.setReachtime(now);
                        source.updateColumn(item, "currcount", "missionstatus", "reachtime");
                    } else {
                        source.updateColumn(item, "currcount");
                        onceAllreached = false;
                    }
                }
                if (onceAllreached) currOnceReaches.add(entry.userid);
            }
        }
        if (!currDayReaches.contains(daykey)) {
            FilterNode node = FilterNode.create("userid", entry.userid)
                .and("intday", intday).and("missiontype", entry.missiontype)
                .and("reachobjid", entry.reachobjid).and("missionstatus", MISSION_STATUS_DOING);
            List<MissionDayRecord> records = source.queryList(MissionDayRecord.class, node);
            if (records == null || records.isEmpty()) {
                currDayReaches.add(daykey);
            } else {
                boolean dayAllreached = true;
                for (MissionDayRecord item : records) {
                    if (entry.incrable) {
                        item.increCurrcount(entry.reachcount);
                    } else {
                        item.setCurrcount(entry.reachcount);
                    }
                    if (item.isReached()) {
                        item.setCurrcount(item.getReachcount());
                        item.setMissionstatus(MISSION_STATUS_REACH);
                        item.setReachtime(now);
                        source.updateColumn(item, "currcount", "missionstatus", "reachtime");
                    } else {
                        source.updateColumn(item, "currcount");
                        dayAllreached = false;
                    }
                }
                if (dayAllreached) currDayReaches.add(daykey);
            }
        }
    }

    protected void reloadConfig() {
        List<MissionInfo> missions = source.queryList(MissionInfo.class, new Flipper(100, "display ASC"), (FilterNode) null);
        List<MissionInfo> onces = new ArrayList<>();
        List<MissionInfo> dayes = new ArrayList<>();
        for (MissionInfo info : missions) {
            if (info.getMissionkind() == MissionInfo.MISSION_KIND_DAY) {
                dayes.add(info);
            } else if (info.getMissionkind() == MissionInfo.MISSION_KIND_ONCE) {
                onces.add(info);
            }
        }
        this.onceMissions = onces;
        this.dayMissions = dayes;
    }

    //用户登录操作
    public void updatePlatfMissionLogin(int userid, long time) {
        this.insertMissionDayRecord(userid, time);
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_LOGIN, 1, time));
    }

    //用户支付操作
    public void updatePlatfMissionPay(int userid, long paymoney, long time) {
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_PAYCOUNT, 1, time));
    }

    //用户消耗金币操作
    public void updatePlatfMissionCostCoin(int userid, long coin, String gameid, long time) {
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_COSTCOIN, coin, time));
    }

    //用户消耗晶石操作
    public void updatePlatfMissionCostDiamond(int userid, long diamond, String gameid, long time) {
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_COSTDIAMOND, diamond, time));
    }

    //用户消耗奖券操作
    public void updatePlatfMissionCostCoupon(int userid, long coupon, String gameid, long time) {
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_COSTCOUPON, coupon, time));
    }

    //用户获得金币操作
    public void updatePlatfMissionWinCoin(int userid, long coin, String gameid, long time) {
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_WINCOIN, coin, time));
    }

    //用户获得晶石操作
    public void updatePlatfMissionWinDiamond(int userid, long diamond, String gameid, long time) {
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_WINDIAMOND, diamond, time));
    }

    //用户获得奖券操作
    public void updatePlatfMissionWinCoupon(int userid, long coupon, String gameid, long time) {
        missionQueue.add(new MissionUpdateEntry(userid, true, MISSION_TYPE_PLATF_WINCOUPON, coupon, time));
    }

    //获取可领取新手任务的数量
    public int checkMissionOnceRecord(int userid) {
        FilterNode node = FilterNode.create("userid", userid).and("missionstatus", MissionDayRecord.MISSION_STATUS_REACH);
        return source.getNumberResult(MissionOnceRecord.class, FilterFunc.COUNT, 0, null, node).intValue();
    }

    //获取可领取活跃任务的数量
    public int checkMissionDayRecord(int userid, int intday) {
        FilterNode node = FilterNode.create("userid", userid).and("intday", intday).and("missionstatus", MissionDayRecord.MISSION_STATUS_REACH);
        return source.getNumberResult(MissionDayRecord.class, FilterFunc.COUNT, 0, null, node).intValue();
    }

    //领取指定任务
    public RetResult<MissionOnceRecord> reachMissionOnceRecord(int userid, String recordid) {
        MissionOnceRecord record = source.find(MissionOnceRecord.class, recordid);
        if (record == null) return RetCodes.retResult(RetCodes.RET_MISSION_NOT_EXISTS);
        if (record.getMissionstatus() != MissionRecord.MISSION_STATUS_REACH) return RetCodes.retResult(RetCodes.RET_MISSION_STATUS_ILLEGAL);
        long now = System.currentTimeMillis();
        long userliveness = 0;
        if (record.getGoodsitems() != null) {
            RetResult rs = goodsService.receiveGoodsItems(userid, GoodsInfo.GOODS_TYPE_PACKETS, 1, now, "mission", "领取任务; recordid=" + recordid, record.getGoodsitems());
            if (!rs.isSuccess()) return rs;
            userliveness = Long.parseLong(rs.getAttach("userliveness", "0"));
        }
        source.insert(record.createRecordHis(now));
        source.delete(record);
        if (userliveness > 0) livenessService.directUpdateUserLiveness(userid, userliveness, now);
        RetResult rs = new RetResult(record);
        if (userliveness > 0) rs.attach("newuserliveness", userliveness);
        return rs;
    }

    //领取指定活跃任务
    public RetResult<MissionDayRecord> reachMissionDayRecord(int userid, String recordid) {
        MissionDayRecord record = source.find(MissionDayRecord.class, recordid);
        if (record == null) return RetCodes.retResult(RetCodes.RET_MISSION_NOT_EXISTS);
        if (record.getMissionstatus() != MissionRecord.MISSION_STATUS_REACH) return RetCodes.retResult(RetCodes.RET_MISSION_STATUS_ILLEGAL);
        long now = System.currentTimeMillis();
        long userliveness = 0;
        if (record.getGoodsitems() != null) {
            RetResult rs = goodsService.receiveGoodsItems(userid, GoodsInfo.GOODS_TYPE_PACKETS, 1, now, "mission", "领取任务; recordid=" + recordid, record.getGoodsitems());
            if (!rs.isSuccess()) return rs;
            userliveness = Long.parseLong(rs.getAttach("userliveness", "0"));
        }
        record.setMissionstatus(MissionRecord.MISSION_STATUS_FINISH);
        record.setFinishtime(now);
        source.updateColumn(record, "missionstatus", "finishtime");
        if (userliveness > 0) livenessService.directUpdateUserLiveness(userid, userliveness, now);
        RetResult rs = new RetResult(record);
        if (userliveness > 0) rs.attach("newuserliveness", userliveness);
        return rs;
    }

    //给指定用户批量新增活跃任务
    public RetResult<Integer> insertMissionDayRecord(int userid, long createtime) {
        if (dayMissions.isEmpty()) return RetResult.success(0);
        FilterNode node = FilterNode.create("userid", userid).and("intday", Utility.yyyyMMdd(createtime));
        if (source.getNumberResult(MissionDayRecord.class, FilterFunc.COUNT, 0, null, node).intValue() > 0) return RetResult.success(0);
        List<MissionDayRecord> dayRecords = new ArrayList<>();
        for (MissionInfo info : dayMissions) {
            dayRecords.add(new MissionDayRecord(info, userid, createtime));
        }
        if (!dayRecords.isEmpty()) source.insert(dayRecords);
        return RetResult.success(dayRecords.size());
    }

    //给指定用户批量新增新手任务
    public RetResult<Integer> insertMissionOnceRecord(int userid, long createtime) {
        List<MissionOnceRecord> onceRecords = new ArrayList<>();
        for (MissionInfo info : onceMissions) {
            onceRecords.add(new MissionOnceRecord(info, userid, createtime));
        }
        if (!onceRecords.isEmpty()) source.insert(onceRecords);
        return RetResult.success(onceRecords.size());
    }

    //查询指定用户的新手任务列表
    public List<MissionOnceRecord> queryMissionOnceRecord(int userid) {
        return source.queryList(MissionOnceRecord.class, MYRECORD_FLIPPER, FilterNode.create("userid", userid));
    }

    //查询指定用户的活跃任务列表
    public List<MissionDayRecord> queryMissionDayRecord(int userid, int intday) {
        return source.queryList(MissionDayRecord.class, MYRECORD_FLIPPER, FilterNode.create("userid", userid).and("intday", intday));
    }

}
