/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
public class BattleEnemyKind extends BaseEntity implements Comparable<BattleEnemyKind> {

    @Id
    @Column(comment = "种类ID(3位)")
    protected int kindid;

    @Column(comment = "类别")
    protected short kindtype;

    @Column(length = 32, comment = "种类名称")
    protected String kindname = "";

    @Column(comment = "可进入的场次，格式: [1,2,3,4,5]")
    protected int[] roomlevels;

    @Column(comment = "奖励的金币")
    protected int kindcoin;

    @Column(comment = "奖励的晶石")
    protected int kinddiamond;

    @Column(comment = "奖励的奖券")
    protected int kindcoupon;

    @Column(length = 2048, comment = "种类说明")
    protected String kinddesc = "";

    @Column(comment = "[状态]: 10:正常;40:冻结;")
    protected short status = STATUS_NORMAL;

    @Column(comment = "长度，像素值")
    protected int width;

    @Column(comment = "宽度，像素值")
    protected int height;

    @Column(comment = "生命值，普通值为100")
    protected int hp = 100;

    @Column(comment = "生命周期，存活的耗秒数")
    protected int lifems;

    @Column(comment = "刷新数量")
    protected int newnum = 1;

    @Column(comment = "刷新概率")
    protected int newrate = 1;

    @Column(comment = "吃币期命中率，千分制, 100%的命中率值为1000")
    protected int hitrate;

    @Column(comment = "吐币期命中率，千分制, 100%的命中率值为1000")
    protected int hitrate2;

    public int getKindid() {
        return kindid;
    }

    public void setKindid(int kindid) {
        this.kindid = kindid;
    }

    public short getKindtype() {
        return kindtype;
    }

    public void setKindtype(short kindtype) {
        this.kindtype = kindtype;
    }

    public String getKindname() {
        return kindname;
    }

    public void setKindname(String kindname) {
        this.kindname = kindname;
    }

    public int[] getRoomlevels() {
        return roomlevels;
    }

    public void setRoomlevels(int[] roomlevels) {
        this.roomlevels = roomlevels;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getKindcoin() {
        return kindcoin;
    }

    public void setKindcoin(int kindcoin) {
        this.kindcoin = kindcoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getKinddiamond() {
        return kinddiamond;
    }

    public void setKinddiamond(int kinddiamond) {
        this.kinddiamond = kinddiamond;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getKindcoupon() {
        return kindcoupon;
    }

    public void setKindcoupon(int kindcoupon) {
        this.kindcoupon = kindcoupon;
    }

    public String getKinddesc() {
        return kinddesc;
    }

    public void setKinddesc(String kinddesc) {
        this.kinddesc = kinddesc;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getLifems() {
        return lifems;
    }

    public void setLifems(int lifems) {
        this.lifems = lifems;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getNewnum() {
        return newnum;
    }

    public void setNewnum(int newnum) {
        this.newnum = newnum;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getNewrate() {
        return newrate;
    }

    public void setNewrate(int newrate) {
        this.newrate = newrate;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getHitrate() {
        return hitrate;
    }

    public void setHitrate(int hitrate) {
        this.hitrate = hitrate;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getHitrate2() {
        return hitrate2;
    }

    public void setHitrate2(int hitrate2) {
        this.hitrate2 = hitrate2;
    }

    @Override
    public int compareTo(BattleEnemyKind o) {
        return o == null ? 1 : o.kindid - this.kindid;
    }

}
