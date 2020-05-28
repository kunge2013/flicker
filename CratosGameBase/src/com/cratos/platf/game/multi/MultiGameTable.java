/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.game.GameActionEvent;
import com.cratos.platf.base.RetCodes;
import com.cratos.platf.game.*;
import static com.cratos.platf.game.GamePlayer.READYSTATUS_PLAYING;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.*;
import org.redkale.service.RetResult;
import org.redkale.util.Comment;

/**
 * 一般操作顺序:
 * ACTID_TABLE_PRESTART
 * ACTID_TABLE_START
 *
 * ACTID_PLAYER_JOIN
 * ACTID_PLAYER_READY
 *
 * ACTID_ROUND_START
 * ACTID_PLAYER_BANKER
 * ACTID_PLAYER_BETTING / ACTID_PLAYER_DEAL
 * ACTID_ROUND_SETTLE
 * ACTID_ROUND_PREREADY / ACTID_ROUND_PRESTART
 *
 * ACTID_TABLE_SETTLE
 * ACTID_TABLE_DIMISS
 *
 * @author zhangjx
 * @param <R> MultiGameRound
 * @param <P> MultiGamePlayer
 */
public abstract class MultiGameTable<R extends MultiGameRound, P extends MultiGamePlayer> extends GameTable<P> {

    //--------------- 房间操作 --------------------------------------
    @Comment("房间预备")
    public static final int ACTID_TABLE_PRESTART = 8001;

    @Comment("房间开局")
    public static final int ACTID_TABLE_START = 8002;

    @Comment("房间结束")
    public static final int ACTID_TABLE_FINISH = 8003;

    @Comment("房间解散")
    public static final int ACTID_TABLE_DISMISS = 8004;

    @Comment("回合预备")
    public static final int ACTID_ROUND_PREREADY = 8011;

    @Comment("中转预备")
    public static final int ACTID_ROUND_PRESTART = 8012;

    @Comment("回合开始")
    public static final int ACTID_ROUND_START = 8013;

    @Comment("回合结束")
    public static final int ACTID_ROUND_FINISH = 8014;

    //--------------- 玩家操作 --------------------------------------
    @Comment("加入")
    public static final int ACTID_ROBOT_JOIN = 7011;

    @Comment("准备")
    public static final int ACTID_PLAYER_READY = 7012;

    //--------------- 操作超时时长 ------------------------------------
    //
    @Comment("延迟(毫秒)--房间结束")
    public static final long DELAY_ACT_TABLE_FINISH = 10;

    @Comment("延迟(毫秒)--房间解散")
    public static final long DELAY_ACT_TABLE_DISMISS = 10;

    @Comment("延迟(毫秒)--中转预备")
    public static final long DELAY_ACT_ROUND_PRESTART = 10_000;

    @Comment("延迟(毫秒)--回合预备")
    public static final long DELAY_ACT_ROUND_PREREADY = 10;

    @Comment("延迟(毫秒)--回合开始")
    public static final long DELAY_ACT_ROUND_START = 300;

    @Comment("延迟(毫秒)--回合结束")
    public static final long DELAY_ACT_ROUND_FINISH = 10;

    @Comment("延迟(毫秒)--回合结算结果显示逗留时间")
    public static final long DELAY_ACT_ROUND_RESULT = 3_000;

    @Comment("延迟(毫秒)--电脑加入")
    public static final long DELAY_ACT_ROBOT_JOIN = 1000;

    @Comment("延迟(毫秒)--电脑准备")
    public static final long DELAY_ACT_ROBOT_READY = 300;

    @Comment("延迟(毫秒)--准备")
    public static final long DELAY_ACT_PLAYER_READY = 5_000;

    protected static JsonConvert allConvert = JsonFactory.create().skipAllIgnore(true).getConvert();

    //tableid = create36time+'-'+user36id+'-'+nodeid
    //
    @Column(comment = "亲友圈ID")
    protected int clubid;

    @Column(comment = "亲友圈预扣的钻石")
    protected int clubdiamond;

    @Column(comment = "付费方式; 10:AA付费; 20:房主付费;")
    protected short chargeType;

    @Comment("金币底注，为0表示非金币场")
    protected int baseBetCoin;

    @Column(comment = "初始点数") //比如比赛，先给一定点数， 输完为止
    protected int initTableScore;

    @Column(comment = "状态")
    protected short tableStatus = TABLE_STATUS_READYING;

    @Column(comment = "最大局数")
    protected int maxRoundCount;

    @Column(comment = "最大玩家数")
    protected int maxPlayerCount;

    @Column(comment = "每人4回合耗钻数")
    protected int chargePerOneDiamond = 1;

