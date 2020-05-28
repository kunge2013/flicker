/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.hundred;

import com.cratos.platf.base.BaseBean;
import org.redkale.convert.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class HundredBetEntry extends BaseBean implements Comparable<HundredBetEntry> {

    @Comment("押注位置，从1开始")
    private int betpos;

    @Comment("押注金币数")
    private long betcoin;

    @Comment("输赢金币数；结算时才用到此字段")
    private long wincoin;

    public HundredBetEntry() {
    }

    public HundredBetEntry(long betcoin, int betpos) {
        this.betcoin = betcoin;
        this.betpos = betpos;
    }

    public void increBetCoin(long coin) {
        this.betcoin += coin;
    }

    public long getBetcoin() {
        return betcoin;
    }

    public void setBetcoin(long betcoin) {
        this.betcoin = betcoin;
    }

    public int getBetpos() {
        return betpos;
    }

    public void setBetpos(int betpos) {
        this.betpos = betpos;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getWincoin() {
        return wincoin;
    }

    public void setWincoin(long wincoin) {
        this.wincoin = wincoin;
    }

    @Override
    public int compareTo(HundredBetEntry o) {
        if (this.wincoin != o.wincoin) return this.wincoin > o.wincoin ? 1 : -1; //必须输的排前面，否则奖池按顺序increPool会击穿底线
        return (this.betcoin == o.betcoin) ? 0 : (this.betcoin > o.betcoin ? 1 : -1);
    }

}
