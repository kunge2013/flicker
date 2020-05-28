package com.cratos.platf.order;

import javax.persistence.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "未使用点卡信息历史表")
public class DotCardHis extends DotCard {

    @Column(comment = "使用者ID")
    private int userid;

    @Column(comment = "[代理商ID]")
    private int agencyid;  //

    @Column(comment = "使用时间")
    private long movetime;

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public void setMovetime(long movetime) {
        this.movetime = movetime;
    }

    public long getMovetime() {
        return this.movetime;
    }

    public int getAgencyid() {
        return agencyid;
    }

    public void setAgencyid(int agencyid) {
        this.agencyid = agencyid;
    }

}
