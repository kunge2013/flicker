/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.BaseBean;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class UserBlackBean extends BaseBean implements FilterBean {

    @FilterColumn(comment = "类型")
    private short blacktype;

    @FilterColumn(comment = "值")
    private String blackvalue = "";

    @FilterColumn(comment = "创建时间")
    private Range.LongRange createtime;

    public short getBlacktype() {
        return blacktype;
    }

    public void setBlacktype(short blacktype) {
        this.blacktype = blacktype;
    }

    public String getBlackvalue() {
        return blackvalue;
    }

    public void setBlackvalue(String blackvalue) {
        this.blackvalue = blackvalue;
    }

    public Range.LongRange getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Range.LongRange createtime) {
        this.createtime = createtime;
    }

}
