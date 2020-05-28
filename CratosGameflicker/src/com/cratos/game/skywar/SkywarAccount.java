/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.battle.BattleGameAccount;

import static com.cratos.game.skywar.SkywarSkinInfo.DEFAULT_SKINID;

import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
public class SkywarAccount extends BattleGameAccount {

    @Column(comment = "当天血量")
    protected int currBlood;

    @Column(comment = "当天补血的日期")
    protected int bloodDay;

    @Column(comment = "当天补血的次数")
    protected int bloodCount;

    @Column(comment = "当天最后补血的时间点")
    protected long bloodTime;

    @Column(comment = "解锁僚机的数量")
    protected int wingUnlocks;

    @Column(comment = "当前正在使用的僚机数")
    protected int wingRunnings;

    @Column(comment = "当前使用僚机的状态: 10:使用中;20:暂停中;wingRunnings>0时此字段值才有意义")
    protected short wingRunState;

    @Column(comment = "当前僚机结束时间点，wingUnlocks>0时此字段值没意义")
    protected long wingingEndTime;

    @Column(comment = "当前巡航结束时间点")
    protected long radaringEndtime;

    @Column(comment = "本游戏的火力等级")
    protected int fireLevel = 10;

    @Column(comment = "当前战机皮肤ID")
    protected int currSkinid = DEFAULT_SKINID;

    @Column(length = 2048, nullable = false, comment = "当前可用皮肤ID列表")
    protected SkywarSkinRecord[] skinRecords;

    @Column(comment = "当前夺奖券的积分")
    protected long currAwardScore;

    @Column(comment = "点赞日期，格式:20191201")
    protected int zanRankDay;

    @Column(comment = "首充日期，格式:20191201")
    protected int onceGoodsDay;

    @Column(comment = "最后签到序号")
    protected int lastDutyIndex;

    @Column(comment = "最后签到日期，格式:20191201")
    protected int lastDutyDay;

    @Column(comment = "本游戏的总耗券数")
    protected long costcoupons;

    @Column(comment = "当前火箭弹的个数")
    protected int currRockets;

    @Column(comment = "当前导弹的个数")
    protected int currMissiles;

    @Column(comment = "当前核弹的个数")
    protected int currNuclears;

    @Column(comment = "当前巡航的个数")
    protected int currRadars;

    @Column(comment = "当前僚机的个数")
    protected int currWings;

    @Column(comment = "当前狂暴的次数")
    protected int currFrenzys;

    @Column(comment = "当前追踪的次数")
    protected int currTracks;

    @Column(comment = "当前传送的次数")
    protected int currTransmits;

    @Column(comment = "本游戏的使用火箭总次数")
    protected long costrockets;

    @Column(comment = "本游戏的使用导弹总次数")
    protected long costmissiles;

    @Column(comment = "本游戏的使用核弹总次数")
    protected long costnuclears;

    @Column(comment = "本游戏的使用巡航总次数")
    protected long costradars;

    @Column(comment = "本游戏的使用僚机总次数")
    protected long costwings;

    @Column(comment = "本游戏的使用狂暴总次数")
    protected long costfrenzys;

    @Column(comment = "本游戏的使用追踪总次数")
    protected long costtracks;

    @Column(comment = "本游戏的使用传输总次数")
    protected long costtransmits;

    @Transient //使用弹头的UUID
    protected String useDantouid = "";

    @Transient //使用弹头的时间
    protected long useDantouTime;

    public SkywarAccount() {
        super();
    }

    @Override  //加载后调用
    public void postLoad() {
        if (this.radaringEndtime > 0 && System.currentTimeMillis() >= this.radaringEndtime) {
            this.radaringEndtime = 0;
        }
        if (this.wingUnlocks > 0 || System.currentTimeMillis() >= this.wingingEndTime) {
            this.wingingEndTime = 0;
        }
        if (this.fireLevel < 10) this.fireLevel = 10;

        if (this.bloodDay != Utility.today()) {
            this.bloodDay = Utility.today();
            this.bloodCount = 0;
            this.bloodTime = System.currentTimeMillis();
        }
    }

    @Override  //入库之前调用
    public void preSave() {
        if (this.radaringEndtime > 0 && System.currentTimeMillis() >= this.radaringEndtime) {
            this.radaringEndtime = 0;
        }
        if (this.wingUnlocks > 0 || System.currentTimeMillis() >= this.wingingEndTime) {
            this.wingingEndTime = 0;
        }
        if (this.fireLevel < 10) this.fireLevel = 10;
    }

    public SkywarAccount(int userid) {
        super(userid);
    }

