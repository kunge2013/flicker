/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.hundred;

import com.cratos.platf.base.UserInfo;
import com.cratos.platf.game.GamePlayer;
import java.net.InetSocketAddress;

/**
 *
 * @author zhangjx
 */
public class HundredGamePlayer extends GamePlayer {

    public HundredGamePlayer() {
        super();
    }

    public HundredGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, int sitepos) {
        super(user, clientAddr, sncpAddress, roomlevel, sitepos);
    }
}
