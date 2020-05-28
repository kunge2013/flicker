/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.*;
import static com.cratos.platf.base.BaseEntity.*;
import com.cratos.platf.notice.SmsService;
import com.cratos.platf.notice.RandomCode;
import java.security.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.redkale.convert.json.JsonConvert;
import static com.cratos.platf.base.RetCodes.*;
import static com.cratos.platf.base.UserInfo.*;
import com.cratos.platf.info.*;
import com.cratos.platf.liveness.LivenessService;
import com.cratos.platf.mission.MissionService;
import com.cratos.platf.notice.*;
import com.cratos.platf.order.OrderPeriodService;
import static com.cratos.platf.user.UserDetail.*;
import com.cratos.platf.util.*;
import com.cratos.platf.util.QueueTask.InsertBiConsumer;
import com.cratos.platf.vip.VipService;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.regex.Pattern;
import javax.annotation.*;
import javax.persistence.Transient;
import org.redkale.net.http.WebSocketNode;
import org.redkalex.weixin.WeiXinMPService;
import org.redkale.service.*;
import org.redkale.source.*;
import static org.redkale.source.FilterExpress.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("用户服务模块")
@Priority(1000000)
public class UserService extends BaseService {

    public static final Type TYPE_MAP_STRING_LONG = new TypeToken<java.util.HashMap<String, Long>>() {
    }.getType();

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    private static final MessageDigest sha1;

    private static final MessageDigest md5;

    public static final String AES_KEY = "REDKALE_20200202";

    private static final Cipher aesEncrypter; //加密

    private static final Cipher aesDecrypter; //解密

    @Transient //随机源
    protected final SecureRandom idRandom = ShuffleRandom.createRandom();

    static {

        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new Error(ex);
        }
        sha1 = d;
        try {
            d = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new Error(ex);
        }
        md5 = d;

