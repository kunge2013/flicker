/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.base;

import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.source.VirtualEntity;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
/**
 * 头像的url： http://redkale.org/dir/face_xx/{userid的36进制}.jpg
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@VirtualEntity(loader = UserInfoLoader.class)
public class UserInfo extends Player {

    public static final short USER_TYPE_AGENCY = 2; //代理用户

    public static final short USER_TYPE_PLAYER = 4; //玩家用户

    public static final short USER_TYPE_MATE = 8; //陪练用户

    private static final Reproduce<UserInfo, UserInfo> reproduce = Reproduce.create(UserInfo.class, UserInfo.class);

    private static final Reproduce<Player, UserInfo> playerReproduce = Reproduce.create(Player.class, UserInfo.class);

    private static final Reproduce<IntroPlayer, UserInfo> introPlayerReproduce = Reproduce.create(IntroPlayer.class, UserInfo.class);

    //男
    public static final short GENDER_MALE = 2;

    //女
    public static final short GENDER_FEMALE = 4;

    //银行转账等级: VIP10
    public static final short BANKLEVEL_VIP10 = 10;

    //平台的虚拟用户ID
    public static final int USERID_SYSTEM = 200_0000;

    //平台机器人最高ID  200_0001 -- 279_9999  为机器人ID
    public static final int USERID_MAXROBOT = 280_0000;

    //平台客服人员最低ID  280_0001 -- 289_9999  为客服人员ID
    public static final int USERID_MINKEFU = USERID_MAXROBOT;

    //平台测试人最低ID  290_0001 -- 299_9999  为测试账号ID
    public static final int USERID_MINTEST = 290_0000;

    // 普通用户的最小用户id值(区分于机器人)
    public static final int MIN_NORMAL_USERID = 3000000;

    public static final UserInfo USER_SYSTEM = new UserInfo();

    static {
        USER_SYSTEM.setUserid(USERID_SYSTEM);
        USER_SYSTEM.setUsername("系统");
        USER_SYSTEM.setEmail("");
        USER_SYSTEM.setMobile("");
    }

    @Column(length = 128, comment = "[用户账号]")
    protected String account = "";  //用户账号（前端不可见）

    @Column(comment = "[用户类型]2: 代理；4:玩家")
    protected short type = USER_TYPE_PLAYER;    //用户类型 （前端不可见）

    @Column(length = 32, comment = "[APP类型],适用多个app连同一服务器")
    protected String apposid = "";

    @Column(length = 128, comment = "[用户密码]")
    protected String password = ""; //密码（前端不可见） 数据库存放的密码规则为: HEX-SHA1( HEX-MD5( HEX-MD5(明文)+"-盐" ) +"-盐" )

    @Column(length = 128, comment = "[手机号码]")
    protected String mobile = "";  //手机号码（前端不可见）

    @Column(length = 128, comment = "[邮箱地址]")
    protected String email = "";  //邮箱  （前端不可见）

    @Column(length = 128, comment = "用户当前游戏")
    protected String currgame = "";  //用户当前游戏

    @Column(comment = "用户开始当前游戏的时间")
    protected long currgamingtime = 0;

    @Column(length = 127, comment = "[个人介绍]")
    protected String intro = ""; //备注

    @Column(comment = "密码连续错误次数")
    protected int pwdillcount;  //

    @Column(comment = "[代理商ID]")
    protected int agencyid;  //

    @Column(comment = "绑定代理的时间")
    protected long agencytime;

    @Column(comment = "[VIP等级]")
    protected int viplevel;  //

    @Column(comment = "个人累计充值金额总数, 单位:分")
    protected long paymoney;  //

    @Column(comment = "[个人金币数]")
    protected long coins;  //个人金币数  

    @Column(comment = "当日活跃度")
    protected long liveness;

    @Column(comment = "银行转账等级")
    protected short banklevel;  //

    @Column(comment = "[虚拟银行的金币数]")
    protected long bankcoins;

    @Column(comment = "[个人钻石数]")
    protected long diamonds;  //个人钻石数  

    @Column(comment = "个人累计购买的金币总数")
    protected long paycoins;  //

    @Column(comment = "[个人奖券数]")
    protected long coupons;  //个人奖券数  

    @Column(length = 128, comment = "[虚拟银行密码]")
    protected String bankpwd = "";

    @Column(comment = "实名制姓名")
    protected String shenfenname = "";  //

    @Column(comment = "实名制身份证号码")
    protected String shenfenno = "";  //

    @Column(length = 255, comment = "微信openid")
    protected String wxunionid = "";  //微信openid （前端不可见）

    @Column(length = 255, comment = "QQ openid")
    protected String qqunionid = "";  //QQ openid （前端不可见）

    @Column(length = 255, comment = "城信 openid")
    protected String cxunionid = "";  //城信 openid （前端不可见）

    @Column(length = 16, comment = "APP的设备系统(小写); android/ios/web/wap")
    protected String appos = "";//APP的设备系统 （前端不可见） 

    @Column(length = 255, comment = "APP的设备ID")
    protected String apptoken = "";  //APP的设备ID （前端不可见） 通常用于IOS的APNS推送

    @Column(comment = "[状态]: 10:正常;40:冻结;")
    protected short status;    //状态 （前端不可见）  值见BaseEntity的STATUS常量

    @Column(comment = "最后登录时间")
    protected long lastlogintime;

    @Column(comment = "连续登陆天数")
    protected int loginseries;

    @Column(comment = "在线总时长，单位:秒")
    protected long onlineseconds;  //

    @Column(updatable = false, comment = "[注册时间]")
    protected long regtime;

    @Column(updatable = false, length = 255, comment = "[注册app]")
    protected String regapptoken = "";

    public UserInfo copy() {
        return reproduce.apply(new UserInfo(), this);
    }

    public Player createPlayer() {
        return playerReproduce.apply(new Player(), this);
    }

    public IntroPlayer createIntroPlayer() {
        return introPlayerReproduce.apply(new IntroPlayer(), this);
    }

    public UserInfo copyTo(UserInfo dest) {
        return reproduce.apply(dest, this);
    }

    public static int getAge(String shenfenno) {
        if (shenfenno == null || shenfenno.length() < 18) return 0;
        int birthday = Integer.parseInt(shenfenno.substring(6, 14));
        java.util.Calendar now = java.util.Calendar.getInstance();
        int age = now.get(java.util.Calendar.YEAR) - birthday / 10000;
        int nmonth = now.get(java.util.Calendar.MONTH) + 1;
        int bmonth = birthday % 10000 / 100;
        if (nmonth > bmonth) return age;
        if (nmonth < bmonth) return age - 1;
        //同月
        int nday = now.get(java.util.Calendar.DAY_OF_MONTH);
        int bday = birthday % 100;
        if (nday >= bday) return age;
        return age - 1;
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public int getAge() {
        return getAge(this.shenfenno);
    }

    //当前的时长
    public long currOnlineseconds() {
        return (System.currentTimeMillis() - this.lastlogintime) / 1000 + this.onlineseconds;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRegapptoken() {
        return regapptoken;
    }

    public void setRegapptoken(String regapptoken) {
        this.regapptoken = regapptoken;
    }

    //用户是否设置过银行密码
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isBanksetted() {
        return this.bankpwd != null && !this.bankpwd.isEmpty();
    }

    //是否为代理用户
    public boolean isAgency() {
        return (this.type & USER_TYPE_AGENCY) > 0;
    }

    //是否为陪练用户
    public boolean isMate() {
        return (this.type & USER_TYPE_MATE) > 0;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public boolean isRobot() {
        return isRobot(userid);
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public boolean isKefu() {
        return isKefu(userid);
    }

    public static boolean isRobot(int userid) {
        return userid >= 2000000 && userid < USERID_MINKEFU;
    }

    public static boolean isKefu(int userid) {
        return userid > USERID_MINKEFU && userid < 3000000;
    }

    public boolean checkAuth(int moduleid, int actionid) {
        if (status == STATUS_CLOSED || status == STATUS_DELETED || status == STATUS_EXPIRE) return false;
        if (moduleid == 0 || actionid == 0) return true;
        //权限判断
        return true;
    }

    //用户是否绑定了微信
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isWxable() {
        return this.wxunionid != null && !this.wxunionid.isEmpty();
    }

    //用户是否绑定了QQ
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isQqable() {
        return this.qqunionid != null && !this.qqunionid.isEmpty();
    }

    //用户是否绑定了城信
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isCxable() {
        return this.cxunionid != null && !this.cxunionid.isEmpty();
    }

    //用户是否绑定了手机
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isMobilable() {
        return this.mobile != null && !this.mobile.isEmpty();
    }

    //用户是否设置了密码
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isPwdable() {
        return this.password != null && !this.password.isEmpty();
    }

    //用户是否处于正常状态
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isNormal() {
        return this.status == STATUS_NORMAL;
    }

    //用户是否处于禁用状态
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isFrobid() {
        return this.status == STATUS_FREEZE;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getPwdillcount() {
        return pwdillcount;
    }

    public void setPwdillcount(int pwdillcount) {
        this.pwdillcount = pwdillcount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getAgencyid() {
        return agencyid;
    }

    public void setAgencyid(int agencyid) {
        this.agencyid = agencyid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getAgencytime() {
        return agencytime;
    }

    public void setAgencytime(long agencytime) {
        this.agencytime = agencytime;
    }

    public int getViplevel() {
        return viplevel;
    }

    public void setViplevel(int viplevel) {
        this.viplevel = viplevel;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public void increCoins(long coins) {
        this.coins += coins;
    }

    public long getLiveness() {
        return liveness;
    }

    public void setLiveness(long liveness) {
        this.liveness = liveness;
    }

    public void incrLiveness(long liveness) {
        this.liveness += liveness;
    }

    public short getBanklevel() {
        return banklevel;
    }

    public void setBanklevel(short banklevel) {
        this.banklevel = banklevel;
    }

    public long getBankcoins() {
        return bankcoins;
    }

    public void setBankcoins(long bankcoins) {
        this.bankcoins = bankcoins;
    }

    public long getDiamonds() {
        return diamonds;
    }

    public void setDiamonds(long diamonds) {
        this.diamonds = diamonds;
    }

    public long getCoupons() {
        return coupons;
    }

    public void setCoupons(long coupons) {
        this.coupons = coupons;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getPaycoins() {
        return paycoins;
    }

    public void setPaycoins(long paycoins) {
        this.paycoins = paycoins;
    }

    @ConvertDisabled(type = ConvertType.JSON)
    public String getBankpwd() {
        return bankpwd;
    }

    public void setBankpwd(String bankpwd) {
        this.bankpwd = bankpwd;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getCurrgame() {
        return currgame == null ? "" : currgame;
    }

    public void setCurrgame(String currgame) {
        this.currgame = currgame;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCurrgamingtime() {
        return currgamingtime;
    }

    public void setCurrgamingtime(long currgamingtime) {
        this.currgamingtime = currgamingtime;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getAppos() {
        return appos;
    }

    public void setAppos(String appos) {
        this.appos = appos;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getUser36id() {
        return Integer.toString(userid, 36);
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getApposid() {
        return apposid;
    }

    public void setApposid(String apposid) {
        this.apposid = apposid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    //密码不允许输出给外部接口    
    @ConvertDisabled(type = ConvertType.JSON)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password.trim();
    }

    //手机号码不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getMobile2() {
        return (mobile == null || mobile.isEmpty()) ? "" : mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    //手机号码不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getMobile() {
        return mobile == null ? "" : mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile == null ? "" : mobile.trim();
    }

    public long getPaymoney() {
        return paymoney;
    }

    public void setPaymoney(long paymoney) {
        this.paymoney = paymoney;
    }

    //邮箱地址不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    //实名制不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getShenfenname2() {
        if (shenfenname == null || shenfenname.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        char[] chars = shenfenname.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i == chars.length - 1) {
                sb.append(chars[i]);
            } else {
                sb.append('*');
            }
        }
        return sb.toString();
    }

    //实名制不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getShenfenno2() {
        if (shenfenno == null || shenfenno.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        char[] chars = shenfenno.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i <= 1 || i >= chars.length - 1) {
                sb.append(chars[i]);
            } else {
                sb.append('*');
            }
        }
        return sb.toString();
    }

    //实名制不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getShenfenname() {
        return shenfenname;
    }

    public void setShenfenname(String shenfenname) {
        this.shenfenname = shenfenname;
    }

    //实名制不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getShenfenno() {
        return shenfenno;
    }

    public void setShenfenno(String shenfenno) {
        this.shenfenno = shenfenno;
    }

    //微信绑定ID不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getWxunionid() {
        return wxunionid == null ? "" : wxunionid;
    }

    public void setWxunionid(String wxunionid) {
        this.wxunionid = wxunionid == null ? "" : wxunionid.trim();
    }

    //QQ绑定ID不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getQqunionid() {
        return qqunionid == null ? "" : qqunionid;
    }

    public void setQqunionid(String qqunionid) {
        this.qqunionid = qqunionid == null ? "" : qqunionid.trim();
    }

    //城信绑定ID不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getCxunionid() {
        return cxunionid == null ? "" : cxunionid;
    }

    public void setCxunionid(String cxunionid) {
        this.cxunionid = cxunionid == null ? "" : cxunionid.trim();
    }

    //APP设备ID不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getApptoken() {
        return apptoken == null ? "" : apptoken;
    }

    public void setApptoken(String apptoken) {
        this.apptoken = apptoken == null ? "" : apptoken.trim();
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOnlineseconds() {
        return onlineseconds;
    }

    public void setOnlineseconds(long onlineseconds) {
        this.onlineseconds = onlineseconds;
    }

    //用户状态值不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public long getLastlogintime() {
        return lastlogintime;
    }

    public void setLastlogintime(long lastlogintime) {
        this.lastlogintime = lastlogintime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getLoginseries() {
        return loginseries;
    }

    public void setLoginseries(int loginseries) {
        this.loginseries = loginseries;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getRegtime() {
        return regtime;
    }

    public void setRegtime(long regtime) {
        this.regtime = regtime;
    }

}
