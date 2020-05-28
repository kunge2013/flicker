/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.line;

import com.cratos.platf.base.BaseBean;
import com.cratos.platf.game.GameAccount;
import java.io.Serializable;
import java.util.Map;
import javax.persistence.*;

/**
 *
 * @author zhangjx
 * @param <E> SmallEntry
 */
@Table(name = "lineaccount", comment = "连线游戏用户账目表")
public class LineGameAccount<E extends LineGameAccount.LineEntry> extends GameAccount {

    @Column(comment = "WILD小游戏总金币数")
    protected long wildcoins;

    @Column(comment = "BONUS奖励总金币数")
    protected long bonuscoins;

    @Column(comment = "免费总次数")
    protected long freecounts;

    @Column(comment = "当前需要奖励时的场次")
    protected int currRoomlevel;

    @Column(comment = "当前需要奖励时的单注")
    protected long currLinecoin;

    @Column(comment = "当前需要奖励时的备注")
    protected int currLinenum;

    @Column(length = 64, comment = "当前需要奖励时的回合")
    protected String currRoundid = "";

    @Column(length = 4096, nullable = false, comment = "当前操作数据")
    protected E currLineEntry;

    public LineGameAccount() {
        super();
    }

    public LineGameAccount(int userid) {
        super(userid);
    }

    public boolean isGameing() {
        return currLineEntry != null;
    }

    public Map<String, Object> newLoadTableEntryMap() {
        return null;
    }

    public void increWildCoin(long wildcoin) {
        this.wildcoins += wildcoin;
    }

    public void increBonusCoin(long bonuscoin) {
        this.bonuscoins += bonuscoin;
    }

    public void increRound(long freecount) {
        this.rounds++;
        this.freecounts += freecount;
        this.lastgametime = System.currentTimeMillis();
    }

    public long getWildcoins() {
        return wildcoins;
    }

    public void setWildcoins(long wildcoins) {
        this.wildcoins = wildcoins;
    }

    public long getBonuscoins() {
        return bonuscoins;
    }

    public void setBonuscoins(long bonuscoins) {
        this.bonuscoins = bonuscoins;
    }

    public long getFreecounts() {
        return freecounts;
    }

    public void setFreecounts(long freecounts) {
        this.freecounts = freecounts;
    }

    public int getCurrRoomlevel() {
        return currRoomlevel;
    }

    public void setCurrRoomlevel(int currRoomlevel) {
        this.currRoomlevel = currRoomlevel;
    }

    public long getCurrLinecoin() {
        return currLinecoin;
    }

    public void setCurrLinecoin(long currLinecoin) {
        this.currLinecoin = currLinecoin;
    }

    public int getCurrLinenum() {
        return currLinenum;
    }

    public void setCurrLinenum(int currLinenum) {
        this.currLinenum = currLinenum;
    }

    public String getCurrRoundid() {
        return currRoundid;
    }

    public void setCurrRoundid(String currRoundid) {
        this.currRoundid = currRoundid;
    }

    public E getCurrLineEntry() {
        return currLineEntry;
    }

    public void setCurrLineEntry(E currLineEntry) {
        this.currLineEntry = currLineEntry;
    }

    public static class LineEntry extends BaseBean implements Serializable {

        @Column(comment = "Wild是否存在(>0)")
        public int currWild;

        @Column(comment = "BONUS是否开宝箱(>0)")
        public int currBonus;

        @Column(comment = "SCATTER是否能免费次数(>0)")
        public int currScatter;

        @Column(comment = "当前剩余的免费次数")
        public int currFreecount;

    }
}
