package com.cratos.platf.duty;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zhangjx
 */
@Table(comment = "用户签到表")
public class DutyAccount extends BaseEntity {

    @Id
    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "最近连续签到的次数")
    private int lastdutyseries;

    @Column(comment = "最近签到的日期，格式:20200304")
    private int lastdutyday;

    @Column(comment = "累计签到次数")
    private long dutycount;

    @Column(comment = "累计签到领取的金币数")
    private long dutycoins;

    @Column(comment = "累计签到领取的钻石数")
    private long dutydiamonds;

    @Column(comment = "累计签到领取的奖券数")
    private long dutycoupons;

    @Column(comment = "更新时间")
    private long updatetime;

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    private long createtime;

    public void increLastdutyseries() {
        this.lastdutyseries++;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setLastdutyseries(int lastdutyseries) {
        this.lastdutyseries = lastdutyseries;
    }

    public int getLastdutyseries() {
        return this.lastdutyseries;
    }

    public void setLastdutyday(int lastdutyday) {
        this.lastdutyday = lastdutyday;
    }

    public int getLastdutyday() {
        return this.lastdutyday;
    }

    public void setDutycount(long dutycount) {
        this.dutycount = dutycount;
    }

    public long getDutycount() {
        return this.dutycount;
    }

    public void setDutycoins(long dutycoins) {
        this.dutycoins = dutycoins;
    }

    public long getDutycoins() {
        return this.dutycoins;
    }

    public void setDutydiamonds(long dutydiamonds) {
        this.dutydiamonds = dutydiamonds;
    }

    public long getDutydiamonds() {
        return this.dutydiamonds;
    }

    public void setDutycoupons(long dutycoupons) {
        this.dutycoupons = dutycoupons;
    }

    public long getDutycoupons() {
        return this.dutycoupons;
    }

    public void setUpdatetime(long updatetime) {
        this.updatetime = updatetime;
    }

    public long getUpdatetime() {
        return this.updatetime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
