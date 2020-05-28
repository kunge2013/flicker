/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.game.GameActionEvent;
import com.cratos.platf.base.*;
import com.cratos.platf.game.*;
import static com.cratos.platf.game.GameTable.TABLE_STATUS_FINISHED;
import static com.cratos.platf.game.multi.MultiGameTable.*;
import static com.cratos.platf.util.Utils.HEADNAME_WS_SNCP_ADDRESS;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.redkale.convert.json.*;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.source.*;
import org.redkale.util.*;
import static org.redkale.util.Utility.ofMap;

/**
 * 房间游戏
 *
 * @author zhangjx
 * @param <GT>      MultiGameTable
 * @param <R>       MultiGameRound
 * @param <P>       GamePlayer
 * @param <MGTBean> GameTableBean
 */
@AutoLoad(false)
public abstract class MultiCoinGameService<GT extends MultiGameTable<R, P>, R extends MultiGameRound, P extends MultiGamePlayer, MGTBean extends MultiGameTableBean> extends CoinGameService<GT, P, MGTBean> {

    @Comment("当前正在进行的房间")
    protected final Queue<GT> currTables = new ConcurrentLinkedQueue();

    //最少多少人准备完后就可以开始， =0表示必须坐满且都准备完才开始
    protected abstract int minStartReadyedCount();

    //其他分发
    protected abstract void doGameAction(final GT table, final GameActionEvent actevent);

    //创建房间
    protected abstract RetResult<GT> createGameTable(P player, MGTBean bean);

