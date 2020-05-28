/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseBean;
import java.util.List;
import javax.persistence.Column;
import org.redkale.convert.ConvertDisabled;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class BattleKillBean extends BaseBean {

    @Column(comment = "用户ID")
    protected int userid;

    @Column(comment = "子弹的坐标X")
    protected float pointx;

    @Column(comment = "子弹的坐标Y")
    protected float pointy;

    @Column(comment = "击中的敌机ID列表")
    protected int[] enemyids;

    @Column(comment = "击落的敌机ID列表")
    protected List<Integer> killedEnemyids;

    @Comment("用户击落赢得的金币数")
    protected long wincoin;

    @Comment("用户击落后账号的金币数")
    protected long usercoins;

    @Comment("用户击落赢得的晶石数")
    protected long windiamond;

    @Comment("用户击落后账号的晶石数")
    protected long userdiamonds;
    
    @Comment("用户击落赢得的奖券数")
    protected long wincoupons;

    @Comment("用户击落后账号的奖券数")
    protected long usercoupons;
    
    @ConvertDisabled
    public int getEnemyCount() {
        return enemyids == null ? 0 : enemyids.length;
    }

    @ConvertDisabled
    public int getKilledEnemyCount() {
        return killedEnemyids == null ? 0 : killedEnemyids.size();
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public float getPointx() {
        return pointx;
    }

    public void setPointx(float pointx) {
        this.pointx = pointx;
    }

    public float getPointy() {
        return pointy;
    }

    public void setPointy(float pointy) {
        this.pointy = pointy;
    }

    public int[] getEnemyids() {
        return enemyids;
    }

    public void setEnemyids(int[] enemyids) {
        this.enemyids = enemyids;
    }

    public List<Integer> getKilledEnemyids() {
        return killedEnemyids;
    }

    public void setKilledEnemyids(List<Integer> killedEnemyids) {
        this.killedEnemyids = killedEnemyids;
    }

    public long getWincoin() {
        return wincoin;
    }

    public void setWincoin(long wincoin) {
        this.wincoin = wincoin;
    }

    public long getUsercoins() {
        return usercoins;
    }

    public void setUsercoins(long usercoins) {
        this.usercoins = usercoins;
    }

    public long getWindiamond() {
        return windiamond;
    }

    public void setWindiamond(long windiamond) {
        this.windiamond = windiamond;
    }

    public long getUserdiamonds() {
        return userdiamonds;
    }

    public void setUserdiamonds(long userdiamonds) {
        this.userdiamonds = userdiamonds;
    }

    public long getWincoupons() {
        return wincoupons;
    }

    public void setWincoupons(long wincoupons) {
        this.wincoupons = wincoupons;
    }

    public long getUsercoupons() {
        return usercoupons;
    }

    public void setUsercoupons(long usercoupons) {
        this.usercoupons = usercoupons;
    }

}
