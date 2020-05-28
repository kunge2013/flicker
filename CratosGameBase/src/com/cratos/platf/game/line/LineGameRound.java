package com.cratos.platf.game.line;

import com.cratos.platf.game.GamePlayer;
import com.cratos.platf.game.GameRound;
import java.io.Serializable;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.source.*;

/**
 * roundid: 招财进宝回合ID, 值=create36time(9位) + 6位user36id + 2位nodeid
 *
 * @author zhangjx
 */
@Table(name = "lineround", comment = "连线游戏回合信息表")
@DistributeTable(strategy = LineGameRound.TableStrategy.class)
public class LineGameRound extends GameRound {

    @Column(comment = "用户ID")
    protected int userid;

    @Column(comment = "押注线条")
    protected int linenum = 0;

    @Column(comment = "单线金币")
    protected int linecoin = 0;

    @Column(comment = "押注位置")
    protected int betpos;

    @Column(comment = "下注金币或回合成本金币,值=linenum*linecoin")
    protected long betcoin = 0;

    @Column(comment = "盈利倍数")
    protected int factor = 0;

    @Column(comment = "WILD连线数")
    protected int wild;

    @Column(comment = "BONUS连线成功数")
    protected int bonus;

    @Column(comment = "SCATTER连线成功数")
    protected int scatter;

    @Column(comment = "本回合WILD小游戏的金币数")
    protected long wildcoin;

    @Column(comment = "本回合BONUS奖励的金币数")
    protected long bonuscoin;

    @Column(comment = "本回合免费次数")
    protected int freecount;

    @Column(comment = "所赢金币,值不包含成本金币数，玩家真实的输赢是wincoin-betcoin; bba/fqzs: 输赢金币数(已减掉抽水费)，负数表示输给系统")
    protected long wincoin;

    @Column(comment = "系统所赢的金币数， 负数表示平台亏损的")
    protected long oswincoins;

    @Column(comment = "手续费")
    protected long taxcoin;

    @Column(length = 64, comment = "节点列表,A固定为百搭节点, 格式:ABCDBDEFEFDCEFD")
    protected String rsgems = "";

    @Column(length = 255, comment = "本回合得分明细,key是坐标(00开始，固定两位),value是分数:  {'0001020506':400,'07081314':2400}")
    protected String rscoinjson = "";

    @Transient
    protected GamePlayer player;

    @Transient
    protected char[][] gems;

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public GamePlayer getPlayer() {
        return player;
    }

    public void setPlayer(GamePlayer player) {
        this.player = player;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public char[][] getGems() {
        return gems;
    }

    public void setGems(char[][] gems) {
        this.gems = gems;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setLinenum(int linenum) {
        this.linenum = linenum;
    }

    public int getLinenum() {
        return this.linenum;
    }

    public void setLinecoin(int linecoin) {
        this.linecoin = linecoin;
    }

    public int getLinecoin() {
        return this.linecoin;
    }

    public void setBetcoin(long betcoin) {
        this.betcoin = betcoin;
    }

    public long getBetcoin() {
        return this.betcoin;
    }

    public int getBetpos() {
        return betpos;
    }

    public void setBetpos(int betpos) {
        this.betpos = betpos;
    }

    public int getFactor() {
        return factor;
    }

    public void setFactor(int factor) {
        this.factor = factor;
    }

    public void setWincoin(long wincoin) {
        this.wincoin = wincoin;
    }

    public long getWincoin() {
        return this.wincoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOswincoins() {
        return oswincoins;
    }

    public void setOswincoins(long oswincoins) {
        this.oswincoins = oswincoins;
    }

    public long getTaxcoin() {
        return taxcoin;
    }

    public void setTaxcoin(long taxcoin) {
        this.taxcoin = taxcoin;
    }

    public String getRsgems() {
        return rsgems;
    }

    public void setRsgems(String rsgems) {
        this.rsgems = rsgems;
    }

    public String getRscoinjson() {
        return rscoinjson;
    }

    public void setRscoinjson(String rscoinjson) {
        this.rscoinjson = rscoinjson;
    }

    public int getWild() {
        return wild;
    }

    public void setWild(int wild) {
        this.wild = wild;
    }

    public int getBonus() {
        return bonus;
    }

    public void setBonus(int bonus) {
        this.bonus = bonus;
    }

    public int getScatter() {
        return scatter;
    }

    public void setScatter(int scatter) {
        this.scatter = scatter;
    }

    public long getWildcoin() {
        return wildcoin;
    }

    public void setWildcoin(long wildcoin) {
        this.wildcoin = wildcoin;
    }

    public long getBonuscoin() {
        return bonuscoin;
    }

    public void setBonuscoin(long bonuscoin) {
        this.bonuscoin = bonuscoin;
    }

    public int getFreecount() {
        return freecount;
    }

    public void setFreecount(int freecount) {
        this.freecount = freecount;
    }

    public static class TableStrategy implements DistributeTableStrategy<LineGameRound> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, LineGameRound bean) {
            return getTable(table, (Serializable) bean.getRoundid());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getSingleTable(table, Long.parseLong(id.substring(0, id.indexOf('-')), 36));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Object time = node.findValue("createtime");
            if (time instanceof Long) return getSingleTable(table, (Long) time);
            Range.LongRange createtime = (Range.LongRange) time;
            return getSingleTable(table, createtime.getMin());
        }

        private String getSingleTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + String.format(format, createtime);
        }
    }
}
