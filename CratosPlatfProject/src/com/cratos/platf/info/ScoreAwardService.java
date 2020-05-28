/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.*;
import com.cratos.platf.order.GoodsInfo;
import com.cratos.platf.user.*;
import com.cratos.platf.util.ShuffleRandom;
import com.cratos.platf.util.Utils;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.persistence.Transient;
import org.redkale.net.http.WebSocketNode;
import org.redkale.service.RetResult;
import org.redkale.source.DataSource;
import org.redkale.source.FilterNode;
import org.redkale.source.Flipper;
import org.redkale.util.AnyValue;
import org.redkale.util.Comment;
import org.redkale.util.Utility;

/**
 * 幸运摇奖服务
 *
 * @author zhangjx
 */
public class ScoreAwardService extends BaseService {

    @Resource(name = "wsgame")
    protected WebSocketNode webSocketNode;

    //[userid] 在摇奖界面的用户ID
    protected final CopyOnWriteArrayList<Integer> awardingPlayers = new CopyOnWriteArrayList();

    @Resource(name = "platf")
    protected DataSource source;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Resource
    protected UserService userService;

    @Resource
    protected DictService dictService;

    protected final SecureRandom gameRandom = ShuffleRandom.createRandom();

    @Transient //白银盘中奖的权重
    protected int[] weights1 = new int[100];

    @Transient //黄金盘中奖的权重
    protected int[] weights2 = new int[100];

    @Transient //钻石盘中奖的权重
    protected int[] weights3 = new int[100];

    @Transient //白银盘最低积分要求
    protected int awardScoreLevel1 = 0;

    @Transient //黄金盘最低积分要求
    protected int awardScoreLevel2 = 0;

    @Transient //钻石盘最低积分要求
    protected int awardScoreLevel3 = 0;

    @Transient //白银盘配置项
    protected List<ScoreAwardInfo> awardInfoList1 = new ArrayList<>();

    @Transient //黄金盘配置项
    protected List<ScoreAwardInfo> awardInfoList2 = new ArrayList<>();

    @Transient //钻石盘配置项
    protected List<ScoreAwardInfo> awardInfoList3 = new ArrayList<>();

