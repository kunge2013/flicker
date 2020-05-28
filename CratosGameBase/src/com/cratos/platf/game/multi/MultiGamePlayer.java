/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.base.UserInfo;
import com.cratos.platf.game.*;
import java.net.InetSocketAddress;
import org.redkale.convert.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class MultiGamePlayer extends GamePlayer {

    @Comment("整局总战绩")
    protected int tableScore;

    @Comment("整局总战绩")
    protected long tableCoin;

    @Comment("当前回合的输赢分数")
    protected int roundScore;

    @Comment("当前回合的输赢金币数")
    protected long roundCoin;

    @Comment("当前回合的押注金币")
    protected long roundCostCoin;

    @Comment("当前回合的备注")
    protected String roundRemark = "";

    public MultiGamePlayer() {
        super();
    }

    public MultiGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, int sitepos) {
        super(user, clientAddr, sncpAddress, roomlevel, sitepos);
    }

    public void increTableCoinScore() {
        this.tableCoin += this.roundCoin;
        this.tableScore += this.roundScore;
    }

    public void increRoundCoin(long wincoin) {
        this.roundCoin += wincoin;
    }

    public void increRoundScore(int score) {
        this.roundScore += score;
    }

    public void appendRoundRemark(String remark) {
        if (this.roundRemark == null) this.roundRemark = "";
        this.roundRemark += remark;
    }

    @Comment("追加信息，自动去重")
    public void appendUniqueRoundRemark(String remark) {
        if (this.roundRemark == null) this.roundRemark = "";
        if (!this.roundRemark.contains(remark)) this.roundRemark += remark;
    }

    public void roundReset(MultiGameTable table) {
        this.readystatus = READYSTATUS_UNREADY;
        this.roundScore = 0;
        this.roundCoin = 0;
        this.roundCostCoin = 0;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getRoundScore() {
        return roundScore;
    }

    public void setRoundScore(int roundScore) {
        this.roundScore = roundScore;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getRoundCoin() {
        return roundCoin;
    }

    public void setRoundCoin(long roundCoin) {
        this.roundCoin = roundCoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getRoundCostCoin() {
        return roundCostCoin;
    }

    public void setRoundCostCoin(long roundCostCoin) {
        this.roundCostCoin = roundCostCoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRoundRemark() {
        return roundRemark;
    }

    public void setRoundRemark(String roundRemark) {
        this.roundRemark = roundRemark;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getTableScore() {
        return tableScore;
    }

    public void setTableScore(int tableScore) {
        this.tableScore = tableScore;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getTableCoin() {
        return tableCoin;
    }

    public void setTableCoin(long tableCoin) {
        this.tableCoin = tableCoin;
    }

}
