/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.profit;

import com.cratos.platf.base.BaseBean;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class ProfitTradeBean extends BaseBean implements FilterBean {

    @FilterColumn(comment = "用户ID")
    private int userid;

    @FilterColumn(comment = "提取方式; 10:银行卡; 20:支付宝")
    private short tradetype;

    @FilterColumn(comment = "创建时间")
    private Range.LongRange createtime;

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public short getTradetype() {
        return tradetype;
    }

    public void setTradetype(short tradetype) {
        this.tradetype = tradetype;
    }

    public Range.LongRange getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Range.LongRange createtime) {
        this.createtime = createtime;
    }

}
