package com.cratos.platf.mission;

import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import javax.persistence.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "日活跃任务信息表")
public class MissionInfo extends BaseEntity {

    //任务种类
    public static final short MISSION_KIND_ONCE = 10;  //新手任务

    public static final short MISSION_KIND_DAY = 20;  //活跃任务

    //任务类型
    public static final int MISSION_TYPE_PLATF_LOGIN = 101;  //登录次数

    public static final int MISSION_TYPE_PLATF_PAYCOUNT = 106;  //充值次数

    //public static final int MISSION_TYPE_PLATF_PAYMONEY = 107;  //充值金额
    //
    public static final int MISSION_TYPE_PLATF_COSTCOIN = 111;  //消耗金币数

    public static final int MISSION_TYPE_PLATF_COSTDIAMOND = 112;  //消耗晶石数

    public static final int MISSION_TYPE_PLATF_COSTCOUPON = 113;  //消耗奖券数

    public static final int MISSION_TYPE_PLATF_WINCOIN = 121;  //获得金币数

    public static final int MISSION_TYPE_PLATF_WINDIAMOND = 122;  //获得晶石数

    public static final int MISSION_TYPE_PLATF_WINCOUPON = 123;  //获得奖券数

    //游戏任务
    public static final int MISSION_TYPE_GAME_SHOTCOUNT = 201;  //开炮次数

    public static final int MISSION_TYPE_GAME_KILLENEMY = 205;  //击落敌人个数

    public static final int MISSION_TYPE_GAME_KILLBOSS = 206;  //击落BOSS个数

    public static final int MISSION_TYPE_GAME_USEPROP = 211;  //使用道具次数

    public static final int MISSION_TYPE_GAME_FIRELEVEL = 221;  //火力等级

    @Id
    @Column(comment = "任务ID")
    private int missionid;

    @Column(length = 128, comment = "任务名称")
    private String missiontitle = "";

    @Column(length = 32, comment = "游戏ID")
    private String gameid = "";

    @Column(comment = "任务种类; 20:日活跃任务;")
    private short missionkind;

    @Column(comment = "任务达标类型")
    private int missiontype;

    @Column(comment = "任务达标所属ID")
    private int reachobjid;

    @Column(comment = "任务达标值")
    private long reachcount;

    @Column(length = 4096, nullable = false, comment = "任务奖励, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(comment = "排序顺序，值小靠前")
    private int display = 1000;

    @Column(length = 128, comment = "备注")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setMissionid(int missionid) {
        this.missionid = missionid;
    }

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

    public short getMissionkind() {
        return missionkind;
    }

    public void setMissionkind(short missionkind) {
        this.missionkind = missionkind;
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

    public long getCreatetime() {
        return this.createtime;
    }
}
