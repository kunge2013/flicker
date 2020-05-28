/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.line;

import com.cratos.platf.base.*;
import javax.persistence.Column;

/**
 *
 * @author zhangjx
 */
public class LineGameRoundBean extends BaseBean {

    @Column(comment = "用户ID")
    protected int userid;

    @Column(comment = "押注线条")
    protected int linenum = 1;

    protected int wildtest = 0;

    protected int bonustest = 0;

    protected int scattertest = 0;

    public static LineGameRoundBean createTest() {
        LineGameRoundBean bean = new LineGameRoundBean();
        bean.setUserid(UserInfo.USERID_SYSTEM);
        bean.setLinenum(1);
        return bean;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getLinenum() {
        return linenum;
    }

    public void setLinenum(int linenum) {
        this.linenum = linenum;
    }

    public int getWildtest() {
        return wildtest;
    }

    public void setWildtest(int wildtest) {
        this.wildtest = wildtest;
    }

    public int getBonustest() {
        return bonustest;
    }

    public void setBonustest(int bonustest) {
        this.bonustest = bonustest;
    }

    public int getScattertest() {
        return scattertest;
    }

    public void setScattertest(int scattertest) {
        this.scattertest = scattertest;
    }

}
