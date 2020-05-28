/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.base.BaseBean;

/**
 *
 * @author zhangjx
 */
public class SkywarGiveBean extends BaseBean {

    private int touserid;

    private int propid;

    private int propcount;

    public int getTouserid() {
        return touserid;
    }

    public void setTouserid(int touserid) {
        this.touserid = touserid;
    }

    public int getPropid() {
        return propid;
    }

    public void setPropid(int propid) {
        this.propid = propid;
    }

    public int getPropcount() {
        return propcount;
    }

    public void setPropcount(int propcount) {
        this.propcount = propcount;
    }

}
