/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.duty.DutyRecord;
import com.cratos.platf.base.*;
import com.cratos.platf.info.*;
import com.cratos.platf.mission.*;
import static com.cratos.platf.mission.MissionInfo.*;
import static com.cratos.platf.mission.MissionRecord.*;
import com.cratos.platf.mission.MissionRecord.MissionUpdateEntry;
import com.cratos.platf.notice.Announcement;
import com.cratos.platf.order.*;
import com.cratos.platf.user.*;
import com.cratos.platf.util.*;
import static com.cratos.platf.util.Utils.HEADNAME_WS_SNCP_ADDRESS;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.Stream;
import javax.annotation.Resource;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.source.*;
import org.redkale.util.*;
import static org.redkale.util.Utility.ofMap;

/**
 *
 * @author zhangjx
 * @param <Table>  GameTable
 * @param <P>      GamePlayer
 * @param <GTBean> GameTableBean
 */
@AutoLoad(false)
public abstract class GameService<Table extends GameTable, P extends GamePlayer, GTBean extends GameTableBean> extends BaseService {

    protected boolean testrun;

    //是否给机器人发送消息
    public static final boolean ROBOT_WSSEND = System.getProperty("os.name").contains("Window");

    protected static final String format = "%1$ty%1$tm%1$td%1$tH%1$tM%1$tS";

    public static final int ANNOUNCE_TYPE_COINS = 1; //

    public static final int ANNOUNCE_TYPE_FACTOR = 2; //

    protected final SecureRandom gameRandom = ShuffleRandom.createRandom();

    @Comment("平台进程是否正在运行")
    private final AtomicBoolean platfRunning = new AtomicBoolean(true);

    @Comment("平台等待标识")
    private final AtomicInteger platfAwaitRetry = new AtomicInteger();

    @Resource(name = "system.property.nodeid")
    protected int nodeid = 10;

    @Resource(name = "wsgame")
    protected WebSocketNode webSocketNode;

    @Resource(name = "platf")
    private DataSource gplatfSource;

    @Resource
    @Comment("全服账号服务")
    private UserService userService;

    @Resource
    @Comment("商品服务")
    protected GoodsService goodsService;

    @Resource
    @Comment("JSON序列化")
    protected JsonConvert jsonConvert;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Resource
    @Comment("全服参数配置服务")
    protected DictService dictService;

    private Creator<? extends GameAccount> accountCreator;

    //key:用户ID, value:GameAccount
    protected final ConcurrentHashMap<Integer, GameAccount> playerAccounts = new ConcurrentHashMap();

    //key:用户ID， 供CoinGameService访问
    final ConcurrentHashMap<Integer, P> livingPlayers = new ConcurrentHashMap();

    private static final WebSocketRange announceWebSocketRange = new WebSocketRange("userid", ofMap("minuserid", "" + UserInfo.MIN_NORMAL_USERID, "module", "announce"));

    protected final QueueTask<Announcement[]> announceQueue = new QueueTask<>(1);

    protected final QueueTask<GameData> dataQueue = new QueueTask<>(1);

    protected final QueueTask<Integer> userEnterQueue = new QueueTask<>(1);

    protected final QueueTask<int[]> userLeaveQueue = new QueueTask<>(1);

    protected final QueueTask<BaseEntity> insertQueue = new QueueTask<>(1);

    protected final QueueTask<Runnable> updateQueue = new QueueTask<>(1);

    @Transient //WS消息缓存队列
    protected final QueueTask<Runnable> wsmessageQueue = new QueueTask<>(1);

    //------------------------ 公告字段 ----------------------------------
    protected final AtomicLong announceLastTime = new AtomicLong();

    //------------------------ 通用字段 ----------------------------------
    //防沉迷.游客试玩时长
    @Transient
    protected int confIndulgeGuestTryplaySeconds = 0;

    //防沉迷.未成年人每日游戏限时长秒数，0表示无限制
    @Transient
    protected int confIndulgeUn18AgeDayplaySeconds = 0;

    //防沉迷.定时器
    @Transient
    protected ScheduledThreadPoolExecutor indulgeScheduler;

    //防沉迷.是否开启防沉迷限制; 10:开启;20:关闭;
    @Transient
    protected boolean confIndulgeActivate = true;

    @Transient //是否开启机器人自动加入, 10:开启; 20:禁止
    protected boolean confRobotActivate = true;

    @Transient //MissionUpdateEntry 队列
    private final QueueTask<GameMissionUpdateEntry> missionQueue = new QueueTask<>(1);

