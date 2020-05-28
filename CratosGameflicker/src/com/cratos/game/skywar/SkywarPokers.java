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
public class SkywarPokers extends BaseBean {

    private int[] pokers = Skywars.random52Pokers();

    private int currpos;

    public int[] getPokers() {
        return pokers;
    }

    public void setPokers(int[] pokers) {
        this.pokers = pokers;
    }

    public int getCurrpos() {
        return currpos;
    }

    public int pollCard() {
        return pokers[currpos++];
    }

    public int remaining() {
        return pokers.length - currpos;
    }

}