    //检查当前皮肤是否过期，过期了自动更换当前皮肤且返回true， 其他返回false
    public boolean checkExpireSkinRecord() {
        if (this.skinRecords == null) return false;
        long now = System.currentTimeMillis();
        SkywarSkinRecord[] newRecords = this.skinRecords;
        for (SkywarSkinRecord skin : this.skinRecords) {
            if (skin.getEndtime() > 0 && skin.getEndtime() <= now) {
                newRecords = Utility.remove(newRecords, skin);
            }
        }
        if (newRecords.length == 0) newRecords = null;
        this.skinRecords = newRecords;
        if (containsSkinid(this.currSkinid)) return false;
        this.currSkinid = SkywarSkinInfo.DEFAULT_SKINID;
        return true;
    }

    public boolean containsUnExpireSkinid(int skinid) {
        if (skinid == SkywarSkinInfo.DEFAULT_SKINID) return true;
        if (this.skinRecords == null) return false;
        for (SkywarSkinRecord skin : this.skinRecords) {
            if (skin.getSkinid() == skinid && skin.getEndtime() < 1) return true;
        }
        return false;
    }

    public boolean containsSkinid(int skinid) {
        if (skinid == SkywarSkinInfo.DEFAULT_SKINID) return true;
        if (this.skinRecords == null) return false;
        for (SkywarSkinRecord skin : this.skinRecords) {
            if (skin.getSkinid() == skinid) return true;
        }
        return false;
    }

    public void updateSkin(SkywarSkinInfo skin) {
        this.currSkinid = skin.getSkinid();
    }

    @ConvertDisabled
    public int getFactor(SkywarPlayer player) {
        int wingcount = wingRunningCount();
        return player.getShotlevel() * (1 + wingcount);
    }

    public void addSkinRecord(SkywarSkinRecord record) {
        if (record == null) return;
        if (this.skinRecords == null || this.skinRecords.length == 0) {
            this.skinRecords = new SkywarSkinRecord[]{record};
        } else {
            SkywarSkinRecord old = null;
            for (SkywarSkinRecord one : this.skinRecords) {
                if (one.getSkinid() == record.getSkinid()) {
                    old = one;
                    break;
                }
            }
            if (old == null) {
                this.skinRecords = Utility.append(this.skinRecords, record);
            } else if (old.getEndtime() != 0) {
                old.setEndtime(record.getEndtime());
            }
        }
    }

    //当前正在使用僚机的数量， 过期或暂停时都返回0
    public int wingRunningCount() {
        if (this.wingRunnings < 1) return 0;
        if (this.wingPausing()) return 0;
        if (this.wingingEndTime > 0 && System.currentTimeMillis() >= this.wingingEndTime) return 0;
        return this.wingRunnings;
    }

    public boolean wingPausing() {
        return this.wingRunState == 20;
    }

    public void wingPausing(boolean pause) {
        this.wingRunState = pause ? (short) 20 : (short) 10;
    }

    public int wingRemains() {
        if (this.wingUnlocks > 0) return 0;
        int wingcha = (int) ((System.currentTimeMillis() - wingingEndTime) / 1000);
        return wingcha < 1 ? 0 : wingcha;
    }

    public void incrCurrAwardScore(long score) {
        this.currAwardScore += score;
    }

    public void decCurrRockets() {
        this.currRockets--;
        this.costrockets++;
        this.useDantouTime = System.currentTimeMillis();
    }

    public void decCurrMissiles() {
        this.currMissiles--;
        this.costmissiles++;
        this.useDantouTime = System.currentTimeMillis();
    }

    public void decCurrNuclears() {
        this.currNuclears--;
        this.costnuclears++;
        this.useDantouTime = System.currentTimeMillis();
    }

    public void decCurrRadars() {
        this.currRadars--;
        this.costradars++;
    }

    public void decCurrTransmits() {
        this.currTransmits--;
        this.costtransmits++;
    }

    public void decCurrFrenzys() {
        this.currFrenzys--;
        this.costfrenzys++;
    }

    public void decCurrTracks() {
        this.currTracks--;
        this.costtracks++;
    }

