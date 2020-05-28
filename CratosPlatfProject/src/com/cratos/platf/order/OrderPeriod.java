package com.cratos.platf.order;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "正在周期中商品订单表")
public class OrderPeriod extends BaseEntity {

    private static final Reproduce<OrderPeriod, OrderRecord> reproduce = Reproduce.create(OrderPeriod.class, OrderRecord.class);

    private static final Reproduce<OrderPeriodHis, OrderPeriod> hisreproduce = Reproduce.create(OrderPeriodHis.class, OrderPeriod.class);

    @Id
    @Column(length = 64, comment = "订单编号; 值=类型(3位)+user36id(6位)+create36time(9位)")
    protected String orderno = "";

    @Column(comment = "付款人用户ID")
    protected int userid;

    @Column(comment = "周期起始日期， 格式为: 20200101")
    protected int startday;

    @Column(comment = "已发邮件处理的日期， 格式为: 20200101")
    protected int doneday;

    @Column(comment = "周期结束日期， 格式为: 20200101")
    protected int endday;

    @Column(comment = "商品类型；只能是301/302; 周卡或月卡")
    protected short goodstype;

    @Column(comment = "商品价值; 不同的goodstype表示不同的商品价值")
    protected int goodscount;

    @Column(length = 4096, nullable = false, comment = "商品复合值")
    protected GoodsItem[] goodsitems;

    @Column(length = 2048, nullable = false, comment = "商品扩展值, GoodsItem[]数组")
    protected GoodsItem[] giftitems;

    @Column(updatable = false, comment = "开始时间，单位毫秒")
    protected long createtime;

    public OrderPeriod() {
    }

    public OrderPeriod(OrderRecord order) {
        reproduce.apply(this, order);
        if (order.getGoodstype() != GoodsInfo.GOODS_TYPE_WEEKCARD
            && order.getGoodstype() != GoodsInfo.GOODS_TYPE_MONTHCARD) {
            throw new IllegalArgumentException("goodstype error, order=" + order);
        }
        this.startday = Utility.yyyyMMdd(this.createtime);
        int days = order.getGoodstype() == GoodsInfo.GOODS_TYPE_WEEKCARD ? 7 : 30;
        this.endday = Utility.yyyyMMdd(this.createtime + days * 24 * 60 * 60 * 1000L);
    }

    public OrderPeriodHis createOrderPeriodHis(long now) {
        OrderPeriodHis his = hisreproduce.apply(new OrderPeriodHis(), this);
        his.setFinishtime(now);
        return his;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public String getOrderno() {
        return this.orderno;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setStartday(int startday) {
        this.startday = startday;
    }

    public int getStartday() {
        return this.startday;
    }

    public int getDoneday() {
        return doneday;
    }

    public void setDoneday(int doneday) {
        this.doneday = doneday;
    }

    public void setEndday(int endday) {
        this.endday = endday;
    }

    public int getEndday() {
        return this.endday;
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

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public GoodsItem[] getGoodsitems() {
        return this.goodsitems;
    }

    public void setGiftitems(GoodsItem[] giftitems) {
        this.giftitems = giftitems;
    }

    public GoodsItem[] getGiftitems() {
        return this.giftitems;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
