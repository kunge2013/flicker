/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.UserInfo;
import com.cratos.platf.game.*;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import javax.persistence.Transient;
import org.redkale.convert.ConvertDisabled;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class BattleGamePlayer extends GamePlayer {

    @Comment("位置坐标X")
    protected float pointx;

    @Comment("位置坐标Y")
    protected float pointy;

    @Comment("子弹等级")
    protected int shotlevel = 1;

    @Comment("自动开火")
    protected boolean shotauto;

    @Comment("子弹登记的统计信息") //shotlevel:count
    protected final Map<Integer, BattleKindCounter> kindCounterMap = new ConcurrentHashMap<>();

    @Transient
    @ConvertDisabled
    @Comment("子弹消耗队列")
    public final BlockingQueue<Long> costQueue = new LinkedBlockingQueue<>();

    @Transient
    @ConvertDisabled
    public PoolDataRecord data1Record;

    @Transient
    @ConvertDisabled
    public PoolDataRecord data2Record;

    @Transient
    @ConvertDisabled
    public PoolDataRecord data3Record;

    public BattleGamePlayer() {
        super();
    }

    public BattleGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, int sitepos) {
        super(user, clientAddr, sncpAddress, roomlevel, sitepos);
    }

    public void setPoint(float pointx, float pointy) {
        this.pointx = pointx;
        this.pointy = pointy;
    }

    public BattleKindCounter findKindCounter(int kindid) {
        return this.kindCounterMap.get(kindid);
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

    public int getShotlevel() {
        return shotlevel;
    }

    public void setShotlevel(int shotlevel) {
        this.shotlevel = shotlevel;
    }

    public boolean isShotauto() {
        return shotauto;
    }

    public void setShotauto(boolean shotauto) {
        this.shotauto = shotauto;
    }

}
