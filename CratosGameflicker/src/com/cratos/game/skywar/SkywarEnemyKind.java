/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.battle.BattleEnemyKind;
import javax.persistence.*;

/**
 * kindtype： 敌机类别: 2:巡逻机;3:普通不攻击机4:普通罚开炮敌机;5:BOSS敌机;6:奖券运输机;8:特殊敌机;
 *
 * @author zhangjx
 */
public class SkywarEnemyKind extends BattleEnemyKind {

    @Column(comment = "进入屏幕后多少秒后开始第一次发射炮弹")
    protected int shotdelay;

    @Column(comment = "炮弹发射频率，多少秒发射一次")
    protected int shotrate;

    @Column(comment = "炮弹威力")
    protected int shotblood;

    public int getShotdelay() {
        return shotdelay;
    }

    public void setShotdelay(int shotdelay) {
        this.shotdelay = shotdelay;
    }

    public int getShotrate() {
        return shotrate;
    }

    public void setShotrate(int shotrate) {
        this.shotrate = shotrate;
    }

    public int getShotblood() {
        return shotblood;
    }

    public void setShotblood(int shotblood) {
        this.shotblood = shotblood;
    }

}
