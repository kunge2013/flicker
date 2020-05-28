/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
public abstract class GameRound extends BaseEntity {

    public static final short ROUND_STATUS_UNSTART = 10; //回合状态; 未开始;

    public static final short ROUND_STATUS_READYING = 20; //回合状态; 准备中;

    public static final short ROUND_STATUS_BANKERING = 30; //回合状态; 抢庄中;

    public static final short ROUND_STATUS_PLAYING = 40; //回合状态; 游戏中;

    public static final short ROUND_STATUS_BETTING = 50; //回合状态; 押注中;

    public static final short ROUND_STATUS_SHOWING = 60; //回合状态; 明牌中;

    public static final short ROUND_STATUS_SETTLED = 70; //回合状态; 已结算;

    public static final short ROUND_STATUS_FINISH = 80;  //回合状态; 已结束;

    @Id
    @Column(length = 127, comment = "回合ID")
    protected String roundid;

    @Column(comment = "游戏ID")
    protected String gameid;

    @Column(comment = "场次，0表示无场次概念")
    protected int roomlevel;

    @Column(length = 255, comment = "备注描述")
    protected String remark = "";

    @Column(comment = "创建时间")
    protected long createtime;

    @Column(comment = "结束时间")
    protected long finishtime;

    public String getRoundid() {
        return roundid;
    }

    public void setRoundid(String roundid) {
        this.roundid = roundid;
    }

    public String getGameid() {
        return gameid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getFinishtime() {
        return finishtime;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

}
