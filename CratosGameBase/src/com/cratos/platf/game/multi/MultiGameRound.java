/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.base.UserInfo;
import com.cratos.platf.game.*;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.JsonConvert;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Table(name = "multiround")
@DistributeTable(strategy = MultiGameRound.TableStrategy.class)
public class MultiGameRound extends GameRound {

    //roundid = finish36time+'-'+create36time+'-'+user36id+'-'+currRoundIndex+'-'+nodeid
    //
    @Column(comment = "房间ID")
    protected String tableid = "";

    @Column(comment = "亲友圈ID")
    protected int clubid;

    @Column(comment = "当前回合index")
    protected int currRoundIndex;

    @Column(comment = "金币底注，为0表示非金币场")
    protected int baseBetCoin;

    @Column(nullable = false, comment = "扩展选项")
    protected Map<String, String> extmap;

    //-----------------ROUND_FINIS H状态时字段-------------------
    @Column(comment = "系统所赢的金币数， 负数表示平台亏损的")
    protected long oswincoins;

    @Column(comment = "手续费")
    protected long taxcoin;

    @Column(comment = "玩家列表")
    protected MultiRoundResultPlayer[] players;

    //-----------------非数据库字段-------------------
    @Transient
    @Column(comment = "预扣奖池金币数， 必须是正数")
    protected long preFreezePoolCoin;

    @Transient
    @Column(comment = "回合状态; 10:未开始;20:准备中;30:选庄中;40:游戏中;50:押注中;60:明牌中;70:已结算;80:已结束;")
    protected short roudStatus;

    @Transient
    @Column(comment = "当前操作玩家ID")
    protected int currPlayerid;

    @Transient
    @Comment("回合记录")
    protected final CopyOnWriteArrayList<StringWrapper> eventRecords = new CopyOnWriteArrayList<>();

    public VideoRound createVideoRound(MultiGameTable table, MultiRoundResult roudResult) {
        return null;
    }

    public MultiGameRound createRoundHis(MultiGameTable table, int nodeid, long finishtime, MultiRoundResult roudResult, long roundTaxCoin) {
        this.oswincoins = 0;
        this.setPlayers(roudResult.getPlayers());
        if (roudResult.getPlayers() != null) { //可能无人押注
            for (MultiRoundResultPlayer mp : roudResult.getPlayers()) {
                if (!UserInfo.isRobot(mp.getUserid())) continue;
                this.oswincoins += mp.getRoundCoin();
            }
        }
        this.finishtime = finishtime;
        this.taxcoin = roundTaxCoin;
        return this;
    }

    public void initRound(MultiGameTable table, int nodeid) {
        this.preFreezePoolCoin = 0L;
        this.gameid = table.getGameid();
        this.currRoundIndex = table.getCurrRoundIndex();
        this.baseBetCoin = table.getBaseBetCoin();
        this.roomlevel = table.getRoomlevel();
        this.tableid = table.getTableid();
        this.roundid = tableid + (currRoundIndex < 10 ? "_0" : "_") + currRoundIndex + (nodeid < 10 ? "-0" : "-") + nodeid;
        this.createtime = table.getRoundtime();
    }

    public void addEventRecord(Map map) {
        this.eventRecords.add(new StringWrapper(JsonConvert.root().convertTo(map)));
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getTableid() {
        return tableid;
    }

    public void setTableid(String tableid) {
        this.tableid = tableid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getPreFreezePoolCoin() {
        return preFreezePoolCoin;
    }

    public void setPreFreezePoolCoin(long preFreezePoolCoin) {
        this.preFreezePoolCoin = preFreezePoolCoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getClubid() {
        return clubid;
    }

    public void setClubid(int clubid) {
        this.clubid = clubid;
    }

    public int getCurrRoundIndex() {
        return currRoundIndex;
    }

    public void setCurrRoundIndex(int currRoundIndex) {
        this.currRoundIndex = currRoundIndex;
    }

    public int getBaseBetCoin() {
        return baseBetCoin;
    }

    public void setBaseBetCoin(int baseBetCoin) {
        this.baseBetCoin = baseBetCoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public Map<String, String> getExtmap() {
        return extmap;
    }

    public void setExtmap(Map<String, String> extmap) {
        this.extmap = extmap;
    }

    public short getRoudStatus() {
        return roudStatus;
    }

    public void setRoudStatus(short roudStatus) {
        this.roudStatus = roudStatus;
    }

    public int getCurrPlayerid() {
        return currPlayerid;
    }

    public void setCurrPlayerid(int currPlayerid) {
        this.currPlayerid = currPlayerid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOswincoins() {
        return oswincoins;
    }

    public void setOswincoins(long oswincoins) {
        this.oswincoins = oswincoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getTaxcoin() {
        return taxcoin;
    }

    public void setTaxcoin(long taxcoin) {
        this.taxcoin = taxcoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public MultiRoundResultPlayer[] getPlayers() {
        return players;
    }

    public void setPlayers(MultiRoundResultPlayer[] players) {
        this.players = players;
    }

    public static class TableStrategy<T extends MultiGameRound> implements DistributeTableStrategy<T> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, T bean) {
            return getTable(table, (Serializable) bean.getRoundid());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getSingleTable(table, Long.parseLong(id.substring(0, id.indexOf('-')), 36));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Object time = node.findValue("finishtime");
            if (time == null) time = node.findValue("#finishtime");
            if (time instanceof Long) return getSingleTable(table, (Long) time);
            Range.LongRange finishtime = (Range.LongRange) time;
            return getSingleTable(table, finishtime.getMin());
        }

        private String getSingleTable(String table, long finishtime) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + String.format(format, finishtime);
        }
    }
}
