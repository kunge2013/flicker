package com.cratos.platf.order;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.json.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "点卡组表")
public class DotCardGroup extends BaseEntity {

    @Id
    @Column(comment = "点卡组ID 值从100001开始")
    private int cardgroupid;

    @Column(comment = "类型; 10:邀请卡; 20:点卡; ")
    private short cardtype = 10;

    @Column(comment = "金币面值")
    private long coins;

    @Column(comment = "晶石面值")
    private long diamonds;

    @Column(comment = "奖券面值")
    private long coupons;
    
    @Column(comment = "点卡数量， 必须大于1")
    private int amount;

    @Column(comment = "未使用的点卡数量")
    private int remains;

    @Column(length = 1024, comment = "原因备注")
    private String reason = "";

    @Column(comment = "操作人ID")
    private int memberid;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setCardgroupid(int cardgroupid) {
        this.cardgroupid = cardgroupid;
    }

    public int getCardgroupid() {
        return this.cardgroupid;
    }

    public short getCardtype() {
        return cardtype;
    }

    public void setCardtype(short cardtype) {
        this.cardtype = cardtype;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public long getCoins() {
        return this.coins;
    }

    public long getDiamonds() {
        return diamonds;
    }

    public void setDiamonds(long diamonds) {
        this.diamonds = diamonds;
    }

    public long getCoupons() {
        return coupons;
    }

    public void setCoupons(long coupons) {
        this.coupons = coupons;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setRemains(int remains) {
        this.remains = remains;
    }

    public int getRemains() {
        return this.remains;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return this.reason;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    public int getMemberid() {
        return this.memberid;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }
}
