package com.cratos.platf.crowd;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.convert.*;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
@Table(comment = "全民打卡奖池记录表")
public class CrowdPoolRecord extends BaseEntity {

    @Id
    @Column(comment = "日期; 20190909")
    private int intday;

    @Column(comment = "每人每日报名的次数")
    private int applylimit;

    @Column(comment = "报名的起始金币数")
    private long applystartcoins;

    @Column(comment = "报名增幅金币数")
    private long applyincrecoins;

    @Column(comment = "玩家报名费总和")
    private long humancoins;

    @Column(comment = "电脑报名费总和")
    private long robotcoins;

    @Column(comment = "客户端显示的奖池数，值=humancoins+robotcoins")
    private long poolcoins;

    @Column(comment = "已打卡玩家的报名费总和")
    private long dakacoins;

    @Column(comment = "打卡起始时间，从凌晨到时间点的毫秒数")
    private long dakastartmills;

    @Column(comment = "打卡结束时间，从凌晨到时间点的毫秒数")
    private long dakaendmills;

    @Column(comment = "打卡活动起始时间戳")
    private long crowdstarttime;

    @Column(comment = "打卡活动结束时间戳")
    private long crowdendtime;

    @Column(comment = "玩家报名总次数")
    private long humanapplys;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    @Transient
    @Column(comment = "昨日奖池金币数")
    private long yesterdaypoolcoins;

    public void increHumanCoins(long coin) {
        this.humancoins += coin;
        this.poolcoins += coin;
        this.humanapplys++;
    }

    public void increRobotCoins(long coin) {
        this.robotcoins += coin;
        this.poolcoins += coin;
    }

    //1:报名中; 2:打卡中;3:发奖中
    public int getCrowdstatus() {
        long now = System.currentTimeMillis();
        long mid = Utility.midnight();
        if (now < mid + this.dakastartmills) return 1;
        if (now >= mid + this.dakaendmills) return 3;
        return 2;
    }

    public long getDakacoins() {
        return dakacoins;
    }

    public void setDakacoins(long dakacoins) {
        this.dakacoins = dakacoins;
    }

    public long getYesterdaypoolcoins() {
        return yesterdaypoolcoins;
    }

    public void setYesterdaypoolcoins(long yesterdaypoolcoins) {
        this.yesterdaypoolcoins = yesterdaypoolcoins;
    }

    public void setIntday(int intday) {
        this.intday = intday;
    }

    public int getIntday() {
        return this.intday;
    }

    public void setApplylimit(int applylimit) {
        this.applylimit = applylimit;
    }

    public int getApplylimit() {
        return this.applylimit;
    }

    public void setApplystartcoins(long applystartcoins) {
        this.applystartcoins = applystartcoins;
    }

    public long getApplystartcoins() {
        return this.applystartcoins;
    }

    public void setApplyincrecoins(long applyincrecoins) {
        this.applyincrecoins = applyincrecoins;
    }

    public long getApplyincrecoins() {
        return this.applyincrecoins;
    }

    public void setHumancoins(long humancoins) {
        this.humancoins = humancoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getHumancoins() {
        return this.humancoins;
    }

    public void setRobotcoins(long robotcoins) {
        this.robotcoins = robotcoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getRobotcoins() {
        return this.robotcoins;
    }

    public void setPoolcoins(long poolcoins) {
        this.poolcoins = poolcoins;
    }

    public long getPoolcoins() {
        return this.poolcoins;
    }

    public void setDakastartmills(long dakastartmills) {
        this.dakastartmills = dakastartmills;
    }

    public long getDakastartmills() {
        return this.dakastartmills;
    }

    public void setDakaendmills(long dakaendmills) {
        this.dakaendmills = dakaendmills;
    }

    public long getDakaendmills() {
        return this.dakaendmills;
    }

    public void setCrowdstarttime(long crowdstarttime) {
        this.crowdstarttime = crowdstarttime;
    }

    public long getCrowdstarttime() {
        return this.crowdstarttime;
    }

    public void setCrowdendtime(long crowdendtime) {
        this.crowdendtime = crowdendtime;
    }

    public long getCrowdendtime() {
        return this.crowdendtime;
    }

    public void setHumanapplys(long humanapplys) {
        this.humanapplys = humanapplys;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getHumanapplys() {
        return this.humanapplys;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }
}
