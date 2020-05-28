/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.mission;

import com.cratos.platf.base.*;
import com.cratos.platf.order.GoodsItem;
import javax.persistence.*;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
public class MissionRecord extends BaseEntity {

    //任务状态  
    public static final short MISSION_STATUS_REACH = 10;  //可领取

    public static final short MISSION_STATUS_DOING = 20;  //未完成

    public static final short MISSION_STATUS_FINISH = 30;  //已领取

    @Id
    @Column(length = 128, comment = "任务记录ID,值=kind+'-'+missionid+'-'+userid+'-'+intday")
    protected String missionrecordid = "";

    @Column(comment = "用户ID")
    protected int userid;

    @Column(comment = "任务ID")
    protected int missionid;

    @Column(length = 128, comment = "任务名称")
    protected String missiontitle = "";

    @Column(length = 32, comment = "游戏ID")
    protected String gameid = "";

    @Column(comment = "任务种类; 20:日活跃任务;")
    protected short missionkind;

    @Column(comment = "任务达标类型")
    protected int missiontype;

    @Column(comment = "任务达标所属ID")
    protected int reachobjid;

    @Column(comment = "任务达标值")
    protected long reachcount;

    @Column(comment = "当前任务完成值")
    protected long currcount;

    @Column(comment = "任务状态; 10:可领取; 20:未完成; 30:已领取")
    protected short missionstatus = MISSION_STATUS_DOING;

    @Column(length = 4096, nullable = false, comment = "任务奖励, GoodsItem[]数组")
    protected GoodsItem[] goodsitems;

    @Column(comment = "排序顺序，值小靠前")
    protected int display = 1000;

    @Column(length = 128, comment = "备注")
    protected String remark = "";

    @Column(updatable = false, comment = "创建时间")
    protected long createtime;

    @Column(updatable = false, comment = "达标时间")
    protected long reachtime;

    @ConvertDisabled
    public boolean isReached() {
        return this.currcount >= this.reachcount;
    }

    public void setMissionstatus(short missionstatus) {
        this.missionstatus = missionstatus;
    }

    public short getMissionstatus() {
        return this.missionstatus;
    }

    public void increCurrcount(long count) {
        this.currcount += count;
    }

    public void setMissionrecordid(String missionrecordid) {
        this.missionrecordid = missionrecordid;
    }

    public String getMissionrecordid() {
        return this.missionrecordid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getUserid() {
        return this.userid;
    }

    public void setMissionid(int missionid) {
        this.missionid = missionid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMissionid() {
        return this.missionid;
    }

    public void setMissiontitle(String missiontitle) {
        this.missiontitle = missiontitle;
    }

    public String getMissiontitle() {
        return this.missiontitle;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public String getGameid() {
        return this.gameid;
    }

    public void setMissionkind(short missionkind) {
        this.missionkind = missionkind;
    }

    public short getMissionkind() {
        return this.missionkind;
    }

    public void setMissiontype(int missiontype) {
        this.missiontype = missiontype;
    }

    public int getMissiontype() {
        return this.missiontype;
    }

    public void setReachobjid(int reachobjid) {
        this.reachobjid = reachobjid;
    }

    public int getReachobjid() {
        return this.reachobjid;
    }

    public void setReachcount(long reachcount) {
        this.reachcount = reachcount;
    }

    public long getReachcount() {
        return this.reachcount;
    }

    public void setCurrcount(long currcount) {
        this.currcount = currcount;
    }

    public long getCurrcount() {
        return this.currcount;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public GoodsItem[] getGoodsitems() {
        return this.goodsitems;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    public int getDisplay() {
        return this.display;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }

    public void setReachtime(long reachtime) {
        this.reachtime = reachtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getReachtime() {
        return reachtime;
    }

    public static class MissionUpdateEntry extends BaseBean {

        public int userid;

        public int missiontype;

        public int reachobjid;

        public long reachcount;

        public long reachtime;

        public boolean incrable; //是否增量

        public MissionUpdateEntry(int userid, boolean incrable, int missiontype, long reachcount, long reachtime) {
            this.userid = userid;
            this.incrable = incrable;
            this.missiontype = missiontype;
            this.reachcount = reachcount;
            this.reachtime = reachtime;
        }

        public MissionUpdateEntry(int userid, boolean incrable, int missiontype, int reachobjid, long reachcount, long reachtime) {
            this.userid = userid;
            this.incrable = incrable;
            this.missiontype = missiontype;
            this.reachobjid = reachobjid;
            this.reachcount = reachcount;
            this.reachtime = reachtime;
        }

    }
}