    @Override
    public void init(AnyValue config) {
        nodeid = nodeid % 100;

        insertQueue.init(logger, (queue, entity) -> {
            if (entity instanceof SourceAndEntity) {
                SourceAndEntity es = (SourceAndEntity) entity;
                es.getSource().insert(es.getEntity());
            } else {
                dataSource().insert(entity);
            }
        });
        updateQueue.init(logger, (queue, runner) -> runner.run());

        missionQueue.init(logger, (queue, entry) -> {
            runMissionUpdateEntry(entry);
        });

        BiConsumer<BlockingQueue<GameData>, GameData> consumer = (queue, data) -> {
            int c = updateDataValue(data.keyname, data.getNumvalue());
            if (c < 1 && !existsDataValue(data.keyname)) {
                GameData rdata = null;
                try {
                    rdata = createGameData(data.keyname, data.getNumvalue(), "", "");
                    rdata.setUpdatetime(System.currentTimeMillis());
                    insertGameData(rdata);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "insert GameData(" + rdata + ") error", e);
                }
            }
        };
        dataQueue.init(logger, consumer);

        BiConsumer<BlockingQueue<Announcement[]>, Announcement[]> annConsumer = (queue, announces) -> {
            if (announces.length < 1) return;
            long time = announces[0].getCreatetime() + 10 * 1000 - System.currentTimeMillis();
            if (time > 0) await(time);
            webSocketNode.broadcastMessage(announceWebSocketRange, ofMap("onHrollAnnounceMessage", announces));
        };
        announceQueue.init(logger, annConsumer);

