package com.cratos.platf.user;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zhangjx
 */
@Table(comment = "限制记录表")
public class UserBlackRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "限制ID 值=create36time(9位)+UUID(32位)")
    private String blackrecordid = "";

    @Column(length = 128, comment = "[用户账号]")
    private String account = "";

    @Column(comment = "类型; 10:ip；20:apptoken；")
    private short blacktype = 10;

    @Column(length = 1024, comment = "值")
    private String blackvalue = "";

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    private long createtime;

    public void setBlackrecordid(String blackrecordid) {
        this.blackrecordid = blackrecordid;
    }

    public String getBlackrecordid() {
        return this.blackrecordid;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return this.account;
    }

    public void setBlacktype(short blacktype) {
        this.blacktype = blacktype;
    }

    public short getBlacktype() {
        return this.blacktype;
    }

    public void setBlackvalue(String blackvalue) {
        this.blackvalue = blackvalue;
    }

    public String getBlackvalue() {
        return this.blackvalue;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