    public void decCurrBlood(int blood) {
        this.currBlood -= blood;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getCurrBlood() {
        return currBlood;
    }

    public void setCurrBlood(int currBlood) {
        this.currBlood = currBlood;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getBloodDay() {
        return bloodDay;
    }

    public void setBloodDay(int bloodDay) {
        this.bloodDay = bloodDay;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getBloodCount() {
        return bloodCount;
    }

    public void setBloodCount(int bloodCount) {
        this.bloodCount = bloodCount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getBloodTime() {
        return bloodTime;
    }

    public void setBloodTime(long bloodTime) {
        this.bloodTime = bloodTime;
    }

    public int getFireLevel() {
        return fireLevel;
    }

    public void setFireLevel(int fireLevel) {
        this.fireLevel = fireLevel;
    }

    public int getCurrSkinid() {
        return currSkinid;
    }

    public void setCurrSkinid(int currSkinid) {
        this.currSkinid = currSkinid;
    }

    public SkywarSkinRecord[] getSkinRecords() {
        return skinRecords;
    }

    public int getOnceGoodsDay() {
        return onceGoodsDay;
    }

    public void setOnceGoodsDay(int onceGoodsDay) {
        this.onceGoodsDay = onceGoodsDay;
    }

    public int getZanRankDay() {
        return zanRankDay;
    }

    public void setZanRankDay(int zanRankDay) {
        this.zanRankDay = zanRankDay;
    }

    public int getLastDutyIndex() {
        return lastDutyIndex;
    }

    public void setLastDutyIndex(int lastDutyIndex) {
        this.lastDutyIndex = lastDutyIndex;
    }

    public int getLastDutyDay() {
        return lastDutyDay;
    }

    public void setLastDutyDay(int lastDutyDay) {
        this.lastDutyDay = lastDutyDay;
    }

    public void setSkinRecords(SkywarSkinRecord[] skinRecords) {
        this.skinRecords = skinRecords;
    }

    public int getWingUnlocks() {
        return wingUnlocks;
    }

    public void setWingUnlocks(int wingUnlocks) {
        this.wingUnlocks = wingUnlocks;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getWingRunnings() {
        return wingRunnings;
    }

    public void setWingRunnings(int wingRunnings) {
        this.wingRunnings = wingRunnings;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getWingingEndTime() {
        return wingingEndTime;
    }

    public void setWingingEndTime(long wingingEndTime) {
        this.wingingEndTime = wingingEndTime;
    }

    public short getWingRunState() {
        return wingRunState;
    }

    public void setWingRunState(short wingRunState) {
        this.wingRunState = wingRunState;
    }

    public int getCurrRockets() {
        return currRockets;
    }

    public void setCurrRockets(int currRockets) {
        this.currRockets = currRockets;
    }

    public int getCurrMissiles() {
        return currMissiles;
    }

    public void setCurrMissiles(int currMissiles) {
        this.currMissiles = currMissiles;
    }

    public int getCurrNuclears() {
        return currNuclears;
    }

    public void setCurrNuclears(int currNuclears) {
        this.currNuclears = currNuclears;
    }

    public int getCurrRadars() {
        return currRadars;
    }

    public void setCurrRadars(int currRadars) {
        this.currRadars = currRadars;
    }

    public int getCurrWings() {
        return currWings;
    }

    public void setCurrWings(int currWings) {
        this.currWings = currWings;
    }

    public int getCurrFrenzys() {
        return currFrenzys;
    }

    public void setCurrFrenzys(int currFrenzys) {
        this.currFrenzys = currFrenzys;
    }

    public int getCurrTracks() {
        return currTracks;
    }

    public void setCurrTracks(int currTracks) {
        this.currTracks = currTracks;
    }

    public int getCurrTransmits() {
        return currTransmits;
    }

    public void setCurrTransmits(int currTransmits) {
        this.currTransmits = currTransmits;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostcoupons() {
        return costcoupons;
    }

    public void setCostcoupons(long costcoupons) {
        this.costcoupons = costcoupons;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getRadaringEndtime() {
        return radaringEndtime;
    }

    public void setRadaringEndtime(long radaringEndtime) {
        this.radaringEndtime = radaringEndtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostrockets() {
        return costrockets;
    }

    public void setCostrockets(long costrockets) {
        this.costrockets = costrockets;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostmissiles() {
        return costmissiles;
    }

    public void setCostmissiles(long costmissiles) {
        this.costmissiles = costmissiles;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostnuclears() {
        return costnuclears;
    }

    public void setCostnuclears(long costnuclears) {
        this.costnuclears = costnuclears;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostradars() {
        return costradars;
    }

    public void setCostradars(long costradars) {
        this.costradars = costradars;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostwings() {
        return costwings;
    }

    public void setCostwings(long costwings) {
        this.costwings = costwings;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCostfrenzys() {
        return costfrenzys;
    }

    public void setCostfrenzys(long costfrenzys) {
        this.costfrenzys = costfrenzys;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCosttracks() {
        return costtracks;
    }

    public void setCosttracks(long costtracks) {
        this.costtracks = costtracks;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCosttransmits() {
        return costtransmits;
    }

    public void setCosttransmits(long costtransmits) {
        this.costtransmits = costtransmits;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCurrAwardScore() {
        return currAwardScore;
    }

    public void setCurrAwardScore(long currAwardScore) {
        this.currAwardScore = currAwardScore;
    }

}
