/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.base.UserInfo;
import com.cratos.platf.game.*;

import static com.cratos.game.skywar.SkywarSkinInfo.DEFAULT_SKINID;
import static com.cratos.platf.game.GamePlayer.READYSTATUS_UNREADY;
import com.cratos.platf.game.battle.*;
import java.net.InetSocketAddress;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class SkywarPlayer extends BattleGamePlayer {

    public static final short RADAR_STATUS_UNUSE = 10; //未使用巡航

    public static final short RADAR_STATUS_USING = 20; //巡航中

    public static final short RADAR_STATUS_PAUSE = 30; //暂停巡航中

    @Column(comment = "当前战机皮肤ID")
    protected int skinid = DEFAULT_SKINID;

    @Comment("狂暴结束时间点")
    protected long shotfrenzyendtime;

    @Comment("追踪的敌机")
    protected int shottrackenemyid;

    @Comment("追踪结束时间点")
    protected long shottrackendtime;

    @Comment("巡航结束时间点")
    protected long shotradarendtime;

    @Comment("巡航状态;20:巡航中;30:暂停巡航中;")
    protected short radarstatus;

    @Comment("激光结束时间点")
    protected long shotjiguangtime;

    @Transient
    protected SkywarSkinInfo skin;

    public SkywarPlayer() {
    }

    public SkywarPlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel) {
        super(user, clientAddr, sncpAddress, roomlevel, 0);
        this.readystatus = READYSTATUS_UNREADY;
    }

    public void updateSkin(SkywarSkinInfo skin) {
        this.skinid = skin.getSkinid();
        this.skin = skin;
    }

    public long startShotfrenzy() {
        long time = System.currentTimeMillis() + 30_000;
        if (skin != null) time += skin.getIncfrenzyseconds() * 1000;
        this.shotfrenzyendtime = time;
        return time;
    }

    public long startShottrack(int enemyid) {
        this.shottrackenemyid = enemyid;
        long time = System.currentTimeMillis() + 30_000;
        if (skin != null) time += skin.getIncfrenzyseconds() * 1000;
        this.shottrackendtime = time;
        return time;
    }

    public long startShotradar() {
        long time = System.currentTimeMillis() + 3600_000;
        this.shotradarendtime = time;
        this.radarstatus = RADAR_STATUS_USING;
        return time;
    }

    //激光剩余秒数
    public int getShotjiguangremains() {
        if (this.shotjiguangtime < 1) return 0;
        long time = System.currentTimeMillis();
        return time > this.shotjiguangtime ? 0 : (int) (this.shotjiguangtime - time) / 1000;
    }

    //巡航剩余秒数
    public int getShotadarremains() {
        long r = this.shotradarendtime - System.currentTimeMillis();
        return r > 0 ? (int) (r / 1000) : 0;
    }

    //是否巡航中
    public boolean isRadarusing() {
        return radarstatus == RADAR_STATUS_USING;
    }

    //巡航是否暂停中
    public boolean isRadarpause() {
        return radarstatus == RADAR_STATUS_PAUSE;
    }

    @Comment("获取狂暴剩余的秒数, 无追踪返回0")
    public int getShotfrenzyremains() {
        long ms = this.shotfrenzyendtime - System.currentTimeMillis();
        return ms < 0 ? 0 : (int) (ms / 1000);
    }

    @Comment("获取追踪剩余的秒数, 无追踪返回0")
    public int getShottrackremains() {
        long ms = this.shottrackendtime - System.currentTimeMillis();
        return ms < 0 ? 0 : (int) (ms / 1000);
    }

    public boolean isShotfrenzy() {
        return getShotfrenzyremains() > 0;
    }

    public boolean isShottrack() {
        return getShottrackremains() > 0;
    }

    @Override
    public long getCoins() {
        return this.coins;
    }

    public int getSkinid() {
        return skinid;
    }

    public void setSkinid(int skinid) {
        this.skinid = skinid;
    }

    public int getShottrackenemyid() {
        return shottrackenemyid;
    }

    public void setShottrackenemyid(int shottrackenemyid) {
        this.shottrackenemyid = shottrackenemyid;
    }

    public long getShotradarendtime() {
        return shotradarendtime;
    }

    public void setShotradarendtime(long shotradarendtime) {
        this.shotradarendtime = shotradarendtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getRadarstatus() {
        return radarstatus;
    }

    public void setRadarstatus(short radarstatus) {
        this.radarstatus = radarstatus;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getShottrackendtime() {
        return shottrackendtime;
    }

    public void setShottrackendtime(long shottrackendtime) {
        this.shottrackendtime = shottrackendtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getShotfrenzyendtime() {
        return shotfrenzyendtime;
    }

    public void setShotfrenzyendtime(long shotfrenzyendtime) {
        this.shotfrenzyendtime = shotfrenzyendtime;
    }

    public PoolDataRecord getData1Record() {
        return data1Record;
    }

    public void setData1Record(PoolDataRecord data1Record) {
        this.data1Record = data1Record;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getShotjiguangtime() {
        return shotjiguangtime;
    }

    public void setShotjiguangtime(long shotjiguangtime) {
        this.shotjiguangtime = shotjiguangtime;
    }

}
