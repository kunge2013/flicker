/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.mission.MissionRecord;
import java.util.List;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
public abstract class GameAccount extends BaseEntity {

    @Id
    @Column(comment = "用户ID")
    protected int userid;

    @Column(comment = "总回合数")
    protected long rounds;

    @Column(comment = "本游戏输赢总金币数")
    protected long wincoins;

    @Column(comment = "本游戏的总耗金币数")
    protected long costcoins;

    @Column(comment = "本游戏的总耗钻数")
    protected long costdiamonds;

    @Column(comment = "最近游戏时间")
    protected long lastgametime;

    @Column(updatable = false, comment = "创建时间")
    protected long createtime;

    @Column(comment = "当天日期: 20200101")
    protected int onlinetodaytime;

    @Column(comment = "当天游戏时长")
    protected long onlinetodayseconds;

    @Column(comment = "总游戏时长")
    protected long onlineseconds;

    @Transient
    @Column(comment = "当前进入游戏时间")
    protected long onlinetodaystarttime;

    @Transient
    protected boolean leaving;

    @Transient
    List<MissionRecord> doingMissions;

    public GameAccount() {
    }

    public GameAccount(int userid) {
        this.userid = userid;
        this.createtime = System.currentTimeMillis();
    }

    //加载后调用
    public void postLoad() {
    }

    //入库之前调用
    public void preSave() {
    }

    public void increCostDiamond(long diamond) {
        this.costdiamonds += diamond;
    }

    public void increWinCoin(long coin) {
        this.wincoins += coin;
    }

    public void increCostCoin(long coin) {
        this.costcoins += coin;
    }

    public void increRound() {
        this.rounds++;
        this.lastgametime = System.currentTimeMillis();
    }

    //当前今日在线时长
    public long currTodayOnlineSeconds() {
        if (this.onlinetodaystarttime < 1) return -1;
        int today = Utility.today();
        if (onlinetodaytime != today) {
            this.onlinetodaytime = today;
            this.onlinetodaystarttime = Utility.midnight();
        }
        return onlinetodayseconds + (System.currentTimeMillis() - this.onlinetodaystarttime) / 1000;
    }

    //当前总在线时长
    public long currOnlineSeconds() {
        if (this.onlinetodaystarttime < 1) return -1;
        return onlineseconds + (System.currentTimeMillis() - this.onlinetodaystarttime) / 1000;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getUserid() {
        return this.userid;
    }

    public void setRounds(long rounds) {
        this.rounds = rounds;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getRounds() {
        return this.rounds;
    }

    public void setWincoins(long wincoins) {
        this.wincoins = wincoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getWincoins() {
        return this.wincoins;
    }

    public void setLastgametime(long lastgametime) {
        this.lastgametime = lastgametime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getLastgametime() {
        return this.lastgametime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public boolean isLeaving() {
        return leaving;
    }

    public void setLeaving(boolean leaving) {
        this.leaving = leaving;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostcoins() {
        return costcoins;
    }

    public void setCostcoins(long costcoins) {
        this.costcoins = costcoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostdiamonds() {
        return costdiamonds;
    }

    public void setCostdiamonds(long costdiamonds) {
        this.costdiamonds = costdiamonds;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOnlinetodayseconds() {
        return onlinetodayseconds;
    }

    public void setOnlinetodayseconds(long onlinetodayseconds) {
        this.onlinetodayseconds = onlinetodayseconds;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOnlineseconds() {
        return onlineseconds;
    }

    public void setOnlineseconds(long onlineseconds) {
        this.onlineseconds = onlineseconds;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getOnlinetodaytime() {
        return onlinetodaytime;
    }

    public void setOnlinetodaytime(int onlinetodaytime) {
        this.onlinetodaytime = onlinetodaytime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOnlinetodaystarttime() {
        return onlinetodaystarttime;
    }

    public void setOnlinetodaystarttime(long onlinetodaystarttime) {
        this.onlinetodaystarttime = onlinetodaystarttime;
    }

}
