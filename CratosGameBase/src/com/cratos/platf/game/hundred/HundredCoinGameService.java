/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.hundred;

import com.cratos.platf.base.*;
import com.cratos.platf.game.*;
import static com.cratos.platf.game.GameRetCodes.*;
import static com.cratos.platf.game.GameRound.ROUND_STATUS_SETTLED;
import com.cratos.platf.notice.Announcement;
import com.cratos.platf.util.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Level;
import java.util.stream.*;
import org.redkale.convert.ConvertField;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;
import static org.redkale.util.Utility.ofMap;

/**
 * 百人游戏
 *
 * @author zhangjx
 * @param <GT> HundredGameTable
 */
@AutoLoad(false)
public abstract class HundredCoinGameService<GT extends HundredGameTable> extends CoinGameService<GameTable, HundredGamePlayer, GameTableBean> {

    public static final HundredGamePlayer GAME_SYSTEM_BANKER = new HundredGamePlayer(UserInfo.USER_SYSTEM, null, null, 1, 0).userCoin(1000_0000_00);

    //可押注的选项      1:1  
    protected long[] betCoinOptions;// {500, 1000, 5000, 1_0000, 2_0000, 5_0000};

    //机器人押注权重
    protected int[] betCoinWeights;// Utils.calcIndexWeights(new int[]{36, 35, 34, 13, 12, 11});

    //当前庄家
    protected HundredGamePlayer currBanker;

    //下一个回合的起始时间
    protected long nextRoundStartTime = System.currentTimeMillis();

    //当前房间
    protected GT currTable;
    //上局赢的公告

    protected List<Announcement> winAnnounces;

    //座位
    protected HundredGamePlayer[] sittingPlayers;

    protected HundredGameResult lastGameResult;

    //最近若干条的结果记录
    protected final Deque<HundredGameResult> recentResults = new ConcurrentLinkedDeque();

    @Override
    public void init(AnyValue config) {
        super.init(config);
        this.sittingPlayers = createSitPositions();
        this.currBanker = GAME_SYSTEM_BANKER;
        initGame(4);

        final long roundMills = roundMills();
        final long bettingMills = bettingMills();
        if (bettingMills >= roundMills - 2000L) throw new RuntimeException("押注时长居然比整个回合时间还长!");

        final long delay = winos ? 3000 : (roundMills - System.currentTimeMillis() % roundMills); //整秒执行
        final String serviceName = this.getClass().getSimpleName();
        this.nextRoundStartTime = delay;
        this.scheduler.scheduleAtFixedRate(() -> {
            this.nextRoundStartTime = System.currentTimeMillis() + roundMills;
            try {
                doRoundStart();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, serviceName + ".doRoundStart error", e);
            }
        }, delay, roundMills, TimeUnit.MILLISECONDS);

