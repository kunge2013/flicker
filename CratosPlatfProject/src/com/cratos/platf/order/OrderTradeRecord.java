package com.cratos.platf.order;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zhangjx
 */
@Table(comment = "银行卡打款充值未处理的记录表")
public class OrderTradeRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "主键 user36id(6位)+'-'+create36time(9位)")
    private String tradeid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(updatable = false, comment = "商品ID; 不同的goodstype则商品表不同")
    private int goodsid;

    @Column(updatable = false, comment = "购买货币; 10:RMB;20:金币兑换;30:钻石兑换;40:奖券兑换;")
    private short buytype;

    @Column(comment = "商品类型(必须三位数); 102:自定义购买金币;106:自定义购买钻石")
    private short goodstype;

    @Column(comment = "商品价值数量")
    private int goodscount;

    @Column(length = 4096, updatable = false, nullable = false, comment = "商品复合值")
    private GoodsItem[] goodsitems;

    @Column(length = 2048, updatable = false, nullable = false, comment = "商品扩展值")
    private GoodsItem[] giftitems;

    @Column(comment = "充值金额，单位:分")
    private int money;

    @Column(length = 1024, comment = "收款人信息")
    private String osbankjson = "";

    @Column(length = 2048, comment = "打款人信息")
    private String tradejson = "";

    @Column(comment = "提取状态; 10:处理中; 20:已完成;30:失败;")
    private short tradestatus;

    @Column(length = 1024, comment = "备注信息")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setTradeid(String tradeid) {
        this.tradeid = tradeid;
    }

    public String getTradeid() {
        return this.tradeid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public int getGoodsid() {
        return goodsid;
    }

    public void setGoodsid(int goodsid) {
        this.goodsid = goodsid;
    }

    public short getBuytype() {
        return buytype;
    }

    public void setBuytype(short buytype) {
        this.buytype = buytype;
    }

    public void setGoodstype(short goodstype) {
        this.goodstype = goodstype;
    }

    public short getGoodstype() {
        return this.goodstype;
    }

    public void setGoodscount(int goodscount) {
        this.goodscount = goodscount;
    }

    public int getGoodscount() {
        return this.goodscount;
    }

    public GoodsItem[] getGoodsitems() {
        return goodsitems;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public GoodsItem[] getGiftitems() {
        return giftitems;
    }

    public void setGiftitems(GoodsItem[] giftitems) {
        this.giftitems = giftitems;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getMoney() {
        return this.money;
    }

    public void setOsbankjson(String osbankjson) {
        this.osbankjson = osbankjson;
    }

    public String getOsbankjson() {
        return this.osbankjson;
    }

    public void setTradejson(String tradejson) {
        this.tradejson = tradejson;
    }

    public String getTradejson() {
        return this.tradejson;
    }

    public void setTradestatus(short tradestatus) {
        this.tradestatus = tradestatus;
    }

    public short getTradestatus() {
        return this.tradestatus;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
