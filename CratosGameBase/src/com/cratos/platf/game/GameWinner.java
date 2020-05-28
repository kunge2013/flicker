/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.Player;
import org.redkale.util.*;
 
/**
 *
 * @author zhangjx
 */
public class GameWinner extends Player implements Comparable<GameWinner> {

    private static final Reproduce<GameWinner, GamePlayer> reproduce = Reproduce.create(GameWinner.class, GamePlayer.class);

    @Comment("玩家当前金币数")
    protected long coins;
    
    @Comment("输赢金币数(已减掉抽水费)，负数表示输给系统")
    protected long wincoin;

    public GameWinner() {
    }

    public GameWinner(GamePlayer player, long wincoin) {
        reproduce.apply(this, player);
        this.wincoin = wincoin;
    }
    
    public GameWinner(GameRound round, GamePlayer player, long wincoin) {
        reproduce.apply(this, player);
        this.wincoin = wincoin;
    }

    public void increWincoins(long wincoin) {
        this.wincoin += wincoin;
    }

    @Override
    public int compareTo(GameWinner o) {
        if (o == null) return -1;
        return (int) (o.wincoin - this.wincoin);
    }

    public long getWincoin() {
        return wincoin;
    }

    public void setWincoin(long wincoin) {
        this.wincoin = wincoin;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

}