    @Column(comment = "本房间的总耗钻数")
    protected int costDiamonds;

    @Column(nullable = false, comment = "扩展选项")
    protected Map<String, String> extmap;

    //-----------------非数据库字段-------------------   
    @Transient
    protected int currBankerid;

    @Transient
    protected int currRoundIndex;

    @Transient
    protected GameTableBean paramBean;

    @Transient
    protected R currRound;

    @Transient
    protected long roundtime;

    @Transient
    private final List<GameActionEvent> actevents = new CopyOnWriteArrayList<>();

    public abstract P[] createPlayers(GameTableBean bean);

    public abstract Map<String, Object> afterRoundFinished(P winPlayer, MultiRoundResult roudResult);

    public void removeAllActionEvents() {
        if (actevents.isEmpty()) return;
        for (GameActionEvent event : new ArrayList<>(actevents)) {
            this.removeAction(event);
        }
    }

    //获取当前操作人的剩余时长
    public int getCurrRemains() {
        R round = this.currRound;
        if (round == null || round.currPlayerid < 1) return -1;
        int userid = round.currPlayerid;
        Optional<GameActionEvent> def = actevents.stream().filter(x -> x.containsUserid(userid)).findAny();
        GameActionEvent event = def.isPresent() ? def.get() : null;
        return event == null ? -1 : event.getRemains();
    }

    //延迟(毫秒)--房间结束
    @ConvertDisabled
    public long delayActTableFinish() {
        return DELAY_ACT_TABLE_FINISH;
    }

    //延迟(毫秒)--房间解散
    @ConvertDisabled
    public long delayActTableDismiss() {
        return DELAY_ACT_TABLE_DISMISS;
    }

    //延迟(毫秒)--回合预备
    @ConvertDisabled
    public long delayActRoundPreReady() {
        return DELAY_ACT_ROUND_PREREADY;
    }

    //延迟(毫秒)--回合开始
    @ConvertDisabled
    public long delayActRoundStart() {
        return DELAY_ACT_ROUND_START;
    }

    //延迟(毫秒)--回合结算
    @ConvertDisabled
    public long delayActRoundFinish() {
        return DELAY_ACT_ROUND_FINISH;
    }

    //延迟(毫秒)--中转预备
    @ConvertDisabled
    public long delayActRoundPreStart() {
        return DELAY_ACT_ROUND_PRESTART;
    }

    //延迟(毫秒)--回合结算结果显示逗留时间
    @ConvertDisabled
    public long delayActRoundResult() {
        return DELAY_ACT_ROUND_RESULT;
    }

    //延迟(毫秒)--电脑加入
    @ConvertDisabled
    public long delayActRobotJoin() {
        return DELAY_ACT_ROBOT_JOIN;
    }

    //延迟(毫秒)--电脑准备
    @ConvertDisabled
    public long delayActRobotReady() {
        return DELAY_ACT_ROBOT_READY;
    }

    //延迟(毫秒)--准备
    @ConvertDisabled
    public long delayActPlayerReady() {
        return DELAY_ACT_PLAYER_READY;
    }

    @Comment("房间结算，方法内不准更新GamePlayer的任何字段")
    public RetResult<MultiTableResult> tableFinish(GameService gameService, GameActionEvent actevent) {
        this.tableStatus = TABLE_STATUS_FINISHED;
        return RetCodes.retResult(RetCodes.RET_SUPPORT_ILLEGAL);
    }

    @Comment("回合结算，方法内不准更新GamePlayer的coins字段")
    public RetResult<MultiRoundResult> roundFinish(GameService gameService, GameActionEvent actevent, AtomicReference<P> winPlayerRef) {
        return RetCodes.retResult(RetCodes.RET_SUPPORT_ILLEGAL);
    }

    @Comment("回合开始")
    public RetResult<MultiRoundStartBean> roundStart(GameService gameService, GameActionEvent actevent) {
        this.roundtime = System.currentTimeMillis();
        return RetResult.success();
    }

    @Comment("回合结束")
    public void roundReset(GameService gameService) {
    }

