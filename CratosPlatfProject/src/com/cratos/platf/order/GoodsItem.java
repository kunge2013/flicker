/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.Column;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
public class GoodsItem extends BaseEntity {

    private static final Reproduce<GoodsItem, GoodsItem> reproduce = Reproduce.create(GoodsItem.class, GoodsItem.class);

    @Column(comment = "商品类型, 不能是GOODS_TYPE_MIXES")
    protected short goodstype;

    @Column(comment = "实物ID所属子游戏ID，为空表示平台")
    protected String gameid;

    @Column(comment = "实物ID， 根据goodstype来指定不同itemid")
    protected int goodsobjid;

    @Column(comment = "商品数量")
    protected int goodscount;

    @Column(comment = "商品过期秒数，为0表示不过期")
    protected long goodsexpires;

    public GoodsItem() {
    }

    public GoodsItem(short goodstype, int goodsitemcount) {
        this.goodstype = goodstype;
        this.goodscount = goodsitemcount;
    }

    public GoodsItem(short goodstype, String gameid, int goodsitemid, int goodsitemcount) {
        this(goodstype, gameid, goodsitemid, goodsitemcount, 0L);
    }

    public GoodsItem(short goodstype, String gameid, int goodsitemid, int goodsitemcount, long goodsexpireseconds) {
        this.goodstype = goodstype;
        this.gameid = gameid;
        this.goodsobjid = goodsitemid;
        this.goodscount = goodsitemcount;
        this.goodsexpires = goodsexpireseconds;
    }

    public static GoodsItem createCoin(int coin) {
        return new GoodsItem(GoodsInfo.GOODS_TYPE_COIN, "", 0, coin, 0);
    }

    public static GoodsItem createDiamond(int diamond) {
        return new GoodsItem(GoodsInfo.GOODS_TYPE_DIAMOND, "", 0, diamond, 0);
    }

    public static GoodsItem createCoupon(int coupon) {
        return new GoodsItem(GoodsInfo.GOODS_TYPE_COUPON, "", 0, coupon, 0);
    }

    public GoodsItem copy() {
        return reproduce.apply(new GoodsItem(), this);
    }

    public short getGoodstype() {
        return goodstype;
    }

    public void setGoodstype(short goodstype) {
        this.goodstype = goodstype;
    }

    public int getGoodsobjid() {
        return goodsobjid;
    }

    public void setGoodsobjid(int goodsobjid) {
        this.goodsobjid = goodsobjid;
    }

    public int getGoodscount() {
        return goodscount;
    }

    public void setGoodscount(int goodscount) {
        this.goodscount = goodscount;
    }

    public String getGameid() {
        return gameid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public long getGoodsexpires() {
        return goodsexpires;
    }

    public void setGoodsexpires(long goodsexpires) {
        this.goodsexpires = goodsexpires;
    }

}
