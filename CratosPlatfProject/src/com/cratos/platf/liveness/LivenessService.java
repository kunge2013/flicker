/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.liveness;

import com.cratos.platf.base.*;
import static com.cratos.platf.liveness.LivenessRewardDayRecord.*;
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
import static org.redkale.source.FilterExpress.GREATERTHANOREQUALTO;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class LivenessService extends BaseService {

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    protected static final Flipper MYRECORD_FLIPPER = new Flipper(1000, "rewardindex ASC");

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected GoodsService goodsService;

    @Comment("定时活跃度奖励")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //VIP的选项
    protected List<LivenessRewardInfo> rewardInfos;

    @Transient //VIP的选项
    protected int currDay = Utility.today();

    @Transient //已完成活跃度奖励类型的活跃度奖励组合 userid+intday
    protected Set<String> currDayReachedLivenessRewards = new CopyOnWriteArraySet<>();

    @Transient //LivenessRewardUpdateEntry 队列
    protected final QueueTask<LivenessRewardUpdateEntry> updateQueue = new QueueTask<>(3);

    @Override
    public void init(AnyValue conf) {
        reloadConfig();
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        updateQueue.init(logger, (queue, entry) -> {
            runLivenessRewardUpdateEntry(entry);
        });
        final long seconds = 1 * 60 * 1000L;
        final long delay = seconds - System.currentTimeMillis() % seconds; //每分钟执行
        scheduler.scheduleAtFixedRate(() -> {
            try {
                reloadConfig();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, LivenessService.class.getSimpleName() + " scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);
        final long midDelay = 24 * 60 * 60 * 1000 + Utility.midnight() - System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(() -> { //凌晨前1.5分钟建表LivenessRewardDayRecord
            try {
                long time = 24 * 60 * 60 * 1000 + Utility.midnight() + 10;
                Flipper flipper = new Flipper(500);
                FilterNode node = FilterNode.create("userid", GREATERTHANOREQUALTO, UserInfo.MIN_NORMAL_USERID);
                Sheet<Integer> sheet = source.queryColumnSheet("userid", UserDetail.class, flipper, node);
                while (!sheet.isEmpty()) {
                    for (Integer userid : sheet) {
                        insertLivenessRewardRecord(userid, time);
                    }
                    sheet = source.queryColumnSheet("userid", UserDetail.class, flipper.next(), node);
                }
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, midDelay - 70000 > 0 ? midDelay - 70000 : 0, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
        updateQueue.destroy();
    }

    //判断活跃度操作队列
    public void updateUserLiveness(int userid, long userliveness, long time) {
        updateQueue.add(new LivenessRewardUpdateEntry(userid, userliveness, time));
    }

    //判断活跃度操作队列
    public void directUpdateUserLiveness(int userid, long userliveness, long time) {
        runLivenessRewardUpdateEntry(new LivenessRewardUpdateEntry(userid, userliveness, time));
    }

    protected void runLivenessRewardUpdateEntry(LivenessRewardUpdateEntry entry) {
        if (entry.userliveness < 1) {
            logger.log(Level.SEVERE, "runLivenessRewardUpdateEntry " + entry + " error, userliveness less 1");
            return;
        }
        Class clazz = LivenessRewardDayRecord.class;
        final int intday = Utility.yyyyMMdd(entry.reachtime);
        final String mkey = entry.userid + "-" + intday;
        if (intday != this.currDay) {
            synchronized (this) {
                final int today = Utility.today();
                if (today != this.currDay) {
                    this.currDay = today;
                    this.currDayReachedLivenessRewards = new CopyOnWriteArraySet<>();
                }
            }
        } else {
            if (currDayReachedLivenessRewards.contains(mkey)) return;
        }
        FilterNode node = FilterNode.create("userid", entry.userid)
            .and("intday", intday).and("livenessstatus", LIVENESS_STATUS_DOING);
        List<LivenessRewardDayRecord> records = source.queryList(clazz, node);
        if (records == null || records.isEmpty()) {
            currDayReachedLivenessRewards.add(mkey);
            return;
        }
        long now = System.currentTimeMillis();
        boolean allreached = true;
        for (LivenessRewardDayRecord item : records) {
            if (entry.userliveness >= item.getReachliveness()) {
                item.setLivenessstatus(LIVENESS_STATUS_REACH);
                item.setReachtime(now);
                source.updateColumn(item, "livenessstatus", "reachtime");
            } else {
                allreached = false;
            }
        }
        if (allreached) currDayReachedLivenessRewards.add(mkey);
    }

    protected void reloadConfig() {
        this.rewardInfos = source.queryList(LivenessRewardInfo.class, MYRECORD_FLIPPER, (FilterNode) null);
    }

    //获取可领取活跃度奖励的数量
    public int checkLivenessRewardRecord(int userid, int intday) {
        FilterNode node = FilterNode.create("userid", userid).and("intday", intday).and("livenessstatus", LivenessRewardDayRecord.LIVENESS_STATUS_REACH);
        return source.getNumberResult(LivenessRewardDayRecord.class, FilterFunc.COUNT, 0, null, node).intValue();
    }

    //领取指定活跃度奖励
    public RetResult<LivenessRewardDayRecord> reachLivenessRewardRecord(int userid, String recordid) {
        LivenessRewardDayRecord record = source.find(LivenessRewardDayRecord.class, recordid);
        if (record == null) return RetCodes.retResult(RetCodes.RET_LIVENESSREWARD_NOT_EXISTS);
        if (record.getLivenessstatus() != LIVENESS_STATUS_REACH) return RetCodes.retResult(RetCodes.RET_LIVENESSREWARD_ILLEGAL);
        long now = System.currentTimeMillis();
        GoodsItem[] items = record.getGoodsitems();
        if (items == null) {
            items = new GoodsItem[]{new GoodsItem(GoodsInfo.GOODS_TYPE_COIN, 10000)};
            record.setGoodsitems(items);
        }
        RetResult rs = goodsService.receiveGoodsItems(userid, GoodsInfo.GOODS_TYPE_PACKETS, 1, now, "livenessreward", "领取活跃度奖励; recordid=" + recordid, items);
        if (!rs.isSuccess()) return rs;
        record.setLivenessstatus(LIVENESS_STATUS_FINISH);
        record.setFinishtime(now);
        source.updateColumn(record, "goodsitems", "livenessstatus", "finishtime");
        return new RetResult(record);
    }

    //给指定用户批量新增活跃度奖励
    public RetResult<Integer> insertLivenessRewardRecord(int userid, long createtime) {
        List<LivenessRewardDayRecord> records = new ArrayList<>();
        for (LivenessRewardInfo info : rewardInfos) {
            records.add(new LivenessRewardDayRecord(info, userid, createtime));
        }
        source.insert(records);
        return RetResult.success(records.size());
    }

    //查询指定用户的活跃度奖励列表
    public List<LivenessRewardDayRecord> queryLivenessRewardRecord(int userid, int intday) {
        return source.queryList(LivenessRewardDayRecord.class, MYRECORD_FLIPPER, FilterNode.create("userid", userid).and("intday", intday));
    }

    protected static class LivenessRewardUpdateEntry extends BaseBean {

        public int userid;

        public long userliveness;

        public long reachtime;

        public LivenessRewardUpdateEntry(int userid, long userliveness, long reachtime) {
            this.userid = userid;
            this.userliveness = userliveness;
            this.reachtime = reachtime;
        }

    }
}
