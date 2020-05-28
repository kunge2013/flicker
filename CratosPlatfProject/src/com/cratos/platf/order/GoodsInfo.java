/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
@Cacheable(interval = 60)
public class GoodsInfo extends BaseEntity implements Comparable<GoodsInfo> {

    public static final float EXCHANGE_RMB_COIN = 100; //1元兑换多少金币

    public static final float EXCHANGE_RMB_DIAMOND = 1; //1元兑换多少钻石

    @Comment("购买类型：RMB")
    public static final short GOODS_BUY_RMB = 10;

    @Comment("购买类型：金币")
    public static final short GOODS_BUY_COIN = 20;

    @Comment("购买类型：钻石")
    public static final short GOODS_BUY_DIAMOND = 30;

    @Comment("购买类型：奖券")
    public static final short GOODS_BUY_COUPON = 40;

    @Comment("金币商品")
    public static final short GOODS_TYPE_COIN = 101;

    @Comment("钻石商品")
    public static final short GOODS_TYPE_DIAMOND = 102;

    @Comment("奖券商品")
    public static final short GOODS_TYPE_COUPON = 103;

    @Comment("活跃度商品")
    public static final short GOODS_TYPE_LIVENESS = 104;

    @Comment("自定义金币商品")
    public static final short GOODS_TYPE_XCOIN = 106;

    @Comment("自定义钻石商品")
    public static final short GOODS_TYPE_XDIAMOND = 107;

    @Comment("自定义奖券商品")
    public static final short GOODS_TYPE_XCOUPON = 108;

    @Comment("复合礼包商品")
    public static final short GOODS_TYPE_PACKETS = 200;

    @Comment("首充礼包商品")
    public static final short GOODS_TYPE_ONCEPACKET = 201;

    @Comment("活动礼包商品")
    public static final short GOODS_TYPE_ACTIPACKET = 202;

    @Comment("日礼包商品")
    public static final short GOODS_TYPE_DAYPACKET = 203;

    @Comment("周卡商品")
    public static final short GOODS_TYPE_WEEKCARD = 301;

    @Comment("月卡商品")
    public static final short GOODS_TYPE_MONTHCARD = 302;

    @Comment("话费卡商品")
    public static final short GOODS_TYPE_TELCARD = 401;

    @Comment("京东卡商品")
    public static final short GOODS_TYPE_JDCARD = 402;

    @Comment("道具商品")
    public static final short GOODS_TYPE_ITEM_PROP = 600;

    @Comment("皮肤商品")
    public static final short GOODS_TYPE_ITEM_SKIN = 700;

    @Comment("装备商品")
    public static final short GOODS_TYPE_ITEM_EQUIP = 800;

    @Id
    @Column(comment = "商品ID 值=2位buytype+3位类型+3位序号")
    protected int goodsid;

    @Column(updatable = false, comment = "购买货币; 10:RMB;20:金币兑换;30:钻石兑换;40:奖券兑换;")
    protected short buytype;

    @Column(updatable = false, comment = "[商品类型]: 1xx:货币商品;200:混合商品;")
    protected short goodstype;

    @Column(comment = "日购买上限, goodstype = GOODS_TYPE_DAYPACKET 且 值> 0才有效")
    protected int daylimit;

    @Column(comment = "[显示标记]: 10:hot;20:new;")
    protected short showflag;

    @Column(comment = "购买价格; 单位：分")
    protected int price;

    @Column(comment = "赠送结束时间点，为0表示永久有效")
    protected long giftendtime;

    @Column(comment = "[状态]: 10:正常;80:删除;")
    protected short status;

    @Column(comment = "商品值数量(已包含赠送量)")
    protected int goodscount;

    @Column(length = 255, comment = "商品名称")
    protected String goodsname = "";

    @Column(length = 4096, nullable = false, comment = "商品复合值")
    protected GoodsItem[] goodsitems;

    @Column(length = 2048, nullable = false, comment = "商品扩展值")
    protected GoodsItem[] giftitems;

    @Column(comment = "赠送商品值数量")
    protected int giftcount;

    @Column(comment = "排序顺序，值小靠前")
    protected int display = 1000;

    @Column(comment = "出售起始时间")
    protected long starttime;

    @Column(comment = "出售结束时间，为0表示永久有效")
    protected long endtime;

    @Column(comment = "操作人ID")
    protected int memberid;

    @Column(updatable = false, comment = "创建时间")
    protected long createtime;

    @Override
    public int compareTo(GoodsInfo o) {
        if (o == null) return 1;
        if (this.status != this.status) return this.status == BaseEntity.STATUS_NORMAL ? -1 : 1;
        return this.display - o.display;
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

    public short getGoodstype() {
        return goodstype;
    }

    public void setGoodstype(short goodstype) {
        this.goodstype = goodstype;
    }

    public int getDaylimit() {
        return daylimit;
    }

    public void setDaylimit(int daylimit) {
        this.daylimit = daylimit;
    }

    public short getShowflag() {
        return showflag;
    }

    public void setShowflag(short showflag) {
        this.showflag = showflag;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getGiftendtime() {
        return giftendtime;
    }

    public void setGiftendtime(long giftendtime) {
        this.giftendtime = giftendtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getGoodsname() {
        return goodsname;
    }

    public void setGoodsname(String goodsname) {
        this.goodsname = goodsname;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getDisplay() {
        return display;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getEndtime() {
        return endtime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMemberid() {
        return memberid;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public int getGoodscount() {
        return goodscount;
    }

    public void setGoodscount(int goodscount) {
        this.goodscount = goodscount;
    }

    public int getGiftcount() {
        return giftcount;
    }

    public void setGiftcount(int giftcount) {
        this.giftcount = giftcount;
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

}
