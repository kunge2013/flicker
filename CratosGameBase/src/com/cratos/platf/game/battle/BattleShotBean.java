/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseBean;
import org.redkale.convert.ConvertDisabled;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class BattleShotBean extends BaseBean {

    @Comment("用户ID")
    protected int userid;

    @Comment("射击属性，位存储;按16进制分段(个位开始):子弹等级(4位)+发射角度(2位, 0-180)+自动穿透锁定[位存储](2位,1:自动;2:锁定;4:穿透;)")
    protected long shotbits;

    @Comment("位置坐标X")
    protected double pointx;

    @Comment("位置坐标Y")
    protected double pointy;

    @Comment("锁定的敌机ID")
    protected int lockenemyid;

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public long getShotbits() {
        return shotbits;
    }

    public void setShotbits(long shotbits) {
        this.shotbits = shotbits;
    }

    public int getLockenemyid() {
        return lockenemyid;
    }

    public void setLockenemyid(int lockenemyid) {
        this.lockenemyid = lockenemyid;
    }

    public double getPointx() {
        return pointx;
    }

    public void setPointx(double pointx) {
        this.pointx = pointx;
    }

    public double getPointy() {
        return pointy;
    }

    public void setPointy(double pointy) {
        this.pointy = pointy;
    }

    @Comment("子弹等级")
    @ConvertDisabled
    public int getShotlevel() {
        return getShotlevel(this.shotbits);
    }

    @Comment("发射角度, 0-180之间,左边起点为0")
    @ConvertDisabled
    public int getShotsin() {
        return getShotsin(this.shotbits);
    }

    @Comment("是否自动")
    @ConvertDisabled
    public boolean isShotauto() {
        return isShotauto(this.shotbits);
    }

    @Comment("是否锁定")
    @ConvertDisabled
    public boolean isShotlock() {
        return isShotlock(this.shotbits);
    }

    @Comment("是否穿透")
    @ConvertDisabled
    public boolean isShotthrough() {
        return isShotthrough(this.shotbits);
    }

    public void setShotLevel(int level) {
        this.shotbits = ((this.shotbits >> 16) << 16) + level;
    }

    public void setShotSin(int sin) {
        this.shotbits = ((this.shotbits >> 24) << 24) + (sin << 16) + (shotbits & 0xffff);
    }

    public void setShotAuto(boolean auto) {
        if (auto) {
            this.shotbits |= (1 << 24);
        } else {
            this.shotbits ^= (1 << 24);
        }
    }

    public void setShotLock(boolean lock) {
        if (lock) {
            this.shotbits |= (2 << 24);
        } else {
            this.shotbits ^= (2 << 24);
        }
    }

    public void setShotThrough(boolean through) {
        if (through) {
            this.shotbits |= (4 << 24);
        } else {
            this.shotbits ^= (4 << 24);
        }
    }

    //------------- 静态方法 ----------------
    @Comment("子弹等级")
    public static int getShotlevel(long shotbits) {
        return (int) (shotbits & 0xffff);
    }

    @Comment("发射角度, 0-180之间,左边起点为0")
    public static int getShotsin(long shotbits) {
        return (int) (shotbits & 0xff0000) >> 16;
    }

    @Comment("是否自动")
    public static boolean isShotauto(long shotbits) {
        return (((shotbits & 0xff000000) >> 24) & 1) > 0;
    }

    @Comment("是否锁定")
    public static boolean isShotlock(long shotbits) {
        return (((shotbits & 0xff000000) >> 24) & 2) > 0;
    }

    @Comment("是否穿透")
    public static boolean isShotthrough(long shotbits) {
        return (((shotbits & 0xff000000) >> 24) & 4) > 0;
    }
}
