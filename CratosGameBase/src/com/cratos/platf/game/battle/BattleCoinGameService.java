/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.*;
import com.cratos.platf.game.*;
import static com.cratos.platf.util.Utils.HEADNAME_WS_SNCP_ADDRESS;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Level;
import javax.persistence.Transient;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.source.*;
import org.redkale.util.*;
import static org.redkale.util.Utility.ofMap;

/**
 *
 * @author zhangjx
 * @param <GT>     BattleGameTable
 * @param <R>      BattleGameRound
 * @param <P>      BattleGamePlayer
 * @param <E>      BattleEnemyRecord
 * @param <K>      BattleEnemyKind
 * @param <GTBean> GameTableBean
 */
@AutoLoad(false)
public abstract class BattleCoinGameService<GT extends BattleGameTable<R, P, E, K>, R extends BattleGameRound, P extends BattleGamePlayer, E extends BattleEnemyRecord<K>, K extends BattleEnemyKind, GTBean extends GameTableBean> extends CoinGameService<GT, P, GTBean> {

    @Comment("当前正在进行的房间")
    protected final Queue<GT> currTables = new ConcurrentLinkedQueue();

    //创建玩家
    protected abstract P createGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean);

    //种类分组 key=lineid
    protected LinkedHashMap<Integer, BattleEnemyLine> enemyLines = new LinkedHashMap<>();

    //种类
    protected LinkedHashMap<Integer, K> enemyKinds = new LinkedHashMap<>();

    //种类分组 key=kindtype
    protected LinkedHashMap<Short, Integer> enemyKindTypeToNormalCreateRates = new LinkedHashMap<>();

    //种类分组 key=kindtype
    protected LinkedHashMap<Short, Integer> enemyKindTypeToNormalCreateCounts = new LinkedHashMap<>();

    //种类分组 key=roomlevel, subkey=kindtype
    protected HashMap<Integer, LinkedHashMap<Short, Integer>> enemyKindTypeToSportCreateRates = new HashMap<>();

    //种类分组 key=roomlevel, subkey=kindtype
    protected HashMap<Integer, LinkedHashMap<Short, Integer>> enemyKindTypeToSportCreateCounts = new HashMap<>();

    //种类分组 key=kindtype
    protected LinkedHashMap<Short, List<K>> enemyKindTypeToKinds = new LinkedHashMap<>();

    //种类权重 key=kindtype, value=[kindid]
    protected LinkedHashMap<Short, int[]> enemyKindTypeToWeights = new LinkedHashMap<>();

    //种类权重 key1=roomlevel key2=kindtype, value2=[kindid]
    protected LinkedHashMap<Integer, Map<Short, int[]>> roomlevelKindTypeToWeights = new LinkedHashMap<>();

    //种类分组 key=kindtype
    protected LinkedHashMap<Short, List<BattleEnemyLine>> enemyKindTypeToLines = new LinkedHashMap<>();

    //创建房间
    protected abstract GT createGameTable(P player, GTBean bean);

    protected abstract Class<K> enemyKindClass();

    @Transient //射击频率每秒多少次
    protected int confShotRate = 5;

    @Transient //普通敌机的屏幕上限
    protected int confNormalKindScreenLimit = 20;

    @Override
    public void init(AnyValue config) {
        super.init(config);
        loadEnemyKind(); //必须在reloadConfig
        initGame(4);
        {
            Map<Integer, BattleEnemyLine> linemap = loadEnemyLines();
            this.enemyLines.putAll(linemap);
            linemap.values().stream().forEach(line -> {
                for (short kindtype : line.getKinds()) {
                    this.enemyKindTypeToLines.computeIfAbsent(kindtype, (t) -> new ArrayList<>()).add(line);
                }
            });
        }
        this.scheduler.scheduleAtFixedRate(() -> { //定时加载种类配置
            try {
                loadEnemyKind();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "loadEnemyKind scheduleAtFixedRate error", e);
            }
            try {
                currTables.stream().forEach(table -> table.initConfig(this));
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "initConfig scheduleAtFixedRate error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Local
    public SecureRandom gameRandom() {
        return gameRandom;
    }

    @Local
    public void broadcastHrollAnnouncement(P player, E fish, long money) {
        if (canBroadcastAnnounce(money, 0)) {
            broadcastHrollAnnouncement(gameId(), player.getUserid(), player.getUsername(), gameName(), "击杀" + fish.getKind().getKindname() + "", money, 0);
        }
    }

    protected Map<Integer, BattleEnemyLine> loadEnemyLines() {
        return BattleEnemyLine.load(enemyKindClass(), "enemylines.json");
    }

    @Override
    protected void reloadConfig(GameConfigFunc func) {
        super.reloadConfig(func);
        this.confShotRate = func.getInt("GAME_" + gameId().toUpperCase() + "_SHOT_RATE", 5);
        this.confNormalKindScreenLimit = func.getInt("GAME_" + gameId().toUpperCase() + "_KIND_NORMAL_SCREENLIMIT", 20);
        LinkedHashMap<Short, Integer> enemyRates = new LinkedHashMap<>();
        this.enemyKindTypeToKinds.keySet().forEach(t -> enemyRates.put(t, func.getInt("GAME_" + gameId().toUpperCase() + "_CREATE_NORMAL_RATE_KINDTYPE_" + t, 100)));
        LinkedHashMap<Short, Integer> enemyCounts = new LinkedHashMap<>();
        this.enemyKindTypeToKinds.keySet().forEach(t -> enemyCounts.put(t, func.getInt("GAME_" + gameId().toUpperCase() + "_CREATE_NORMAL_COUNT_KINDTYPE_" + t, 1)));
        this.enemyKindTypeToNormalCreateRates = enemyRates;
        this.enemyKindTypeToNormalCreateCounts = enemyCounts;
        Map<String, List<String>> levels = new HashMap<>();
        for (String name : func.getNames()) {
            if (!name.startsWith("GAME_" + gameId().toUpperCase() + "_CREATE_SPORT_")) continue;
            String level = name.substring(name.indexOf("_SPORT_") + "_SPORT_".length());
            level = level.substring(0, level.indexOf('_'));
            String kindtype = name.substring(name.lastIndexOf('_') + 1);
            levels.computeIfAbsent(level, (r) -> new ArrayList()).add(kindtype);
        }
        HashMap<Integer, LinkedHashMap<Short, Integer>> sportRates = new HashMap<>();
        HashMap<Integer, LinkedHashMap<Short, Integer>> sportCounts = new HashMap<>();
        for (Map.Entry<String, List<String>> en : levels.entrySet()) {
            LinkedHashMap<Short, Integer> rates = new LinkedHashMap<>();
            LinkedHashMap<Short, Integer> counts = new LinkedHashMap<>();
            for (String kindtype : en.getValue()) {
                rates.put(Short.parseShort(kindtype), func.getInt("GAME_" + gameId().toUpperCase() + "_CREATE_SPORT_" + en.getKey() + "_RATE_KINDTYPE_" + kindtype, 100));
                counts.put(Short.parseShort(kindtype), func.getInt("GAME_" + gameId().toUpperCase() + "_CREATE_SPORT_" + en.getKey() + "_COUNT_KINDTYPE_" + kindtype, 1));
            }
            sportRates.put(Integer.parseInt(en.getKey()), rates);
            sportCounts.put(Integer.parseInt(en.getKey()), counts);
        }
        this.enemyKindTypeToSportCreateRates = sportRates;
        this.enemyKindTypeToSportCreateCounts = sportCounts;
    }

    protected void loadEnemyKind() {
        List<K> list = dataSource().queryList(enemyKindClass(), FilterNode.create("status", BaseEntity.STATUS_NORMAL));
        Collections.sort(list);
        LinkedHashMap<Integer, K> map = new LinkedHashMap<>();
        LinkedHashMap<Short, AtomicInteger> weightSizes = new LinkedHashMap<>();
        LinkedHashMap<Short, Map<Integer, Integer>> weightLists = new LinkedHashMap<>();
        LinkedHashMap<Integer, Map<Short, AtomicInteger>> weightSize2s = new LinkedHashMap<>();
        LinkedHashMap<Integer, Map<Short, Map<Integer, Integer>>> weightList2s = new LinkedHashMap<>();
        LinkedHashMap<Short, List<K>> types = new LinkedHashMap<>();
        for (K kind : list) {
            if (kind.getStatus() != BaseEntity.STATUS_NORMAL) continue;
            map.put(kind.getKindid(), kind);
            types.computeIfAbsent(kind.getKindtype(), (t) -> new CopyOnWriteArrayList<>()).add(kind);
            weightSizes.computeIfAbsent(kind.getKindtype(), (t) -> new AtomicInteger()).addAndGet(kind.getNewrate());
            weightLists.computeIfAbsent(kind.getKindtype(), (t) -> new LinkedHashMap<>()).put(kind.getKindid(), kind.getNewrate());
            if (kind.getRoomlevels() != null) {
                for (int roomlevel : kind.getRoomlevels()) {
                    Map<Short, AtomicInteger> map1 = weightSize2s.computeIfAbsent(roomlevel, (t) -> new LinkedHashMap());
                    map1.computeIfAbsent(kind.getKindtype(), (k) -> new AtomicInteger()).addAndGet(kind.getNewrate());
                    Map<Short, Map<Integer, Integer>> map2 = weightList2s.computeIfAbsent(roomlevel, (t) -> new LinkedHashMap());
                    map2.computeIfAbsent(kind.getKindtype(), (k) -> new LinkedHashMap<>()).put(kind.getKindid(), kind.getNewrate());
                }
            }
        }
        LinkedHashMap<Short, int[]> enemyWeights = new LinkedHashMap<>();
        weightSizes.forEach((kindtype, size) -> {
            final int[] weight = new int[size.get()];
            AtomicInteger index = new AtomicInteger(-1);
            weightLists.get(kindtype).forEach((kindid, newrate) -> {
                for (int i = 0; i < newrate; i++) {
                    weight[index.incrementAndGet()] = kindid;
                }
            });
            enemyWeights.put(kindtype, weight);
        });

        LinkedHashMap<Integer, Map<Short, int[]>> enemyWeight2s = new LinkedHashMap<>();
        weightSize2s.forEach((roomlevel, weightSize0s) -> {
            LinkedHashMap<Short, int[]> enemyWeight0s = new LinkedHashMap<>();
            weightSize0s.forEach((kindtype, size) -> {
                final int[] weight = new int[size.get()];
                AtomicInteger index = new AtomicInteger(-1);
                weightList2s.get(roomlevel).get(kindtype).forEach((kindid, newrate) -> {
                    for (int i = 0; i < newrate; i++) {
                        weight[index.incrementAndGet()] = kindid;
                    }
                });
                enemyWeight0s.put(kindtype, weight);
            });
            enemyWeight2s.put(roomlevel, enemyWeight0s);
        });

        this.enemyKinds = map;
        this.enemyKindTypeToKinds = types;
        this.enemyKindTypeToWeights = enemyWeights;
        this.roomlevelKindTypeToWeights = enemyWeight2s;
    }

    @Override
    protected P loadGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean, P oldPlayer) {
        if (oldPlayer != null) return oldPlayer.copyFromUserAndOnline(user, clientAddr, sncpAddress);
        P player = findLivingPlayer(user.getUserid());
        if (player != null) return player.copyFromUserAndOnline(user, clientAddr, sncpAddress);
        return createGamePlayer(user, clientAddr, sncpAddress, roomlevel, bean);
    }

    protected void cleanCostCoins(BattleGameTable table, final BattleGameAccount account, BattleGamePlayer player, final long now) {
        long sum = 0;
        int count = 0;
        Long costcoin; // 正数
        while ((costcoin = player.costQueue.poll()) != null) {
            sum += costcoin;
            count++;
        }
        if (sum == 0) return;
        if (player.data1Record != null) {
            player.data1Record.setCreatetime(now);
            player.data1Record.refreshPoolrecordid();
            player.data1Record.setRemark("子弹汇总成本(" + count + "次)");
        }
        if (player.data2Record != null) {
            player.data2Record.setCreatetime(now);
            player.data2Record.refreshPoolrecordid();
            player.data2Record.setRemark("子弹汇总成本(" + count + "次)");
        }
        if (player.data3Record != null) {
            player.data3Record.setCreatetime(now);
            player.data3Record.refreshPoolrecordid();
            player.data3Record.setRemark("子弹汇总成本(" + count + "次)");
        }
        updatePoolDataRecord(player.data1Record, player.data2Record, player.data3Record);
        player.data1Record = null;
        player.data2Record = null;
        player.data3Record = null;
        updateGameUserCoins(account, player.getUserid(), table.getRoomlevel(), -sum, sum, now, "", "子弹汇总成本(" + count + "次)");
    }

    public Map<Integer, BattleKindCounter> createKindCounter() {
        Map<Integer, BattleKindCounter> map = new LinkedHashMap<>();
        enemyKinds.keySet().stream().forEach(kindid -> {
            map.put(kindid, new BattleKindCounter());
        });
        return map;
    }

    public K findEnemyKind(int kindid) {
        return enemyKinds.get(kindid);
    }

    public K randomEnemyKind(final int roomlevel, final short kindtype) {
        final int[] kinds = roomlevelKindTypeToWeights.get(roomlevel).get(kindtype);
        if (kinds == null) return null;
        return enemyKinds.get(kinds[gameRandom.nextInt(kinds.length)]);
    }

    public BattleEnemyLine randomEnemyLine(final int roomlevel, short kindtype, Set<Integer> set) {
        List<BattleEnemyLine> lines = enemyKindTypeToLines.get(kindtype);
        BattleEnemyLine line = lines.get(gameRandom.nextInt(lines.size()));
        if (set != null) {
            int count = 0;
            while (set.contains(line.getLineid()) && count < 100) {
                line = lines.get(gameRandom.nextInt(lines.size()));
                count++;
            }
            set.add(line.getLineid());
        }
        return line;
    }

    protected Object leaveStart(P player, GT table, Map<String, String> bean) {
        return null;
    }

    protected void leaveEnd(P player, GT table, Object startObject, Map<String, String> bean) {
    }

    protected boolean canDismissTable(GT table) {
        return table.isAllRobot();
    }

    @Comment("发送新敌机消息")
    public CompletableFuture<Integer> sendEnemyRecord(Collection<P> players, BattleEnemyRecord... enemys) {
        return sendMap(players, "onEnemyJoinMessage", new RetResult(enemys));
    }

    @Comment("发送新敌机消息")
    public CompletableFuture<Integer> sendEnemyRecord(Collection<P> players, List<BattleEnemyRecord> enemys) {
        return sendMap(players, "onEnemyJoinMessage", new RetResult(enemys));
    }

    @Comment("发送新敌机消息")
    public CompletableFuture<Integer> sendEnemyRecord2(Collection<P> players, BattleEnemyRecord... enemys) {
        return sendMap(players, "onEnemyJoin2Message", new RetResult(enemys));
    }

    @Comment("发送新敌机消息")
    public CompletableFuture<Integer> sendEnemyRecord2(Collection<P> players, List<BattleEnemyRecord> enemys) {
        return sendMap(players, "onEnemyJoin2Message", new RetResult(enemys));
    }

    @Comment("发送BOSS即将加入的消息")
    public CompletableFuture<Integer> sendBossComing(Collection<P> players, E boss) {
        return sendMap(players, "onEnemyBossComingMessage", new RetResult(ofMap("enemyid", boss.getEnemyid(), "kindid", boss.getKindid())));
    }

    protected RetResult dismissTable(GT table, P player, final Map<String, String> bean) {
        currTables.remove(table);
        table.stop(this);
        for (P p : table.getPlayers()) { //机器人离线
            if (p != null) {
                table.removePlayer(p.getUserid());
                if (player == null) { //系统主动解散
                    RetResult rr = new RetResult(Utility.ofMap("userid", p.getUserid()));
                    if (bean != null && bean.containsKey("retinfo")) rr.retinfo(bean.get("retinfo"));
                    sendMap(p, "onPlayerLeaveMessage", rr);
                }
                leaveGame(p.getUserid());
            }
        }
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = false, comment = "平台用户信息更新通知")
    public RetResult<String> notifyPlatfPlayer(final int userid, final Map<String, String> bean) {
        P player = findLivingPlayer(userid);
        if (player != null) player.copyFromUser(findUserInfo(userid));
        return RetResult.success();
    }

    @Override
    protected <T> RetResult<T> authEnterGame(int userid, UserInfo user, Map<String, String> bean) {
        int roomlevel = Integer.parseInt(bean.get("roomlevel"));
        if (roomlevel < 1 || roomlevel > this.confRoomCoinStages.length) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
        if (!this.confRoomCoinStages[roomlevel - 1].test(user.getCoins())) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);
        return null;
    }

    @Override
    @RestMapping(auth = true, comment = "离开游戏")
    public <T> RetResult<T> leaveGame(int userid, final Map<String, String> bean) {
        P player = findLivingPlayer(userid);
        if (player == null) {
            leaveGame(userid);
            logger.log(Level.FINEST, "userid=" + userid + " not in " + this.getClass().getSimpleName());
            return RetResult.success();
        }
        GT table = player.table();
        if (table == null) { //enterGame后并不会一定进joinTable，如bjl的房间列表
            leaveGame(userid);
            return RetResult.success();
        }

        Object startObject = leaveStart(player, table, bean);

        RetResult rs = table.removePlayer(userid);
        if (!rs.isSuccess()) return rs;
        if (canDismissTable(table)) {
            try {
                if (bean != null && bean.containsKey("retinfo")) {
                    RetResult rr = new RetResult(Utility.ofMap("userid", player.getUserid()));
                    rr.retinfo(bean.get("retinfo"));
                    try {
                        sendMap(player, "onPlayerLeaveMessage", rr).get(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, table + " leave player(" + player + ") when dismissing error", e);
                    }
                }
            } finally {
                dismissTable(table, player, bean);
            }
        } else {
            RetResult rr = new RetResult(Utility.ofMap("userid", player.getUserid()));
            if (bean != null && bean.containsKey("retinfo")) rr.retinfo(bean.get("retinfo"));
            Set<P> userids = table.onlinePlayers();
            userids.add(player);
            sendMap(userids, "onPlayerLeaveMessage", rr).join();
            leaveEnd(player, table, startObject, bean);
        }
        leaveGame(userid);
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = true, comment = "玩家离线")
    public <T> RetResult<T> offlineGame(int userid, Map<String, String> bean) {
        P player = findLivingPlayer(userid);
        if (player == null) return RetResult.success();
        RetResult rs = leaveGame(userid, bean);
        if (rs.isSuccess()) return rs;
        GT table = player.table();
        if (table != null) table.offlinePlayer(userid);
        sendMap(player.table().onlinePlayers(userid), "onPlayerOfflineMessage", new RetResult(Utility.ofMap("userid", player.getUserid())));

        //调试用 if (winos) addTableDismissAction(table, player, bean);
        if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "offlineGame " + gameId() + ": userid=" + userid);
        return RetResult.success();
    }

    @Comment("获取房间信息")
    public GT findTableByPlayerid(int userid) {
        P player = findLivingPlayer(userid);
        return player == null ? null : player.table();
    }

    protected JsonConvert createTableConvert(final GT table, final P player) {
        return JsonConvert.root();
    }

    //重新进入房间
    protected RetResult<GT> joinOldTable(GT table, P player, GTBean bean) {
        return new RetResult(createTableConvert(table, player), table);
    }

    //进入已有房间
    protected RetResult<GT> joinRunTable(GT table, P player, GTBean bean) {
        return new RetResult(createTableConvert(table, player), table);
    }

    //创建新房间之后的操作
    protected RetResult<GT> joinNewTable(GT table, P player, GTBean bean) {
        return new RetResult(createTableConvert(table, player), table);
    }

    protected RetResult<GT> joinSportTable(int userid, String clientAddr, InetSocketAddress sncpAddress, GTBean bean) {
        return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
    }

    @Override
    @RestMapping(auth = true, comment = "玩家加入房间")
    public RetResult<GT> joinTable(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, GTBean bean) {
        final P player = findLivingPlayer(userid);
        if (player == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        GT oldTable = player.table();
        if (oldTable != null) {//掉线后重新进入房间
            RetResult<GT> rs = joinOldTable(oldTable, player, bean);
            if (rs.isSuccess()) updateAccountOnLine(userid);
            return rs;
        }

        final int roomlevel = player.getRoomlevel();
        if (roomlevel > 100) {
            RetResult<GT> rs = joinSportTable(userid, clientAddr, sncpAddress, bean);
            if (rs.isSuccess()) updateAccountOnLine(userid);
            return rs;
        }

        if (roomlevel < 1 || roomlevel > this.confRoomCoinStages.length) {
            return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
        }
        Range.LongRange range = this.confRoomCoinStages[roomlevel - 1];
        if (!range.test(player.getCoins())) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);

        final GameService service = this;
        synchronized (lockDataCoinPool(roomlevel)) {
            AtomicReference<GT> ref = new AtomicReference(null);
            GT old = player.table();
            if (old == null) {
                if (bean != null && bean.getTableid() != null && !bean.getTableid().isEmpty()) {
                    GT optTable = currTables.stream().filter(t -> bean.getTableid().equals(t.getTableid())).findAny().orElse(null);
                    if (optTable == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_NOTEXISTS);
                    RetResult rr = optTable.addPlayer(player, service);
                    if (!rr.isSuccess()) return rr;
                    ref.set(optTable);
                } else {
                    currTables.stream().sorted().filter(t -> t.getRoomlevel() == roomlevel).filter(t -> ref.get() == null).forEach(t -> {
                        if (!t.isFull()) {
                            RetResult rr = t.addPlayer(player, service);
                            if (!rr.isSuccess()) return;
                            ref.set(t);
                        }
                    });
                }
            } else {
                ref.set(old);
            }
            if (ref.get() != null) { //加入已有的房间
                GT table = ref.get();
                sendMap(table.onlinePlayers(userid), "onPlayerJoinMessage", new RetResult(player));
                RetResult<GT> rs = joinRunTable(table, player, bean);
                if (rs.isSuccess()) updateAccountOnLine(userid);
                return rs;
            }

            final GT table = createGameTable(player, bean);
            table.setGameid(gameId());
            table.shotrate = this.confShotRate;
            table.paramBean = bean;
            final Map<Integer, BattleKindCounter> counterMap = new LinkedHashMap<>();
            this.enemyKinds.forEach((kindid, kind) -> counterMap.put(kindid, new BattleKindCounter()));
            table.kindCounterMap = counterMap;
            table.initConfig(this);
            if (table.getCreatetime() < 1) table.setCreatetime(System.currentTimeMillis());
            table.setTableid(Utility.format36time(table.getCreatetime()) + "-" + Integer.toString(player.getUserid(), 36) + "-" + nodeid);
            RetResult addrs = table.addPlayer(player, this);
            if (!addrs.isSuccess()) return addrs;
            player.online(player.getClientAddr(), player.sncpAddress());
            currTables.add(table);
            table.start(this);
            RetResult<GT> rs = joinNewTable(table, player, bean);
            if (rs.isSuccess()) updateAccountOnLine(userid);
            return rs;
        }
    }

}
