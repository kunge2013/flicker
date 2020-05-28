/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.battle.BattleKillBean;
import java.util.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class SkywarKillBean extends BattleKillBean {

    @Comment("弹头UUID")
    protected String dantouid = "";

    int innerDantouKillPropid;

    long innerSpecialWinCoins;

    long innerSpecialWinDiamonds;
    
    long innerSpecialWinCoupons;

    boolean innerJiguangable;

    protected Map<String, Map<String, Object>> specialmap;

    public void addSpecialResult(int enemyid, Map<String, Object> map) {
        if (this.specialmap == null) this.specialmap = new HashMap<>();
        this.specialmap.put(String.valueOf(enemyid), map);
    }

    public String getDantouid() {
        return dantouid;
    }

    public void setDantouid(String dantouid) {
        this.dantouid = dantouid;
    }

    public Map<String, Map<String, Object>> getSpecialmap() {
        return specialmap;
    }

}