        Cipher cipher = null;
        final SecretKeySpec aesKey = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        } catch (Exception e) {
            throw new Error(e);
        }
        aesEncrypter = cipher;  //加密
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
        } catch (Exception e) {
            throw new Error(e);
        }
        aesDecrypter = cipher; //解密
    }

    private final int sessionExpireSeconds = 5 * 60;

    //不在线的机器人
    private final FilterNode robotNode = FilterNode.create("userid", LESSTHAN, USERID_MAXROBOT).and("currgame", "");

    @Resource(name = "system.property.nodeid")
    protected int nodeid = 10;

    @Resource(name = "usersessions")
    protected CacheSource<Integer> sessions;

    /**
     * 存放的信息类型: (IP端口为HTTP服务对应的地址)
     * 1、模块对应的nodeid和ip端口信息。 格式： key = module:xxx, value = 10@192.168.1.1:6161; 20@192.168.1.2:6262;
     *
     */
    @Resource(name = "wsgame")
    protected CacheSource<InetSocketAddress> gatewayNodes;

    @Resource(name = "wsgame")
    protected WebSocketNode webSocketNode;

    @Resource(name = "platf")
    protected DataSource userSource;

    @Resource(name = "property.qq.mp.appid") // 
    protected String qqappid = "";

    @Transient //更新当前游戏缓存队列
    protected final QueueTask<UserCurrgameEntry> currgameQueue = new QueueTask<>(1);

    @Transient //登录缓存队列
    protected final QueueTask<UserLoginRecord> loginQueue = new QueueTask<>(1);

    @Transient //电脑充值队列
    protected final QueueTask<RobotCoinRecord> robotCoinQueue = new QueueTask<>(1);

    @Transient //更新Detail缓存队列
    protected final QueueTask<UserUpdateEntry> detailQueue = new QueueTask<>(1);

    @Transient //更新UserDayRecord缓存队列
    protected final QueueTask<DayRecordUpdateEntry> dayrecordQueue = new QueueTask<>(1);

    @Transient //注销缓存队列
    protected final QueueTask<UserLogoutEntry> logoutQueue = new QueueTask<>(1);

    @Transient //UserCoinRecord 队列
    protected final QueueTask<UserCoinRecord> coinQueue = new QueueTask<>(1);

    @Transient //UserDiamondRecord 队列
    protected final QueueTask<UserDiamondRecord> diamondQueue = new QueueTask<>(1);

    @Transient //UserCouponRecord 队列
    protected final QueueTask<UserCouponRecord> couponQueue = new QueueTask<>(1);

    @Transient //游戏同步信息队列
    protected final QueueTask<UserInfo> syncGameQueue = new QueueTask<>(2);

    @Transient  //randomRobot自动创建的虚拟账号自增序列化
    protected final AtomicInteger robotIndex = new AtomicInteger();

    @Transient //机器人ID集合
    protected final List<Integer> robotUserids = new ArrayList<>();

    //@Resource
    //protected FileService fileService;
    @Resource
    private WeiXinMPService wxMPService;

    @Resource
    private ModuleAddressService moduleService;

    @Resource
    private VipService vipService;

    @Resource
    private SmsService smsService;

    @Resource
    protected DictService dictService;

    @Resource
    protected MissionService missionService;

    @Resource
    protected LivenessService livenessService;

    @Resource
    protected OrderPeriodService orderPeriodService;

    @Resource
    private JsonConvert convert;

    @Resource
    private RandomService randomCodeService;

    @Transient
    protected List<String> blackIpList;

    @Transient
    protected List<String> blackApptokenList;

    @Resource
    protected UserBlackService blackService;

    protected ScheduledThreadPoolExecutor scheduler;

    protected final Object regLock = new Object();

    protected final Object robotLock = new Object();

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(3, (Runnable r) -> {
            final Thread t = new Thread(r, "UserService-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        initQueueTask();
        reloadConfig();

        this.robotUserids.addAll(userSource.queryColumnList("userid", UserInfo.class, FilterNode.create("userid", LESSTHAN, USERID_MAXROBOT)));
        //
        scheduler.scheduleAtFixedRate(() -> {
            reloadConfig();
        }, 1, 1, TimeUnit.MINUTES);

        //
        final long midDelay = 24 * 60 * 60 * 1000 + Utility.midnight() - System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(() -> { //凌晨前1分钟建表UserDayRecord
            try {
                UserDayRecord r = new UserDayRecord(1, 0, 0, 0);
                r.setCreatetime(24 * 60 * 60 * 1000 + Utility.midnight() + 1);
                r.setIntday(Utility.yyyyMMdd(r.getCreatetime()));
                userSource.insert(r);
                userSource.delete(UserDayRecord.class, FilterNode.create("createtime", r.getCreatetime()).and("userid", r.getUserid()));
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, midDelay - 60000 > 0 ? midDelay - 60000 : 0, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> { //凌晨清空 liveness
            try {
                userSource.updateColumn(UserDetail.class, (FilterNode) null, ColumnValue.mov("liveness", 0));
                userSource.updateColumn(UserInfo.class, (FilterNode) null, ColumnValue.mov("liveness", 0));
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, midDelay, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> { //建表UserDayRecord
            try {
                final int useRobotCount = userSource.getNumberResult(UserInfo.class, FilterFunc.COUNT, 0, null, robotNode).intValue();
                final Map<String, AtomicInteger> playingMap = new HashMap<>();
                for (UserInfo one : userSource.queryList(UserInfo.class, FilterNode.create("userid", LESSTHAN, USERID_MAXROBOT).and("currgame", NOTEQUAL, ""))) {
                    playingMap.computeIfAbsent(one.getCurrgame(), s -> new AtomicInteger()).incrementAndGet();
                }
                logger.finest("可用机器人数: " + useRobotCount + ", 游戏中的机器人: " + playingMap);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, 60, 300, TimeUnit.SECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
        notifyPlatfAction("notifyPlatfShutdown");
        logger.log(Level.FINEST, "logoutQueue.size = " + logoutQueue.size() + ", currgameQueue.size = " + currgameQueue.size());
        logoutQueue.destroy();
        currgameQueue.destroy();
        syncGameQueue.destroy();
        detailQueue.destroy();
        dayrecordQueue.destroy();
        loginQueue.destroy();
        robotCoinQueue.destroy();
        coinQueue.destroy();
        diamondQueue.destroy();
        couponQueue.destroy();
    }

    void postStart() {
        notifyPlatfAction("notifyPlatfStartup");
    }

    private void notifyPlatfAction(String path) {
        if (winos) return;
        Map<String, Map<String, InetSocketAddress>> moduleMap = moduleService.loadModuleMap();
        if (moduleMap == null || moduleMap.isEmpty()) return;
        String url = null;
        for (Map.Entry<String, Map<String, InetSocketAddress>> en : moduleMap.entrySet()) {
            final String gameid = en.getKey();
            if ("platf".equals(gameid) || "room".equals(gameid) || "video".equals(gameid)) continue;
            final String uri = "/pipes/" + gameid + "/" + path + "?platfnodeid=" + this.nodeid;
            for (InetSocketAddress addr : en.getValue().values()) {
                try {
                    url = "http://" + addr.getHostString() + ":" + addr.getPort() + uri;
                    String rs = Utility.getHttpContent(url, 1000);
                    logger.log(Level.FINE, "url=" + url + ", result=" + rs);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, url + " remote error", e);
                }
            }
        }
    }

    private void reloadConfig() {
        this.blackIpList = blackService.queryUserBlackIp();
        this.blackApptokenList = blackService.queryUserBlackApptoken();
    }

    private void initQueueTask() {
        //--------------游戏-----------------
        BiConsumer<BlockingQueue<UserCurrgameEntry>, UserCurrgameEntry> currgameConsumer = (queue, entry) -> {
            if (entry.userids.length < 2) {
                userSource.updateColumn(UserDetail.class, entry.userids[0], ColumnValue.mov("currgame", entry.currgame), ColumnValue.mov("currgamingtime", entry.currgamingtime));
            } else {
                userSource.updateColumn(UserDetail.class, FilterNode.create("userid", IN, entry.userids), ColumnValue.mov("currgame", entry.currgame), ColumnValue.mov("currgamingtime", entry.currgamingtime));
            }
        };
        currgameQueue.init(logger, currgameConsumer);
        //--------------注销-----------------
        BiConsumer<BlockingQueue<UserLogoutEntry>, UserLogoutEntry> logoutConsumer = (queue, entry) -> {
            long now = System.currentTimeMillis();
            long onlineseconds = 0;
            UserLoginRecord record = userSource.find(UserLoginRecord.class, FilterNode.create("sessionid", entry.sessionid).and("#createtime", LESSTHANOREQUALTO, now));
            if (record == null) record = userSource.find(UserLoginRecord.class, FilterNode.create("sessionid", entry.sessionid).and("#createtime", LESSTHANOREQUALTO, Utility.midnight() - 1)); //昨天
            if (record == null) record = userSource.find(UserLoginRecord.class, FilterNode.create("sessionid", entry.sessionid).and("#createtime", LESSTHANOREQUALTO, Utility.midnight() - 24 * 60 * 60 * 1000L - 1)); //前天
            if (record != null) {
                record.setLogouttime(now);
                onlineseconds = (record.getLogouttime() - record.getCreatetime()) / 1000;
                record.setOnlineseconds(onlineseconds);
                userSource.updateColumn(record, "onlineseconds", "logouttime");
            }
            userSource.updateColumn(UserDetail.class, entry.userid, ColumnValue.inc("onlineseconds", onlineseconds));
            userSource.updateColumn(UserInfo.class, entry.userid, ColumnValue.inc("onlineseconds", onlineseconds));
        };
        logoutQueue.init(logger, logoutConsumer);
        //--------------同步-------------------
        syncGameQueue.init(logger, (queue, user) -> syncRemoteGameModule(user));
        detailQueue.init(logger, (queue, entry) -> userSource.updateColumn(UserDetail.class, entry.userid, entry.values));
        dayrecordQueue.init(logger, (queue, entry) -> {
            int intday = Utility.yyyyMMdd(entry.createtime);
            FilterNode node = FilterNode.create("userid", entry.userid).and("intday", intday);
            int c = 0;
            try {
                c = userSource.updateColumn(UserDayRecord.class, node, entry.values);
            } catch (Exception e) {
                if (!entry.needinsert) return;
                UserDayRecord r = new UserDayRecord(1, 0, 0, 0);
                r.setCreatetime(entry.createtime);
                r.setIntday(Utility.yyyyMMdd(r.getCreatetime()));
                userSource.insert(r);
                userSource.delete(UserDayRecord.class, FilterNode.create("createtime", r.getCreatetime()).and("userid", r.getUserid()));
            }
            if (c == 0 && entry.needinsert) {
                UserDayRecord record = new UserDayRecord();
                record.setUserid(entry.userid);
                record.setIntday(intday);
                record.setCreatetime(entry.createtime);
                userSource.insert(record);
                userSource.updateColumn(UserDayRecord.class, node, entry.values);
            }
        });
        loginQueue.init(logger, new InsertBiConsumer(userSource));
        robotCoinQueue.init(logger, new InsertBiConsumer(userSource));
        coinQueue.init(logger, new InsertBiConsumer(userSource));
        diamondQueue.init(logger, new InsertBiConsumer(userSource));
        couponQueue.init(logger, new InsertBiConsumer(userSource));
    }

    public boolean existsUserInfo(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) return false;
        return userSource.exists(UserInfo.class, userid);
    }

    public UserInfo findUserInfo(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) return UserInfo.USER_SYSTEM;
        UserInfo user = userSource.find(UserInfo.class, userid);
        return user;
    }

    public Player findPlayer(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) return UserInfo.USER_SYSTEM.createPlayer();
        UserInfo user = userSource.find(UserInfo.class, userid);
        return user == null ? null : user.createPlayer();
    }

    public IntroPlayer findIntroPlayer(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) return UserInfo.USER_SYSTEM.createIntroPlayer();
        UserInfo user = userSource.find(UserInfo.class, userid);
        return user == null ? null : user.createIntroPlayer();
    }

    @Comment("获取当前在线用户数, 包含大厅和游戏里的玩家人数")
    public int getCurrUserSize() {
        List<String> userids = gatewayNodes.queryKeysStartsWith(WebSocketNode.SOURCE_SNCP_USERID_PREFIX);
        if (userids == null || userids.isEmpty()) return 0;
        int size = 0;
        for (String useridstr : userids) {
            Integer userid = Integer.parseInt(useridstr.substring(WebSocketNode.SOURCE_SNCP_USERID_PREFIX.length()));
            if (!existsUserInfo(userid)) continue;
            size++;
        }
        return size;
    }

    public Map<String, Long> getUserPlayingCount() {
        final Map<String, Long> rs = new ConcurrentHashMap<>();
        Map<String, Map<String, InetSocketAddress>> moduleMap = moduleService.loadModuleMap();
        for (Map.Entry<String, Map<String, InetSocketAddress>> modval : moduleMap.entrySet()) {
            final String gameid = modval.getKey();
            if (gameid.isEmpty() || "platf".equals(gameid)) continue;
            final Map<String, InetSocketAddress> addrMap = modval.getValue();
            if (addrMap == null) continue;
            long count = -1;
            final String module = gameid;
            final int sub = module.indexOf('_');
            String url = null;
            final String uri = "/pipes/" + (sub > 0 ? module.substring(sub + 1) : module) + "/getPlayingRoomUserMap";
            for (final InetSocketAddress addr : addrMap.values()) {
                try {
                    url = "http://" + addr.getHostString() + ":" + addr.getPort() + uri;
                    String content = Utility.getHttpContent(url, 1000);
                    if (count == -1) count = 0;
                    Map<String, Long> map = JsonConvert.root().convertFrom(TYPE_MAP_STRING_LONG, content);
                    count += map.values().stream().mapToLong(x -> x).sum();
                } catch (Exception e) {
                    logger.log(Level.INFO, url + " remote error", e);
                }
            }
            rs.put(gameid, count);
        }
        return rs;
    }

    @Comment("查找用户金币数")
    public long findUserCoins(int userid) {
        UserInfo user = findUserInfo(userid);
        return user == null ? 0 : user.getCoins();
    }

    @Comment("查找用户活跃度值")
    public long findUserLiveness(int userid) {
        UserInfo user = findUserInfo(userid);
        return user == null ? 0 : user.getLiveness();
    }

    @Comment("增加/减少游戏金币数, 机器人也要算清楚， 坐庄可能是玩家")
    public long updateGameUserCoins(final int userid, final int roomlevel, final long coin, final long costcoin, final long createtime, final String game, final String module, final String remark) {
        UserInfo user = null;
        synchronized (userLock(userid)) {
            if (userid != USERID_SYSTEM && userid != 0) {
                user = findUserInfo(userid);
                if (user == null) {
                    removeUserLock(userid);
                    return -1L;
                }
                if (coin == 0 && costcoin == 0) return user.getCoins();  //只是coins为0就返回会导致押注返利数据不对
                if (coin != 0 && user.getCoins() < -coin) throw new RuntimeException("updateGameUserCoins: user = " + user + ", game=" + game + ", module=" + module + "， loss coin =" + coin + ", but negative coins");

                userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coins", coin));
                user.setCoins(user.getCoins() + coin);
                detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coins", coin)));
                dayrecordQueue.add(new DayRecordUpdateEntry(userid, createtime, ColumnValue.inc("wincoins", coin), ColumnValue.inc("costcoins", costcoin)));
                if (coin > 0) {
                    missionService.updatePlatfMissionWinCoin(userid, coin, game, createtime);
                } else if (coin < 0) {
                    missionService.updatePlatfMissionCostCoin(userid, -coin, game, createtime);
                }
            }
            coinQueue.add(new UserCoinRecord(userid, roomlevel, coin, costcoin, user == null ? -1 : user.getCoins(), createtime, game, module, remark));
        }
        logger.finer("user(" + userid + ").updateGameUserCoins coins=" + coin + ", after usercoins=" + (user == null ? -1 : user.getCoins()));
        return (user == null ? -1 : user.getCoins());
    }

    @Comment("增加/减少游戏晶石数, 机器人也要算清楚， 坐庄可能是玩家")
    public long updateGameUserDiamonds(final int userid, final int roomlevel, final long diamond, final long costdiamond, final long createtime, final String game, final String module, final String remark) {
        UserInfo user = null;
        synchronized (userLock(userid)) {
            if (userid != USERID_SYSTEM && userid != 0) {
                user = findUserInfo(userid);
                if (user == null) {
                    removeUserLock(userid);
                    return -1L;
                }
                if (diamond == 0 && costdiamond == 0) return user.getDiamonds();  //只是coins为0就返回会导致押注返利数据不对
                if (diamond != 0 && user.getDiamonds() < -diamond) throw new RuntimeException("updateGameUserDiamonds: user = " + user + ", game=" + game + ", module=" + module + "， loss diamond =" + diamond + ", but negative diamonds");

                userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("diamonds", diamond));
                user.setDiamonds(user.getDiamonds() + diamond);
                detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("diamonds", diamond)));
                //dayrecordQueue.add(new DayRecordUpdateEntry(userid, createtime, ColumnValue.inc("windiamonds", diamond)));
                if (diamond > 0) {
                    missionService.updatePlatfMissionWinDiamond(userid, diamond, game, createtime);
                } else if (diamond < 0) {
                    missionService.updatePlatfMissionCostDiamond(userid, -diamond, game, createtime);
                }
            }
            diamondQueue.add(new UserDiamondRecord(userid, diamond, user == null ? -1 : user.getDiamonds(), createtime, game, module, remark));
        }
        logger.finer("user(" + userid + ").updateGameUserDiamonds diamonds=" + diamond + ", after userdiamonds=" + (user == null ? -1 : user.getDiamonds()));
        return (user == null ? -1 : user.getDiamonds());
    }

    @Comment("增加/减少游戏奖券数, 机器人也要算清楚， 坐庄可能是玩家")
    public long updateGameUserCoupons(final int userid, final int roomlevel, final long coupon, final long costcoupon, final long createtime, final String game, final String module, final String remark) {
        UserInfo user = null;
        synchronized (userLock(userid)) {
            if (userid != USERID_SYSTEM && userid != 0) {
                user = findUserInfo(userid);
                if (user == null) {
                    removeUserLock(userid);
                    return -1L;
                }
                if (coupon == 0 && costcoupon == 0) return user.getCoupons();  //只是coins为0就返回会导致押注返利数据不对
                if (coupon != 0 && user.getCoupons() < -coupon) throw new RuntimeException("updateGameUserCoupons: user = " + user + ", game=" + game + ", module=" + module + "， loss coupon =" + coupon + ", but negative coupons");

                userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coupons", coupon));
                user.setCoupons(user.getCoupons() + coupon);
                detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coupons", coupon)));
                //dayrecordQueue.add(new DayRecordUpdateEntry(userid, createtime, ColumnValue.inc("wincoupons", coupon)));
                if (coupon > 0) {
                    missionService.updatePlatfMissionWinCoupon(userid, coupon, game, createtime);
                } else if (coupon < 0) {
                    missionService.updatePlatfMissionCostCoupon(userid, -coupon, game, createtime);
                }
            }
            couponQueue.add(new UserCouponRecord(userid, coupon, user == null ? -1 : user.getCoupons(), createtime, game, module, remark));
        }
        logger.finer("user(" + userid + ").updateGameUserCoupons coupons=" + coupon + ", after usercoupons=" + (user == null ? -1 : user.getCoupons()));
        return (user == null ? -1 : user.getCoupons());
    }

    @Comment("减少非游戏金币数, 优先减银行")
    public long decrePlatfUserCoins(final int userid, final long coin, final long time, final String module, final String remark) {
        if (coin < 0) throw new RuntimeException("decrePlatfUserCoins: coin =" + coin + ", is negative coin, module=" + module + "， remark=" + remark);
        UserInfo user;
        synchronized (userLock(userid)) {
            user = findUserInfo(userid);
            if (user == null) {
                removeUserLock(userid);
                return -1L;
            }
            if (coin == 0) return user.getCoins();
            if ((user.getCoins() + user.getBankcoins()) < coin) throw new RuntimeException("decrePlatfUserCoins: user = " + user + ", loss coin =" + coin + ", bu negative coin");
            if (user.getBankcoins() >= coin) {
                userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("bankcoins", -coin));
                user.setBankcoins(user.getBankcoins() - coin);
                detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("bankcoins", -coin)));
                coinQueue.add(new UserCoinRecord(userid, 0, -coin, 0, user.getCoins(), time, "platf", module, remark));
            } else {
                long decusercoin = user.getBankcoins() - coin;
                userSource.updateColumn(UserInfo.class, userid, ColumnValue.mov("bankcoins", 0L), ColumnValue.inc("coins", decusercoin));
                user.setBankcoins(0);
                user.setCoins(user.getCoins() + decusercoin);
                detailQueue.add(new UserUpdateEntry(userid, ColumnValue.mov("bankcoins", 0L), ColumnValue.inc("coins", decusercoin)));
                coinQueue.add(new UserCoinRecord(userid, 0, -coin, 0, user.getCoins(), time, "platf", module, remark));
            }
        }
        logger.finer("user(" + userid + ").decrePlatfUserCoins coin=" + coin + ", after usercoins=" + user.getCoins() + ", bankcoins=" + user.getBankcoins());
        syncGameQueue.add(user);
        return user.getCoins();
    }

    @Comment("增加非游戏金币数")
    public long increPlatfUserCoins(final int userid, final long coin, final long time, final String module, final String remark) {
        if (coin < 0) throw new RuntimeException("increPlatfUserCoins: coin =" + coin + ", is negative coin");
        UserInfo user;
        synchronized (userLock(userid)) {
            user = findUserInfo(userid);
            if (user == null) {
                removeUserLock(userid);
                return -1L;
            }
            if (coin == 0) return user.getCoins();
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coins", coin));
            user.setCoins(user.getCoins() + coin);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coins", coin)));
            coinQueue.add(new UserCoinRecord(userid, 0, coin, 0, user.getCoins(), time, "platf", module, remark));
        }
        logger.finer("user(" + userid + ").increPlatfUserCoins coin=" + coin + ", after usercoins=" + user.getCoins());
        syncGameQueue.add(user);
        return user.getCoins();
    }

    @Comment("消耗金币数, coin必须是正数")
    public RetResult<Integer> costGameUserCoins(final int userid, final int coin, final long time, String gameid, String module, String remark) {
        if (userid == UserInfo.USERID_SYSTEM) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (coin < 0) {
                logger.log(Level.SEVERE, "user = " + user + "， costcoin =" + coin + " is negative");
                return RetResult.success();
            }
            if (user.getCoins() < coin) {
                logger.log(Level.SEVERE, "costGameUserCoins: user = " + user + "， costcoin =" + coin + ", but costcoin bigger usercoins");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coins", -coin));
            user.setCoins(user.getCoins() - coin);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coins", -coin)));
            if (coin != 0 && !UserInfo.isRobot(userid)) {
                coinQueue.add(new UserCoinRecord(userid, 0, -coin, 0, user.getCoins(), time, gameid, module, remark));
                missionService.updatePlatfMissionCostCoin(userid, coin, gameid, time);
            }
        }
        logger.finer("user(" + userid + ").costGameUserCoins costcoin=" + coin + ", after usercoins=" + user.getCoins());
        return RetResult.success();
    }

    @Comment("消耗钻石数, diamond必须是正数")
    public RetResult<Integer> costGameUserDiamonds(final int userid, final int diamond, final long time, String gameid, String module, String remark) {
        if (userid == UserInfo.USERID_SYSTEM) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (diamond < 0) {
                logger.log(Level.SEVERE, "user = " + user + "， costdiamond =" + diamond + " is negative");
                return RetResult.success();
            }
            if (user.getDiamonds() < diamond) {
                logger.log(Level.SEVERE, "costGameUserDiamonds: user = " + user + "， costdiamond =" + diamond + ", but costdiamond bigger userdiamonds");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("diamonds", -diamond));
            user.setDiamonds(user.getDiamonds() - diamond);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("diamonds", -diamond)));
            if (diamond != 0 && !UserInfo.isRobot(userid)) {
                diamondQueue.add(new UserDiamondRecord(userid, -diamond, user.getDiamonds(), time, gameid, module, remark));
                missionService.updatePlatfMissionCostDiamond(userid, diamond, gameid, time);
            }
        }
        logger.finer("user(" + userid + ").costGameUserDiamonds costdiamond=" + diamond + ", after userdiamonds=" + user.getDiamonds());
        return RetResult.success();
    }

    @Comment("消耗奖券数, coupon必须是正数")
    public RetResult<Integer> costGameUserCoupons(final int userid, final int coupon, final long time, String gameid, String module, String remark) {
        if (userid == UserInfo.USERID_SYSTEM) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (coupon < 0) {
                logger.log(Level.SEVERE, "user = " + user + "， costcoupon =" + coupon + " is negative");
                return RetResult.success();
            }
            if (user.getCoupons() < coupon) {
                logger.log(Level.SEVERE, "costGameUserCoupons: user = " + user + "， costcoupon =" + coupon + ", but costcoupon bigger usercoupons");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coupons", -coupon));
            user.setCoupons(user.getCoupons() - coupon);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coupons", -coupon)));
            if (coupon != 0 && !UserInfo.isRobot(userid)) {
                couponQueue.add(new UserCouponRecord(userid, -coupon, user.getDiamonds(), time, gameid, module, remark));
                missionService.updatePlatfMissionCostCoupon(userid, coupon, gameid, time);
            }
        }
        logger.finer("user(" + userid + ").costGameUserCoupons costcoupon=" + coupon + ", after usercoupons=" + user.getCoupons());
        return RetResult.success();
    }

    @Comment("退还金币数, coin必须是正数")
    public RetResult<Integer> refundGameUserCoins(final int userid, final int coin, final long time, String gameid, String module, String remark) {
        if (userid == UserInfo.USERID_SYSTEM) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (coin < 0) {
                logger.log(Level.SEVERE, "user = " + user + "， refundGameUserCoins =" + coin + " is negative");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coins", coin));
            user.setCoins(user.getCoins() + coin);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coins", coin)));
            if (coin != 0 && !UserInfo.isRobot(userid)) {
                coinQueue.add(new UserCoinRecord(userid, 0, coin, 0, user.getCoins(), time, gameid, module, remark));
            }
        }
        logger.finer("user(" + userid + ").refundGameUserCoins refundCoin=" + coin + ", after usercoins=" + user.getCoins());
        return RetResult.success();
    }

    @Comment("退还钻石数, diamond必须是正数")
    public RetResult<Integer> refundGameUserDiamonds(final int userid, final int diamond, final long time, String gameid, String module, String remark) {
        if (userid == UserInfo.USERID_SYSTEM) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (diamond < 0) {
                logger.log(Level.SEVERE, "user = " + user + "， refundGameUserDiamonds =" + diamond + " is negative");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("diamonds", diamond));
            user.setDiamonds(user.getDiamonds() + diamond);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("diamonds", diamond)));
            if (diamond != 0 && !UserInfo.isRobot(userid)) {
                diamondQueue.add(new UserDiamondRecord(userid, diamond, user.getDiamonds(), time, gameid, module, remark));
            }
        }
        logger.finer("user(" + userid + ").refundGameUserDiamonds refundDiamond=" + diamond + ", after userdiamonds=" + user.getDiamonds());
        return RetResult.success();
    }

    @Comment("退还奖券数, coupon必须是正数")
    public RetResult<Integer> refundGameUserCoupons(final int userid, final int coupon, final long time, String gameid, String module, String remark) {
        if (userid == UserInfo.USERID_SYSTEM) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (coupon < 0) {
                logger.log(Level.SEVERE, "user = " + user + "， refundGameUserCoupons =" + coupon + " is negative");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coupons", coupon));
            user.setCoupons(user.getCoupons() + coupon);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coupons", coupon)));
            if (coupon != 0 && !UserInfo.isRobot(userid)) {
                couponQueue.add(new UserCouponRecord(userid, coupon, user.getCoupons(), time, gameid, module, remark));
            }
        }
        logger.finer("user(" + userid + ").refundGameUserCoupons refundCoupon=" + coupon + ", after usercoupons=" + user.getCoupons());
        return RetResult.success();
    }

    @Comment("增加金币钻石券数")
    public RetResult updatePlatfUserCoinDiamondCoupons(final int userid, final long coin, final long diamond, final long coupon, final long time, final String module, final String remark) {
        if (userid == UserInfo.USERID_SYSTEM) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (coin != 0 && user.getCoins() < -coin) {
                logger.log(Level.SEVERE, "updatePlatfUserCoinDiamondCoupons: user = " + user + "， loss coin =" + coin + ", but negative coins");
                return RetResult.success();
            }
            if (diamond != 0 && user.getDiamonds() < -diamond) {
                logger.log(Level.SEVERE, "updatePlatfUserCoinDiamondCoupons: user = " + user + "， loss diamond =" + diamond + ", but negative diamonds");
                return RetResult.success();
            }
            if (coupon != 0 && user.getCoupons() < -coupon) {
                logger.log(Level.SEVERE, "updatePlatfUserCoinDiamondCoupons: user = " + user + "， loss coupon =" + coupon + ", but negative coupons");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("coins", coin), ColumnValue.inc("diamonds", diamond), ColumnValue.inc("coupons", coupon));
            user.setCoins(user.getCoins() + coin);
            user.setDiamonds(user.getDiamonds() + diamond);
            user.setCoupons(user.getCoupons() + coupon);
            detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("coins", coin), ColumnValue.inc("diamonds", diamond), ColumnValue.inc("coupons", coupon)));
            if (!UserInfo.isRobot(userid)) {
                if (coin != 0) coinQueue.add(new UserCoinRecord(userid, 0, coin, 0L, user.getCoins(), time, "platf", module, remark));
                if (diamond != 0) diamondQueue.add(new UserDiamondRecord(userid, (int) diamond, user.getDiamonds(), time, "platf", module, remark));
                if (coupon != 0) couponQueue.add(new UserCouponRecord(userid, (int) coupon, user.getCoupons(), time, "platf", module, remark));
            }
        }
        logger.finer("user(" + userid + ").updatePlatfUserCoinDiamondCoupons coins=" + coin
            + ", after usercoins=" + user.getCoins() + " diamond=" + diamond + ", after userdiamonds=" + user.getDiamonds() + " coupon=" + coupon + ", after usercoupons=" + user.getCoupons());
        return RetResult.success();
    }

    @Comment("增加/减少活跃度")
    public long updateGameUserLiveness(final int userid, final long liveness, final long createtime, final String game, final String module, final String remark) {
        UserInfo user = null;
        synchronized (userLock(userid)) {
            if (userid != USERID_SYSTEM && userid != 0) {
                user = findUserInfo(userid);
                if (user == null) {
                    removeUserLock(userid);
                    return -1L;
                }
                if (liveness == 0) return user.getLiveness();
                if (liveness != 0 && user.getLiveness() < -liveness) throw new RuntimeException("updateGameUserLiveness: user = " + user + ", game=" + game + ", module=" + module + "， loss liveness =" + liveness + ", but negative liveness");

                userSource.updateColumn(UserInfo.class, userid, ColumnValue.inc("liveness", liveness));
                user.setLiveness(user.getLiveness() + liveness);
                detailQueue.add(new UserUpdateEntry(userid, ColumnValue.inc("liveness", liveness)));
                dayrecordQueue.add(new DayRecordUpdateEntry(userid, createtime, ColumnValue.inc("liveness", liveness)));
            }
        }
        logger.finer("user(" + userid + ").updateGameUserLiveness liveness=" + liveness + ", after userliveness=" + (user == null ? -1L : user.getLiveness()));
        return (user == null ? -1L : user.getLiveness());
    }

    //银行的金币与钱包的金币互转， 正数表示钱包往银行转，负数为银行往钱包转
    public RetResult<UserInfo> transferDepositCoins(int userid, long bankcoins) {
        if (bankcoins == 0) return RetResult.success();
        UserInfo user;
        synchronized (userLock(userid)) {
            user = userSource.find(UserInfo.class, userid);
            if (user == null) {
                removeUserLock(userid);
                return RetResult.success();
            }
            if (bankcoins < 0) { //银行往钱包转
                if (user.getBankcoins() < -bankcoins) return RetCodes.retResult(RET_USER_COINS_NOTENOUGH);
            } else { //钱包往银行转
                if (user.getCoins() < bankcoins) return RetCodes.retResult(RET_USER_COINS_NOTENOUGH);
            }
            userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.inc("coins", -bankcoins), ColumnValue.inc("bankcoins", bankcoins));
            user.setCoins(user.getCoins() - bankcoins);
            user.setBankcoins(user.getBankcoins() + bankcoins);
            detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.inc("coins", -bankcoins), ColumnValue.inc("bankcoins", bankcoins)));
        }
        logger.finer("user(" + userid + ").transferBankCoins coins=" + bankcoins + ", after coins=" + user.getCoins() + ", after bankcoins=" + user.getBankcoins());
        syncGameQueue.add(user);
        return new RetResult<>(user);
    }

    @Comment("充值金币&钻石&奖券")
    public RetResult rechargeUserCoinDiamondCoupons(final int userid, final long money, final long coin, final long diamond, final long coupon, final long paytime, String remark) {
        if (money < 0 || coin < 0 || diamond < 0 || coupon < 0) return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        UserDetail user;
        synchronized (userLock(userid)) {
            user = findUserDetail(userid);
            if (user == null) {
                removeUserLock(userid);
                return RetCodes.retResult(RET_USER_NOTEXISTS);
            }
            if (user.getCoins() < -coin) {
                logger.log(Level.SEVERE, "rechargeCoins: user = " + user + "， loss coin =" + coin + ", but negative coins");
                return RetResult.success();
            }
            if (user.getDiamonds() < -diamond) {
                logger.log(Level.SEVERE, "rechargeDiamonds: user = " + user + "， loss diamond =" + diamond + ", but negative diamonds");
                return RetResult.success();
            }
            if (user.getCoupons() < -coupon) {
                logger.log(Level.SEVERE, "rechargeCoupons: user = " + user + "， loss coupon =" + coupon + ", but negative coupons");
                return RetResult.success();
            }
            userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.inc("paymoney", money), ColumnValue.inc("coins", coin), ColumnValue.inc("diamonds", diamond), ColumnValue.inc("coupons", coupon));
            user.setCoins(user.getCoins() + coin);
            user.setDiamonds(user.getDiamonds() + diamond);
            user.setCoupons(user.getCoupons() + coupon);
            user.setPaycount(user.getPaycount() + 1);
            user.setPaymoney(user.getPaymoney() + money);

            if (user.getFirstpaymoney() == 0) {
                detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.inc("coins", coin), ColumnValue.inc("diamonds", diamond), ColumnValue.inc("coupons", coupon),
                    ColumnValue.inc("paycoins", coin), ColumnValue.inc("paydiamonds", diamond), ColumnValue.inc("paycoupons", coupon),
                    ColumnValue.inc("paymoney", money),
                    ColumnValue.inc("paycount", 1),
                    ColumnValue.mov("paytime", paytime),
                    ColumnValue.mov("firstpaymoney", money),
                    ColumnValue.mov("firstpaytime", paytime)));
            } else {
                detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.inc("coins", coin), ColumnValue.inc("diamonds", diamond), ColumnValue.inc("coupons", coupon),
                    ColumnValue.inc("paycoins", coin), ColumnValue.inc("paydiamonds", diamond), ColumnValue.inc("paycoupons", coupon),
                    ColumnValue.inc("paymoney", money),
                    ColumnValue.inc("paycount", 1),
                    ColumnValue.mov("paytime", paytime)));
            }
            int oldviplevel = user.getViplevel();
            user = vipService.checkUpgradeVipLevel(user);
            if (user.getViplevel() != oldviplevel) {
                userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("viplevel", user.getViplevel()));
                detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.mov("viplevel", user.getViplevel())));
            }
            if (!UserInfo.isRobot(userid)) {
                if (coin != 0) coinQueue.add(new UserCoinRecord(userid, 0, coin, 0L, user.getCoins(), paytime, "platf", "recharge", remark));
                if (diamond != 0) diamondQueue.add(new UserDiamondRecord(userid, (int) diamond, user.getDiamonds(), paytime, "platf", "recharge", remark));
                if (coupon != 0) couponQueue.add(new UserCouponRecord(userid, (int) coupon, user.getCoupons(), paytime, "platf", "recharge", remark));
                missionService.updatePlatfMissionPay(userid, money, paytime);
            }
        }
        logger.finer("user(" + userid + ").rechargeUserCoinDiamondCoupons: coin=" + coin + ", after usercoins=" + user.getCoins()
            + ", diamond=" + diamond + ", after userdiamonds=" + user.getDiamonds()
            + ", coupon=" + coupon + ", after usercoupons=" + user.getCoupons());
        syncGameQueue.add(user);
        return RetResult.success();
    }

    @Comment("充值金币&钻石&奖券")
    public RetResult rechargeUserCoins(final int userid, final long money, final long coin, final long paytime, String remark) {
        return rechargeUserCoinDiamondCoupons(userid, money, coin, 0, 0, paytime, remark);
    }

    @Comment("充值金币")
    public RetResult rechargeUserDiamonds(final int userid, final long money, final long diamond, final long paytime, String remark) {
        return rechargeUserCoinDiamondCoupons(userid, money, 0, diamond, 0, paytime, remark);
    }

    @Comment("充值奖券")
    public RetResult rechargeUserCoupons(final int userid, final long money, final long coupon, final long paytime, String remark) {
        return rechargeUserCoinDiamondCoupons(userid, money, 0, 0, coupon, paytime, remark);
    }

    @Comment("根据手机号码查找用户")
    public UserInfo findUserInfoByMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) return null;
        return userSource.find(UserInfo.class, FilterNode.create("mobile", IGNORECASEEQUAL, mobile));
    }

    @Comment("根据用户账号查找用户")
    public UserInfo findUserInfoByAccount(String account) {
        if (account == null || account.isEmpty()) return null;
        return userSource.find(UserInfo.class, FilterNode.create("account", IGNORECASEEQUAL, account));
    }

    @Comment("根据邮箱地址查找用户")
    public UserInfo findUserInfoByEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        return userSource.find(UserInfo.class, FilterNode.create("email", IGNORECASEEQUAL, email));
    }

    @Comment("根据微信绑定ID查找用户")
    public UserInfo findUserInfoByWxunionid(String wxunionid) {
        if (wxunionid == null || wxunionid.isEmpty()) return null;
        return userSource.find(UserInfo.class, FilterNode.create("wxunionid", EQUAL, wxunionid));
    }

    @Comment("根据QQ绑定ID查找用户")
    public UserInfo findUserInfoByQqunionid(String qqunionid) {
        if (qqunionid == null || qqunionid.isEmpty()) return null;
        return userSource.find(UserInfo.class, FilterNode.create("qqunionid", EQUAL, qqunionid));
    }

    @Comment("根据城信绑定ID查找用户")
    public UserInfo findUserInfoByCxunionid(String cxunionid) {
        if (cxunionid == null || cxunionid.isEmpty()) return null;
        return userSource.find(UserInfo.class, FilterNode.create("cxunionid", EQUAL, cxunionid));
    }

    @Comment("根据APP设备ID查找用户")
    public UserInfo findUserInfoByApptoken(String apptoken) {
        if (apptoken == null || apptoken.isEmpty()) return null;
        return userSource.find(UserInfo.class, FilterNode.create("apptoken", EQUAL, apptoken));
    }

    @Comment("查询用户列表， 通常用于后台管理系统查询")
    public Sheet<UserDetail> queryUserDetail(FilterNode node, Flipper flipper) {
        return userSource.querySheet(UserDetail.class, flipper, node);
    }

    @Comment("根据登录态获取当前用户信息")
    public UserInfo current(String sessionid) {
        Integer userid = sessions.getAndRefresh(sessionid, sessionExpireSeconds);
        return userid == null ? null : findUserInfo(userid);
    }

    @Comment("根据登录态获取当前用户信息")
    public CompletableFuture<Integer> currentUserid(String sessionid) {
        return sessions.getAndRefreshAsync(sessionid, sessionExpireSeconds);
    }

    public UserInfo reloadUserInfo(int userid) {
        UserDetail user = userSource.find(UserDetail.class, userid);
        if (user == null) return null;
        userSource.delete(UserInfo.class, userid);
        UserInfo info = user.createUserInfo();
        userSource.insert(info);
        return info;
    }

    @Comment("发送短信验证码")
    public RetResult smscode(int userid, final short type, String mobile) {
        if (mobile == null) return new RetResult(RET_USER_MOBILE_ILLEGAL, type + " mobile is null"); //手机号码无效
        if (mobile.indexOf('+') == 0) mobile = mobile.substring(1);
        UserInfo info = findUserInfoByMobile(mobile);
        if (type == RandomCode.TYPE_SMSREG || type == RandomCode.TYPE_SMSMOB) { //手机注册或手机修改的号码不能已存在
            if (info != null) return retResult(RET_USER_MOBILE_EXISTS);
        } else if (type == RandomCode.TYPE_SMSPWD) { //修改密码
            if (info == null) return retResult(RET_USER_MOBILE_ILLEGAL);
        } else if (type == RandomCode.TYPE_SMSBAK) { //修改银行密码
            if (info == null) return retResult(RET_USER_MOBILE_ILLEGAL);
        } else if (type == RandomCode.TYPE_SMSLGN) { //手机登录
            if (info == null) return retResult(RET_USER_MOBILE_ILLEGAL);
        } else if (type == RandomCode.TYPE_SMSODM) { //原手机
            if (info == null) return retResult(RET_USER_MOBILE_ILLEGAL);
        } else {
            return retResult(RET_PARAMS_ILLEGAL);
        }
        List<RandomCode> codes = randomCodeService.queryRandomCodeByMobile(mobile);
        if (!codes.isEmpty()) {
            RandomCode last = codes.get(codes.size() - 1);
            if (last.getCreatetime() + 60 * 1000 > System.currentTimeMillis()) return RetCodes.retResult(RET_USER_MOBILE_SMSFREQUENT);
        }
        final int smscode = smsService.isDebug() ? 123456 : RandomCode.randomSmsCode();
        try {
            if (!smsService.sendRandomSmsCode(type, mobile, smscode)) return retResult(RET_USER_MOBILE_SMSFREQUENT);
        } catch (Exception e) {
            logger.log(Level.WARNING, "mobile(" + mobile + ", type=" + type + ") send smscode " + smscode + " error", e);
            return retResult(RET_USER_MOBILE_SMSFREQUENT);
        }
        if (smsService.isDebug() && randomCodeService.existsRandomCode(mobile + "-" + smscode)) {
            randomCodeService.freshRandomCodeTime(mobile + "-" + smscode, type);
            return RetResult.success();
        }
        RandomCode code = new RandomCode();
        code.setUserid(userid);
        code.setCreatetime(System.currentTimeMillis());
        if (info != null) code.setUserid(info.getUserid());
        code.setRandomcode(mobile + "-" + smscode);
        code.setType(type);
        randomCodeService.createRandomCode(code);
        return RetResult.success();
    }

    @Comment("QQ登录")
    public RetResult<UserInfo> qqlogin(LoginQQBean bean) {
        try {
            String url = "https://graph.qq.com/user/get_user_info?oauth_consumer_key=" + qqappid + "&access_token=" + bean.getAccesstoken() + "&openid=" + bean.getOpenid() + "&format=json";
            String json = Utility.getHttpContent(url);
            if (finest) logger.finest(url + "--->" + json);
            Map<String, String> jsonmap = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, json);
            if (!"0".equals(jsonmap.get("ret"))) return RetCodes.retResult(RET_USER_QQID_INFO_FAIL);
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByQqunionid(bean.getOpenid());
            if (user == null) {
                UserDetail detail = new UserDetail();
                detail.setUsername(jsonmap.getOrDefault("nickname", "qq-user"));
                detail.setQqunionid(bean.getOpenid());
                detail.setRegagent(bean.getLoginagent());
                detail.setRegaddr(bean.getLoginaddr());
                detail.setRegnetmode(bean.getNetmode());
                detail.setApposid(bean.getApposid());
                detail.setAppos(bean.getAppos());
                detail.setApptoken(bean.getApptoken());
                detail.setRegtype(REGTYPE_QQOPEN);
                String genstr = jsonmap.getOrDefault("gender", "");
                detail.setGender("男".equals(genstr) ? UserInfo.GENDER_MALE : ("女".equals(genstr) ? UserInfo.GENDER_FEMALE : (short) 0));
                String headimgurl = jsonmap.get("figureurl_qq_2");
                if (headimgurl != null) detail.setFace(headimgurl);
                if (finer) logger.fine(bean + " --qqlogin-->" + convert.convertTo(jsonmap));
                rr = register(detail, false);
                if (rr.isSuccess()) {
                    rr.setRetinfo(jsonmap.get(bean.getOpenid()));
//                    if (headimgurl != null) {
//                        super.runAsync(() -> {
//                            try {
//                                byte[] bytes = Utility.getHttpBytesContent(headimgurl);
//                                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
//                                fileService.storeFace(rr.getResult().getUserid(), image);
//                            } catch (Exception e) {
//                                logger.log(Level.INFO, "qqlogin get headimgurl fail (" + rr.getResult() + ", " + jsonmap + ")", e);
//                            }
//                        });
//                    }
                }
            } else {
                rr = new RetResult<>(user);
                rr.setRetinfo(jsonmap.get(bean.getOpenid()));
            }
            if (rr.isSuccess()) {
                this.createUserLoginRecord(user, bean);
                this.sessions.set(sessionExpireSeconds, bean.getSessionid(), rr.getResult().getUserid());
                //APP的苹果审批版本
                rr.attach("iosver", dictService.findDictValue(DictInfo.PLATF_APP_IOS_AUDIT_VERSION, "0.0.0"));
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "qqlogin failed (" + bean + ")", e);
            return RetCodes.retResult(RET_USER_LOGIN_FAIL);
        }
    }

    @Comment("微信登陆")
    public RetResult<UserInfo> wxlogin(LoginWXBean bean) {
        try {
            Map<String, String> wxmap = bean.emptyAccesstoken()
                ? wxMPService.getMPUserTokenByCode(bean.getApposid(), bean.getCode())
                : wxMPService.getMPUserTokenByOpenid(bean.getAccesstoken(), bean.getOpenid());
            final String unionid = wxmap.get("unionid");
            if (unionid == null) return RetCodes.retResult(RET_USER_WXID_ILLEGAL);
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByWxunionid(unionid);
            if (user == null && bean.getUserid() > 0) user = findUserInfo(bean.getUserid());
            if (user == null) {
                if (!bean.isAutoreg()) return new RetResult<>(0, convert.convertTo(wxmap));
                UserDetail detail = new UserDetail();
                detail.setUsername(wxmap.getOrDefault("nickname", "wx-user"));
                detail.setWxunionid(unionid);
                detail.setApposid(bean.getApposid());
                detail.setAppos(bean.getAppos());
                detail.setApptoken(bean.getApptoken());
                detail.setRegtype(REGTYPE_WEIXIN);
                detail.setRegagent(bean.getLoginagent());
                detail.setRegaddr(bean.getLoginaddr());
                detail.setRegnetmode(bean.getNetmode());
                detail.setGender((short) (Short.parseShort(wxmap.getOrDefault("sex", "0")) * 2));
                String headimgurl = wxmap.get("headimgurl");
                if (headimgurl != null) detail.setFace(headimgurl);
                logger.fine(bean + " --wxlogin-->" + convert.convertTo(wxmap));
                rr = register(detail, false);
                if (rr.isSuccess()) {
                    user = rr.getResult();
                    rr.setRetinfo(wxmap.get("openid"));
//                    if (headimgurl != null) { 
//                        super.runAsync(() -> {
//                            try {
//                                byte[] bytes = Utility.getHttpBytesContent(headimgurl);
//                                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
//                                fileService.storeFace(rr.getResult().getUserid(), image);
//                            } catch (Exception e) {
//                                logger.log(Level.INFO, "wxlogin get headimgurl fail (" + rr.getResult() + ", " + wxmap + ")", e);
//                            }
//                        });
//                    }
                }
            } else {
                if (!Objects.equals(user.getWxunionid(), unionid)) {
                    user.setWxunionid(unionid);
                    userSource.updateColumn(UserDetail.class, user.getUserid(), "wxunionid", user.getWxunionid());
                    userSource.updateColumn(UserInfo.class, user.getUserid(), "wxunionid", user.getWxunionid());
                }
                if (!Objects.equals(user.getUsername(), wxmap.getOrDefault("nickname", "wx-user"))) {
                    user.setUsername(wxmap.getOrDefault("nickname", "wx-user"));
                    userSource.updateColumn(UserDetail.class, user.getUserid(), "username", user.getUsername());
                    userSource.updateColumn(UserInfo.class, user.getUserid(), "username", user.getUsername());
                }
                if (!Objects.equals(user.getGender(), (short) (Short.parseShort(wxmap.getOrDefault("sex", "0")) * 2))) {
                    user.setGender((short) (Short.parseShort(wxmap.getOrDefault("sex", "0")) * 2));
                    userSource.updateColumn(UserDetail.class, user.getUserid(), "gender", user.getGender());
                    userSource.updateColumn(UserInfo.class, user.getUserid(), "gender", user.getGender());
                }
                if (!Objects.equals(user.getFace(), wxmap.getOrDefault("headimgurl", ""))) {
                    user.setFace(wxmap.getOrDefault("headimgurl", ""));
                    userSource.updateColumn(UserDetail.class, user.getUserid(), "face", user.getFace());
                    userSource.updateColumn(UserInfo.class, user.getUserid(), "face", user.getFace());
                }
                rr = new RetResult<>(user);
                rr.setRetinfo(wxmap.get("openid"));

                if (!Objects.equals(user.getApptoken(), bean.getApptoken())) {
                    user.setAppos(bean.getAppos());
                    user.setApptoken(bean.getApptoken());
                    userSource.updateColumn(UserDetail.class, user.getUserid(), ColumnValue.mov("appos", bean.getAppos()), ColumnValue.mov("apptoken", bean.getApptoken()));
                    userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("appos", bean.getAppos()), ColumnValue.mov("apptoken", bean.getApptoken()));
                }
            }
            if (rr.isSuccess()) {
                this.createUserLoginRecord(user, bean);
                this.sessions.set(sessionExpireSeconds, bean.getSessionid(), rr.getResult().getUserid());
                //APP的苹果审批版本
                rr.attach("iosver", dictService.findDictValue(DictInfo.PLATF_APP_IOS_AUDIT_VERSION, "0.0.0"));
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "wxlogin failed (" + bean + ")", e);
            return RetCodes.retResult(RET_USER_LOGIN_FAIL);
        }
    }

    @Comment("用户密码登录")
    public RetResult<UserInfo> login(LoginBean bean) {
        final boolean lfinest = logger.isLoggable(Level.FINEST);
        final String beanstr = String.valueOf(bean);
        if (lfinest) logger.finest("platf.login.bean = " + beanstr + ", blackIpList checking");
        if (blackIpList != null && blackIpList.contains(bean.getLoginaddr())) {
            blackService.insertUserBlackRecord(UserBlackItem.BLACKTYPE_IP, bean.getLoginaddr(), bean.getAccount());
            return RetCodes.retResult(RET_USER_LOGINORREG_LIMIT);
        }
        if (blackApptokenList != null && blackApptokenList.contains(bean.getApptoken())) {
            blackService.insertUserBlackRecord(UserBlackItem.BLACKTYPE_APPTOKEN, bean.getApptoken(), bean.getAccount());
            return RetCodes.retResult(RET_USER_LOGINORREG_LIMIT);
        }
        UserInfo user = null;
        boolean unok = true;
        if (bean != null && !bean.emptyCookieinfo() && bean.emptyAccount()) {
            String cookie;
            try {
                cookie = decryptAES(bean.getCookieinfo());
            } catch (Exception e) {
                return retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
            }
            if (lfinest) logger.finest("platf.login.bean = " + beanstr + ", cookie = " + cookie);
            int sharp = cookie.indexOf('#');
            if (sharp > 0) bean.setApptoken(cookie.substring(0, sharp));
            int pos = cookie.indexOf('$');
            int userid = Integer.parseInt(cookie.substring(sharp + 1, pos), 36);
            if (lfinest) logger.finest("platf.login.bean = " + beanstr + ", userid = " + userid);
            user = this.findUserInfo(userid);
            if (user != null) {
                char type = cookie.charAt(pos + 1);
                int wen = cookie.indexOf('?');
                String val = wen > 0 ? cookie.substring(pos + 2, wen) : cookie.substring(pos + 2);
                if (type == '0') { //密码
                    bean.setPassword(val);
                } else if (type == '1') { //游客
                    if (!user.getAccount().isEmpty()) {
                        unok = !Objects.equals(val, (user.getAccount()));
                    }
                } else if (type == '2') { //微信
                    if (!user.getWxunionid().isEmpty()) {
                        unok = !Objects.equals(val, (user.getWxunionid()));
                    }
                } else if (type == '3') { //QQ
                    if (!user.getQqunionid().isEmpty()) {
                        unok = !Objects.equals(val, (user.getQqunionid()));
                    }
                } else if (type == '4') { //城信
                    if (!user.getCxunionid().isEmpty()) {
                        unok = !Objects.equals(val, (user.getCxunionid()));
                    }
                }
            }
        }
        if (bean == null || bean.emptySessionid() || (user == null && bean.emptyAccount())) return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
        String key = "";
        if (user == null && !bean.emptyAccount()) {
            if (bean.getAccount().indexOf('@') > 0) {
                key = "email";
                user = findUserInfoByEmail(bean.getAccount());
            } else if ((bean.getAccount().length() == 11 || bean.getAccount().length() == 13) && Character.isDigit(bean.getAccount().charAt(0))) {
                key = "mobile";
                user = findUserInfoByMobile(bean.getAccount());
            } else if (bean.getAccount().length() > 32) {
                key = "guest";
                user = findUserInfoByAccount(bean.getAccount());
            } else {
                key = "account";
                user = findUserInfoByAccount(bean.getAccount());
            }
        }
        if (user == null && "guest".equals(key)) {
            UserDetail detail = new UserDetail();
            detail.setAccount(bean.getAccount());
            detail.setPassword(bean.getPassword());
            detail.setRegagent(bean.getLoginagent());
            detail.setRegaddr(bean.getLoginaddr());
            detail.setRegnetmode(bean.getNetmode());
            detail.setRegapptoken(bean.getApptoken());
            detail.setRegtype(REGTYPE_VISITOR);
            if (lfinest) logger.finest("platf.login.bean = " + beanstr + ", registering ");
            RetResult<UserInfo> regresult = register(detail, false);
            if (lfinest) logger.finest("platf.login.bean = " + beanstr + ", register = " + regresult);
            if (!regresult.isSuccess()) return regresult;
            user = regresult.getResult();
        }
        final RetResult<UserInfo> result = new RetResult();
        if (user == null) return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
        if (user.isFrobid()) return RetCodes.retResult(RET_USER_FREEZED);
        if (user.isRobot()) return RetCodes.retResult(RET_USER_FREEZED); //机器人不容许登陆
        if (user.getStatus() == STATUS_CLOSED || user.getStatus() == STATUS_DELETED || user.getStatus() == STATUS_EXPIRE) {
            return RetCodes.retResult(RET_USER_STATUS_ILLEGAL);
        }
        if (unok && !user.getPassword().equals(digestPassword(bean.getPassword()))) {
            user.setPwdillcount(user.getPwdillcount() + 1);
            detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.inc("pwdillcount", 1)));
            userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.inc("pwdillcount", 1));
            return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL); //用户或密码错误   
        }
        if (!"web".equals(bean.getAppos()) && !user.getApptoken().equals(bean.getApptoken())) { //用户设备变更了
            user.setAppos(bean.getAppos());
            user.setApptoken(bean.getApptoken());
            detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.mov("appos", bean.getAppos()), ColumnValue.mov("apptoken", bean.getApptoken())));
            userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("appos", bean.getAppos()), ColumnValue.mov("apptoken", bean.getApptoken()));
        }
        if (user.getPwdillcount() != 0) {
            user.setPwdillcount(0);
            detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.mov("pwdillcount", user.getPwdillcount())));
            userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("pwdillcount", user.getPwdillcount()));
        }
        result.setRetcode(0);
        result.setResult(user);
        this.createUserLoginRecord(user, bean);
        if (lfinest) logger.finest("platf.login.bean = " + beanstr + ", createUserLoginRecord end");
        this.sessions.set(sessionExpireSeconds, bean.getSessionid(), result.getResult().getUserid());
        if (lfinest) logger.finest("platf.login.bean = " + beanstr + ", set sessionid");
        //APP的苹果审批版本
        result.attach("iosver", dictService.findDictValue(DictInfo.PLATF_APP_IOS_AUDIT_VERSION, "0.0.0"));
        return result;
    }

    private void createUserLoginRecord(UserInfo user, LoginAbstractBean bean) {
        boolean nogame = false;
        String roomlevelstr = "-1";
        if (user.getCurrgame() != null && !user.getCurrgame().isEmpty()) {
            Map<String, Map<String, InetSocketAddress>> moduleMap = moduleService.loadModuleMap();
            Map<String, InetSocketAddress> addrMap = moduleMap.get(user.getCurrgame());
            if (addrMap != null) {
                final String module = user.getCurrgame();
                final int sub = module.indexOf('_');
                String url = null;
                String uri = "/pipes/" + (sub > 0 ? module.substring(sub + 1) : module) + "/getPlayingRoomLevel?userid=" + user.getUserid();

                for (InetSocketAddress addr : addrMap.values()) {
                    try {
                        url = "http://" + addr.getHostString() + ":" + addr.getPort() + uri;
                        roomlevelstr = Utility.getHttpContent(url, 1000);
                        if (logger.isLoggable(Level.FINEST)) logger.finest("platf.url = " + url + ", result:" + roomlevelstr);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, url + " remote error", e);
                    }
                }
                nogame = roomlevelstr.indexOf('-') >= 0;
            }
        }
        if (nogame) {
            logger.log(Level.INFO, user + " is not gaming");
        } else if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, user + " is in gaming " + user.getCurrgame() + " and roomlevel =" + roomlevelstr);
        }
        UserLoginRecord record = bean.createUserLoginRecord(user);
        loginQueue.add(record);
        int loginseries = Math.max(1, user.getLoginseries());
        long midnight = Utility.midnight();
        if (user.getLastlogintime() < midnight) {
            if (midnight - user.getLastlogintime() <= 1 * 24 * 60 * 60 * 1000L) {
                loginseries++;
            } else {
                loginseries = 1;
            }
        }
        user.setLoginseries(loginseries);
        user.setLastlogintime(record.getCreatetime());
        missionService.updatePlatfMissionLogin(user.getUserid(), record.getCreatetime());
        orderPeriodService.letterOrderPeriod(user.getUserid());
        if (nogame) {
            user.setCurrgame("");
            detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.mov("lastloginaddr", bean.getLoginaddr()), ColumnValue.mov("lastloginlongitude", bean.getLongitude()), ColumnValue.mov("lastloginlatitude", bean.getLatitude()), ColumnValue.mov("lastloginstreet", bean.getStreet()), ColumnValue.mov("lastlogintime", user.getLastlogintime()), ColumnValue.mov("loginseries", user.getLoginseries()), ColumnValue.mov("currgame", "")));
            userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("lastlogintime", user.getLastlogintime()), ColumnValue.mov("loginseries", user.getLoginseries()), ColumnValue.mov("currgame", ""));
        } else {
            detailQueue.add(new UserUpdateEntry(user.getUserid(), ColumnValue.mov("lastloginaddr", bean.getLoginaddr()), ColumnValue.mov("lastloginlongitude", bean.getLongitude()), ColumnValue.mov("lastloginlatitude", bean.getLatitude()), ColumnValue.mov("lastloginstreet", bean.getStreet()), ColumnValue.mov("lastlogintime", user.getLastlogintime()), ColumnValue.mov("loginseries", user.getLoginseries())));
            userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("lastlogintime", user.getLastlogintime()), ColumnValue.mov("loginseries", user.getLoginseries()));
        }
    }

    @Comment("用户注册")
    public RetResult<UserInfo> register(UserDetail user, boolean testAccount) {
        if (blackIpList != null && blackIpList.contains(user.getRegaddr())) {
            blackService.insertUserBlackRecord(UserBlackItem.BLACKTYPE_IP, user.getRegaddr(), user.getAccount());
            return RetCodes.retResult(RET_USER_LOGINORREG_LIMIT);
        }
        if (blackApptokenList != null && blackApptokenList.contains(user.getRegapptoken())) {
            blackService.insertUserBlackRecord(UserBlackItem.BLACKTYPE_APPTOKEN, user.getRegapptoken(), user.getAccount());
            return RetCodes.retResult(RET_USER_LOGINORREG_LIMIT);
        }
        synchronized (regLock) {
            RetResult<UserInfo> result = new RetResult();
            if (user == null) return RetCodes.retResult(RET_USER_SIGNUP_ILLEGAL);
            if (!testAccount && user.getMobile().isEmpty() && user.getAccount().isEmpty()
                && user.getEmail().isEmpty() && user.getWxunionid().isEmpty()
                && user.getQqunionid().isEmpty()) return RetCodes.retResult(RET_USER_SIGNUP_ILLEGAL);
            short gender = user.getGender();
            if (gender != 0 && gender != GENDER_MALE && gender != GENDER_FEMALE) return RetCodes.retResult(RET_USER_GENDER_ILLEGAL);
            int retcode = 0;
            if (!user.getAccount().isEmpty() && (retcode = checkAccount(user.getAccount())) != 0) return RetCodes.retResult(retcode);
            if (!user.getMobile().isEmpty() && (retcode = checkMobile(user.getMobile())) != 0) return RetCodes.retResult(retcode);
            if (!user.getEmail().isEmpty() && (retcode = checkEmail(user.getEmail())) != 0) return RetCodes.retResult(retcode);
            if (!user.getWxunionid().isEmpty() && (retcode = checkWxunionid(user.getWxunionid())) != 0) return RetCodes.retResult(retcode);
            if (!user.getQqunionid().isEmpty() && (retcode = checkQqunionid(user.getQqunionid())) != 0) return RetCodes.retResult(retcode);
            if (!user.getCxunionid().isEmpty() && (retcode = checkCxunionid(user.getCxunionid())) != 0) return RetCodes.retResult(retcode);
            if (!user.getMobile().isEmpty()) {
                user.setRegtype(REGTYPE_MOBILE);
                if (user.getPassword().isEmpty()) return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
            } else if (!user.getEmail().isEmpty()) {
                user.setRegtype(REGTYPE_EMAIL);
                if (user.getPassword().isEmpty()) return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
            } else if (!user.getWxunionid().isEmpty()) {
                user.setRegtype(REGTYPE_WEIXIN);
            } else if (!user.getQqunionid().isEmpty()) {
                user.setRegtype(REGTYPE_QQOPEN);
            } else if (!user.getCxunionid().isEmpty()) {
                user.setRegtype(REGTYPE_CXOPEN);
            } else if (user.getAccount().length() > 32) {
                user.setRegtype(REGTYPE_VISITOR);
            } else {
                user.setRegtype(REGTYPE_ACCOUNT);
                if (user.getPassword().isEmpty()) return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
            }
            RetResult rs = checkAgency(user, user.getAgencyid(), 0);
            if (!rs.isSuccess()) return rs;
            user.setRegtime(System.currentTimeMillis());

            user.setUpdatetime(0);
            if (!user.getPassword().isEmpty()) {
                user.setPassword(digestPassword(user.getPassword()));
            }
            user.setCurrgame("");
            user.setStatus(UserInfo.STATUS_NORMAL);
            int coins = dictService.findDictValue(DictInfo.PLATF_USER_REG_GIFT_COIN, 1000);
            user.setCoins(coins);
            user.setRegcoins(coins);
            user.setGiftcoins(coins);
            int diamond = dictService.findDictValue(DictInfo.PLATF_USER_REG_GIFT_DIAMOND, 0);
            user.setDiamonds(diamond);
            user.setRegdiamonds(diamond);
            user.setGiftdiamonds(diamond);
            user.setRegapptoken(user.getApptoken());
            if (user.getAgencyid() > 0) user.setAgencytime(user.getRegtime());
            if ("web".equals(user.getAppos())) {
                user.setAppos("");
                user.setApptoken("");
            }
            FilterNode node = null;
            if (testAccount) node = FilterNode.create("userid", FilterExpress.LESSTHAN, UserInfo.MIN_NORMAL_USERID).and("userid", FilterExpress.GREATERTHAN, UserInfo.USERID_MINTEST);
            int defid = testAccount ? UserInfo.USERID_MINTEST : UserInfo.USERID_SYSTEM;
            int maxid = userSource.getNumberResult(UserDetail.class, FilterFunc.MAX, defid, "userid", node).intValue();
            boolean ok = false;
            if (user.getUserid() > 0 && userSource.exists(UserInfo.class, user.getUserid())) return RetCodes.retResult(RET_USER_SIGNUP_ILLEGAL);
            for (int i = 0; i < 50; i++) {
                if (user.getUserid() > 0) {
                    if (user.getUsername().isEmpty()) user.setUsername("玩家" + Integer.toString(user.getUserid(), 36).toUpperCase());
                    userSource.insert(user);
                    ok = true;
                    break;
                } else if (!testAccount) { //随机生成userid版本
                    try {
                        user.setUserid(300_0000 + idRandom.nextInt(500_0000));
                        if (userSource.exists(UserInfo.class, user.getUserid())) continue;
                        if (user.getUsername().isEmpty()) user.setUsername("玩家" + Integer.toString(user.getUserid(), 36).toUpperCase());
                        userSource.insert(user);
                        ok = true;
                        break;
                    } catch (Exception e) { //并发时可能会重复创建， 忽略异常
                        logger.log(Level.INFO, "create userdetail error: " + user, e);
                        maxid = userSource.getNumberResult(UserDetail.class, FilterFunc.MAX, UserInfo.USERID_SYSTEM, "userid", (FilterNode) null).intValue();
                    }
                } else {  //自增长userid版本
                    try {
                        user.setUserid(maxid + 1);
                        if (testAccount) user.setAccount("test" + user.getUserid() % 100000);
                        if (user.getUsername().isEmpty()) user.setUsername("玩家" + Integer.toString(user.getUserid(), 36).toUpperCase());
                        userSource.insert(user);
                        ok = true;
                        break;
                    } catch (Exception e) { //并发时可能会重复创建， 忽略异常
                        logger.log(Level.INFO, "create userdetail error: " + user, e);
                        maxid = userSource.getNumberResult(UserDetail.class, FilterFunc.MAX, UserInfo.USERID_SYSTEM, "userid", (FilterNode) null).intValue();
                    }
                }
            }
            if (!ok) return RetCodes.retResult(RET_PARAMS_ILLEGAL);
            if (user.getAgencyid() > 0) {
                dayrecordQueue.add(new DayRecordUpdateEntry(false, user.getAgencyid(), user.getRegtime(), ColumnValue.inc("childcount", 1)));
            }
            if (!UserInfo.isRobot(user.getUserid())) {
                if (coins != 0) coinQueue.add(new UserCoinRecord(user.getUserid(), 0, coins, 0L, user.getCoins(), user.getRegtime(), "platf", "reg", "注册赠送"));
                if (diamond != 0) diamondQueue.add(new UserDiamondRecord(user.getUserid(), (int) diamond, user.getDiamonds(), user.getRegtime(), "platf", "reg", "注册赠送"));
                try {
                    long now = System.currentTimeMillis();
                    missionService.insertMissionOnceRecord(user.getUserid(), now);
                    livenessService.insertLivenessRewardRecord(user.getUserid(), now);
                } catch (Exception regex) {
                    logger.log(Level.SEVERE, user.getUserid() + " insertMissionRecord error", regex);
                }
            }
            //------------------------扩展信息-----------------------------
            UserInfo info = user.createUserInfo();
            userSource.insert(info);
            result.setResult(info);
            //可以在此处给企业微信号推送注册消息
            return result;
        }
    }

    @Comment("注销登录")
    public boolean logout(final String sessionid) {
        UserInfo user = current(sessionid);
        if (user != null) {
            logoutQueue.add(new UserLogoutEntry(user.getUserid(), sessionid));
        }
        sessions.remove(sessionid);
        return true;
    }

    public RetResult enterGame(int userid, String currgame, long time) {
        return updateCurrgame(findUserInfo(userid), currgame, time);
    }

    public RetResult leaveGame(String currgame, int... userids) {
        if (currgame == null) currgame = "";
        for (int userid : userids) {
            UserInfo user = findUserInfo(userid);
            if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
            if (user.getCurrgame() == null || user.getCurrgame().isEmpty()) {
                //logger.log(Level.WARNING, "UserSerivce.leaveGame: userid=" + (user == null ? 0 : user.getUserid()) + ", leavinggame=" + (currgame.isEmpty() ? "''" : currgame) + ", but nowcurrgame is empty");
                continue;
            }
            if (!currgame.equals(user.getCurrgame())) {
                logger.log(Level.WARNING, "UserSerivce.leaveGame: userid=" + user.getUserid() + ", leavinggame=" + (currgame.isEmpty() ? "''" : currgame) + ", but nowcurrgame=" + user.getCurrgame());
                continue;
            }
            updateCurrgame(user, "", 0);
        }
        return RetResult.success();
    }

    public String getCurrgame(int userid) {
        UserInfo user = findUserInfo(userid);
        return user == null ? null : user.getCurrgame();
    }

    protected RetResult updateCurrgame(UserInfo user, String currgame, long time) {
        if (currgame == null) currgame = "";
        if (finest && (user == null || !user.isRobot())) {
            logger.log(Level.FINEST, "UserSerivce.updateCurrgame: userid=" + (user == null ? 0 : user.getUserid()) + ", currgame=" + (currgame.isEmpty() ? "''" : currgame) + ", oldgame=" + (user == null ? "''" : (user.getCurrgame().isEmpty() ? "''" : user.getCurrgame())));
        }
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (!currgame.isEmpty() && currgame.equals(user.getCurrgame())) {
            logger.log(Level.WARNING, "UserSerivce.enterGame: userid=" + user.getUserid() + ", currgame=" + currgame + " repeat!");
            return RetResult.success();
        }
        user.setCurrgame(currgame);
        user.setCurrgamingtime(currgame.isEmpty() ? 0L : (time > 0 ? time : System.currentTimeMillis()));
        userSource.updateColumn(user, "currgame", "currgamingtime");
        if (!user.isRobot()) {
            currgameQueue.add(new UserCurrgameEntry(currgame, user.getCurrgamingtime(), user.getUserid()));
        }
        return RetResult.success();
    }

    @Comment("绑定微信号")
    public RetResult<UserInfo> updateWxunionid(UserInfo user, String code) {
        try {
            if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
            Map<String, String> wxmap = wxMPService.getMPUserTokenByCode(code);
            final String wxunionid = wxmap.get("unionid");
            if (wxunionid == null || wxunionid.isEmpty()) return RetCodes.retResult(RET_USER_WXID_ILLEGAL);
            if (checkWxunionid(wxunionid) != 0) return RetCodes.retResult(RET_USER_WXID_EXISTS);
            userSource.updateColumn(UserDetail.class, user.getUserid(), "wxunionid", wxunionid);
            userSource.updateColumn(UserInfo.class, user.getUserid(), "wxunionid", wxunionid);
            user.setWxunionid(wxunionid);
            return new RetResult<>(user);
        } catch (Exception e) {
            logger.log(Level.FINE, "updateWxunionid failed (" + user + ", " + code + ")", e);
            return RetCodes.retResult(RET_USER_WXID_BIND_FAIL);
        }
    }

    public RetResult updateApptoken(int userid, String appos, String apptoken) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (appos == null) appos = "";
        if (apptoken == null) apptoken = "";
        userSource.updateColumn(UserDetail.class, user.getUserid(), ColumnValue.mov("appos", appos.toLowerCase()), ColumnValue.mov("apptoken", apptoken));
        userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("appos", appos.toLowerCase()), ColumnValue.mov("apptoken", apptoken));
        user.setAppos(appos.toLowerCase());
        user.setApptoken(apptoken);
        return RetResult.success();
    }

    public RetResult<String> updateUsername(int userid, String newusername) {
        if (newusername == null || newusername.isEmpty()) return RetCodes.retResult(RET_USER_USERNAME_ILLEGAL);
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (user.getUsername().equals(newusername)) return RetResult.success();
        UserDetail ud = new UserDetail();
        ud.setUserid(userid);
        ud.setUsername(newusername);
        userSource.updateColumn(ud, "username");
        user.setUsername(newusername);
        userSource.updateColumn(user, "username");
        syncRemoteGameModule(user);
        return new RetResult<>(newusername);
    }

    public RetResult updateFaceAndGender(int userid, String face, short gender) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (gender != GENDER_MALE && gender != GENDER_FEMALE) return RetCodes.retResult(RET_USER_GENDER_ILLEGAL);
        if (user.getFace().equals(face)) return RetResult.success();
        UserDetail ud = new UserDetail();
        ud.setUserid(userid);
        ud.setFace(face);
        ud.setGender(gender);
        userSource.updateColumn(ud, "face", "gender");
        user.setFace(face);
        user.setGender(gender);
        userSource.updateColumn(user, "face", "gender");
        syncRemoteGameModule(user);
        return RetResult.success();
    }

    public RetResult updateFace(int userid, String face) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (user.getFace().equals(face)) return RetResult.success();
        UserDetail ud = new UserDetail();
        ud.setUserid(userid);
        ud.setFace(face);
        userSource.updateColumn(ud, "face");
        user.setFace(face);
        userSource.updateColumn(user, "face");
        syncRemoteGameModule(user);
        return RetResult.success();
    }

    public RetResult updateGender(int userid, short gender) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (gender != GENDER_MALE && gender != GENDER_FEMALE) return RetCodes.retResult(RET_USER_GENDER_ILLEGAL);
        userSource.updateColumn(UserDetail.class, user.getUserid(), "gender", gender);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "gender", gender);
        user.setGender(gender);
        syncRemoteGameModule(user);
        return RetResult.success();
    }

    public RetResult updateIntro(int userid, String intro) {
        if (intro == null) intro = "";
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        userSource.updateColumn(UserDetail.class, user.getUserid(), "intro", intro);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "intro", intro);
        user.setIntro(intro);
        syncRemoteGameModule(user);
        return RetResult.success();
    }

    public RetResult<Map<String, String>> updateShenfen(int userid, String shenfenname, String shenfenno) {
        if (shenfenname == null) shenfenname = "";
        if (shenfenno == null) shenfenno = "";
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        userSource.updateColumn(UserDetail.class, user.getUserid(), ColumnValue.mov("shenfenname", shenfenname), ColumnValue.mov("shenfenno", shenfenno));
        userSource.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("shenfenname", shenfenname), ColumnValue.mov("shenfenno", shenfenno));
        user.setShenfenname(shenfenname);
        user.setShenfenno(shenfenno);
        return new RetResult(Utility.ofMap("shenfenname2", user.getShenfenname2(), "shenfenno2", user.getShenfenno2()));
    }

    public RetResult updateBanklevel(int userid, short banklevel) {
        if (banklevel < 0) return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        userSource.updateColumn(UserDetail.class, user.getUserid(), "banklevel", banklevel);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "banklevel", banklevel);
        user.setBanklevel(banklevel);
        syncRemoteGameModule(user);
        return RetResult.success();
    }

    public RetResult updateType(int userid, short type) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (type != USER_TYPE_PLAYER && type != USER_TYPE_AGENCY && type != USER_TYPE_MATE
            && type != (USER_TYPE_AGENCY + USER_TYPE_PLAYER) && type != (USER_TYPE_MATE + USER_TYPE_PLAYER)
            && type != (USER_TYPE_AGENCY + USER_TYPE_MATE + USER_TYPE_PLAYER)) return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        if ((user.getType() & USER_TYPE_MATE) > 0 && (type & USER_TYPE_MATE) < 1) { //不能取消陪练
            return RetCodes.retResult(RET_USER_UNMATE_ILLEGAL);
        }
        userSource.updateColumn(UserDetail.class, user.getUserid(), "type", type);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "type", type);
        user.setType(type);
        syncRemoteGameModule(user);
        return RetResult.success();
    }

    private void syncRemoteGameModule(UserInfo user) {
        if (gatewayNodes == null) return;
        final int userid = user.getUserid();
        String currgame = null;
        if (!user.getCurrgame().isEmpty()) {
            currgame = user.getCurrgame();
            moduleService.remoteGameModule(userid, user.getCurrgame(), "notifyPlatfPlayer", null);
        }
        String gameids = dictService.findDictValue(DictInfo.PLATF_USER_MUSTSYNC_GAMEIDS, "");
        if (gameids.isEmpty()) return;
        for (String gameid : gameids.split(";")) {
            if (gameid.trim().isEmpty()) continue;
            if (currgame != null && currgame.equals(gameid.trim())) continue;
            moduleService.remoteGameModule(userid, gameid.trim(), "notifyPlatfPlayer", null);
        }
    }

    private UserInfo createRobotUserInfo() {
        UserInfo user = new UserInfo();
        user.setUserid(USERID_MINTEST - robotIndex.incrementAndGet());
        user.setUsername("电脑" + Integer.toString(user.getUserid(), 36));
        user.setStatus(STATUS_NORMAL);
        user.setCoins(1000_10000);
        user.setDiamonds(1000);
        user.setGender(user.getUserid() % 2 == 1 ? GENDER_MALE : GENDER_FEMALE);
        return user;
    }

    public UserInfo randomRobot() {
        if (robotUserids.isEmpty()) return null;
        return findUserInfo(robotUserids.get(random.nextInt(robotUserids.size())));
    }

    public List<UserInfo> randomRobot(String currgame, int size) {
        return randomRobot(currgame, null, size);
    }

    public List<UserInfo> randomRobot(String currgame, Range.LongRange range, int size) {
        if (currgame == null) currgame = "";
        if (size < 1) size = 1;
        List<UserInfo> rs = new ArrayList<>();
        FilterNode filter = robotNode;
        synchronized (robotLock) {
            int count = 0;
            if (range != null) {
                FilterNode firstFilter = range.getMax() <= 0 ? FilterNode.create("coins", FilterExpress.GREATERTHAN, range.getMin() * 2) : FilterNode.create("coins", range);
                filter = firstFilter.and(robotNode);
                count = userSource.getNumberResult(UserInfo.class, FilterFunc.COUNT, 0, null, filter).intValue();
            }
            if (count < size) {
                filter = robotNode;
                count = userSource.getNumberResult(UserInfo.class, FilterFunc.COUNT, 0, null, filter).intValue();
            }
            //if (logger.isLoggable(Level.FINEST)) logger.finest("UserService.randomRobot: count=" + count + ", limit=" + size);
            if (count > 0) {
                for (int i = 0; i < size && count > 0; i++) {
                    Flipper flipper = new Flipper(1, (int) (System.currentTimeMillis() % count));
                    List<UserInfo> list = userSource.queryList(UserInfo.class, flipper, filter);
                    for (UserInfo user : list) {
                        if (range != null && !range.test(user.getCoins())) {
                            long c = (range.getMax() < 0) ? range.getMin() * 10 : ((range.getMax() + range.getMin()) / 2 - user.getCoins());
                            //无需实际增加
                            //RetResult rr = increRobotCoins(user.getUserid(), c, 0, null);
                            //if (!rr.isSuccess()) continue;
                            user.increCoins(c);
                        } else if (user.getCoins() < 100_0000) {
                            long c = (random.nextInt(5) + 1) * 100_0000;
                            //无需实际增加
                            //RetResult rr = increRobotCoins(user.getUserid(), c, 0, null);
                            //if (!rr.isSuccess()) continue;
                            user.increCoins(c);
                        }
                        rs.add(user);
                        count--;
                    }
                }
            }
            long now = System.currentTimeMillis();
            if (!currgame.isEmpty()) {
                for (UserInfo user : rs) {
                    updateCurrgame(user, currgame, now);
                }
            }
        }
        return rs;
    }

    //precode 表示原手机号码收到的短信验证码，如果当前用户没有配置手机号码，则该值忽略
    public RetResult updateMobile(int userid, String newmobile, String vercode, String precode) {
        int retcode = checkMobile(newmobile);
        if (retcode != 0) return RetCodes.retResult(retcode);
        RandomCode code = userSource.find(RandomCode.class, newmobile + "-" + vercode);
        if (code == null) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        if (code.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);

        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        RandomCode rc = null;
        if (!user.getMobile().isEmpty()) {
            rc = userSource.find(RandomCode.class, user.getMobile() + "-" + precode);
            if (rc == null) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
            if (rc.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);
        }
        userSource.updateColumn(UserDetail.class, user.getUserid(), "mobile", newmobile);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "mobile", newmobile);
        user.setMobile(newmobile);
        code.setUserid(user.getUserid());
        userSource.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        userSource.delete(RandomCode.class, code.getRandomcode());
        if (rc != null) {
            userSource.insert(rc.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
            userSource.delete(RandomCode.class, rc.getRandomcode());
        }
        return RetResult.success();
    }

    //重置密码
    public RetResult<UserInfo> resetPwd(int userid, String newpwdmd5) {
        UserInfo user = findUserInfo(userid);
        final String newpwd = digestPassword(secondPasswordMD5(newpwdmd5));
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        userSource.updateColumn(UserDetail.class, user.getUserid(), "password", newpwd);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "password", newpwd);
        user.setPassword(newpwd);
        return new RetResult<>(user);
    }

    //重置密码
    public RetResult<UserInfo> resetBankPwd(int userid, String newbankpwdmd5) {
        UserInfo user = findUserInfo(userid);
        final String newbankpwd = digestPassword(secondPasswordMD5(newbankpwdmd5));
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        userSource.updateColumn(UserDetail.class, user.getUserid(), "bankpwd", newbankpwd);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "bankpwd", newbankpwd);
        user.setBankpwd(newbankpwd);
        return new RetResult<>(user);
    }

    //更改密码
    public RetResult<UserInfo> updatePwd(UserPwdBean bean) {
        UserInfo user = bean.getUser();
        if (user == null) user = bean.getSessionid() == null ? null : current(bean.getSessionid());
        final String newpwd = digestPassword(secondPasswordMD5(bean.getNewpwd())); //HEX-MD5(密码明文)
        if (user == null) {  //表示忘记密码后进行重置密码
            bean.setSessionid(null);
            String randomcode = bean.getRandomcode();
            if (randomcode == null || randomcode.isEmpty()) {
                if (bean.getAccount() != null && !bean.getAccount().isEmpty()
                    && bean.getVercode() != null && !bean.getVercode().isEmpty()) {
                    randomcode = bean.getAccount() + "-" + bean.getVercode();
                }
            }
            if (randomcode != null && !randomcode.isEmpty()) {
                RandomCode code = userSource.find(RandomCode.class, randomcode);
                if (code == null || code.getType() != RandomCode.TYPE_SMSPWD) return retResult(RET_USER_RANDCODE_ILLEGAL);
                if (code.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);

                user = findUserInfo((int) code.getUserid());
                if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);

                userSource.updateColumn(UserDetail.class, user.getUserid(), "password", newpwd);
                userSource.updateColumn(UserInfo.class, user.getUserid(), "password", newpwd);
                user.setPassword(newpwd);
                userSource.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
                userSource.delete(RandomCode.class, code.getRandomcode());
                return new RetResult<>(user);
            }
            return RetCodes.retResult(RET_USER_NOTEXISTS);
        }
        //用户或密码错误
        if (!Objects.equals(user.getPassword(), digestPassword(secondPasswordMD5(bean.getOldpwd())))) {
            return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);  //原密码错误
        }
        userSource.updateColumn(UserDetail.class, user.getUserid(), "password", newpwd);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "password", newpwd);
        user.setPassword(newpwd);
        return new RetResult<>(user);
    }

    //校验虚拟银行密码
    public RetResult checkBankPwd(UserPwdBean bean) {
        UserInfo user = bean.getUser();
        if (user == null) user = bean.getSessionid() == null ? null : current(bean.getSessionid());
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (!Objects.equals(user.getBankpwd(), digestPassword(secondPasswordMD5(bean.getOldpwd())))) {
            return RetCodes.retResult(RET_USER_BANKPWD_ILLEGAL);  //原密码错误
        }
        return RetResult.success();
    }

    //修改用户虚拟银行密码
    public RetResult updateBankPwd(UserPwdBean bean) {
        UserInfo user = bean.getUser();
        if (user == null) user = bean.getSessionid() == null ? null : current(bean.getSessionid());
        final String newpwd = digestPassword(secondPasswordMD5(bean.getNewpwd())); //HEX-MD5(密码明文)
        boolean smsver = false;
        if (user == null) {  //表示忘记密码后进行重置密码
            bean.setSessionid(null);
            String randomcode = bean.getRandomcode();
            if (randomcode == null || randomcode.isEmpty()) {
                if (bean.getAccount() != null && !bean.getAccount().isEmpty()
                    && bean.getVercode() != null && !bean.getVercode().isEmpty()) {
                    randomcode = bean.getAccount() + "-" + bean.getVercode();
                    smsver = true;
                }
            }
            if (randomcode != null && !randomcode.isEmpty()) {
                RandomCode code = userSource.find(RandomCode.class, randomcode);
                if (code == null || code.getType() != RandomCode.TYPE_SMSBAK) return retResult(RET_USER_RANDCODE_ILLEGAL);
                if (code.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);

                user = findUserInfo((int) code.getUserid());
                if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);

                userSource.updateColumn(UserDetail.class, user.getUserid(), "bankpwd", newpwd);
                userSource.updateColumn(UserInfo.class, user.getUserid(), "bankpwd", newpwd);
                user.setBankpwd(newpwd);
                userSource.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
                userSource.delete(RandomCode.class, code.getRandomcode());
                return new RetResult<>(user);
            }
            return RetCodes.retResult(RET_USER_NOTEXISTS);
        } else {
            String randomcode = bean.getRandomcode();
            if (randomcode == null || randomcode.isEmpty()) {
                if (bean.getAccount() != null && !bean.getAccount().isEmpty()
                    && bean.getVercode() != null && !bean.getVercode().isEmpty()) {
                    randomcode = bean.getAccount() + "-" + bean.getVercode();
                    smsver = true;
                }
            }
            if (randomcode != null && !randomcode.isEmpty()) {
                RandomCode code = userSource.find(RandomCode.class, randomcode);
                if (code == null || code.getType() != RandomCode.TYPE_SMSBAK) return retResult(RET_USER_RANDCODE_ILLEGAL);
                if (code.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);
            }
        }
        //用户或密码错误
        if (!smsver && !user.getBankpwd().isEmpty() && !Objects.equals(user.getBankpwd(), digestPassword(secondPasswordMD5(bean.getOldpwd())))) {
            return RetCodes.retResult(RET_USER_BANKPWD_ILLEGAL);  //原密码错误
        }
        userSource.updateColumn(UserDetail.class, user.getUserid(), "bankpwd", newpwd);
        userSource.updateColumn(UserInfo.class, user.getUserid(), "bankpwd", newpwd);
        user.setBankpwd(newpwd);
        return RetResult.success();
    }

    protected UserDetail findUserDetail(int userid) {
        return userSource.find(UserDetail.class, userid);
    }

    public RetResult<RandomCode> checkRandomCode(String targetid, String randomcode, short type) {
        if (randomcode == null || randomcode.isEmpty()) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        if (targetid != null && targetid.length() > 5 && randomcode.length() < 30) randomcode = targetid + "-" + randomcode;
        RandomCode code = userSource.find(RandomCode.class, randomcode);
        if (code != null && type > 0 && code.getType() != type) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        return code == null ? RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL) : (code.isExpired() ? RetCodes.retResult(RET_USER_RANDCODE_EXPIRED) : new RetResult<>(code));
    }

    public void removeRandomCode(RandomCode code) {
        userSource.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        userSource.delete(RandomCode.class, code.getRandomcode());
    }

    private static final Predicate<String> accountReg = Pattern.compile("^[a-zA-Z][\\w_.]{5,64}$").asPredicate();

    /**
     * 检测账号是否有效, 返回0表示手机号码可用
     * 账号不能以数字开头、不能包含@ ， 用于区分手机号码和邮箱
     *
     * @param account
     *
     * @return
     */
    public int checkAccount(String account) {
        if (account == null) return RET_USER_ACCOUNT_ILLEGAL;
        if (false && !accountReg.test(account)) return RET_USER_ACCOUNT_ILLEGAL;
        if (account.toLowerCase().startsWith("test")) return RET_USER_ACCOUNT_ILLEGAL;
        return userSource.exists(UserInfo.class, FilterNode.create("account", IGNORECASEEQUAL, account)) ? RET_USER_ACCOUNT_EXISTS : 0;
    }

    private static final Predicate<String> mobileReg = Pattern.compile("^\\d{11,13}$").asPredicate();

    /**
     * 检测手机号码是否有效, 返回0表示手机号码可用
     *
     * @param mobile
     *
     * @return
     */
    public int checkMobile(String mobile) {
        if (mobile == null) return RET_USER_MOBILE_ILLEGAL;
        if (!mobileReg.test(mobile)) return RET_USER_MOBILE_ILLEGAL;
        return userSource.exists(UserInfo.class, FilterNode.create("mobile", EQUAL, mobile)) ? RET_USER_MOBILE_EXISTS : 0;
    }

    private static final Predicate<String> emailReg = Pattern.compile("^(\\w|\\.|-)+@(\\w|-)+(\\.(\\w|-)+)+$").asPredicate();

    /**
     * 检测邮箱地址是否有效, 返回0表示邮箱地址可用.给新用户注册使用
     *
     * @param email
     *
     * @return
     */
    public int checkEmail(String email) {
        if (email == null) return RET_USER_EMAIL_ILLEGAL;
        if (!emailReg.test(email)) return RET_USER_EMAIL_ILLEGAL;
        return userSource.exists(UserInfo.class, FilterNode.create("email", IGNORECASEEQUAL, email)) ? RET_USER_EMAIL_EXISTS : 0;
    }

    public int checkWxunionid(String wxunionid) {
        if (wxunionid == null || wxunionid.isEmpty()) return 0;
        return userSource.exists(UserInfo.class, FilterNode.create("wxunionid", EQUAL, wxunionid)) ? RET_USER_WXID_EXISTS : 0;
    }

    public int checkQqunionid(String qqunionid) {
        if (qqunionid == null || qqunionid.isEmpty()) return 0;
        return userSource.exists(UserInfo.class, FilterNode.create("qqunionid", EQUAL, qqunionid)) ? RET_USER_QQID_EXISTS : 0;
    }

    public int checkCxunionid(String cxunionid) {
        if (cxunionid == null || cxunionid.isEmpty()) return 0;
        return userSource.exists(UserInfo.class, FilterNode.create("cxunionid", EQUAL, cxunionid)) ? RET_USER_CXID_EXISTS : 0;
    }

    //AES加密
    public static String encryptAES(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            synchronized (aesEncrypter) {
                return Utility.binToHexString(aesEncrypter.doFinal(value.getBytes()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //AES解密
    public static String decryptAES(String value) {
        if (value == null || value.isEmpty()) return value;
        byte[] hex = Utility.hexToBin(value);
        try {
            synchronized (aesEncrypter) {
                return new String(aesDecrypter.doFinal(hex));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //第一次MD5
    public static String firstPasswordMD5(String password) {
        byte[] bytes = password.getBytes();
        synchronized (md5) {
            bytes = md5.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

    //第二次MD5
    public static String secondPasswordMD5(String passwordoncemd5) {
        if (passwordoncemd5 == null || passwordoncemd5.isEmpty()) return passwordoncemd5;
        byte[] bytes = ("REDKALE-" + passwordoncemd5.trim().toLowerCase()).getBytes();
        synchronized (md5) {
            bytes = md5.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

    //第三次密码加密
    public static String digestPassword(String passwordtwicemd5) {
        if (passwordtwicemd5 == null || passwordtwicemd5.isEmpty()) return passwordtwicemd5;
        byte[] bytes = (passwordtwicemd5.trim().toLowerCase() + "-REDKALE").getBytes();
        synchronized (sha1) {
            bytes = sha1.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

    public RetResult updateAgencyid(int userid, int agencyid, int memberid) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        long now = System.currentTimeMillis();
        synchronized (regLock) {
            RetResult rs = checkAgency(user, agencyid, memberid);
            if (!rs.isSuccess()) return rs;
            UserDetail ud = new UserDetail();
            ud.setUserid(userid);
            ud.setAgencyid(agencyid);
            ud.setAgencytime(now);
            userSource.updateColumn(ud, "agencyid", "agencytime");
            user.setAgencyid(agencyid);
            user.setAgencytime(now);
            userSource.updateColumn(user, "agencyid", "agencytime");
        }
        syncRemoteGameModule(user);
        return RetResult.success();
    }

    //禁止循环嵌套代理
    private RetResult checkAgency(UserInfo user, int agencyid, int memberid) {
        if (agencyid == 0) return RetResult.success();
        if (memberid == 0 && user.getAgencyid() > 0) return RetCodes.retResult(RET_USER_AGENCY_REPEAT);
        if (user.getUserid() == agencyid) return RetCodes.retResult(RET_USER_AGENCY_REPEAT);
        UserInfo agency = findUserInfo(agencyid);
        if (agency == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (agency.getAgencyid() == 0) return RetResult.success();
        List<Integer> userids = new ArrayList<>();
        userids.add(user.getUserid());
        userids.add(agencyid);
        UserInfo subagency = agency;
        while (subagency != null && subagency.getAgencyid() > 0) {
            if (userids.contains(subagency.getAgencyid())) return RetCodes.retResult(RET_USER_AGENCY_REPEAT);
            userids.add(subagency.getAgencyid());
            subagency = findUserInfo(subagency.getAgencyid());
        }
        return RetResult.success();
    }

    /**
     * 修改用户的备注信息
     *
     * @param userid 用户id
     * @param remark 备注
     *
     * @return
     */
    public RetResult updateRemark(int userid, String remark) {
        if (remark == null) remark = "";
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        UserDetail ud = new UserDetail();
        ud.setUserid(userid);
        ud.setRemark(remark);
        userSource.updateColumn(ud, "remark");
        return RetResult.success();
    }

    /**
     * 更新用户状态
     *
     * @param userid 用户id
     * @param status 用户的新状态
     *
     * @return
     */
    public RetResult updateStatus(int userid, short status) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (user.getStatus() == status) return RetResult.success();
        if (status < 10 || status > 90 || status % 10 != 0) return RetCodes.retResult(RET_USER_STATUS_ILLEGAL);
        UserDetail ud = new UserDetail();
        ud.setUserid(userid);
        ud.setStatus(status);
        if (status == UserInfo.STATUS_NORMAL) ud.setPwdillcount(0);
        userSource.updateColumn(ud, "status", "pwdillcount");
        user.setStatus(status);
        if (status == UserInfo.STATUS_NORMAL) user.setPwdillcount(0);
        userSource.updateColumn(user, "status", "pwdillcount");
        if (status != UserInfo.STATUS_NORMAL) {
            webSocketNode.forceCloseWebSocket(userid).join();
        }
        return RetResult.success();
    }

    protected static class UserCurrgameEntry extends BaseBean {

        public int[] userids;

        public String currgame;

        public long currgamingtime;

        public UserCurrgameEntry(String currgame, long currgamingtime, int... userids) {
            this.userids = userids;
            this.currgame = currgame;
            this.currgamingtime = currgamingtime;
        }
    }

    protected static class UserLogoutEntry extends BaseBean {

        public int userid;

        public String sessionid;

        public UserLogoutEntry(int userid, String sessionid) {
            this.userid = userid;
            this.sessionid = sessionid;
        }
    }

    protected static class UserUpdateEntry extends BaseBean {

        public int userid;

        public ColumnValue[] values;

        public UserUpdateEntry(int userid, ColumnValue... values) {
            this.userid = userid;
            this.values = values;
        }
    }

    protected static class DayRecordUpdateEntry extends BaseBean {

        public int userid;

        public long createtime;

        public ColumnValue[] values;

        public boolean needinsert = true;

        public DayRecordUpdateEntry(int userid, long createtime, ColumnValue... values) {
            this.userid = userid;
            this.createtime = createtime;
            this.values = values;
        }

        public DayRecordUpdateEntry(boolean needinsert, int userid, long createtime, ColumnValue... values) {
            this.needinsert = needinsert;
            this.userid = userid;
            this.createtime = createtime;
            this.values = values;
        }
    }
}
