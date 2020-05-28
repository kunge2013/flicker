/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.game.GameAccount;
import javax.persistence.Table;

/**
 *
 * @author zhangjx
 */
@Table(name = "multiaccount", comment = "本游戏用户账目表")
public class MultiGameAccount extends GameAccount {

    public MultiGameAccount() {
        super();
    }

    public MultiGameAccount(int userid) {
        super(userid);
    }
}
