package com.cratos.platf.info;

import com.cratos.platf.base.*;
import java.util.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
@Table(comment = "玩家排行榜表")
public class RankRecord extends BaseEntity implements Comparable<RankRecord> {

    @Comment("盈利排行")
    public static final short RANK_TYPE_WIN = 10;

    @Comment("幸运排行")
    public static final short RANK_TYPE_LUCK = 20;

    @Comment("奖券排行")
    public static final short RANK_TYPE_COUPON = 30;

    @Comment("装备排行")
    public static final short RANK_TYPE_EQUIP = 40;

    @Id
    @Column(length = 32, comment = "排行ID; R+ranktype+intday+三位rankindex")
    private String rankid = "";

    @Column(comment = "玩家ID")
    private int userid;

    @Column(comment = "日期; 20190909")
    private int intday;

    @Column(comment = "排行类型: 10:盈利排行榜; 20:幸运排行榜;30:奖券排行榜;40:装备排行榜;")
    private short ranktype;

    @Column(comment = "排行序号，从1开始;")
    private int rankindex;

    @Column(length = 32, comment = "游戏ID，ranktype=20/40才用到该值")
    private String gameid = "";

    @Column(comment = "排行数值")
    private long rankvalue;

    @Column(updatable = false, comment = "游戏时间，ranktype=20该值才有效")
    private long playtime;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    @Transient
    private String gamename = "";

    @Transient
    private IntroPlayer player;

    public void createRankid() {
        this.rankid = "r" + ranktype + intday + (rankindex >= 100 ? rankindex : (rankindex >= 10 ? ("0" + rankindex) : ("00" + rankindex)));
    }

    @Override
    public int compareTo(RankRecord o) {
        if (o == null) return -1;
        return (int) (o.rankvalue - this.rankvalue);
    }

    public static void sort(List<RankRecord> records) {
        Collections.sort(records);
        for (int i = 0; i < records.size(); i++) {
            records.get(i).setRankindex(i + 1);
        }
    }

    public void setRankid(String rankid) {
        this.rankid = rankid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRankid() {
        return this.rankid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setIntday(int intday) {
        this.intday = intday;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getIntday() {
        return this.intday;
    }

    public void setRanktype(short ranktype) {
        this.ranktype = ranktype;
    }

    public short getRanktype() {
        return this.ranktype;
    }

    public void setRankindex(int rankindex) {
        this.rankindex = rankindex;
    }

    public int getRankindex() {
        return this.rankindex;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public String getGameid() {
        return this.gameid;
    }

    public String getGamename() {
        return gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public void setRankvalue(long rankvalue) {
        this.rankvalue = rankvalue;
    }

    public long getRankvalue() {
        return this.rankvalue;
    }

    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }

    public IntroPlayer getPlayer() {
        return player;
    }

    public void setPlayer(IntroPlayer player) {
        this.player = player;
    }

}
