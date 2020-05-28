/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import javax.persistence.*;
import org.redkale.convert.*;
import com.cratos.platf.base.UserInfo;
import org.redkale.util.*;

/**
 * ALTER DATABASE platf_core CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
 * ALTER TABLE userdetail CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 *
 * @author zhangjx
 */
@Table(comment = "用户信息表")
//@LogExcludeLevel(levels = "FINEST", keys = "SET currgame =")
public class UserDetail extends UserInfo {

    private static final Reproduce<UserInfo, UserDetail> reproduce = Reproduce.create(UserInfo.class, UserDetail.class);

    public static final short REGTYPE_VISITOR = 10; //游客注册

    public static final short REGTYPE_ACCOUNT = 20; //账号注册

    public static final short REGTYPE_MOBILE = 30; //手机注册

    public static final short REGTYPE_WEIXIN = 40;  //微信注册

    public static final short REGTYPE_EMAIL = 50; //邮箱注册

    public static final short REGTYPE_QQOPEN = 60; //QQ注册

    public static final short REGTYPE_CXOPEN = 70; //城信注册

    public static final short REGTYPE_HAND = 90; //人工注册

    @Column(updatable = false, comment = "[注册类型]: 10:游客注册; 20:账号注册; 30:手机注册; 40:微信注册; 50:邮箱注册;60:QQ注册;70:城信注册;90:人工注册;")
    private short regtype;

    @Column(comment = "个人累计赠送的金币总数，包含签到、福利等途径免费获取的金币")
    private long giftcoins;  //

    @Column(comment = "注册时赠送的金币数")
    private long regcoins;  //

    @Column(comment = "个人累计购买的钻石总数,包含购买时优惠的钻石")
    private long paydiamonds;  //

    @Column(comment = "个人累计赠送的钻石总数，包含签到、福利等途径免费获取的钻石")
    private long giftdiamonds;  //

    @Column(comment = "注册时赠送的钻石数")
    private long regdiamonds;  //

    @Column(comment = "个人累计购买的奖券总数,包含购买时优惠的奖券")
    private long paycoupons;  //

    @Column(comment = "个人累计赠送的奖券总数，包含签到、福利等途径免费获取的奖券")
    private long giftcoupons;  //

    @Column(comment = "注册时赠送的奖券数")
    private long regcoupons;  //

    @Column(comment = "个人累计充值次数")
    private int paycount;  //

    @Column(comment = "最后一次充值时间")
    private long paytime;  //

    @Column(comment = "首次充金额数, 单位:分")
    private long firstpaymoney;  //

    @Column(comment = "首次充值时间")
    private long firstpaytime;  //

    @Column(length = 64, comment = "最后登录IP")
    protected String lastloginaddr = "";

    @Column(comment = "登录经度")
    protected double lastloginlongitude;

    @Column(comment = "登录纬度")
    protected double lastloginlatitude;

    @Column(length = 255, comment = "登录街道")
    protected String lastloginstreet = "";

    @Column(length = 127, updatable = false, comment = "注册时网络类型; wifi/4g/3g")
    private String regnetmode = ""; //注册时网络类型; wifi/4g/3g（前端不可见）

    @Column(updatable = false, length = 255, comment = "[注册终端]")
    private String regagent = "";//注册终端

    @Column(updatable = false, length = 64, comment = "[注册IP]")
    private String regaddr = "";//注册IP

    @Column(length = 255, comment = "[备注]")
    private String remark = ""; //备注

    @Column(comment = "[更新时间]")
    private long updatetime;  //修改时间

    public UserInfo createUserInfo() {
        return reproduce.apply(new UserInfo(), this);
    }

    public long getGiftcoins() {
        return giftcoins;
    }

    public void setGiftcoins(long giftcoins) {
        this.giftcoins = giftcoins;
    }

    public long getPaycoupons() {
        return paycoupons;
    }

    public void setPaycoupons(long paycoupons) {
        this.paycoupons = paycoupons;
    }

    public long getGiftcoupons() {
        return giftcoupons;
    }

    public void setGiftcoupons(long giftcoupons) {
        this.giftcoupons = giftcoupons;
    }

    public long getRegcoupons() {
        return regcoupons;
    }

    public void setRegcoupons(long regcoupons) {
        this.regcoupons = regcoupons;
    }

    @Override
    public String getMobile() {
        return mobile;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getWxunionid() {
        return wxunionid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getLastloginaddr() {
        return lastloginaddr;
    }

    public void setLastloginaddr(String lastloginaddr) {
        this.lastloginaddr = lastloginaddr;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public double getLastloginlongitude() {
        return lastloginlongitude;
    }

    public void setLastloginlongitude(double lastloginlongitude) {
        this.lastloginlongitude = lastloginlongitude;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public double getLastloginlatitude() {
        return lastloginlatitude;
    }

    public void setLastloginlatitude(double lastloginlatitude) {
        this.lastloginlatitude = lastloginlatitude;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getLastloginstreet() {
        return lastloginstreet;
    }

    public void setLastloginstreet(String lastloginstreet) {
        this.lastloginstreet = lastloginstreet;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getRegtype() {
        return regtype;
    }

    public void setRegtype(short regtype) {
        this.regtype = regtype;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(long updatetime) {
        this.updatetime = updatetime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRegagent() {
        return regagent;
    }

    public void setRegagent(String regagent) {
        this.regagent = regagent;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRegaddr() {
        return regaddr;
    }

    public void setRegaddr(String regaddr) {
        this.regaddr = regaddr;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRegnetmode() {
        return regnetmode;
    }

    public void setRegnetmode(String regnetmode) {
        this.regnetmode = regnetmode;
    }

    public int getPaycount() {
        return paycount;
    }

    public void setPaycount(int paycount) {
        this.paycount = paycount;
    }

    public long getPaytime() {
        return paytime;
    }

    public void setPaytime(long paytime) {
        this.paytime = paytime;
    }

    public long getFirstpaymoney() {
        return firstpaymoney;
    }

    public void setFirstpaymoney(long firstpaymoney) {
        this.firstpaymoney = firstpaymoney;
    }

    public long getFirstpaytime() {
        return firstpaytime;
    }

    public void setFirstpaytime(long firstpaytime) {
        this.firstpaytime = firstpaytime;
    }

    public long getPaydiamonds() {
        return paydiamonds;
    }

    public void setPaydiamonds(long paydiamonds) {
        this.paydiamonds = paydiamonds;
    }

    public long getGiftdiamonds() {
        return giftdiamonds;
    }

    public void setGiftdiamonds(long giftdiamonds) {
        this.giftdiamonds = giftdiamonds;
    }

    public long getRegcoins() {
        return regcoins;
    }

    public void setRegcoins(long regcoins) {
        this.regcoins = regcoins;
    }

    public long getRegdiamonds() {
        return regdiamonds;
    }

    public void setRegdiamonds(long regdiamonds) {
        this.regdiamonds = regdiamonds;
    }

}