        this.scheduler.scheduleAtFixedRate(() -> { //定时结算
            try {
                doRoundSettle();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, serviceName + ".doRoundSettle error", e);
            }
            try {
                saveOldTable();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, serviceName + ".saveOldTable error", e);
            }
            this.currBanker = GAME_SYSTEM_BANKER;
        }, delay + bettingMills, roundMills, TimeUnit.MILLISECONDS);

        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                doRoundPreStart();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, serviceName + ".doRoundPreStart error", e);
            }
        }, delay + roundMills - 2000, roundMills, TimeUnit.MILLISECONDS);

        this.scheduler.scheduleAtFixedRate(() -> { //每1秒执行
            try {
                doRobotBetting();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, serviceName + ".doRobotBetting error", e);
            }
        }, delay + 950, joinRobotRateMills(), TimeUnit.MILLISECONDS);
        this.loadRecentResults();
    }

    protected void initBetOptions(long[] betOptions, int[] betWeights) {
        this.betCoinOptions = betOptions;
        this.betCoinWeights = Utils.calcIndexWeights(betWeights);
    }

    @RestMapping(auth = true, comment = "加载Table信息")
    public <T> RetResult<T> loadTable(int userid, Map<String, String> bean) {
        GamePlayer player = findLivingPlayer(userid);
        if (player != null) sendTableDetailMessage(this.currTable, player);
        return RetResult.success();
    }

    @Override
    protected HundredGamePlayer loadGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean, HundredGamePlayer oldPlayer) {
        if (oldPlayer != null) return oldPlayer.copyFromUserAndOnline(user, clientAddr, sncpAddress);
        return new HundredGamePlayer(user, clientAddr, sncpAddress, roomlevel, 0);
    }

    @Override
    @RestMapping(auth = true, comment = "离开游戏")
    public <T> RetResult<T> leaveGame(int userid, Map<String, String> bean) {
        if (UserInfo.isRobot(userid)) return super.leaveGame(userid, bean);
        final GT table = this.currTable;
        if (table != null) {
            if (table.getBankerid() == userid) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_CANNOTLEAVE_GAMING);
            if (table.isBetting(userid)) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_CANNOTLEAVE_GAMING);
        }
        this.sitDown(userid, 0);
        RetResult rs = super.leaveGame(userid, bean);
        if (rs.isSuccess() && this.sittingPlayers != null) {
            sendMap(streamLivingPlayer().filter(x -> !x.isRobot()), "onPlayerLeaveMessage", new RetResult(Utility.ofMap("userid", userid)));
        }
        return rs;
    }

    @Override
    @RestMapping(auth = true, comment = "玩家离线")
    public <T> RetResult<T> offlineGame(int userid, Map<String, String> bean) {
        final GT table = this.currTable;
        if (table == null || (!table.isBetting(userid) && table.getBankerid() != userid)) {
            this.sitDown(userid, 0);
            return super.offlineGame(userid, bean);
        }
        //已经下注
        GamePlayer player = findLivingPlayer(userid);
        if (player != null) player.offline();
        leaveAccount(userid);
        if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "offlineGame " + gameId() + ": userid=" + userid);
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "获取没有坐下的玩家列表")
    public RetResult<List<GamePlayer>> getUnSitPlayers(int userid) {
        final GamePlayer[] sitPlayers = this.sittingPlayers;
        List<GamePlayer> list = streamLivingPlayer().filter(x -> {
            if (sitPlayers != null) {
                for (GamePlayer p : sitPlayers) {
                    if (p != null && p.getUserid() == x.getUserid()) {
                        return false;
                    }
                }
            }
            return true;
        }).collect(Collectors.toList());
        return new RetResult<>(list);
    }

    @RestMapping(auth = true, comment = "坐下, 0表示离开位置")
    public <T> RetResult<T> sitDown(final int userid, int sitpos) {
        if (sittingPlayers == null) return RetResult.success();
        if (sitpos < 0 || sitpos > sittingPlayers.length) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        final GamePlayer[] sitPlayers = this.sittingPlayers;
        synchronized (sitPlayers) {
            int oldsitpos = 0;
            for (int i = 0; i < sittingPlayers.length; i++) {
                HundredGamePlayer player = sittingPlayers[i];
                if (player != null && player.getUserid() == userid) {
                    oldsitpos = i + 1;
                }
            }
            if (oldsitpos == sitpos) return RetResult.success();
            final HundredGamePlayer player = findLivingPlayer(userid);
            if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
            if (sitpos > 0 && sittingPlayers[sitpos - 1] != null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_SITDOWN_NOPOS);
            RetResult rs = RetResult.success();
            if (oldsitpos > 0) sittingPlayers[oldsitpos - 1] = null;
            if (sitpos > 0) sittingPlayers[sitpos - 1] = player;
            Map msg = ofMap("onPlayerSitDownMessage", new RetResult(player));
            final int oldpos = oldsitpos;
            JsonConvert convert = JsonConvert.root().newConvert(null, obj -> {
                return (obj == player) ? ConvertField.ofArray("sitpos", sitpos, "oldsitpos", oldpos) : null;
            });
            sendMessage(convert, streamLivingPlayer().filter(x -> !x.isRobot()), msg);
            return rs;
        }
    }

    @RestMapping(auth = true, comment = "申请当庄")
    public RetResult applyBanker(final int userid) {
        if (!canApplyBanker()) return GameRetCodes.retResult(RET_GAME_ACTEVENT_ILLEGAL);
        final GT table = this.currTable;
        if (table == null) return GameRetCodes.retResult(RET_GAME_TABLE_STATUS_NOBETTING);
        final HundredGameRound round = table.currRound;
        if (round == null || round.getRoudStatus() != GameRound.ROUND_STATUS_BETTING) return GameRetCodes.retResult(RET_GAME_TABLE_STATUS_NOBETTING);
        if (UserInfo.isRobot(userid)) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);

        HundredGamePlayer player = findLivingPlayer(userid);
        if (player == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        synchronized (table) {
            //rs = table.applyBanker(this, player);
            if (table.bankerid != UserInfo.USERID_SYSTEM && table.bankerid != player.getUserid()) return GameRetCodes.retResult(RET_GAME_TABLE_BANKER_EXISTS);
            if (player.getCoins() < table.getBankerMinApplyCoin()) return GameRetCodes.retResult(RET_GAME_TABLE_BANKER_COINSLESS, table.getBankerMinApplyCoin());
            long maxloses = calcMaxMaybeLoseCoins(table, null);
            if (maxloses > player.getCoins() - round.getUserBetCoinSum(player.getUserid())) {
                return GameRetCodes.retResult(RET_GAME_TABLE_BANKER_COINSLESS, maxloses);
            }
            table.bankerid = player.getUserid();
            table.banker = player;
        }
        { //全局广播
            this.currBanker = player;
            //sendReplyBankerMessage();
            if (!testrun) {
                Map msg = ofMap("onReplyBankerMessage", new RetResult(ofMap("userid", player.getUserid(), "username", player.getUsername(), "coins", player.getCoins())));
                sendMessage(streamLivingPlayer().filter(x -> !x.isRobot()), msg);
            }
        }
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "押注")
    public RetResult betCoin(final int userid, HundredBetEntry bean) {
        if (bean.getBetcoin() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        final GT table = this.currTable;
        if (table == null) return GameRetCodes.retResult(RET_GAME_TABLE_STATUS_NOBETTING);
        final HundredGameRound round = table.currRound;
        if (round == null || round.getRoudStatus() != GameRound.ROUND_STATUS_BETTING) return GameRetCodes.retResult(RET_GAME_TABLE_STATUS_NOBETTING);
        if (table.getBankerid() == userid) return RetCodes.retResult(RetCodes.RET_USER_COINS_NOTENOUGH);
        GamePlayer player = findLivingPlayer(userid);
        long betsum;
        synchronized (table) {
            if (!UserInfo.isRobot(userid)) {
                long usercoins = player.getCoins();
                if (usercoins < (round.getUserBetCoinSum(userid) + bean.getBetcoin()) * betLosFactor()) return RetCodes.retResult(RetCodes.RET_USER_COINS_NOTENOUGH);
            }
            if (table.bankerid != GAME_SYSTEM_BANKER.getUserid()) {
                long maxloses = calcMaxMaybeLoseCoins(table, bean);
                if (maxloses > table.banker.getCoins()) return GameRetCodes.retResult(RET_GAME_TABLE_COINGREAT_BANKER);
            }
            RetResult<Long> r = round.addBetEntry(player.getUserid(), bean);
            if (!r.isSuccess()) return r;
            betsum = r.getResult();
        }
        //sendPlayerBettedMessage(bean, coinsum);
        final long coinsum = betsum;
        if (!testrun) {
            streamLivingPlayer().filter(x -> !x.isRobot()).forEach(p -> {
                RetResult rr = new RetResult(ofMap("userid", userid, "betpos", bean.getBetpos(), "betcoin", bean.getBetcoin(), "betsum", coinsum));
                Object mesasge = ofMap("onPlayerBettedMessage", rr);
                if (webSocketNode == null) {
                    logger.log(Level.WARNING, jsonConvert.convertTo(mesasge));
                } else {
                    sendMessage(p, mesasge);
                }
            });
        }
        return RetResult.success();
    }

    @Deprecated
    @RestMapping(auth = true, comment = "获取最近的结果")
    public RetResult<Deque<HundredGameResult>> getResulthis() {
        return new RetResult<>(recentResults);
    }

    @RestMapping(auth = true, comment = "获取最近的结果")
    public RetResult<Deque<HundredGameResult>> recentResults(int userid) {
        return new RetResult<>(recentResults);
    }

    public static void runTest(HundredCoinGameService service, boolean manBanker) throws Throwable {
        final String clientAddr = "127.0.0.1";
        final InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8800);
        service.testrun = true;
        service.reloadConfig(GameConfigFunc.createEmpty());
        service.confCoinPoolTaxPermillage = 100;
        service.confLimitLosCoinStages = new long[]{0L};
        service.confLimitWinCoinStages = new long[]{5000_00L};

        List<GamePlayer> humans = new ArrayList<>();
        List<GamePlayer> robots = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            UserInfo user = new UserInfo();
            user.setUserid(UserInfo.USERID_SYSTEM + i);
            user.setCoins(1000_0000_0000L);
            GamePlayer player = service.loadGamePlayer(user, clientAddr, addr, 1, null, null);
            robots.add(player);
            service.putLivingPlayer(player.getUserid(), player);
        }
        for (int i = 1; i <= 50; i++) {
            UserInfo user = new UserInfo();
            user.setUserid(400_0000 + i);
            user.setCoins(1000_0000_0000L);
            GamePlayer player = service.loadGamePlayer(user, clientAddr, addr, 1, null, null);
            humans.add(player);
            service.putLivingPlayer(player.getUserid(), player);
        }
        int count = 100000; //总运行次数
        AtomicLong minPool = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxPool = new AtomicLong(Long.MIN_VALUE);
        for (int i = 0; i < count; i++) {
            service.doRoundStart();
            int betposlen = service.currTable.currRound.betOptionEntrys.length;
            for (GamePlayer player : humans) {
                int betpos = service.gameRandom.nextInt(betposlen) + 1;
                RetResult rs = service.betCoin(player.getUserid(), new HundredBetEntry(service.betCoinOptions[0], betpos));
                if (!rs.isSuccess() && player.getUserid() != service.currTable.bankerid) System.out.println("玩家押注不成功: " + rs);
            }
            if (manBanker) service.applyBanker(humans.get(0).getUserid());
            for (GamePlayer player : robots) {
                int betpos = service.gameRandom.nextInt(betposlen) + 1;
                RetResult rs = service.betCoin(player.getUserid(), new HundredBetEntry(service.betCoinOptions[0], betpos));
                if (!rs.isSuccess()) System.out.println("电脑押注不成功: " + rs);
            }
            service.doRoundSettle();
            service.saveOldTable();
            long pool = service.getDataCoinPoolValue(1);
            if (count <= 100) System.out.println("奖池当前值: " + pool);
            if (pool > maxPool.get()) maxPool.set(pool);
            if (pool < minPool.get()) minPool.set(pool);
            if (pool < service.confLimitLosCoinStages[0]) throw new RuntimeException((i + 1) + "--奖池击穿底线: " + pool);
        }
        System.out.println("");
        System.out.println("----------------------");
        System.out.println("奖池下限值: " + service.confLimitLosCoinStages[0]);
        System.out.println("奖池最低值: " + minPool);
        System.out.println("奖池上限值: " + service.confLimitWinCoinStages[0]);
        System.out.println("奖池最大值: " + maxPool);
        System.out.println("奖池当前值: " + service.getDataCoinPoolValue(1));
    }

    protected int betLosFactor() {
        return 1;
    }

    protected void doRoundSettle() {
        final GT table = this.currTable;
        final HundredGameRound round = table.currRound;
        round.setRoudStatus(ROUND_STATUS_SETTLED);
        HundredGameResult result0 = calcRoundResult(table, round);
        {
            final AtomicLong taxcoin = new AtomicLong();
            long humanWinCoins = calcHumanWinCoin(table, round, result0, taxcoin);
            boolean maxloss = humanWinCoins > 0 && breakLimitLosCoin(1, humanWinCoins);
            boolean maxwin = humanWinCoins < 0 && passLimitWinCoin(1, humanWinCoins);
            if (maxloss || maxwin) { //系统会突破上下限状态
                boolean ok = false;
                for (int i = 0; i < 10; i++) {
                    result0 = calcRoundResult(table, round);
                    humanWinCoins = calcHumanWinCoin(table, round, result0, taxcoin);
                    maxloss = humanWinCoins > 0 && breakLimitLosCoin(1, humanWinCoins);
                    maxwin = humanWinCoins < 0 && passLimitWinCoin(1, humanWinCoins);
                    if (maxloss || maxwin) continue;
                    ok = true;
                    break;
                }
                if (!ok) { //随机产生的结果还是突破上下限状态
                    final int[] enumResults = resultIdEnumArray();
                    if (enumResults == null) {
                        logger.log(Level.SEVERE, this.getClass().getSimpleName() + " resultEnumArray error ");
                    } else {
                        long diHumanWinCoin = humanWinCoins;
                        for (int i = 0; i < enumResults.length; i++) {
                            int r0 = enumResults[i];
                            if (result0.getResultid() == r0) continue;
                            long winItemCoin = calcHumanWinCoin(table, round, new HundredGameResult(r0), taxcoin);
                            if (maxloss) {
                                if (winItemCoin < diHumanWinCoin) {
                                    result0.setResultid(r0);
                                    diHumanWinCoin = winItemCoin;
                                    if (diHumanWinCoin < 0 || !breakLimitLosCoin(1, diHumanWinCoin)) {
                                        ok = true;
                                        break;
                                    }
                                }
                            } else {
                                if (winItemCoin > diHumanWinCoin && !(winItemCoin > 0 && breakLimitLosCoin(1, winItemCoin))) {
                                    result0.setResultid(r0);
                                    diHumanWinCoin = winItemCoin;
                                    if (diHumanWinCoin > 0 || !passLimitWinCoin(1, diHumanWinCoin)) {
                                        ok = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!ok) logger.log(Level.SEVERE, this.getClass().getSimpleName() + " 击穿底线了");
                    }
                }
            }
            if (humanWinCoins != 0) logger.log(Level.FINE, gameId() + " 预判 tableid=" + table.getTableid() + "，humanWinCoins=" + humanWinCoins + "，taxCoins=" + taxcoin);
        }
        final HundredGameResult result = result0;
        final AtomicLong bankerWinCoins = new AtomicLong();
        final Map<Integer, HundredBetEntry>[] betEntrys = round.getBetOptionEntrys();
        final Map<Integer, GameWinner> winnermap = new HashMap<>();
        for (int i = 0; i < betEntrys.length; i++) {
            final int betEntryIndex = i;
            betEntrys[i].forEach((userid, entry) -> {
                final long winc = calcBetEntryWinCoin(table, result, betEntryIndex, entry.getBetcoin());
                entry.setWincoin(winc);
                bankerWinCoins.addAndGet(-winc);
                GameWinner winner = winnermap.get(userid);
                if (winner == null) {
                    winnermap.put(userid, new GameWinner(findLivingPlayer(userid), winc));
                } else {
                    winner.increWincoins(winc);
                }
            });
        }
        table.setResultid(result.getResultid());
        table.setCards(result.getCards());
        table.setAllbets(Utility.joining(round.allBetCoins(), ","));
        round.setResultBankerWinner(new GameWinner(this.currBanker, bankerWinCoins.get()));
        if (!this.currBanker.isRobot()) {
            GameWinner oldWinner = winnermap.get(this.currBanker.getUserid());
            if (oldWinner == null) {
                winnermap.put(this.currBanker.getUserid(), round.getResultBankerWinner());
            } else {
                oldWinner.increWincoins(bankerWinCoins.get());
            }
        }
        List<GameWinner> winnerlist = winnermap.values().stream().filter(x -> x.getWincoin() > 0).collect(Collectors.toList());
        Collections.sort(winnerlist);
        round.setResultTopWinners(winnerlist.subList(0, Math.min(topWinnerLimit(), winnerlist.size())));
        this.lastGameResult = result;
        this.doAfterRoundSettled(table, round, result);
        sendTableDetailMessage(table, null);
    }

    protected void doAfterRoundSettled(final GT table, final HundredGameRound round, HundredGameResult result) {
    }

    protected void sendTableDetailMessage(final GT table, final GamePlayer oneplayer) {
        if (testrun) return;
        if (table == null) return;
        final HundredGameRound round = table.currRound;
        table.setAllbets(round == null ? "" : Utility.joining(round.allBetCoins(), ","));
        final int newRemainSeconds = (int) (roundMills() - System.currentTimeMillis() + table.getCreatetime()) / 1000;
        final int betRemainSeconds = Math.max(-1, (int) (bettingMills() - System.currentTimeMillis() + table.getCreatetime()) / 1000);
        (oneplayer == null ? streamLivingPlayer() : Arrays.asList(oneplayer).stream()).filter(p -> !p.isRobot()).forEach(player -> {
            RetResult rr = new RetResult(table);
            JsonConvert convert = JsonConvert.root().newConvert((t, u) -> {
                if (!canApplyBanker() && u instanceof HundredGameTable && (t.field().equals("banker") || t.field().equals("bankerMinApplyCoin"))) {
                    return null;
                }
                if (!canApplyBanker() && u instanceof HundredGameRound && t.field().equals("resultBankerWinner")) {
                    return null;
                }
                return t.get(u);
            }, obj -> {
                if (obj == table) {
                    return ConvertField.ofArray("newRemainSeconds", newRemainSeconds, "betRemainSeconds", betRemainSeconds, "sittingPlayers", sittingPlayers);
                }
                if (obj == table.banker) {
                    return ConvertField.ofArray("coins", table.banker.getCoins());
                }
                if (obj == round) {
                    return ConvertField.ofArray("mybets", round.getUserBetCoins(player.getUserid()),
                        "mywins", round.getUserWinCoins(player.getUserid()));
                }
                return null;
            });
            if (webSocketNode == null) {
                logger.log(Level.WARNING, jsonConvert.convertTo(ofMap("onTableDetailMessage", rr)));
            } else {
                sendMap(convert, player, "onTableDetailMessage", rr);
            }
        });
    }

    protected void doRoundStart() {
        this.currTable = null;
        final GT newTable = createGameTable();
        if (this.currBanker == null) this.currBanker = GAME_SYSTEM_BANKER;
        if (this.currBanker != GAME_SYSTEM_BANKER) {
            if (this.currBanker.getCoins() < newTable.getBankerMinApplyCoin()) {
                GamePlayer oldBanker = this.currBanker;
                this.currBanker = GAME_SYSTEM_BANKER;
                sendMap(oldBanker, "onKickedBankerMessage", new RetResult(ofMap("userid", oldBanker.getUserid())));
            }
        }
        newTable.setGameid(gameId());
        newTable.setRoomlevel(1);
        newTable.roundStart(this);
        this.currTable = newTable;
        try {
            sendTableDetailMessage(newTable, null);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "sendTableDetailMessage error: " + newTable, t);
        }
        if (this.winAnnounces != null && !this.winAnnounces.isEmpty()) {
            broadcastHrollAnnouncement(this.winAnnounces);
            this.winAnnounces = null;
        }
    }

    protected void saveOldTable() {
        final GT oldTable = this.currTable;
        if (oldTable == null) return;
        final HundredGameRound round = oldTable.currRound;
        if (round == null) return;
        final long now = System.currentTimeMillis();
        final GameWinner bankerWinner = round.getResultBankerWinner();
        final boolean humanBanker = bankerWinner != null && !UserInfo.isRobot(bankerWinner.getUserid());
        final Map<Integer, HundredBetEntry>[] betEntrys = round.betOptionEntrys;
        int humensize = humanBanker ? 1 : 0;
        for (Map<Integer, HundredBetEntry> map : betEntrys) { //机器人全部离开
            humensize += map.keySet().stream().filter(x -> !UserInfo.isRobot(x)).count();
        }
        humensize += streamLivingPlayer().filter(x -> !x.isRobot()).count();
        if (humensize < 1) return; //全是机器人且没有玩家观看则不需要存储

        final List<Announcement> announces = new ArrayList<>();
        final Set<Integer> accounts = new HashSet<>();
        final HundredGameTable his = oldTable.createTableHis(now);
        round.setTableid(his.getTableid());  //tableid会重新赋值

        final AtomicLong robotTaxCoins = new AtomicLong();
        final List<AbstractMap.SimpleEntry<Integer, HundredBetEntry>> entryList = new ArrayList<>();
        for (Map<Integer, HundredBetEntry> map : betEntrys) {
            map.forEach((userid, entry) -> {
                entryList.add(new AbstractMap.SimpleEntry<>(userid, entry));
            });
        }
        if (humanBanker && bankerWinner != null) {
            HundredBetEntry entry = new HundredBetEntry();
            entry.setWincoin(bankerWinner.getWincoin());
            entryList.add(new AbstractMap.SimpleEntry<>(0, entry));
        }
        Collections.sort(entryList, (a, b) -> humanBanker ? (int) (b.getValue().getWincoin() - a.getValue().getWincoin()) : (int) (a.getValue().getWincoin() - b.getValue().getWincoin())); //输钱的排前面，否则奖池可能会越界
        AtomicInteger index = new AtomicInteger();
        for (AbstractMap.SimpleEntry<Integer, HundredBetEntry> en : entryList) {
            saveRoundPlayer(index, en.getKey() == 0, announces, accounts, his, now, humanBanker, robotTaxCoins, (en.getKey() == 0 && bankerWinner != null) ? bankerWinner.getUserid() : en.getKey(), en.getValue());
        }
        if (!testrun) insertQueue.add(his);
        this.winAnnounces = announces;
        this.currTable = null;
    }

    private void saveRoundPlayer(AtomicInteger index, final boolean bankerself, final List<Announcement> announces, final Set<Integer> accounts, final HundredGameTable his,
        final long now, final boolean humanBanker, final AtomicLong robotTaxCoins, final int userid, final HundredBetEntry entry) {
        final long betcoin = entry.getBetcoin();
        final long wincoin = entry.getWincoin();
        final GamePlayer player = findLivingPlayer(userid);
        if (canBroadcastAnnounce(wincoin, 0)) {
            announces.add(Announcement.createHrollAnnounceWinCoins(gameId(), userid, player.getUsername(), gameName(), wincoin));
        }
        if (player != null && player.getUserid() != UserInfo.USERID_SYSTEM) player.increCoin(wincoin);
        if (UserInfo.isRobot(userid)) {
            if (humanBanker) {
                long taxcoin = updatePoolCoin(1, userid, wincoin, his.getTableid(), now, "settle-" + entry.getBetpos(), "玩家坐庄" + index.incrementAndGet());
                robotTaxCoins.addAndGet(taxcoin);
            }
            return;
        }
        Exception e = null;
        final GameAccount account = loadAccount(userid);
        if (account != null) {
            if (!accounts.contains(userid)) {
                account.increRound();
                accounts.add(userid);
            }
            account.increCostCoin(betcoin);
            account.increWinCoin(wincoin);
        }
        try {
            if (!testrun) updateGameUserCoins(account, userid, 0, wincoin, betcoin, now, "", "游戏结算table=" + his.getTableid() + ", betpos=" + entry.getBetpos());
        } catch (Exception t) {
            e = t;
        }
        if (humanBanker && !UserInfo.isRobot(userid)) return; //人人
        long taxcoin = humanBanker ? (bankerself ? robotTaxCoins.get() : 0) : updatePoolCoin(1, userid, -wincoin, his.getTableid(), now, "settle-" + entry.getBetpos(), "电脑坐庄" + index.incrementAndGet());
        his.setBetcoins(his.getBetcoins() + betcoin);
        his.setTaxcoin(his.getTaxcoin() + taxcoin);
        his.setOswincoins(his.getOswincoins() - wincoin);

        HundredGameRound rs = new HundredGameRound();
        if (e != null) rs.setRemark("更新用户金币数失败");
        rs.setGameid(gameId());
        rs.setUserid(userid);
        rs.setRoomlevel(1);
        rs.setBetpos(entry.getBetpos());
        rs.setBetcoin(betcoin);
        rs.setWincoin(wincoin);
        rs.setTaxcoin(taxcoin);
        rs.setFactor(betcoin < 1 ? 0 : (int) (Math.abs(wincoin) / betcoin));
        rs.setFinishtime(now);
        rs.setTableid(his.getTableid());
        rs.setRoundid(his.getTableid() + "-" + Integer.toString(userid, 36) + "-" + entry.getBetpos() + "-" + nodeid);
        if (!testrun) insertQueue.add(rs);
    }

    protected void doRoundPreStart() {
        if (this.lastGameResult != null) {
            this.recentResults.addFirst(this.lastGameResult);
            while (recentResults.size() > maxRecentResultCount()) recentResults.removeLast();
            this.lastGameResult = null;
        }
        streamLivingPlayer().filter(p -> !p.isRobot() && !p.isOnline()).forEach(x -> leaveGame(x.getUserid(), null));
        leaveRobotGame(streamLivingPlayer().filter(p -> p.isRobot()));
        int newRemainSeconds = (int) (this.nextRoundStartTime - System.currentTimeMillis()) / 1000;
        streamLivingPlayer().filter(p -> !p.isRobot()).forEach(p -> {
            sendMap(p, "onTablePreStartMessage", new RetResult(ofMap("remains", newRemainSeconds, "usercoins", p.getCoins())));
        });
    }

    protected void leaveRobotGame(Stream<HundredGamePlayer> players) {
        int[] userids = players.mapToInt(p -> p.getUserid()).toArray();
        if (userids.length < 2) return;
        for (int userid : userids) {
            leaveGame(userid, true);
        }
        userLeaveGame(userids);
    }

    protected void doRobotBetting() {
        if (!this.confRobotActivate) return; //禁止机器人
        final GT table = this.currTable;
        if (table == null) return;
        final HundredGameRound round = table.currRound;
        if (round == null || round.getRoudStatus() != GameRound.ROUND_STATUS_BETTING) return;
        final long now = System.currentTimeMillis();
        if (now - round.getCreatetime() <= 2_000) return; //前2秒不加机器人
        if (now - round.getCreatetime() >= bettingMills() - 2_000) return; //后2秒不加机器人
        //继续押注
        int[] indexs = new int[gameRandom.nextInt(2) + 1];
        for (int i = 0; i < indexs.length; i++) {
            indexs[i] = gameRandom.nextInt(round.getBetOptionEntrys().length);
        }
        for (int i = 0; i < indexs.length; i++) {
            int count = gameRandom.nextInt(joinRobotNumCount());
            if ((streamLivingPlayer().filter(u -> u.isRobot()).count() + count) > joinRobotNumLimit()) break;
            List<UserInfo> robots = randomRobot(gameId(), count);

            for (UserInfo robot : robots) {
                HundredGamePlayer player = new HundredGamePlayer(robot, null, null, 1, 0);
                putLivingPlayer(player.getUserid(), player);
                long onebetcoins = betCoinOptions[ShuffleRandom.random(gameRandom, betCoinWeights)];
                int betcount = (gameRandom.nextInt(3) + 1);
                long betcoins = onebetcoins * betcount;
                if (betcoins > robot.getCoins()) {
                    onebetcoins = betCoinOptions[0];
                    betcount = 1;
                }
                //if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, bean + " 押注次数: " + betcount + ", 押注金币: " + onebetcoins);
                for (int j = 0; j < betcount; j++) {
                    final HundredBetEntry bean = new HundredBetEntry();
                    bean.setBetpos(indexs[i] + 1);
                    bean.setBetcoin(onebetcoins);
                    RetResult rs = this.betCoin(robot.getUserid(), bean);
                    if (!rs.isSuccess()) {
                        if (round.getUserBetCoinSum(robot.getUserid()) < 1) super.leaveGame(robot.getUserid());
                        logger.log(Level.INFO, "robot betCoins error: userid=" + robot.getUserid() + ", " + rs);
                        break;
                    }
                }
            }
        }
    }

    //计算指定结果下，真实玩家总共赢得的金币数
    protected long calcHumanWinCoin(final GT table, final HundredGameRound round, final HundredGameResult result, final AtomicLong taxcoin) {
        final Map<Integer, HundredBetEntry>[] betEntrys = round.getBetOptionEntrys();
        final boolean humanBanker = this.currBanker != null && !this.currBanker.isRobot();
        final AtomicLong humanWinCoins = new AtomicLong();
        taxcoin.set(0);
        for (int i = 0; i < betEntrys.length; i++) {
            final int betEntryIndex = i;
            betEntrys[i].entrySet().forEach(en -> {
                final int userid = en.getKey();
                if (this.currBanker.getUserid() == userid) return;
                if (humanBanker && !UserInfo.isRobot(userid)) return;
                if (!humanBanker && UserInfo.isRobot(userid)) return;
                final HundredBetEntry entry = en.getValue();
                long winc = calcBetEntryWinCoin(table, result, betEntryIndex, entry.getBetcoin());
                if ((winc > 0 && UserInfo.isRobot(userid)) || (winc < 0 && !UserInfo.isRobot(userid))) {
                    long tax = winc * (this.confCoinPoolTaxPermillage + this.confCoinPool2Permillage + this.confCoinPool3Permillage) / 1000;
                    taxcoin.addAndGet(Math.abs(tax));
                    winc = winc - tax;
                }
                humanWinCoins.addAndGet(UserInfo.isRobot(userid) ? -winc : winc);
            });
        }
        return humanWinCoins.get();
    }

    @Comment("创建新的房间")
    protected final GT createGameTable() {
        return (GT) new HundredGameTable();
    }

    @Comment("加载最近记录")
    protected final void loadRecentResults() {
        dataSource().queryListAsync(HundredGameTableHis.class, SelectColumn.includes("resultid", "cards"), new Flipper(maxRecentResultCount(), "finishtime DESC"), FilterNode.create("#finishtime", System.currentTimeMillis())).whenComplete((r, t) -> {
            if (r != null) {
                for (HundredGameTableHis his : r) {
                    recentResults.add(new HundredGameResult(his.getResultid(), his.getCards()));
                }
            }
        });
    }

    protected static int[] calcWeightIndexs(int[] weights) {
        int size = 0;
        for (int w : weights) {
            size += w;
        }
        int[] newWeights = new int[size];
        int index = -1;
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i]; j++) {
                newWeights[++index] = i;
            }
        }
        return newWeights;
    }

    protected static int[] calcWeightIds(int[] weights, int[] ids) {
        int size = 0;
        for (int w : weights) {
            size += w;
        }
        int[] newWeights = new int[size];
        int index = -1;
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i]; j++) {
                newWeights[++index] = ids[i];
            }
        }
        return newWeights;
    }

    @Override
    protected int roomLevelSize() {
        return 1;
    }

    @Comment("结果枚举值，返回null表示不能枚举")
    protected abstract int[] resultIdEnumArray();

    @Comment("计算结果")
    protected abstract HundredGameResult calcRoundResult(final GT table, final HundredGameRound round);

    //计算单项的金币输赢, betindex从0开始， 数组下坐标
    protected abstract long calcBetEntryWinCoin(final GT table, final HundredGameResult result, final int betindex, final long betcoins);

    @Comment("获取押注项个数")
    protected abstract int betEntryCount();

    @Comment("计算当前庄家可能赔的最大金币数")
    protected abstract long calcMaxMaybeLoseCoins(GT table, HundredBetEntry bean);

    @Comment("是否能上庄")
    protected abstract boolean canApplyBanker();

    @Comment("初始化座位列表， 为null表示无座位功能")
    protected abstract HundredGamePlayer[] createSitPositions();

    @Comment("回合结果排名的数量")
    protected abstract int topWinnerLimit();

    @Comment("加载最近记录最大条数")
    protected abstract int maxRecentResultCount();

    @Comment("随机加入的电脑玩家数上限")
    protected abstract int joinRobotNumLimit();

    @Comment("随机加入的电脑玩家数")
    protected abstract int joinRobotNumCount();

    @Comment("机器人加入频率毫秒数")
    protected abstract long joinRobotRateMills();

    @Comment("一轮的押注总毫秒数")
    protected abstract long bettingMills();

    @Comment("一轮的耗时毫秒数")
    protected abstract long roundMills();

}
