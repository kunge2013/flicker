/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.game.GameAccount;
import javax.persistence.*;

/**
 *
 * @author zhangjx
 */
@Table(name = "battleaccount", comment = "本游戏用户账目表")
public class BattleGameAccount extends GameAccount {

    @Column(comment = "当前需要奖励时的场次")
    protected int currRoomlevel;

    public BattleGameAccount() {
        super();
    }

    public BattleGameAccount(int userid) {
        super(userid);
    }

    public int getCurrRoomlevel() {
        return currRoomlevel;
    }

    public void setCurrRoomlevel(int currRoomlevel) {
        this.currRoomlevel = currRoomlevel;
    }

}
