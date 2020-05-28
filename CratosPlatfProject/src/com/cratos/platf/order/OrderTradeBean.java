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
public class OrderTradeBean extends BaseBean implements FilterBean { 

    @FilterColumn(comment = "用户ID")
    private int userid;

    @FilterColumn(comment = "创建时间")
    private Range.LongRange createtime;

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public Range.LongRange getCreatetime() { 
        return createtime;
    }

    public void setCreatetime(Range.LongRange createtime) {
        this.createtime = createtime;
    }

}
