/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.BaseBean;
import javax.persistence.Column;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class GoodsBean extends BaseBean implements FilterBean {

    @FilterColumn(comment = "商品ID")
    protected int goodsid;

    @Column(updatable = false, comment = "购买货币; 10:RMB;20:金币兑换;30:钻石兑换;40:奖券兑换;")
    protected short buytype;

    @Column(comment = "商品类型")
    protected short[] goodstype;

    @FilterColumn(comment = "[状态]: 10:正常;80:删除;")
    protected short status;

    @FilterColumn(express = FilterExpress.LESSTHAN, comment = "起始时间")
    protected long starttime;

    @FilterGroup("[OR]e")
    @FilterColumn(express = FilterExpress.GREATERTHAN, comment = "结束时间")
    protected long endtime;

    @FilterGroup("[OR]e")
    @FilterColumn(name = "endtime", least = -1, comment = "结束时间")
    protected long endtime2;

    public void goodstype(short... goodstype) {
        this.goodstype = goodstype;
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

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getEndtime() {
        return endtime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

    public long getEndtime2() {
        return endtime2;
    }

    public void setEndtime2(long endtime2) {
        this.endtime2 = endtime2;
    }

    public short[] getGoodstype() {
        return goodstype;
    }

    public void setGoodstype(short[] goodstype) {
        this.goodstype = goodstype;
    }

}
