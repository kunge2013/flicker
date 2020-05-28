package com.cratos.platf.order;

import javax.persistence.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "正在周期中商品订单表")
public class OrderPeriodHis extends OrderPeriod {


    @Column(comment = "关闭时间，单位毫秒")
    private long finishtime;

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public long getFinishtime() {
        return this.finishtime;
    }
}
