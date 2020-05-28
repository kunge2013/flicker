/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.json.JsonConvert;

/**
 *
 * @author zhangjx
 */
public class DictInfoLog extends BaseEntity {

    @Id
    @Column(comment = "修改ID，值=当前毫秒时间")
    protected long configlogid;

    @Column(length = 32, comment = "KEY, 大写 ")
    protected String keyname = "";

    @Column(comment = "KEY的旧数值")
    protected long numoldvalue;

    @Column(comment = "KEY的新数值")
    protected long numnewvalue;

    @Column(length = 127, comment = "KEY的字符串旧值")
    protected String stroldvalue = "";

    @Column(length = 127, comment = "KEY的字符串新值")
    protected String strnewvalue = "";

    @Column(comment = "修改人")
    protected int memberid;

    @Column(length = 1024, comment = "修改原因")
    protected String reason = "";

    @Column(updatable = false, comment = "创建时间")
    protected long createtime;

    public void setConfiglogid(long configlogid) {
        this.configlogid = configlogid;
    }

    public long getConfiglogid() {
        return this.configlogid;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    public String getKeyname() {
        return this.keyname;
    }

    public void setNumoldvalue(long numoldvalue) {
        this.numoldvalue = numoldvalue;
    }

    public long getNumoldvalue() {
        return this.numoldvalue;
    }

    public void setNumnewvalue(long numnewvalue) {
        this.numnewvalue = numnewvalue;
    }

    public long getNumnewvalue() {
        return this.numnewvalue;
    }

    public void setStroldvalue(String stroldvalue) {
        this.stroldvalue = stroldvalue;
    }

    public String getStroldvalue() {
        return this.stroldvalue;
    }

    public void setStrnewvalue(String strnewvalue) {
        this.strnewvalue = strnewvalue;
    }

    public String getStrnewvalue() {
        return this.strnewvalue;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    public int getMemberid() {
        return this.memberid;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return this.reason;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }
}