        wsmessageQueue.init(logger, (queue, task) -> task.run());
        //
        userEnterQueue.init(logger, (queue, uid) -> {
            try {
                checkPlatfRunning("enterGame");
                userService.enterGame(uid, gameId(), System.currentTimeMillis());
            } catch (Exception e) {
                logger.log(Level.WARNING, "User(userid=" + uid + ") enterGame error", e);
            }
        });
        //
        userLeaveQueue.init(logger, (queue, uid) -> {
            try {
                checkPlatfRunning("leaveGame");
                userService.leaveGame(gameId(), uid);
            } catch (Exception e) {
                logger.log(Level.WARNING, "User(userid=" + uid + ") leaveGame error", e);
            }
        });
    }

    @Override
    public void destroy(AnyValue config) {
        if (scheduler != null) scheduler.shutdownNow();
        restoreData();
        insertQueue.destroy();
        updateQueue.destroy();
        missionQueue.destroy();
        restoreAccount();
        wsmessageQueue.destroy();
        announceQueue.destroy();
        userEnterQueue.destroy();
        userLeaveQueue.destroy();
    }

    public RetResult receiveGoodsItems(int userid, int goodscount, long time, String module, String remark, GoodsItem[] items) {
        return goodsService.receiveGoodsItems(userid, GoodsInfo.GOODS_TYPE_PACKETS, goodscount, time, module, remark, items);
    }

    protected RetResult scheduleGameAction(GameActionEvent event, Runnable runner) {
        long delaymillis = event.getDelaymillis();
        if (delaymillis < 0) delaymillis = Long.MAX_VALUE - 1; //永不超时
        event.future = this.scheduler.schedule(runner, delaymillis, TimeUnit.MILLISECONDS);
        return RetResult.success();
    }

    public void cancelGameAction(GameActionEvent actevent) {
        if (actevent == null) return;
        ScheduledFuture future = actevent.future;
        if (future != null && !future.isCancelled()) future.cancel(false);
    }

    @Local
    public <T extends GameAccount> T loadAccount(int userid) {
        GameAccount account = playerAccounts.get(userid);
        if (account != null) {
            account.setLeaving(false);
            return (T) account;
        }
        if (this.accountCreator == null) this.accountCreator = Creator.create(accountClass());
        if (userid == UserInfo.USERID_SYSTEM || UserInfo.isRobot(userid)) { //机器人则自动创建一个
            account = this.accountCreator.create();
            account.setUserid(userid);
            account.setCreatetime(System.currentTimeMillis());
            playerAccounts.put(userid, account);
            account.postLoad();
            return (T) account;
        }
        synchronized (playerAccounts) {
            account = playerAccounts.get(userid);
            if (account == null && !testrun) {
                account = dataSource().find(accountClass(), userid);
                if (account == null) {
                    account = accountCreator.create();
                    account.setUserid(userid);
                    account.setCreatetime(System.currentTimeMillis());
                    dataSource().insert(account);
                }
                account.postLoad();
                playerAccounts.put(userid, account);
                updateAccountDoingMissions(account);
            }
            return (T) account;
        }
    }

    //用户开火任务操作
    public void updateGameMissionShotCount(int userid, int count, long time) {
        GameAccount account = playerAccounts.get(userid);
        if (account == null) return;
        missionQueue.add(new GameMissionUpdateEntry(account, userid, true, MISSION_TYPE_GAME_SHOTCOUNT, count, time));
    }

    //用户杀敌任务操作
    public void updateGameMissionKillEnemy(int userid, int count, long time) {
        GameAccount account = playerAccounts.get(userid);
        if (account == null) return;
        missionQueue.add(new GameMissionUpdateEntry(account, userid, true, MISSION_TYPE_GAME_KILLENEMY, count, time));
    }

    //用户杀BOSS任务操作
    public void updateGameMissionKillBoss(int userid, int count, long time) {
        GameAccount account = playerAccounts.get(userid);
        if (account == null) return;
        missionQueue.add(new GameMissionUpdateEntry(account, userid, true, MISSION_TYPE_GAME_KILLBOSS, count, time));
    }

    //用户杀BOSS任务操作
    public void updateGameMissionUseProp(int userid, int objid, int count, long time) {
        GameAccount account = playerAccounts.get(userid);
        if (account == null) return;
        missionQueue.add(new GameMissionUpdateEntry(account, userid, true, MISSION_TYPE_GAME_USEPROP, objid, count, time));
    }

    //用户火力任务操作
    public void updateGameMissionFirelevel(int userid, int firelevel, long time) {
        GameAccount account = playerAccounts.get(userid);
        if (account == null) return;
        missionQueue.add(new GameMissionUpdateEntry(account, userid, false, MISSION_TYPE_GAME_FIRELEVEL, firelevel, time));
    }

    private void runMissionUpdateEntry(GameMissionUpdateEntry entry) {
        if (entry.reachcount < 1) return;
        GameAccount account = entry.account;
        List<MissionRecord> missions = account.doingMissions;
        if (missions == null) return;
        long now = System.currentTimeMillis();
        final int intday = Utility.yyyyMMdd(entry.reachtime);
        if (intday != Utility.today()) updateAccountDoingMissions(account);
        missions = account.doingMissions;
        if (missions == null) return;
        List<MissionRecord> removes = new ArrayList<>();
        for (MissionRecord item : missions) {
            if (item.getMissiontype() != entry.missiontype) continue;
            if (item.getReachobjid() != 0 && item.getReachobjid() != entry.reachobjid) continue;
            if (entry.incrable) {
                item.increCurrcount(entry.reachcount);
            } else {
                item.setCurrcount(entry.reachcount);
            }
            if (item.isReached()) {
                item.setCurrcount(item.getReachcount());
                item.setMissionstatus(MISSION_STATUS_REACH);
                item.setReachtime(now);
                gplatfSource.updateColumn(item, "currcount", "missionstatus", "reachtime");
                removes.add(item);
            } else {
                gplatfSource.updateColumn(item, "currcount");
            }
        }
        for (MissionRecord item : removes) {
            missions.remove(item);
        }
        if (missions.isEmpty()) account.doingMissions = null;
    }

    private <T extends GameAccount> void updateAccountDoingMissions(T account) {
        List<MissionRecord> doingMissions = new ArrayList();
        List<MissionOnceRecord> onceRecords = gplatfSource.queryList(MissionOnceRecord.class, FilterNode.create("userid", account.getUserid()).and("gameid", gameId()).and("missionstatus", MissionRecord.MISSION_STATUS_DOING));
        if (onceRecords != null) doingMissions.addAll(onceRecords);
        List<MissionDayRecord> dayRecords = gplatfSource.queryList(MissionDayRecord.class, FilterNode.create("userid", account.getUserid()).and("intday", Utility.today()).and("gameid", gameId()).and("missionstatus", MissionRecord.MISSION_STATUS_DOING));
        if (dayRecords != null) doingMissions.addAll(dayRecords);
        account.doingMissions = doingMissions.isEmpty() ? null : doingMissions;
    }

    protected void updateAccountOnLine(int userid) {
        GameAccount account = loadAccount(userid);
        if (account != null) {
            account.setOnlinetodaystarttime(System.currentTimeMillis());
            int today = Utility.today();
            if (account.getOnlinetodaytime() != today) {
                account.setOnlinetodaytime(today);
                account.setOnlinetodayseconds(0L);
            }
        }
    }

    protected void leaveAccount(int userid) {
        GameAccount account = loadAccount(userid);
        if (account != null) {
            if (account.getOnlinetodaystarttime() > 0) {
                int today = Utility.today();
                if (account.getOnlinetodaytime() != today) {
                    account.setOnlinetodaytime(today);
                    account.setOnlinetodayseconds((System.currentTimeMillis() - Utility.midnight()) / 1000);
                } else {
                    account.setOnlinetodayseconds((System.currentTimeMillis() - account.getOnlinetodaystarttime()) / 1000);
                }
                account.setOnlineseconds(account.getOnlineseconds() + (System.currentTimeMillis() - account.getOnlinetodaystarttime()) / 1000);
                account.setOnlinetodaystarttime(0);
            }
            account.setLeaving(true);
        }
    }

    private void restoreAccount() {
        List<Integer> needLeaves = new ArrayList<>();
        for (GameAccount account : playerAccounts.values()) {
            account.preSave();
            if (!UserInfo.isRobot(account.getUserid())) dataSource().update(account);
            if (account.isLeaving() || account.getLastgametime() + 10 * 60 * 1000 < System.currentTimeMillis()) { //超过10分钟移除
                needLeaves.add(account.getUserid());
            }
        }
        synchronized (playerAccounts) {
            for (int userid : needLeaves) {
                playerAccounts.remove(userid);
            }
        }
    }

    protected List<UserInfo> randomRobot(String currgame, int size) {
        checkPlatfRunning("randomRobot(String currgame, int size)");
        try {
            return userService.randomRobot(currgame, size);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    protected List<UserInfo> randomRobot(String currgame, Range.LongRange range, int size) {
        checkPlatfRunning("randomRobot(String currgame, Range.LongRange range, int size)");
        try {
            return userService.randomRobot(currgame, range, size);
        } catch (Exception e) {
            return null;
        }
    }

    protected void putLivingPlayer(int userid, P player) {
        if (player.getUserid() != userid) throw new RuntimeException("put userid=" + userid + " to player=" + player);
        livingPlayers.put(userid, player);
    }

    protected P removeLivingPlayer(int userid) {
        return livingPlayers.remove((Integer) userid);
    }

    protected P findLivingPlayer(int userid) {
        return livingPlayers.get((Integer) userid);
    }

    protected Stream<P> streamLivingPlayer() {
        return livingPlayers.values().stream();
    }

    protected int sizeLivingPlayer() {
        return livingPlayers.size();
    }

    protected void loadData() {
    }

    protected void restoreData() {
    }

    protected void reloadConfig(GameConfigFunc func) {
    }

    protected void reloadSuperConfig(GameConfigFunc func) {
        if (func == null) return;
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        String name = "GAME_" + gamekey + "_ROBOT_ACTIVATE";
        this.confRobotActivate = !func.contains(name) || func.getInt(name, 10) == 10; //是否开启机器人自动加入

        //防沉迷       
        this.confIndulgeActivate = dictService.findDictValue(DictInfo.PLATF_INDULGE_ACTIVATE, 0) == 10;
        int oldIndulgeGuestTryplaySeconds = this.confIndulgeGuestTryplaySeconds;
        int oldIndulgeUn18AgeDayplaySeconds = this.confIndulgeUn18AgeDayplaySeconds;
        this.confIndulgeGuestTryplaySeconds = dictService.findDictValue(DictInfo.PLATF_INDULGE_GUEST_TRYPLAY_SECONDS, 0);
        this.confIndulgeUn18AgeDayplaySeconds = dictService.findDictValue(DictInfo.PLATF_INDULGE_UN18AGE_DAYPLAY_SECONDS, 0);
        if ((oldIndulgeGuestTryplaySeconds == 0 && this.confIndulgeGuestTryplaySeconds > 0)
            || (oldIndulgeGuestTryplaySeconds > 0 && this.confIndulgeGuestTryplaySeconds == 0)
            || (oldIndulgeUn18AgeDayplaySeconds == 0 && this.confIndulgeUn18AgeDayplaySeconds > 0)
            || (oldIndulgeUn18AgeDayplaySeconds > 0 && this.confIndulgeUn18AgeDayplaySeconds == 0)) {
            if (this.indulgeScheduler != null) {
                this.indulgeScheduler.shutdownNow();
                this.indulgeScheduler = null;
            }
            if (this.confIndulgeActivate && (this.confIndulgeGuestTryplaySeconds > 0 || this.confIndulgeUn18AgeDayplaySeconds > 0)) {
                this.indulgeScheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
                    final Thread t = new Thread(r, gameId() + "-indulgetask-Thread");
                    t.setDaemon(true);
                    return t;
                });
                this.indulgeScheduler.scheduleAtFixedRate(() -> {
                    try {
                        streamLivingPlayer().forEach(player -> {
                            int age = player.getAge();
                            if (age >= 18) return;
                            if (age == 0 && confIndulgeGuestTryplaySeconds > 0) {
                                GameAccount account = loadAccount(player.getUserid());
                                if (account.currOnlineSeconds() > confIndulgeGuestTryplaySeconds) {
                                    leaveGame(player.getUserid(), ofMap("retinfo", RetCodes.retInfo(RetCodes.RET_USER_GUEST_PLAYTIME_LIMIT)));
                                }
                            } else if (age < 18 && confIndulgeUn18AgeDayplaySeconds > 0) {
                                GameAccount account = loadAccount(player.getUserid());
                                if (account.currTodayOnlineSeconds() > confIndulgeUn18AgeDayplaySeconds) {
                                    leaveGame(player.getUserid(), ofMap("retinfo", RetCodes.retInfo(RetCodes.RET_USER_UN18AGE_PLAYTIME_LIMIT)));
                                }
                            }
                        });
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "scheduleAtFixedRate indulge scheduler error", e);
                    }
                }, 1, 30, TimeUnit.SECONDS);
            }
        }
    }

    protected void initGame(int schedulePoolSize) {
        this.scheduler = new ScheduledThreadPoolExecutor(schedulePoolSize + 2, (Runnable r) -> {
            final Thread t = new Thread(r, gameId() + "-task-Thread");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.setRemoveOnCancelPolicy(true);
        loadData();
        GameConfigFunc configFunc0 = GameConfigFunc.createFromMap(loadGameConfig());
        reloadSuperConfig(configFunc0);
        reloadConfig(configFunc0);
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                GameConfigFunc configFunc = GameConfigFunc.createFromMap(loadGameConfig());
                reloadSuperConfig(configFunc);
                reloadConfig(configFunc);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate reloadConfig error", e);
            }
            try {
                restoreAccount();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate restoreAccount error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);

        long deplay = 60 * 1000 - System.currentTimeMillis() % (60 * 1000);
        if (!winos) {
            this.scheduler.scheduleAtFixedRate(() -> {
                try {
                    final java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    long timekey = now.getYear() * 100_00_00_00L + now.getMonthValue() * 100_00_00
                        + now.getDayOfMonth() * 100_00 + now.getHour() * 100 + now.getMinute();
                    PlayingUserRecord record = new PlayingUserRecord();
                    record.setTimekey(timekey);
                    Map<Integer, Long> map = getPlayingRoomUserMap();
                    record.setPlayingcount1(map.getOrDefault(1, 0L));
                    record.setPlayingcount2(map.getOrDefault(2, 0L));
                    record.setPlayingcount3(map.getOrDefault(3, 0L));
                    record.setPlayingcount4(map.getOrDefault(4, 0L));
                    record.setPlayingcount5(map.getOrDefault(5, 0L));
                    if (map.size() == 1 && record.getPlayingcount1() == 0
                        && record.getPlayingcount2() == 0 && record.getPlayingcount3() == 0
                        && record.getPlayingcount4() == 0 && record.getPlayingcount5() == 0) { //无roomlevel
                        record.setPlayingcount1(map.getOrDefault(0, 0L));
                    }
                    record.setPlayingcounts(map.values().stream().mapToLong(x -> x).sum());
                    record.setCreatetime(System.currentTimeMillis() / 1000 * 1000);
                    dataSource().insert(record);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "scheduleAtFixedRate PlayingUserRecord error", e);
                }
            }, deplay, 60 * 1000, TimeUnit.MILLISECONDS);
        }
    }
