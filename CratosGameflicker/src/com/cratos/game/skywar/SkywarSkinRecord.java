/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.Column;

/**
 *
 * @author zhangjx
 */
public class SkywarSkinRecord extends BaseEntity {

    @Column(comment = "皮肤ID")
    private int skinid;

    @Column(comment = "皮肤ID加入的起始时间")
    private long starttime;

    @Column(comment = "皮肤ID有效结束时间，为0表示永不过期")
    private long endtime;

    public int getSkinid() {
        return skinid;
    }

    public void setSkinid(int skinid) {
        this.skinid = skinid;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getEndtime() {
        return endtime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

}
