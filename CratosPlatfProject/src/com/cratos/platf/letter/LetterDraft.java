/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.letter;

import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import javax.persistence.*;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
@Table(comment = "邮件发送组信息表")
public class LetterDraft extends BaseEntity {

    private static final Reproduce< LetterRecord, LetterDraft> reproduce = Reproduce.create(LetterRecord.class, LetterDraft.class);

    @Id
    @Column(length = 20, comment = "邮件草稿ID(12位); 值=类型(2位)+'-'+create36time(9位)")
    private String letterdraftid = "";

    @Column(length = 10000, comment = "收件人ID, 0为全员发送，其他为多个userid，用;隔开")
    private String touserids = "";

    @Column(comment = "邮件发送方用户ID, 为0表示系统发送")
    private int fromuserid;

    @Column(comment = "邮件类型: 10:金钻赠送或奖励;")
    private short lettertype = 10;

    @Column(comment = "赠送金币数")
    private int coins;

    @Column(comment = "赠送钻石数")
    private int diamonds;

    @Column(comment = "赠送奖券数")
    private int coupons;

//    @Deprecated
//    @Column(length = 255, comment = "其他赠品数据,JSON格式")
//    private String goodsjson = "";
    @Column(length = 4096, nullable = false, comment = "签到领取的复合商品, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(length = 32, comment = "子模块")
    private String module = "";

    @Column(length = 1024, comment = "记录描述")
    private String remark = "";
    
    @Column(length = 64, comment = "邮件标题")
    private String title = "";

    @Column(length = 255, comment = "邮件内容")
    private String content = "";

    @Column(comment = "预设时间，单位毫秒, 为0表示及时生效")
    private long starttime;

    @Column(comment = "发送时间，单位毫秒")
    private long sendtime;

    @Column(comment = "操作员ID")
    private int memberid;

    @Transient
    @Column(comment = "操作人")
    private String membername = "";

    public LetterRecord createLetterRecord(int touserid, short status, long time) {
        LetterRecord record = reproduce.apply(new LetterRecord(), this);
        record.setUserid(touserid);
        record.setStatus(status);
        record.setCreatetime(starttime);
        record.setLetterid(letterdraftid);
        return record;
    }

    public String getLetterdraftid() {
        return letterdraftid;
    }

    public void setLetterdraftid(String letterdraftid) {
        this.letterdraftid = letterdraftid;
    }

    public String getTouserids() {
        return touserids;
    }

    public void setTouserids(String touserids) {
        this.touserids = touserids;
    }

    public int getFromuserid() {
        return fromuserid;
    }

    public void setFromuserid(int fromuserid) {
        this.fromuserid = fromuserid;
    }

    public short getLettertype() {
        return lettertype;
    }

    public void setLettertype(short lettertype) {
        this.lettertype = lettertype;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public void setDiamonds(int diamonds) {
        this.diamonds = diamonds;
    }

    public int getCoupons() {
        return coupons;
    }

    public void setCoupons(int coupons) {
        this.coupons = coupons;
    }

    public GoodsItem[] getGoodsitems() {
        return goodsitems;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getSendtime() {
        return sendtime;
    }

    public void setSendtime(long sendtime) {
        this.sendtime = sendtime;
    }

    public int getMemberid() {
        return memberid;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    public String getMembername() {
        return membername;
    }

    public void setMembername(String membername) {
        this.membername = membername;
    }

}
