package com.cratos.platf.info;

import com.cratos.platf.base.*;
import javax.persistence.*;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
@Table(name = "awardrecord", comment = "用户摇奖记录表")
public class ScoreAwardRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "记录ID 值=user36id+'-'+create36time(9位)")
    private String awardrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "日期; 20190909")
    private int intday;

    @Column(comment = "中奖项等级; 1:白银盘;2:黄金盘;3:钻石盘;")
    private short awardlevel;

    @Column(comment = "摇奖中奖项ID")
    private int awardid;

    @Column(length = 32, comment = "中奖项的类型")
    private String awardtype = "";

    @Column(comment = "中奖项的类型数量值")
    private long awardval;

    @Column(comment = "总积分数")
    private long totalscore;

    @Column(comment = "剩下可积分数")
    private long remainscore;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    @Transient
    private Player user;

    public void setAwardrecordid(String awardrecordid) {
        this.awardrecordid = awardrecordid;
    }

    public String getAwardrecordid() {
        return this.awardrecordid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public Player getUser() {
        return user;
    }

    public void setUser(Player user) {
        this.user = user;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getIntday() {
        return intday;
    }

    public void setIntday(int intday) {
        this.intday = intday;
    }

    public void setAwardlevel(short awardlevel) {
        this.awardlevel = awardlevel;
    }

    public short getAwardlevel() {
        return this.awardlevel;
    }

    public void setAwardid(int awardid) {
        this.awardid = awardid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getAwardid() {
        return this.awardid;
    }

    public void setAwardtype(String awardtype) {
        this.awardtype = awardtype;
    }

    public String getAwardtype() {
        return this.awardtype;
    }

    public void setAwardval(long awardval) {
        this.awardval = awardval;
    }

    public long getAwardval() {
        return this.awardval;
    }

    public void setTotalscore(long totalscore) {
        this.totalscore = totalscore;
    }

    public long getTotalscore() {
        return this.totalscore;
    }

    public void setRemainscore(long remainscore) {
        this.remainscore = remainscore;
    }

    public long getRemainscore() {
        return this.remainscore;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    //@ConvertColumn(ignore = true, type = ConvertType.JSON)
    @ConvertDisabled
    public String getFromuser36id() {
        if (userid < 1) return "000000";
        return Integer.toString(userid, 36);
    }
}