    @Override
    public void init(AnyValue conf) {
        reloadConfig();
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "AwardService-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        final long seconds = 1 * 60 * 1000L;
        final long delay = seconds - System.currentTimeMillis() % seconds; //每分钟执行
        scheduler.scheduleAtFixedRate(() -> {
            try {
                reloadConfig();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                randomCreateAwardRecord();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void reloadConfig() {
        //幸运摇奖选项配置
        List<ScoreAwardInfo> list = source.queryList(ScoreAwardInfo.class, new Flipper(100, "awardid ASC"), (FilterNode) null);
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
        List<Integer> list3 = new ArrayList<>();

        List<ScoreAwardInfo> awardList1 = new ArrayList<>();
        List<ScoreAwardInfo> awardList2 = new ArrayList<>();
        List<ScoreAwardInfo> awardList3 = new ArrayList<>();
        if (list != null && list.size() > 0) {
            for (ScoreAwardInfo awardInfo : list) {
                if (awardInfo.getAwardlevel() == ScoreAwardInfo.AWARD_LEVEL_1) {
                    list1.add(awardInfo.getWeight());
                    awardList1.add(awardInfo);
                } else if (awardInfo.getAwardlevel() == ScoreAwardInfo.AWARD_LEVEL_2) {
                    list2.add(awardInfo.getWeight());
                    awardList2.add(awardInfo);
                } else if (awardInfo.getAwardlevel() == ScoreAwardInfo.AWARD_LEVEL_3) {
                    list3.add(awardInfo.getWeight());
                    awardList3.add(awardInfo);
                }
            }
        }
        awardInfoList1 = awardList1;
        awardInfoList2 = awardList2;
        awardInfoList3 = awardList3;
        //权重
        this.weights1 = Utils.calcIndexWeights(list1.stream().mapToInt(Integer::valueOf).toArray());
        this.weights2 = Utils.calcIndexWeights(list2.stream().mapToInt(Integer::valueOf).toArray());
        this.weights3 = Utils.calcIndexWeights(list3.stream().mapToInt(Integer::valueOf).toArray());
        //最低积分要求
        awardScoreLevel1 = dictService.findDictValue(DictInfo.PLATF_AWARD_SCORE_LEVEL_1, 10);
        awardScoreLevel2 = dictService.findDictValue(DictInfo.PLATF_AWARD_SCORE_LEVEL_2, 100);
        awardScoreLevel3 = dictService.findDictValue(DictInfo.PLATF_AWARD_SCORE_LEVEL_3, 1000);
    }

    @Comment("进入摇奖界面")
    public void enterAwardPanel(int userid) {
        if (!awardingPlayers.contains(userid)) {
            awardingPlayers.add(userid);
        }
    }

    @Comment("获取自己当前积分")
    public Map<String, Long> getUserRecord(int userid, boolean isToday) {
        Map<String, Long> scoreMap = new HashMap();
        long remainscore = 0;//剩下可用积分数
        long totalscore = 0; //总积分数
        long todayscore = 0; //今日有效积分（明日可用）
        List<ScoreAwardRecord> awardRecordList = source.queryList(ScoreAwardRecord.class, new Flipper(1, "createtime DESC"),
            FilterNode.create("userid", userid).and("intday", Utility.today()));
        if (awardRecordList != null && awardRecordList.size() > 0) {
            ScoreAwardRecord awardRecord = awardRecordList.get(0);
            remainscore = awardRecord.getRemainscore();
            totalscore = awardRecord.getTotalscore();
        } else {
            UserDayRecord userDayRecord = source.find(UserDayRecord.class, FilterNode.create("userid", userid).and("intday", Utility.yesterday()));
            if (userDayRecord != null) {
                remainscore = (long) (userDayRecord.getCostcoins() / GoodsInfo.EXCHANGE_RMB_COIN);
                totalscore = remainscore;
            }
        }
        if (isToday) {
            UserDayRecord userDayRecord = source.find(UserDayRecord.class, FilterNode.create("userid", userid).and("intday", Utility.today()));
            if (userDayRecord != null) {
                todayscore = (long) (userDayRecord.getCostcoins() / GoodsInfo.EXCHANGE_RMB_COIN);
            }
        }
        scoreMap.put("todayscore", todayscore);
        scoreMap.put("remainscore", remainscore);
        scoreMap.put("totalscore", totalscore);
        return scoreMap;
    }

    @Comment("离开摇奖界面")
    public void leaveAwardPanel(int userid) {
        if (awardingPlayers.contains(userid)) {
            awardingPlayers.remove(Integer.valueOf(userid));
        }
    }

    @Comment("是否可以幸运摇奖")
    public RetResult<Integer> canAward(int userid) {
        RetResult<Integer> rs = new RetResult<>();
        int canAward = 0;
        Map<String, Long> scoreMap = getUserRecord(userid, false);
        if (scoreMap.get("remainscore") >= awardScoreLevel1) {
            canAward = 1;
        }
        return rs.result(canAward);
    }

    @Comment("当前用户摇奖记录")
    public List<ScoreAwardRecord> myRecords(int userid) {
        List<ScoreAwardRecord> recordList = source.queryList(ScoreAwardRecord.class, new Flipper(20, "createtime DESC"), FilterNode.create("userid", userid));
        if (recordList != null && recordList.size() > 0) {
            Player user = userService.findPlayer(recordList.get(0).getUserid());
            for (ScoreAwardRecord awardRecord : recordList) {
                awardRecord.setUser(user);
            }
        }
        return recordList;
    }

    @Comment("全员摇奖记录")
    public List<ScoreAwardRecord> allRecords() {
        List<ScoreAwardRecord> recordList = source.queryList(ScoreAwardRecord.class, new Flipper(25, "createtime DESC"), (FilterNode) null);
        if (recordList != null && recordList.size() > 0) {
            for (ScoreAwardRecord awardRecord : recordList) {
                awardRecord.setUser(userService.findPlayer(awardRecord.getUserid()));
            }
        }
        return recordList;
    }

    @Comment("摇奖")
    public synchronized RetResult runJiang(int userid, short awardlevel) {
        //参数校验
        if (awardlevel < 0 || awardlevel > 3) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (!awardingPlayers.contains(userid)) return RetCodes.retResult(RetCodes.RET_USER_UNLOGIN);
        Map<String, Long> scoreMap = getUserRecord(userid, false);
        Long remainscore = scoreMap.get("remainscore");
        Long totalscore = scoreMap.get("totalscore");
        Long userAwardScore = 0L;//剩下可用积分
        //计算权重
        int awardIndex = 0;
        if (awardlevel == ScoreAwardInfo.AWARD_LEVEL_1) {
            if (remainscore < this.awardScoreLevel1) return RetCodes.retResult(RetCodes.RET_USER_AUTH_ILLEGAL);
            awardIndex = ShuffleRandom.random(gameRandom, this.weights1);
            userAwardScore = remainscore - this.awardScoreLevel1;
        } else if (awardlevel == ScoreAwardInfo.AWARD_LEVEL_2) {
            if (remainscore < this.awardScoreLevel2) return RetCodes.retResult(RetCodes.RET_USER_AUTH_ILLEGAL);
            awardIndex = ShuffleRandom.random(gameRandom, this.weights2);
            userAwardScore = remainscore - this.awardScoreLevel2;
        } else if (awardlevel == ScoreAwardInfo.AWARD_LEVEL_3) {
            if (remainscore < this.awardScoreLevel3) return RetCodes.retResult(RetCodes.RET_USER_AUTH_ILLEGAL);
            awardIndex = ShuffleRandom.random(gameRandom, this.weights3);
            userAwardScore = remainscore - this.awardScoreLevel3;
        }
        int awardid = awardlevel * 100 + (awardIndex + 1);
        ScoreAwardInfo info = source.find(ScoreAwardInfo.class, awardid);
        if (info == null) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        //构造用户摇奖记录
        ScoreAwardRecord record = insertAwardRecord(userid, userAwardScore, totalscore, info);
        //中奖推送
        webSocketNode.sendMessage(Utility.ofMap("onPlayerAwardMessage", new RetResult(record)), awardingPlayers.stream());
        return new RetResult(record);
    }

    //插入摇奖记录
    public ScoreAwardRecord insertAwardRecord(int userid, Long userAwardScore, Long totalscore, ScoreAwardInfo awardInfo) {
        ScoreAwardRecord record = new ScoreAwardRecord();
        record.setUserid(userid);
        record.setIntday(Utility.today());
        record.setCreatetime(System.currentTimeMillis());
        record.setAwardrecordid(record.getFromuser36id() + "-" + Utility.format36time(record.getCreatetime()));
        record.setAwardid(awardInfo.getAwardid());
        record.setAwardtype(awardInfo.getAwardtype());
        record.setAwardval(awardInfo.getAwardval());
        record.setAwardlevel(awardInfo.getAwardlevel());
        record.setRemainscore(userAwardScore); //剩下可用积分
        record.setTotalscore(totalscore);      //总积分
        source.insert(record);
        if (!UserInfo.isRobot(userid) && ScoreAwardInfo.AWARD_TYPE_COIN.equals(awardInfo.getAwardtype())) {
            userService.increPlatfUserCoins(userid, record.getAwardval(), System.currentTimeMillis(), "award", "awardrecordid=" + record.getAwardrecordid());
        }
        record.setUser(userService.findPlayer(userid));
        return record;
    }

    //随机构造摇奖信息
    private void randomCreateAwardRecord() {
        List<ScoreAwardRecord> list = allRecords();
        if (list != null && list.size() > 0) {
            ScoreAwardRecord awardRecord = list.get(0);
            long currTime = System.currentTimeMillis();
            long dbTime = awardRecord.getCreatetime();
            if ((currTime - dbTime) / 1000 > 60) {
                //计算权重
                int awardlevel = gameRandom.nextInt(3) + 1;
                int awardIndex = 0;
                if (awardlevel == ScoreAwardInfo.AWARD_LEVEL_1) {
                    awardIndex = ShuffleRandom.random(gameRandom, this.weights1);
                } else if (awardlevel == ScoreAwardInfo.AWARD_LEVEL_2) {
                    awardIndex = ShuffleRandom.random(gameRandom, this.weights2);
                } else if (awardlevel == ScoreAwardInfo.AWARD_LEVEL_3) {
                    awardIndex = ShuffleRandom.random(gameRandom, this.weights3);
                }
                int awardid = awardlevel * 100 + (awardIndex + 1);
                ScoreAwardInfo awardInfo = source.find(ScoreAwardInfo.class, awardid);
                UserInfo userInfo = userService.randomRobot();
                insertAwardRecord(userInfo.getUserid(), 0L, 0L, awardInfo);
                //中奖推送
                webSocketNode.sendMessage(Utility.ofMap("onPlayerAwardMessage", new RetResult(awardRecord)), awardingPlayers.stream());
            }
        }
    }
}