//----------------------------------- GameConfig 操作 --------------------------------------------------------------------------

    @Comment("加载游戏配置")
    public Map<String, Number> loadGameConfig() {
        List<GameConfig> list = dataSource().queryList(GameConfig.class);
        Map<String, Number> rs = new HashMap<>();
        for (GameConfig c : list) {
            rs.put(c.getKeyname(), c.getNumvalue());
        }
        return rs;
    }
//----------------------------------- GameData 操作 --------------------------------------------------------------------------

    @Comment("获取long值")
    public long findDataValue(String key, long defaultValue) {
        GameData rs = dataSource().find(GameData.class, key);
        if (GameConfigFunc.throwed && rs == null) throw new RuntimeException("缺少GameData值: " + key);
        return rs == null ? defaultValue : rs.getNumvalue();
    }

    @Comment("获取int值")
    public int findDataValue(String key, int defaultValue) {
        GameData rs = dataSource().find(GameData.class, key);
        if (GameConfigFunc.throwed && rs == null) throw new RuntimeException("缺少GameData值: " + key);
        return rs == null ? defaultValue : (int) rs.getNumvalue();
    }

    @Comment("获取字符串值")
    public String findDataValue(String key, String defaultValue) {
        GameData rs = dataSource().find(GameData.class, key);
        if (GameConfigFunc.throwed && rs == null) throw new RuntimeException("缺少GameData值: " + key);
        return rs == null ? defaultValue : rs.getStrvalue();
    }

    @Comment("是否包含key")
    public boolean existsDataValue(String key) {
        return dataSource().exists(GameData.class, key);
    }

    @Comment("追加long值")
    public int increDataValue(String key, long value) {
        return dataSource().updateColumn(GameData.class, key, ColumnValue.inc("numvalue", value), ColumnValue.mov("updatetime", System.currentTimeMillis()));
    }

    @Comment("更新long值")
    public int updateDataValue(String key, long value) {
        return dataSource().updateColumn(GameData.class, key, ColumnValue.mov("numvalue", value), ColumnValue.mov("updatetime", System.currentTimeMillis()));
    }

    @Comment("更新字符串值")
    public int updateDataValue(String key, String value) {
        return dataSource().updateColumn(GameData.class, key, ColumnValue.mov("strvalue", value == null ? "" : value), ColumnValue.mov("updatetime", System.currentTimeMillis()));
    }

    public void insertGameData(GameData data) {
        dataSource().insert(data);
    }

    public GameData createGameData(String keyname, long numvalue) {
        return createGameData(keyname, numvalue, "", "");
    }

    public GameData createGameData(String keyname, long numvalue, String strvalue, String keydesc) {
        GameData data = new GameData();
        data.setKeyname(keyname);
        data.setNumvalue(numvalue);
        data.setStrvalue(strvalue);
        data.setKeydesc(keydesc);
        return data;
    }

    public GameData findGameData(String key) {
        return dataSource().find(GameData.class, key);
    }

    public int nodeid() {
        return nodeid;
    }
    //------------------------------------------------------------------------------------------------------------------

    protected void userEnterGame(int userid) {
        userEnterQueue.add(userid);
    }

    protected void userLeaveGame(int... userids) {
        userLeaveQueue.add(userids);
    }

    protected void afterLeaveGame(P player) {
    }

    protected long findUserCoins(int userid) {
        checkPlatfRunning("findUserCoins");
        return userService == null ? -1 : userService.findUserCoins(userid);
    }

    protected UserInfo findUserInfo(int userid) {
        checkPlatfRunning("findUserInfo");
        return userService == null ? null : userService.findUserInfo(userid);
    }

    protected InetSocketAddress getWebSocketSncpAddress(int userid) {
        Collection<InetSocketAddress> addresses = webSocketNode.getRpcNodeAddresses(userid).join();
        if (addresses == null) return null;
        for (InetSocketAddress addr : addresses) return addr;
        return null;
    }

    protected void unfreezeGameUserCoins(final int roomlevel, final long coin, final long time, final String remark) {
        if (coin < 0) throw new RuntimeException("unfreezeGameUserCoins coin must be greater 0");
        checkPlatfRunning("updateGameUserCoins");
        userService.updateGameUserCoins(UserInfo.USERID_SYSTEM, roomlevel, -coin, 0L, time, gameId(), "unfreeze", remark);
    }

    protected void unfreezeGameUserCoins(final int roomlevel, final long coin, final long time, final String module, final String remark) {
        if (coin < 0) throw new RuntimeException("unfreezeGameUserCoins coin must be greater 0");
        checkPlatfRunning("updateGameUserCoins");
        userService.updateGameUserCoins(UserInfo.USERID_SYSTEM, roomlevel, -coin, 0L, time, gameId(), module, remark);
    }

    protected void freezeupGameUserCoins(final int roomlevel, final long coin, final long time, final String remark) {
        if (coin < 0) throw new RuntimeException("freezeupGameUserCoins coin must be greater 0");
        checkPlatfRunning("updateGameUserCoins");
        userService.updateGameUserCoins(UserInfo.USERID_SYSTEM, roomlevel, coin, 0L, time, gameId(), "freezeup", remark);
    }

    protected void freezeupGameUserCoins(final int roomlevel, final long coin, final long time, final String module, final String remark) {
        if (coin < 0) throw new RuntimeException("freezeupGameUserCoins coin must be greater 0");
        checkPlatfRunning("updateGameUserCoins");
        userService.updateGameUserCoins(UserInfo.USERID_SYSTEM, roomlevel, coin, 0L, time, gameId(), module, remark);
    }

    protected void updateGameUserCoins(final GameAccount account, final int userid, final int roomlevel, final long coin, final long costcoin, final long time, final String module, final String remark) {
        checkPlatfRunning("updateGameUserCoins");
        userService.updateGameUserCoins(userid, roomlevel, coin, costcoin, time, gameId(), module, remark);
    }

    protected void updateGameUserDiamonds(final GameAccount account, final int userid, final int roomlevel, final long diamond, final long costdiamond, final long time, final String module, final String remark) {
        checkPlatfRunning("updateGameUserDiamonds");
        userService.updateGameUserCoupons(userid, roomlevel, diamond, costdiamond, time, gameId(), module, remark);
    }

    protected void updateGameUserCoupons(final GameAccount account, final int userid, final int roomlevel, final long coupon, final long costcoupon, final long time, final String module, final String remark) {
        checkPlatfRunning("updateGameUserCoupons");
        userService.updateGameUserCoupons(userid, roomlevel, coupon, costcoupon, time, gameId(), module, remark);
    }

    protected RetResult<Integer> costGameUserDiamonds(final int userid, final int diamond, final long time, String module, String remark) {
        checkPlatfRunning("costGameUserDiamonds");
        return userService.costGameUserDiamonds(userid, diamond, time, gameId(), module, remark);
    }

    protected void checkPlatfRunning(String action) {
        if (!platfRunning.get() && platfAwaitRetry.get() < 3) {
            for (int i = 0; i < 3; i++) {
                logger.log(Level.FINEST, gameId() + " " + action + " await 2s");
                await(2000);
                platfAwaitRetry.incrementAndGet();
                if (platfRunning.get()) {
                    platfAwaitRetry.set(0);
                    break;
                }
            }
        } else {
            platfAwaitRetry.set(0);
        }
    }

    @Local
    public Logger logger() {
        return this.logger;
    }

    @Local
    public CompletableFuture<Integer> sendMessage(WebSocketUserAddress userAddress, Object message) {
        WebSocketUserAddress userid = WebSocketUserAddress.create(userAddress);
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "sendMap: " + JsonConvert.root().convertTo(message));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(message, userid).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMessage(Stream<? extends WebSocketUserAddress> userAddresses, Object message) {
        Stream<? extends WebSocketUserAddress> userids = userAddresses.map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "sendMap: " + JsonConvert.root().convertTo(message));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(message, userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMessage(Collection<? extends WebSocketUserAddress> userAddresses, Object message) {
        if (userAddresses == null || userAddresses.isEmpty()) return CompletableFuture.completedFuture(0);
        Stream<? extends WebSocketUserAddress> userids = userAddresses.stream().map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(message));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(message, userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMap(WebSocketUserAddress userAddress, Object... messages) {
        WebSocketUserAddress userid = WebSocketUserAddress.create(userAddress);
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(ofMap(messages)));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(ofMap(messages), userid).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMap(Stream<? extends WebSocketUserAddress> userAddresses, Object... messages) {
        Stream<? extends WebSocketUserAddress> userids = userAddresses.map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(ofMap(messages)));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(ofMap(messages), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMap(Collection<? extends WebSocketUserAddress> userAddresses, Object... messages) {
        if (userAddresses == null || userAddresses.isEmpty()) return CompletableFuture.completedFuture(0);
        Stream<? extends WebSocketUserAddress> userids = userAddresses.stream().map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(ofMap(messages)));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(ofMap(messages), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendFutureMap(WebSocketUserAddress userAddress, String key, CompletableFuture future) {
        WebSocketUserAddress userid = WebSocketUserAddress.create(userAddress);
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(ofMap(key, future.join())));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(future.thenApply(value -> ofMap(key, value)), userid).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendFutureMap(Stream<? extends WebSocketUserAddress> userAddresses, String key, CompletableFuture future) {
        Stream<? extends WebSocketUserAddress> userids = userAddresses.map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(ofMap(key, future.join())));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(future.thenApply(value -> ofMap(key, value)), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendFutureMap(Collection<? extends WebSocketUserAddress> userAddresses, String key, CompletableFuture future) {
        if (userAddresses == null || userAddresses.isEmpty()) return CompletableFuture.completedFuture(0);
        Stream<? extends WebSocketUserAddress> userids = userAddresses.stream().map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(ofMap(key, future.join())));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(future.thenApply(value -> ofMap(key, value)), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMessage(Convert convert, WebSocketUserAddress userAddress, Object message) {
        WebSocketUserAddress userid = WebSocketUserAddress.create(userAddress);
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(message));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, message, userid).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMessage(Convert convert, Stream<? extends WebSocketUserAddress> userAddresses, Object message) {
        Stream<? extends WebSocketUserAddress> userids = userAddresses.map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(message));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, message, userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMessage(Convert convert, Collection<? extends WebSocketUserAddress> userAddresses, Object message) {
        if (userAddresses == null || userAddresses.isEmpty()) return CompletableFuture.completedFuture(0);
        Stream<? extends WebSocketUserAddress> userids = userAddresses.stream().map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(message));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, message, userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMap(Convert convert, WebSocketUserAddress userAddress, Object... messages) {
        WebSocketUserAddress userid = WebSocketUserAddress.create(userAddress);
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(ofMap(messages)));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, ofMap(messages), userid).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMap(Convert convert, Stream<? extends WebSocketUserAddress> userAddresses, Object... messages) {
        Stream<? extends WebSocketUserAddress> userids = userAddresses.map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(ofMap(messages)));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, ofMap(messages), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendMap(Convert convert, Collection<? extends WebSocketUserAddress> userAddresses, Object... messages) {
        if (userAddresses == null || userAddresses.isEmpty()) return CompletableFuture.completedFuture(0);
        Stream<? extends WebSocketUserAddress> userids = userAddresses.stream().map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(ofMap(messages)));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, ofMap(messages), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendFutureMap(Convert convert, WebSocketUserAddress userAddress, String key, CompletableFuture future) {
        WebSocketUserAddress userid = WebSocketUserAddress.create(userAddress);
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(ofMap(key, future.join())));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, future.thenApply(value -> ofMap(key, value)), userid).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendFutureMap(Convert convert, Stream<? extends WebSocketUserAddress> userAddresses, String key, CompletableFuture future) {
        Stream<? extends WebSocketUserAddress> userids = userAddresses.map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(ofMap(key, future.join())));
                return;
            }
            webSocketNode.sendMessage(convert, future.thenApply(value -> ofMap(key, value)), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    @Local
    public CompletableFuture<Integer> sendFutureMap(Convert convert, Collection<? extends WebSocketUserAddress> userAddresses, String key, CompletableFuture future) {
        if (userAddresses == null || userAddresses.isEmpty()) return CompletableFuture.completedFuture(0);
        Stream<? extends WebSocketUserAddress> userids = userAddresses.stream().map(x -> WebSocketUserAddress.create(x));
        CompletableFuture<Integer> result = new CompletableFuture<>();
        wsmessageQueue.add(() -> {
            if (webSocketNode == null) {
                logger.log(Level.FINEST, "local_SendMap: " + ((TextConvert) convert).convertTo(ofMap(key, future.join())));
                result.complete(0);
                return;
            }
            webSocketNode.sendMessage(convert, future.thenApply(value -> ofMap(key, value)), userids).whenComplete((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                } else {
                    result.complete(r);
                }
            });
        });
        return result;
    }

    //发送滚动公告
    public void broadcastHrollAnnouncement(Announcement... announces) {
        //if (finest) logger.finest("广播公告: " + JsonConvert.root().convertTo(announces));
        if (announces == null || announces.length < 1) return;
        this.announceLastTime.set(System.currentTimeMillis());
        this.announceQueue.add(announces);
    }

    //发送滚动公告
    public void broadcastHrollAnnouncement(List<Announcement> announces) {
        broadcastHrollAnnouncement(announces.toArray(new Announcement[announces.size()]));
    }

    protected abstract DataSource dataSource();

    protected abstract Class<? extends GameAccount> accountClass();

    public abstract String gameId();

    public abstract String gameName();

    //---------------------------- REST 接口 -------------------------------------
    @RestMapping(auth = false, comment = "平台进程启动通知")
    public RetResult<String> notifyPlatfStartup(int platfnodeid) {
        this.platfRunning.set(true);
        logger.info(gameId() + ": 接收到平台进程启动通知");
        return RetResult.success();
    }

    @RestMapping(auth = false, comment = "平台进程关闭通知")
    public RetResult<String> notifyPlatfShutdown(int platfnodeid) {
        this.platfRunning.set(false);
        logger.info(gameId() + ": 接收到平台进程关闭通知");
        return RetResult.success();
    }

    @RestMapping(auth = false, comment = "用户更新签到")
    public abstract RetResult<String> notifyDutyRecord(final int userid, final DutyRecord duty);

    @RestMapping(auth = false, comment = "用户更新设备商品")
    public abstract RetResult<String> notifyGoodsInfo(final int userid, final short goodstype, final int goodscount, final List<GoodsItem> items);

    @RestMapping(auth = false, comment = "平台用户信息更新通知")
    public abstract RetResult<String> notifyPlatfPlayer(final int userid, final Map<String, String> bean);

    @RestMapping(auth = false, comment = "每个场次的在线玩家数")
    public abstract Map<Integer, Long> getPlayingRoomUserMap();

    @RestMapping(auth = false, comment = "获取玩家正在玩的场次，为-1表示当前没有在玩, 为0表示玩家在此游戏内，但不分场次")
    public abstract int getPlayingRoomLevel(int userid);

    @RestMapping(auth = false, comment = "进入游戏")
    public abstract <T> RetResult<T> enterGame(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, Map<String, String> bean);

    @RestMapping(auth = false, comment = "强制关闭房间")
    public abstract RetResult<Table> forceDismissTable(GTBean bean);

    @RestMapping(auth = true, comment = "离开游戏")
    public abstract <T> RetResult<T> leaveGame(int userid, Map<String, String> bean);

    @RestMapping(auth = true, comment = "玩家离线")
    public abstract <T> RetResult<T> offlineGame(int userid, Map<String, String> bean);

    @RestMapping(auth = true, comment = "检测GameTable是否正常")
    public abstract RetResult<String> checkTable(String tableid);

    //加入游戏房间, bean中只有tableno、sitepos字段用到
    @RestMapping(auth = true, comment = "加入房间")
    public abstract RetResult<Table> joinTable(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, GTBean bean);

    protected static class GameMissionUpdateEntry extends MissionUpdateEntry {

        public GameAccount account;

        public GameMissionUpdateEntry(GameAccount account, int userid, boolean incrable, int missiontype, long reachcount, long reachtime) {
            super(userid, incrable, missiontype, reachcount, reachtime);
            this.account = account;
        }

        public GameMissionUpdateEntry(GameAccount account, int userid, boolean incrable, int missiontype, int reachobjid, long reachcount, long reachtime) {
            super(userid, incrable, missiontype, reachobjid, reachcount, reachtime);
            this.account = account;
        }
    }
}
