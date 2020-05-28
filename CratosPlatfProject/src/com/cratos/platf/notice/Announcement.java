package com.cratos.platf.notice;

import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsInfo;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.*;
import org.redkale.util.LogLevel;

/**
 *
 * @author zhangjx
 */
@Cacheable(interval = 60)
@LogLevel("FINER")
@Table(comment = "登录公告表")
public class Announcement extends BaseEntity {

    public static final short ANNOUNCE_TYPE_LOGIN = 10; //登陆公告

    public static final short ANNOUNCE_TYPE_HROLL = 20; //滚动公告

    public static final short ANNOUNCE_TYPE_PRESS = 30; //紧急公告

    public static final short ANNOUNCE_TYPE_PLAIN = 40; //常规公告

    @Id
    @Column(comment = "公告ID 值=yyMMdd+3位序号")
    private int announceid;

    @Column(comment = "公告类型; 10:登陆公告; 20:滚动公告; 30:紧急公告; 40:常规公告;")
    private short type;

    @Column(length = 128, comment = "公告标题")
    private String title = "";

    @Column(length = 4096, comment = "公告内容")
    private String content = "";

    @Column(comment = "[状态]: 10:正常;70:过期;80:删除;")
    private short status;

    @Column(comment = "滚动公告间隔秒数")
    private int intervals = 0;

    @Column(comment = "滚动公告循环次数,为0表示无限循环")
    private int cycles = 0;

    @Column(comment = "公告起始时间")
    private long starttime;

    @Column(comment = "公告结束时间")
    private long endtime;

    @Column(comment = "操作人ID")
    private int memberid;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    @Column(length = 64, comment = "所属游戏")
    private String game = "";

    @Column(length = 64, comment = "游戏子模块")
    private String module = "";

    @Transient
    @Column(comment = "操作人")
    private String membername = "";

    public static Announcement createHrollAnnounceWinCoins(String game, int userid, String username, String gamename, long coins) {
        return createHrollAnnounceWinCoins(game, userid, username, gamename, "", coins);
    }

    public static Announcement createHrollAnnounceWinFactor(String game, int userid, String username, String gamename, int factor) {
        return createHrollAnnounceWinFactor(game, userid, username, gamename, "", factor);
    }

    public static Announcement createHrollAnnounceWinFactor(String game, int userid, String username, String gamename, float factor) {
        return createHrollAnnounceWinFactor(game, userid, username, gamename, "", factor);
    }

    public static Announcement createHrollAnnounceWinCoins(String game, int userid, String username, String gamename, String module, long coins) {
        String content = "恭喜" + ("<color=#F3D91C>[" + username + "]</color>")
            + "在" + ("<color=#B87938>[" + gamename + "]</color>") + (module == null ? "" : module)
            + "赢得了" + ("<color=#F3D91C>" + coins / GoodsInfo.EXCHANGE_RMB_COIN + "</color>") + "金币！";
        return createHrollAnnouncement(game, module, content);
    }

    public static Announcement createHrollAnnounceWinFactor(String game, int userid, String username, String gamename, String module, Number factor) {
        String content = "恭喜" + ("<color=#F3D91C>[" + username + "]</color>")
            + "在" + ("<color=#B87938>[" + gamename + "]</color>") + (module == null ? "" : module)
            + "赢得了" + ("<color=#F3D91C>" + factor + "</color>") + "倍奖励！";
        return createHrollAnnouncement(game, module, content);
    }

    public static Announcement createHrollAnnouncement(String game, String module, String content) {
        Announcement bean = new Announcement();
        bean.setGame(game);
        bean.setModule(module == null ? "" : module);
        bean.setContent(content);
        bean.setType(ANNOUNCE_TYPE_HROLL);
        bean.setStatus(STATUS_NORMAL);
        bean.setCycles(1);
        bean.setCreatetime(System.currentTimeMillis());
        return bean;
    }

    public void setAnnounceid(int announceid) {
        this.announceid = announceid;
    }

    public int getAnnounceid() {
        return this.announceid;
    }

    public void setType(short type) {
        this.type = type;
    }

    public short getType() {
        return this.type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public void setIntervals(int intervals) {
        this.intervals = intervals;
    }

    public int getIntervals() {
        return this.intervals;
    }

    public int getCycles() {
        return cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getStarttime() {
        return this.starttime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getEndtime() {
        return this.endtime;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMemberid() {
        return this.memberid;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getMembername() {
        return membername;
    }

    public void setMembername(String membername) {
        this.membername = membername;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }
}
