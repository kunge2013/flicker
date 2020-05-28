/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.*;
import com.cratos.platf.user.Location;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.net.http.WebSocketUserAddress;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class GamePlayer extends Location implements WebSocketUserAddress, Comparable<GamePlayer> {

    private static final Reproduce<GamePlayer, UserInfo> reproduce = Reproduce.create(GamePlayer.class, UserInfo.class);

    //--------------------- 准备状态 ----------------------------------
    public static final short READYSTATUS_UNREADY = 10; // 玩家准备状态-未准备

    public static final short READYSTATUS_READYED = 20; // 玩家准备状态-已准备

    public static final short READYSTATUS_PLAYING = 30; // 玩家准备状态-游戏中

    //--------------------- 在线状态 ----------------------------------
    public static final short LIVESTATUS_ONLINE = 10; // 在线状态-在线

    public static final short LIVESTATUS_OFFLINE = 20;// 在线状态-离线

    @Column(comment = "[用户ID] 值从200_0001开始; 36进制固定长度为5位")
    protected int userid;

    @Column(length = 128, comment = "[用户昵称]")
    protected String username;

    @Column(length = 255, comment = "用户头像")
    protected String face = "";

    @Column(comment = "[性别]：2：男； 4:女；")
    protected short gender; //性别; 2:男;  4:女;

    @Column(comment = "[VIP等级]")
    protected int viplevel;

    @Column(comment = "[个人金币数]")
    protected long coins;

    @Column(comment = "[个人钻石数]")
    protected long diamonds;

    @Column(comment = "[个人奖券数]")
    protected long coupons;

    @Column(comment = "银行转账等级")
    protected short banklevel;  //

    @Column(comment = "[虚拟银行的金币数]")
    protected long bankcoins;

    @Column(comment = "实名制姓名")
    protected String shenfenname = "";  //

    @Column(comment = "实名制身份证号码")
    protected String shenfenno = "";  //
    //-----------------------------------------------------------------

    @Comment("玩家准备状态:10:未准备;20:已准备;30:游戏中")
    protected short readystatus;

    @Comment("在线状态:10:在线;20:离线;")
    protected short livestatus;

    @Comment("掉线的时间点")
    protected long offlinetime;

    @Comment("场次")
    protected int roomlevel;

    @Comment("玩家坐位,0表示无坐位概念, 1-4")
    protected int sitepos;

    @Column(comment = "客户端的IP")
    protected String clientAddr = "";

    @Column(comment = "WS连接的地址")
    protected InetSocketAddress sncpAddress;

    @Comment("玩家加入时间")
    protected long jointime;

    @Transient
    protected GameTable table;

    public GamePlayer() {
    }

    public GamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, int sitepos) {
        GamePlayer self = this;
        reproduce.apply(self, user);
        if (!user.isRobot()) {
            Objects.requireNonNull(clientAddr);
            Objects.requireNonNull(sncpAddress);
        }
        this.clientAddr = clientAddr;
        this.sncpAddress = sncpAddress;
        this.roomlevel = roomlevel;
        this.sitepos = sitepos;
        this.readystatus = READYSTATUS_UNREADY;
        this.livestatus = LIVESTATUS_ONLINE;
        this.jointime = System.currentTimeMillis();
    }

    public <T extends GamePlayer> T userCoin(long coin) {
        this.coins = coin;
        return (T) this;
    }

    public <T extends GameTable> T table() {
        return (T) table;
    }

    public <T extends GameTable> void table(T t) {
        this.table = t;
    }

    public <T extends GamePlayer> T copyFromUser(UserInfo user) {
        reproduce.apply(this, user);
        return (T) this;
    }

    public <T extends GamePlayer> T copyFromUserAndOnline(UserInfo user, String clientAddr, InetSocketAddress sncpAddress) {
        reproduce.apply(this, user);
        this.online(clientAddr, sncpAddress);
        return (T) this;
    }

    @Override
    public int compareTo(GamePlayer o) {
        if (o == null) return -1;
        return o.userid - this.userid;
    }

    @Override
    public Serializable userid() {
        return userid;
    }

    @Override
    public InetSocketAddress sncpAddress() {
        return sncpAddress;
    }

    @Override
    public Collection<InetSocketAddress> sncpAddresses() {
        return null;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public InetSocketAddress getSncpAddress() {
        return sncpAddress;
    }

    public void setSncpAddress(InetSocketAddress sncpAddress) {
        this.sncpAddress = sncpAddress;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    public long increCoin(long coin) {
        this.coins += coin;
        return this.coins;
    }

    public long increDiamond(long diamond) {
        this.diamonds += diamond;
        return this.diamonds;
    }

    public long increCoupon(long coupon) {
        this.coupons += coupon;
        return this.coupons;
    }

    @ConvertDisabled
    public boolean isOnline() {
        return this.livestatus == LIVESTATUS_ONLINE;
    }

    public void offline() {
        this.livestatus = LIVESTATUS_OFFLINE;
        this.sncpAddress = null;
        this.offlinetime = System.currentTimeMillis();
    }

    public void online(String clientAddr, InetSocketAddress sncpAddress) {
        this.livestatus = LIVESTATUS_ONLINE;
        if (!UserInfo.isRobot(this.userid)) {
            Objects.requireNonNull(clientAddr);
            Objects.requireNonNull(sncpAddress);
        }
        this.clientAddr = clientAddr;
        this.sncpAddress = sncpAddress;
        this.offlinetime = 0;
    }

    @ConvertDisabled
    public int getAge() {
        return UserInfo.getAge(this.shenfenno);
    }

    public String label() {
        return "玩家(userid=" + this.userid + ")";
    }

    @ConvertDisabled
    public boolean isPlaying() {
        return readystatus == READYSTATUS_PLAYING;
    }

    @ConvertDisabled
    public boolean isRobot() {
        return UserInfo.isRobot(this.userid);
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public short getGender() {
        return gender;
    }

    public void setGender(short gender) {
        this.gender = gender;
    }

    public int getViplevel() {
        return viplevel;
    }

    public void setViplevel(int viplevel) {
        this.viplevel = viplevel;
    }

    public short getReadystatus() {
        return readystatus;
    }

    public void setReadystatus(short readystatus) {
        this.readystatus = readystatus;
    }

    public short getLivestatus() {
        return livestatus;
    }

    public void setLivestatus(short livestatus) {
        this.livestatus = livestatus;
    }

    public int getSitepos() {
        return sitepos;
    }

    public void setSitepos(int sitepos) {
        this.sitepos = sitepos;
    }

    //------------------------ 不可见 ------------------------
    //
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getDiamonds() {
        return diamonds;
    }

    public void setDiamonds(long diamonds) {
        this.diamonds = diamonds;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCoupons() {
        return coupons;
    }

    public void setCoupons(long coupons) {
        this.coupons = coupons;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getBanklevel() {
        return banklevel;
    }

    public void setBanklevel(short banklevel) {
        this.banklevel = banklevel;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getBankcoins() {
        return bankcoins;
    }

    public void setBankcoins(long bankcoins) {
        this.bankcoins = bankcoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOfflinetime() {
        return offlinetime;
    }

    public void setOfflinetime(long offlinetime) {
        this.offlinetime = offlinetime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getJointime() {
        return jointime;
    }

    public void setJointime(long jointime) {
        this.jointime = jointime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getShenfenname() {
        return shenfenname;
    }

    public void setShenfenname(String shenfenname) {
        this.shenfenname = shenfenname;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getShenfenno() {
        return shenfenno;
    }

    public void setShenfenno(String shenfenno) {
        this.shenfenno = shenfenno;
    }

}
