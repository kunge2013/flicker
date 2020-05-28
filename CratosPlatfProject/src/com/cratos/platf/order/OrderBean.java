/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.BaseBean;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class OrderBean extends BaseBean implements FilterBean {

    @FilterColumn(comment = "用户ID")
    private int userid;

    @FilterColumn(comment = "商品ID; 不同的goodstype则商品表不同")
    private int goodsid;

    @FilterColumn(comment = "购买货币; 10:RMB;20:金币兑换;30:钻石兑换;40:奖券兑换;")
    private short[] buytype;

    @FilterColumn(comment = "商品类型(必须三位数); 101:购买金币;105:购买钻石;")
    private short[] goodstype;

    @FilterColumn(comment = "订单状态; 10:待处理; 20:处理成功; 30:处理失败;")
    private short[] orderstatus;

    @FilterColumn(comment = "支付类型:  10: 信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;(人工支付将不产生payrecord记录)")
    private short[] paytype;

    @FilterColumn(comment = "支付状态; 10:待支付;20:支付中;30:已支付;40:支付失败;50:待退款;60退款中;70:已退款;80:退款失败;90:已关闭;95:已取消; ")
    private short[] paystatus;

    @FilterColumn(comment = "开始时间，单位毫秒")
    private Range.LongRange createtime;

    @FilterColumn(comment = "结束时间，单位毫秒")
    private Range.LongRange finishtime;

    public void buytype(short... buytype) {
        this.buytype = buytype;
    }

    public void goodstype(short... goodstype) {
        this.goodstype = goodstype;
    }

    public void orderstatus(short... orderstatus) {
        this.orderstatus = orderstatus;
    }

    public void paytype(short... paytype) {
        this.paytype = paytype;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getGoodsid() {
        return goodsid;
    }

    public void setGoodsid(int goodsid) {
        this.goodsid = goodsid;
    }

    public short[] getBuytype() {
        return buytype;
    }

    public void setBuytype(short[] buytype) {
        this.buytype = buytype;
    }

    public short[] getGoodstype() {
        return goodstype;
    }

    public void setGoodstype(short[] goodstype) {
        this.goodstype = goodstype;
    }

    public short[] getOrderstatus() {
        return orderstatus;
    }

    public void setOrderstatus(short[] orderstatus) {
        this.orderstatus = orderstatus;
    }

    public short[] getPaytype() {
        return paytype;
    }

    public void setPaytype(short[] paytype) {
        this.paytype = paytype;
    }

    public short[] getPaystatus() {
        return paystatus;
    }

    public void setPaystatus(short[] paystatus) {
        this.paystatus = paystatus;
    }

    public Range.LongRange getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Range.LongRange createtime) {
        this.createtime = createtime;
    }

    public Range.LongRange getFinishtime() {
        return finishtime;
    }

    public void setFinishtime(Range.LongRange finishtime) {
        this.finishtime = finishtime;
    }

}
