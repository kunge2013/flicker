/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseBean;
import javax.persistence.Transient;
import org.redkale.convert.ConvertDisabled;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class VideoBean extends BaseBean implements FilterBean {

    @FilterColumn(express = FilterExpress.LIKE, comment = "tableid")
    protected String roundid;

    @FilterColumn(comment = "亲友圈ID")
    protected int clubid;

    @FilterColumn(comment = "游戏ID")
    protected String gameid;

    @FilterGroup("[OR]userid")
    @FilterColumn(comment = "玩家1")
    protected int userid1;

    @FilterGroup("[OR]userid")
    @FilterColumn(comment = "玩家2")
    protected int userid2;

    @FilterGroup("[OR]userid")
    @FilterColumn(comment = "玩家3")
    protected int userid3;

    @FilterGroup("[OR]userid")
    @FilterColumn(comment = "玩家4")
    protected int userid4;

    @FilterColumn(comment = "时间")
    protected Range.LongRange createtime;

    @Transient
    private int userid;

    @ConvertDisabled
    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
        this.userid1 = userid;
        this.userid2 = userid;
        this.userid3 = userid;
        this.userid4 = userid;
    }

    public String getRoundid() {
        return roundid;
    }

    public void setRoundid(String roundid) {
        this.roundid = roundid;
    }

    public int getClubid() {
        return clubid;
    }

    public void setClubid(int clubid) {
        this.clubid = clubid;
    }

    public String getGameid() {
        return gameid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public int getUserid1() {
        return userid1;
    }

    public void setUserid1(int userid1) {
        this.userid1 = userid1;
    }

    public int getUserid2() {
        return userid2;
    }

    public void setUserid2(int userid2) {
        this.userid2 = userid2;
    }

    public int getUserid3() {
        return userid3;
    }

    public void setUserid3(int userid3) {
        this.userid3 = userid3;
    }

    public int getUserid4() {
        return userid4;
    }

    public void setUserid4(int userid4) {
        this.userid4 = userid4;
    }

    public Range.LongRange getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Range.LongRange createtime) {
        this.createtime = createtime;
    }

}