    protected abstract P createGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean);

    @Override
    public void init(AnyValue config) {
        super.init(config);
        initGame(3);
    }

    protected void initCoinTableGame(int[] roomBaseCoins, Class<P> playerClass) {
        this.roomBaseCoins = roomBaseCoins;
    }

    //房卡模式
    protected boolean modeTicket() {
        return false;
    }

    protected void switchGameAction(final GT table, final GameActionEvent actevent) {
        switch (actevent.getActid()) {
            case ACTID_TABLE_FINISH: //房间结束
                doTableFinishAction(table, actevent);
                break;
            case ACTID_TABLE_DISMISS: //房间解散
                doTableDismissAction(table, actevent);
                break;
            case ACTID_ROUND_PRESTART: //中转预备
                doRoundPreStartAction(table, actevent);
                break;
            case ACTID_ROUND_PREREADY: //新回合预备
                doRoundPreReadyAction(table, actevent);
                break;
            case ACTID_ROUND_START: //回合开始发牌
                doRoundStartAction(table, actevent);
                break;
            case ACTID_ROUND_FINISH: //回合结算处理
                doRoundFinishAction(table, actevent);
                break;
            case ACTID_ROBOT_JOIN: //电脑加入
                doRobotJoinAction(table, actevent);
                break;
            case ACTID_PLAYER_READY: //玩家准备
                doPlayerReadyAction(table, actevent);
                break;
            default:
                doGameAction(table, actevent);
                break;
        }
    }

    @Local
    public RetResult addRobotJoinAction(final GT table, long delay, final P robot, Object... attrs) {
        return addGameAction(table, new GameActionEvent(robot.getUserid(), delay, ACTID_ROBOT_JOIN, attrs).attributes("robot", robot));
    }

    @Local
    public RetResult addRoundPreStartAction(final GT table, Object... attrs) {
        return addRoundPreStartAction(table, table.delayActRoundPreStart(), attrs);
    }

    @Local
    public RetResult addRoundPreStartAction(final GT table, long delay, Object... attrs) {
        return addGameAction(table, new GameActionEvent(UserInfo.USERID_SYSTEM, delay, ACTID_ROUND_PRESTART, attrs));
    }

    @Local
    public RetResult addRoundPreReadyAction(final GT table, Object... attrs) {
        return addRoundPreReadyAction(table, ofMap(attrs));
    }

    @Local
    public RetResult addRoundPreReadyAction(final GT table, Map<String, Object> map) {
        return addGameAction(table, new GameActionEvent(UserInfo.USERID_SYSTEM, table.delayActRoundPreReady(), ACTID_ROUND_PREREADY, map));
    }

    @Local
    public RetResult addPlayerAutoReadyAction(final GT table, final P player, Object... attrs) {
        return addGameAction(table, new GameActionEvent(player.getUserid(), 1000L, ACTID_PLAYER_READY, attrs).attributes("auto_ready", true));
    }

    @Local
    public RetResult addPlayerReadyAction(final GT table, final P player, Object... attrs) {
        long deplay = player.isRobot() ? (1000L + gameRandom.nextInt(1000)) : table.delayActPlayerReady();
        return addGameAction(table, new GameActionEvent(player.getUserid(), deplay, ACTID_PLAYER_READY, attrs));
    }

    @Local
    public RetResult addRoundStartAction(final GT table, Object... attrs) {
        return addRoundStartAction(table.delayActRoundStart(), table, attrs);
    }

    @Local
    public RetResult addRoundStartAction(long delay, final GT table, Object... attrs) {
        return addGameAction(table, new GameActionEvent(UserInfo.USERID_SYSTEM, delay, ACTID_ROUND_START, attrs));
    }

    @Local
    public RetResult addRoundFinishAction(final GT table, P winPlayer, Object... attrs) {
        return addRoundFinishAction(table, table.delayActRoundFinish(), winPlayer, attrs);
    }

    @Local
    public RetResult addRoundFinishAction(final GT table, long delay, P winPlayer, Object... attrs) {
        GameActionEvent event = new GameActionEvent(UserInfo.USERID_SYSTEM, delay, ACTID_ROUND_FINISH, attrs);
        if (winPlayer != null) event.addAttribute("winPlayer", winPlayer);
        return addGameAction(table, event);
    }

    @Local
    public RetResult addTableFinishAction(final GT table, final short finishtype) {
        return addTableFinishAction(table, finishtype, null, null);
    }

    @Local
    public RetResult addTableFinishAction(final GT table, final short finishtype, final P player, final Map<String, String> bean, Object... attrs) {
        GameActionEvent event = new GameActionEvent(UserInfo.USERID_SYSTEM, table.delayActTableFinish(), ACTID_TABLE_FINISH, attrs);
        event.addAttribute("finishtype", finishtype);
        if (player != null) event.addAttribute("player", player);
        if (bean != null) event.addAttribute("bean", bean);
        return addGameAction(table, event);
    }

    @Local
    private RetResult addTableDismissAction(final GT table, final short finishtype, final P player, final Map<String, String> bean, Object... attrs) {
        if (table.getFinishtime() < 1) return addTableFinishAction(table, finishtype, player, bean, attrs);
        GameActionEvent event = new GameActionEvent(UserInfo.USERID_SYSTEM, table.delayActTableDismiss(), ACTID_TABLE_DISMISS, attrs);
        if (player != null) event.addAttribute("player", player);
        if (bean != null) event.addAttribute("bean", bean);
        return addGameAction(table, event);
    }

    //房间结算
    protected RetResult doTableFinishAction(final GT table, final GameActionEvent actevent) {
        try {
            table.setTableStatus(TABLE_STATUS_FINISHED);
            table.setFinishtime(System.currentTimeMillis());
            RetResult<MultiTableResult> rs = table.tableFinish(this, actevent);
            if (logger.isLoggable(Level.FINEST)) logger.finest("房间结束：" + table.getClass().getSimpleName() + " (" + table + ") 结果：" + rs);
            if (rs.isSuccess() && rs.getResult() != null) {
                short finishType = actevent.getAttribute("finishtype");
                rs.getResult().setFinishtype(finishType);
                //if (logger.isLoggable(Level.FINEST)) logger.finest("onTableFinishMessage 在线人员: " + table.onlinePlayers()); 
                this.sendMap(table.onlinePlayers(), "onTableFinishMessage", rs);
            }
            doAfterTableFinish(table, rs, actevent);
        } finally {
            GameActionEvent event = new GameActionEvent(UserInfo.USERID_SYSTEM, table.delayActTableDismiss(), ACTID_TABLE_DISMISS, actevent.getAttributes());
            return addGameAction(table, event);
        }
    }

    //房间结算之后
    protected RetResult doAfterTableFinish(final GT table, RetResult<MultiTableResult> rs, final GameActionEvent actevent) {
        return null;
    }

    //解散房间
    protected RetResult doTableDismissAction(GT table, final GameActionEvent actevent) {
        P player = actevent.getAttribute("player");
        Map<String, String> bean = actevent.getAttribute("bean");
        if (!currTables.contains(table)) return RetResult.success();
        synchronized ((modeTicket() ? table : lockDataCoinPool(table.getRoomlevel()))) { //防止解散时刚好加入房间
            if (logger.isLoggable(Level.FINEST)) logger.finest("房间解散：" + table.getClass().getSimpleName() + " (" + table + ")");
            currTables.remove(table);
            table.removeAllActionEvents();
            doAfterTableDismiss(table, actevent);
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
        }
        if (player != null && !player.isRobot()) {
            RetResult rr = new RetResult(Utility.ofMap("userid", player.getUserid()));
            if (bean != null && bean.containsKey("retinfo")) rr.retinfo(bean.get("retinfo"));
            sendMap(player, "onPlayerLeaveMessage", rr);
        }
        return RetResult.success();
    }

    //房间解散之后
    protected void doAfterTableDismiss(final GT table, final GameActionEvent actevent) {
    }

    //中转预备
    protected RetResult doRoundPreStartAction(final GT table, final GameActionEvent actevent) {
        if (actevent == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL); //防止并发情况
        synchronized (actevent) {
            if (!table.removeAction(actevent)) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL); //防止并发情况
            addGameAction(table, new GameActionEvent(UserInfo.USERID_SYSTEM, 1L, ACTID_ROUND_START));
        }
        return RetResult.success();
    }

    //回合结算
    protected RetResult doRoundFinishAction(final GT table, final GameActionEvent actevent) {
        P winPlayer = actevent.getAttribute("winPlayer");
        AtomicReference<P> winRef = new AtomicReference(winPlayer);
        RetResult<MultiRoundResult> rs = table.roundFinish(this, actevent, winRef);
        winPlayer = winRef.get(); //可能会在roundFinish里更改winPlayer
        if (!rs.isSuccess()) return doAfterRoundFinished(table, winPlayer, rs, null, 0);
        final int roomlevel = table.getRoomlevel();
        MultiRoundResult result = rs.getResult();
        final long now = result.getFinishtime();
        final MultiGameRound round = table.getCurrRound();
        long taxcoin = 0;
        final List<MultiRoundResultPlayer> humanResultPlayers = new ArrayList<>();
        long humanChaCoin = 0;
        long osWinCoin = 0;
        long taxCoin = 0;
        this.chargeRoundDiamond(table); //扣费

        if (result.getPlayers() != null) { //可能为null，表示无真实玩家押注
            for (final Object item : result.getPlayers()) {
                MultiRoundResultPlayer rrp = (MultiRoundResultPlayer) item;
                if (rrp.getRoundCoin() == 0) continue;
                if (!UserInfo.isRobot(rrp.getUserid())) {
                    humanChaCoin += rrp.getRoundCoin();
                    long tax = rrp.getRoundCoin() * this.getConfCoinPoolTaxPermillage() / 1000;
                    taxCoin += tax;
                    osWinCoin -= rrp.getRoundCoin() - tax;
                }
                //---------更新余额------------------
                if (rrp.getRoundCoin() > 0) { //扣税
                    long tax = rrp.getRoundCoin() * this.getConfCoinPoolTaxPermillage() / 1000;
                    taxcoin += tax;
                    rrp.setRoundCoin(rrp.getRoundCoin() - tax);
                }
                MultiGamePlayer gamePlayer = table.findPlayer(rrp.getUserid());
                if (gamePlayer == null) {
                    logger.log(Level.SEVERE, "doRoundFinishAction error, findPlayer (userid=" + rrp.getUserid() + ") is null");
                } else {
                    gamePlayer.increCoin(rrp.getRoundCoin());
                    rrp.setCoins(gamePlayer.getCoins());
                }
                if (gamePlayer != null && !gamePlayer.isRobot()) {
                    humanResultPlayers.add(rrp);
                }
            }
        }
        for (P player : table.getPlayers()) {
            if (player != null) player.increTableCoinScore();
        }
        if (!modeTicket()) {
            synchronized (lockDataCoinPool(roomlevel)) {
                final long humanFreezeCoin = round.getPreFreezePoolCoin();
                if (humanFreezeCoin > 0) {
                    unfreezeGameUserCoins(roomlevel, humanFreezeCoin, now, "开局解冻; roundid=" + round.getRoundid());
                    increUnlockPool1Coin(roomlevel, UserInfo.USERID_SYSTEM, humanFreezeCoin, round.getRoundid(), now, "unfreeze", "开局解冻");
                }
                if (humanChaCoin != 0) {
                    if (osWinCoin > 0) { //系统赢了
                        increUnlockPool1Coin(roomlevel, UserInfo.USERID_SYSTEM, -humanChaCoin, Math.abs(taxCoin), osWinCoin, round.getRoundid(), now, "settle", "系统输赢");
                    } else { //系统输了
                        decreUnlockPool1Coin(roomlevel, UserInfo.USERID_SYSTEM, -osWinCoin, round.getRoundid(), now, "settle", "系统输赢");
                    }
                }
            }
        }
        for (final MultiRoundResultPlayer rrp : humanResultPlayers) {
            MultiGameAccount account = loadAccount(rrp.getUserid());
            account.increRound();
            account.increWinCoin(rrp.getRoundCoin());
            if (!modeTicket()) {
                try {
                    //可能会堵塞，不能放在 synchronized 里
                    updateGameUserCoins(account, rrp.getUserid(), table.getRoomlevel(), rrp.getRoundCoin(), rrp.getRoundCostCoin(), now, "", "roundid=" + table.getCurrRound().getRoundid());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "GamePlayer(" + rrp + ") increGameUserCoins error ", e);
                }
            }
        }
        Map msg = ofMap("onRoundFinishMessage", new RetResult(result));
        CompletableFuture msgFuture = sendMessage(table.onlinePlayers(), msg);
        table.addEventRecord(msg);
        return doAfterRoundFinished(table, winPlayer, rs, msgFuture, taxcoin);
    }

    //玩家结算后进行的操作
    protected RetResult doAfterRoundFinished(final GT table, P winPlayer, RetResult<MultiRoundResult> rs, CompletableFuture msgFuture, long roundTaxCoin) {
        long finishtime = rs.isSuccess() ? rs.getResult().getFinishtime() : System.currentTimeMillis();
        R round = table.getCurrRound();
        if (round != null) {
            MultiGameRound roundhis = round.createRoundHis(table, nodeid, finishtime, rs.getResult(), roundTaxCoin);
            if (roundhis != null) insertQueue.add(roundhis);
            insertVideoRound(table, round, rs.getResult());
        }
        Map<String, Object> map = table.afterRoundFinished(winPlayer, rs.getResult());
        if (modeTicket() && table.getMaxRoundCount() > 0 && table.getCurrRoundIndex() >= table.getMaxRoundCount()) {
            if (msgFuture == null) {
                addTableFinishAction(table, TABLE_FNISHTYPE_NORMAL);
            } else {
                msgFuture.whenComplete((a, t) -> addTableFinishAction(table, TABLE_FNISHTYPE_NORMAL));
            }
        } else {
            addRoundPreReadyAction(table, map);
        }
        return RetResult.success();
    }

    protected void insertVideoRound(GT table, R round, MultiRoundResult roundResult) {
    }

    @Comment("扣费")
    protected void chargeRoundDiamond(final GT table) {
    }

    //电脑加入
    protected RetResult doRobotJoinAction(final GT table, final GameActionEvent actevent) {
        P robot = actevent.getAttribute("robot");
        RetResult<P> rr = table.addPlayer(robot, this);
        if (!rr.isSuccess()) {
            userLeaveGame(robot.getUserid());
            return RetResult.success();
        }
        P robotPlayer = rr.getResult();
        putLivingPlayer(robotPlayer.getUserid(), robotPlayer);
        sendMap(table.onlinePlayers(robotPlayer.getUserid()), "onPlayerJoinMessage", new RetResult(robotPlayer));
        return addPlayerAutoReadyAction(table, robotPlayer);
    }

    //玩家准备
    protected RetResult doPlayerReadyAction(final GT table, final GameActionEvent actevent) {
        synchronized (actevent) {
            if (actevent.getAttribute("dummy") == null && !table.removeAction(actevent)) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL); //防止并发情况
            boolean expired = actevent.isExpired();
            if (logger.isLoggable(Level.FINEST)) logger.finest("准备操作: userid=" + actevent.getUserid() + ", expired=" + expired);

            if (actevent.isRobot() || actevent.getAttribute("auto_ready") != null || !expired) { //机器人超时或者真实玩家准备  
                RetResult<Integer> rr = table.readyPlayer(actevent.getUserid());
                if (rr.isSuccess()) {
                    sendMap(table.onlinePlayers(), "onPlayerReadyMessage", new RetResult(Utility.ofMap("userid", actevent.getUserid())));
                    return doAfterPlayerReadyed(table, rr);
                } else {
                    if (logger.isLoggable(Level.FINEST)) logger.finest("准备失败: userid=" + actevent.getUserid() + ", result=" + rr);
                    return leaveGame(actevent.getUserid(), ofMap("retinfo", rr.getRetinfo()));
                }
            } else { //真实玩家超时，踢出玩家    
                return doPlayerReadyedExpired(table, actevent.getUserid());
            }
        }
    }

    //玩家准备过期进行的操作
    protected RetResult doPlayerReadyedExpired(final GT table, int userid) {
        if (minStartReadyedCount() > 0) return RetResult.success();
        return leaveGame(userid, ofMap("retinfo", GameRetCodes.retInfo(GameRetCodes.RET_GAME_ACTEVENT_TIMEOUT)));
    }

    //玩家准备完后进行的操作
    protected RetResult doAfterPlayerReadyed(final GT table, final RetResult<Integer> lastReadyResult) {
        if (minStartReadyedCount() > 0) {
            synchronized (table) {
                final GameActionEvent preStartEvent = table.findAction(UserInfo.USERID_SYSTEM, ACTID_ROUND_PRESTART);
                if (lastReadyResult.getResult() != 0) {
                    if (table.getReadyedPlayerSize() >= minStartReadyedCount() && table.isAllReadyed()) {
                        //最小可玩玩家数都已准备好
                    } else {
                        if (table.getReadyedPlayerSize() >= minStartReadyedCount() && preStartEvent == null) {
                            addGameAction(table, new GameActionEvent(UserInfo.USERID_SYSTEM, DELAY_ACT_ROUND_PRESTART, ACTID_ROUND_PRESTART));
                            sendMap(table.onlinePlayers(), "onRoundPreStartMessage", new RetResult(Utility.ofMap("remains", DELAY_ACT_ROUND_PRESTART / 1000)));
                        }
                        return RetResult.success();
                    }
                }
                if (preStartEvent != null) table.removeAction(preStartEvent);
                //玩家全部准备完毕
                addGameAction(table, new GameActionEvent(UserInfo.USERID_SYSTEM, 10L, ACTID_ROUND_START));
            }
            return RetResult.success();
        }
        if (lastReadyResult.getResult() != 0) return RetResult.success();
        //玩家全部准备完毕
        addGameAction(table, new GameActionEvent(UserInfo.USERID_SYSTEM, table.delayActRoundStart(), ACTID_ROUND_START));
        return RetResult.success();
    }

    //回合开始
    protected RetResult doRoundStartAction(final GT table, final GameActionEvent actevent) {
        if (actevent == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL);
        List<GameActionEvent> eventList = null;
        synchronized (actevent) {
            if (!table.removeAction(actevent)) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL); //防止并发情况     
            RetResult<MultiRoundStartBean> rs = table.roundStart(this, actevent);
            MultiRoundStartBean starBean = rs.getResult(); //success=false也可能会有events
            eventList = starBean == null ? null : starBean.getEvents();
            if (eventList != null && !eventList.isEmpty()) { //失败了也可能有event
                for (GameActionEvent event : eventList) {
                    addGameAction(table, event);
                }
            }
            if (rs.isSuccess()) {
                final Map takeMsg = Utility.ofMap("onRoundStartMessage", starBean != null && starBean.getAttributes() != null ? new RetResult(starBean.getAttributes()) : new RetResult(table));
                table.addEventRecord(takeMsg);
                for (final P player : table.onlinePlayers()) {
                    sendMessage(createTableConvert(table, player), player, takeMsg);
                }
            } else {
                logger.log(Level.SEVERE, "RoundStart失败, rs=" + rs + ", table=" + table);
                return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ERROR);
            }
        }
        return doAfterRoundStarted(table, eventList);
    }

    //回合开始后进行的操作
    protected RetResult doAfterRoundStarted(final GT table, List<GameActionEvent> eventList) {
        return RetResult.success();
    }

    //预备开局, 只会从RoundFinish过来
    protected RetResult doRoundPreReadyAction(final GT table, final GameActionEvent actevent) {
        if (actevent == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL);
        synchronized (actevent) {
            if (!table.removeAction(actevent)) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL); //防止并发情况     

            table.roundReset(this);
            if (!modeTicket()) {
                final Range.LongRange range = this.confRoomCoinStages[table.getRoomlevel() - 1];
                for (P p : table.getPlayers()) {
                    if (p == null) continue;
                    if (!range.test(p.getCoins())) {
                        ////金币不够   
                        leaveGame(p.getUserid(), ofMap("retinfo", GameRetCodes.retInfo(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL)));
                    } else if (!p.isRobot() && !p.isOnline()) {
                        if (table.removePlayer(p.getUserid()).isSuccess()) {
                            leaveGame(p.getUserid());
                            sendMap(table.onlinePlayers(), "onPlayerLeaveMessage", new RetResult(Utility.ofMap("userid", p.getUserid())));
                        } else {
                            logger.log(Level.SEVERE, "玩家预备时无法踢出, table=" + table);
                        }
                    }
                }
                if (canDismissTable(table)) { //解散了
                    RetResult rs = addTableDismissAction(table, TABLE_FNISHTYPE_PLATFDISMISS, null, ofMap("retinfo", GameRetCodes.retInfo(GameRetCodes.RET_GAME_TABLE_AUTODISMISS)));
                    if (finest) logger.log(Level.FINE, "没有真实玩家存在，房间应该解散了, table=" + table);
                    return rs;
                }
            }
        }
        return doAfterRoundPreReadyed(table);
    }

    //玩家全部准备完后进行的操作
    protected RetResult doAfterRoundPreReadyed(final GT table) {
        long robotdelay = 1500;
        for (P p : table.getPlayers()) {
            if (p == null) continue;
            long delay = p.isRobot() ? robotdelay : (table.delayActRoundResult() + table.delayActPlayerReady());
            if (p.isRobot()) robotdelay += gameRandom.nextInt(2000);
            addGameAction(table, new GameActionEvent(p.getUserid(), (1000 + delay), ACTID_PLAYER_READY));
        }
        return RetResult.success();
    }

    public RetResult addGameAction(GT table, GameActionEvent event) {
        if (event.getUserid() == UserInfo.USERID_SYSTEM) {
            synchronized (table.getActions()) {
                if (table.containsAction(event.getActid())) {
                    logger.log(Level.SEVERE, gameId() + " 重复ACTID操作 " + event.getActid(), new Exception(table.getClass().getSimpleName() + " tableid=" + table.getTableid()));
                    return RetCodes.retResult(RetCodes.RET_REPEAT_ILLEGAL);
                }
                table.addAction(event);
            }
        } else {
            table.addAction(event);
        }
        return scheduleGameAction(event, () -> {
            try {
                switchGameAction(table, event);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "switchGameAction error, event=" + event, t);
            }
        });
    }

    @Override
    @RestMapping(auth = false, comment = "平台用户信息更新通知")
    public RetResult<String> notifyPlatfPlayer(final int userid, final Map<String, String> bean) {
        P player = findLivingPlayer(userid);
        if (player == null) return RetResult.success();
        UserInfo user = findUserInfo(userid);
        player.copyFromUserAndOnline(user, player.getClientAddr(), player.sncpAddress());
        return RetResult.success();
    }

    @Override
    protected <T> RetResult<T> authEnterGame(int userid, UserInfo user, Map<String, String> bean) {
        int roomlevel = bean == null ? 0 : Integer.parseInt(bean.get("roomlevel"));
        if (roomLevelSize() > 0 && (roomlevel < 1 || roomlevel > this.confRoomCoinStages.length)) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
        if (roomLevelSize() > 0 && !this.confRoomCoinStages[roomlevel - 1].test(user.getCoins())) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);
        return null;
    }

    @Override
    protected P loadGamePlayer(UserInfo user, @RestAddress String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean, P oldPlayer) {
        if (oldPlayer != null) return oldPlayer.copyFromUserAndOnline(user, clientAddr, sncpAddress);
        P player = findLivingPlayer(user.getUserid());
        if (player != null) return player.copyFromUserAndOnline(user, clientAddr, sncpAddress);
        return createGamePlayer(user, clientAddr, sncpAddress, roomlevel, bean);
    }

    @Override
    @RestMapping(auth = true, comment = "进入游戏")
    public <T> RetResult<T> enterGame(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, Map<String, String> bean) {
        P player = findLivingPlayer(userid);
        int[] baseCoins = this.roomBaseCoins;
        if (baseCoins != null && baseCoins.length == 0) baseCoins = null;
        if (player != null) {
            player.online(clientAddr, sncpAddress);
            if (player.table() != null) {
                sendMap(player.table().onlinePlayers(userid), "onPlayerOnlineMessage", new RetResult(Utility.ofMap("userid", player.getUserid())));
            }
            return new RetResult(Utility.ofMap("roomlevel", player.getRoomlevel(), "basecoin", baseCoins == null ? 0 : baseCoins[player.getRoomlevel() - 1]));
        }
        RetResult rs = super.enterGame(userid, clientAddr, sncpAddress, bean);
        if (!rs.isSuccess()) return rs;
        final int roomlevel = Integer.parseInt(bean.getOrDefault("roomlevel", "0"));
        return new RetResult(Utility.ofMap("roomlevel", roomlevel, "basecoin", baseCoins == null ? 0 : baseCoins[roomlevel - 1]));
    }

    protected Object leaveStart(P player, GT table, Map<String, String> bean) {
        return null;
    }

    protected void leaveEnd(P player, GT table, Object startObject, Map<String, String> bean) {
    }

    protected boolean canDismissTable(GT table) {
        return table.isAllRobot();
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
            addTableDismissAction(table, TABLE_FNISHTYPE_PLATFDISMISS, player, bean);
            if (bean != null && bean.containsKey("retinfo")) {
                RetResult rr = new RetResult(Utility.ofMap("userid", player.getUserid()));
                rr.retinfo(bean.get("retinfo"));
                sendMap(player, "onPlayerLeaveMessage", rr).join();
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

    @RestMapping(auth = true, comment = "玩家准备")
    public RetResult readyPlayer(int userid, Map<String, String> bean) {
        GT table = findTableByPlayerid(userid);
        if (finer) logger.log(Level.FINER, "玩家准备" + userid + " tableid=" + (table == null ? "0" : table.getTableid()));
        if (table == null) return (RetResult) leaveGame(userid, null);
        GameActionEvent actevent = table.findAction(userid, ACTID_PLAYER_READY);
        if (actevent == null && minStartReadyedCount() < 1 && !modeTicket()) return GameRetCodes.retResult(GameRetCodes.RET_GAME_ACTEVENT_ILLEGAL);
        if (actevent == null && (minStartReadyedCount() > 0 || modeTicket())) actevent = new GameActionEvent(userid, 1, ACTID_PLAYER_READY).addAttribute("dummy", "true");
        return doPlayerReadyAction(table, actevent.addAttribute("auto", "false"));
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
    protected RetResult<GT> joinOldTable(GT table, P player, MGTBean bean) {
        return new RetResult(createTableConvert(table, player), table);
    }

    //进入已有房间
    protected RetResult<GT> joinRunTable(GT table, P player, MGTBean bean) {
        return new RetResult(table);
    }

    //创建新房间之后的操作
    protected RetResult<GT> joinNewTable(GT table, P player, MGTBean bean) {
        return new RetResult(table);
    }

    //可定制，自动加入机器人数
    protected int autoJoinRobot(GT table, P player, MGTBean bean) {
        return 0;
    }

    @Override
    @RestMapping(auth = true, comment = "玩家加入房间")
    public RetResult<GT> joinTable(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, final MGTBean bean) {
        P player0 = findLivingPlayer(userid);
        if (modeTicket() && player0 == null) {
            RetResult ers = enterGame(userid, clientAddr, sncpAddress, bean.createMap());
            if (!ers.isSuccess()) return ers;
            player0 = findLivingPlayer(userid);
        }
        final P player = player0;
        if (bean != null) bean.setUserid(userid);
        if (player == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        GT oldTable = player.table();
        if (oldTable != null) {//掉线后重新进入房间
            RetResult<GT> rs = joinOldTable(oldTable, player, bean);
            if (rs.isSuccess()) updateAccountOnLine(userid);
            return rs;
        }

        final int roomlevel = player.getRoomlevel();
        if (roomLevelSize() > 0) {
            if (roomlevel < 1 || roomlevel > this.confRoomCoinStages.length) {
                return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
            }
            Range.LongRange range = this.confRoomCoinStages[roomlevel - 1];
            if (!range.test(player.getCoins())) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);
        }
        final GameService service = this;
        synchronized (roomLevelSize() > 0 ? (modeTicket() ? player : lockDataCoinPool(roomlevel)) : player) {
            AtomicReference<GT> ref = new AtomicReference(null);
            GT old = player.table();
            if (old == null) {
                if (bean != null && bean.getTableid() != null && !bean.getTableid().isEmpty()) {
                    GT optTable = currTables.stream().filter(t -> bean.getTableid().equals(t.getTableid())).findAny().orElse(null);
                    if (optTable == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_NOTEXISTS);
                    RetResult rr = optTable.addPlayer(player, service);
                    if (!rr.isSuccess()) return rr;
                    ref.set(optTable);
                } else if (bean == null || bean.getClubid() < 1) {
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
                if (minStartReadyedCount() < 1 && table.getTableStatus() != TABLE_STATUS_GAMEING) addPlayerAutoReadyAction(table, player);
                RetResult<GT> rs = joinRunTable(table, player, bean);
                if (rs.isSuccess()) updateAccountOnLine(userid);
                return rs;
            }
            int chargePerOneDiamond = getChargePerOneDiamond(player, bean);
            RetResult rrb = checkCreateTableBean(player, chargePerOneDiamond, bean);
            if (!rrb.isSuccess()) return rrb;
            RetResult<Integer> clubrs = preCostClubDiamond(player, bean, chargePerOneDiamond);
            if (!clubrs.isSuccess()) return (RetResult) clubrs;
            final RetResult<GT> rrtable = createGameTable(player, bean);
            if (!rrtable.isSuccess()) return rrtable;
            final GT table = rrtable.getResult();
            table.paramBean = bean;
            table.setGameid(gameId());
            table.setChargePerOneDiamond(chargePerOneDiamond);
            table.setClubdiamond(clubrs.getResult());
            if (table.getPlayers() == null) table.setPlayers(table.createPlayers(bean));
            if (table.getCreatetime() < 1) table.setCreatetime(System.currentTimeMillis());
            table.setTableid(Utility.format36time(table.getCreatetime()) + "-" + Integer.toString(player.getUserid(), 36) + "-" + nodeid);
            RetResult crs = doAfterCreateJoinTable(table, player, bean);
            if (!crs.isSuccess()) return crs;
            table.roundReset(this);
            if (bean == null || bean.getClubid() < 1) {
                RetResult rs = table.addPlayer(player, this);
                if (!rs.isSuccess()) {
                    failAddPlayerAfterJoinTable(table, player, rs);
                    return rs;
                }
                player.online(player.getClientAddr(), player.sncpAddress());
            }
            currTables.add(table);
            if (minStartReadyedCount() < 1) addPlayerAutoReadyAction(table, player);
            if ((bean == null || bean.getRobot() != 0) && this.confRobotActivate) {
                int robots = bean != null && bean.getRobot() > 0 ? Math.min(bean.getRobot(), table.getMaxPlayerCount() - 1) : autoJoinRobot(table, player, bean);
                if (robots > 0) {
                    List<UserInfo> robotList = randomRobot(gameId(), roomLevelSize() > 0 ? this.confRoomCoinStages[table.getRoomlevel() - 1] : null, robots);
                    if (robotList == null || robotList.size() < robots) {
                        logger.log(Level.WARNING, this.getClass() + " joinTable random JoinRobot count error, need " + robots + ", but " + (robotList == null ? -1 : robotList.size()));
                    }
                    long delay = table.delayActRobotJoin();
                    for (UserInfo robot : robotList) {
                        addRobotJoinAction(table, delay, createGamePlayer(robot, clientAddr, sncpAddress, table.getRoomlevel(), bean == null ? null : bean.createMap()));
                        delay += gameRandom.nextInt(1000);
                    }
                }
            }
            RetResult<GT> rs = joinNewTable(table, player, bean);
            if (rs.isSuccess()) updateAccountOnLine(userid);
            return rs;
        }
    }

    protected int getChargePerOneDiamond(P player, MGTBean bean) {
        return 0;
    }

    //检测参数合法性
    protected RetResult checkCreateTableBean(P player, int chargePerOneDiamond, MGTBean bean) {
        return RetResult.success(0);
    }

    //扣除Club钻石
    protected RetResult<Integer> preCostClubDiamond(P player, MGTBean bean, int chargePerOneDiamond) {
        return RetResult.success(0);
    }

    @Comment("创建Table之后进行的操作")
    protected RetResult doAfterCreateJoinTable(GT table, P player, MGTBean bean) {
        return RetResult.success();
    }

    protected void failAddPlayerAfterJoinTable(GT table, P player, RetResult rs) {
    }
}
