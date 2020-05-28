/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseBean;

/**
 *
 * @author zhangjx
 */
public class GameCoinEntry extends BaseBean implements Comparable<GameCoinEntry> {

    private long betcoins;

    private int betindex;

    public GameCoinEntry() {
    }

    public GameCoinEntry(long coins, int index) {
        this.betcoins = coins;
        this.betindex = index;
    }

    public long getBetcoins() {
        return betcoins;
    }

    public void setBetcoins(long betcoins) {
        this.betcoins = betcoins;
    }

    public int getBetindex() {
        return betindex;
    }

    public void setBetindex(int betindex) {
        this.betindex = betindex;
    }

    @Override
    public int compareTo(GameCoinEntry o) {
        return (this.betcoins == o.betcoins) ? 0 : (this.betcoins > o.betcoins ? 1 : -1);
    }

}