    @Override
    protected RetResult checkTableInReadyPlayer(final P player) {
        if (this.tableStatus == TABLE_STATUS_GAMEING) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_STATUS_PLAYING);
        return null;
    }

    //下一个可操作玩家
    public P nextActPlayer(int currUserid) {
        int currIndex = 0;
        for (int i = 0; i < players.length; i++) {
            P one = players[i];
            if (one != null && one.getUserid() == currUserid) {
                currIndex = i;
                break;
            }
        }
        for (int i = currIndex + 1; i < players.length; i++) {
            P one = players[i];
            if (one != null && one.getReadystatus() == READYSTATUS_PLAYING) return one;
        }

        for (int i = 0; i < currIndex; i++) {
            P one = players[i];
            if (one != null && one.getReadystatus() == READYSTATUS_PLAYING) return one;
        }
        return null;
    }

    @Comment("以指定用户ID进行下家排序有效的玩家列表")
    public List<P> sortPlayers(final int currPlayerid) {
        int start = 0;
        final List<P> list = new ArrayList<>();
        final int size = this.players.length;
        for (int i = 0; i < size; i++) {
            P player = this.players[i];
            if (player == null) continue;
            if (player.getUserid() == currPlayerid) {
                start = i;
                break;
            }
        }
        for (int i = start; i < size; i++) {
            P player = this.players[i];
            if (player == null) continue;
            list.add(player);
        }
        for (int i = 0; i < start; i++) {
            P player = this.players[i];
            if (player == null) continue;
            list.add(player);
        }
        return list;
    }

    @Comment("回放记录")
    public void addEventRecord(Map map) {
    }

    public String findExtConf(String name, String defvalue) {
        return extmap == null ? defvalue : extmap.getOrDefault(name, defvalue);
    }

    public int findExtConf(String name, int defvalue) {
        return extmap == null ? defvalue : Integer.parseInt(extmap.getOrDefault(name, "" + defvalue));
    }

    public short findExtConf(String name, short defvalue) {
        return extmap == null ? defvalue : Short.parseShort(extmap.getOrDefault(name, "" + defvalue));
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getCurrBankerid() {
        return currBankerid;
    }

    public void setCurrBankerid(int currBankerid) {
        this.currBankerid = currBankerid;
    }

    public R getCurrRound() {
        return currRound;
    }

    public void setCurrRound(R round) {
        this.currRound = round;
    }

    public GameTableBean paramBean() {
        return paramBean;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getRoundtime() {
        return roundtime;
    }

    public void setRoundtime(long roundtime) {
        this.roundtime = roundtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public List<GameActionEvent> getActions() {
        return this.actevents;
    }

    public Stream<GameActionEvent> streamAction() {
        return this.actevents.stream();
    }

    void addAction(GameActionEvent actevent) {
        this.actevents.add(actevent);
    }

    public boolean removeAction(GameActionEvent actevent) {
        if (actevent != null) actevent.cancel();
        return this.actevents.remove(actevent);
    }

    public boolean containsAction(int userid, int actid) {
        return actevents.stream().filter(x -> x.contains(userid, actid)).count() > 0;
    }

    public boolean containsAction(int actid) {
        return actevents.stream().filter(x -> x.containsActid(actid)).count() > 0;
    }

    public GameActionEvent findAction(int userid, int actid) {
        Optional<GameActionEvent> def = actevents.stream().filter(x -> x.contains(userid, actid)).findAny();
        return def.isPresent() ? def.get() : null;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getClubid() {
        return clubid;
    }

    public void setClubid(int clubid) {
        this.clubid = clubid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getClubdiamond() {
        return clubdiamond;
    }

    public void setClubdiamond(int clubdiamond) {
        this.clubdiamond = clubdiamond;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getChargeType() {
        return chargeType;
    }

    public void setChargeType(short chargeType) {
        this.chargeType = chargeType;
    }

    public int getBaseBetCoin() {
        return baseBetCoin;
    }

    public void setBaseBetCoin(int baseBetCoin) {
        this.baseBetCoin = baseBetCoin;
    }

    public int getInitTableScore() {
        return initTableScore;
    }

    public void setInitTableScore(int initTableScore) {
        this.initTableScore = initTableScore;
    }

    public short getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(short tableStatus) {
        this.tableStatus = tableStatus;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMaxRoundCount() {
        return maxRoundCount;
    }

    public void setMaxRoundCount(int maxRoundCount) {
        this.maxRoundCount = maxRoundCount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    //是否要结算钻石
    public boolean canChargeRound() {
        return getCurrRoundIndex() % 4 == 1; //不是4局中的第一局结束就不扣费
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getChargePerOneDiamond() {
        return chargePerOneDiamond;
    }

    public void setChargePerOneDiamond(int chargePerOneDiamond) {
        this.chargePerOneDiamond = chargePerOneDiamond;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getCostDiamonds() {
        return costDiamonds;
    }

    public void setCostDiamonds(int costDiamonds) {
        this.costDiamonds = costDiamonds;
    }

    public Map<String, String> getExtmap() {
        return extmap;
    }

    public void setExtmap(Map<String, String> extmap) {
        this.extmap = extmap;
    }

    public int getCurrRoundIndex() {
        return currRoundIndex;
    }

    public void setCurrRoundIndex(int currRoundIndex) {
        this.currRoundIndex = currRoundIndex;
    }

}
