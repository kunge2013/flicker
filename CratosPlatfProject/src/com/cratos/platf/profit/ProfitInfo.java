package com.cratos.platf.profit;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zhangjx
 */
@Table(comment = "用户总收益记录表")
public class ProfitInfo extends BaseEntity {

    @Id
    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "历史总收益，单位:分")
    private long allprofitmoney;

    @Column(comment = "可提取收益，单位:分")
    private long remainmoney;

    @Column(comment = "更新时间")
    private long updatetime;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setAllprofitmoney(long allprofitmoney) {
        this.allprofitmoney = allprofitmoney;
    }

    public long getAllprofitmoney() {
        return this.allprofitmoney;
    }

    public void setRemainmoney(long remainmoney) {
        this.remainmoney = remainmoney;
    }

    public long getRemainmoney() {
        return this.remainmoney;
    }

    public void setUpdatetime(long updatetime) {
        this.updatetime = updatetime;
    }

    public long getUpdatetime() {
        return this.updatetime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
