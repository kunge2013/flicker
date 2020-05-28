/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class LoginAbstractBean extends Location {

    private static final Reproduce<UserLoginRecord, LoginAbstractBean> reproduce = Reproduce.create(UserLoginRecord.class, LoginAbstractBean.class);

    @Comment("[APP类型]")
    protected String apposid = "";

    @Comment("登录网络类型; wifi/4g/3g")
    protected String netmode = "";

    @Comment("APP的设备系统(小写); android/ios/web/wap")
    protected String appos = "";//APP的设备系统

    @Comment("APP设备唯一标识")
    protected String apptoken = ""; //APP设备唯一标识

    @Comment("自动登录Cookie值")
    protected String cookieinfo; //自动登录Cookie值

    @Comment("User-Agent")
    protected String loginagent = ""; //User-Agent

    @Comment("客户端IP地址")
    protected String loginaddr = ""; //客户端IP地址

    @Comment("Session会话ID")
    protected String sessionid = ""; // session ID

    @Comment("是否为自动登陆")
    private boolean autologin = false; //是否为自动登陆

    public UserLoginRecord createUserLoginRecord(UserInfo user) {
        UserLoginRecord record = reproduce.apply(new UserLoginRecord(), this);
        record.setUserid(user.getUserid());
        record.setCreatetime(System.currentTimeMillis());
        if (this.longitude == null) this.longitude = 0.0;
        if (this.latitude == null) this.latitude = 0.0;
        if (this.street == null) this.street = "";
        record.setLongitude(this.longitude);
        record.setLatitude(this.latitude);
        record.setStreet(this.street);
        record.setLoginid(Utility.format36time(record.getCreatetime()) + "-" + user.getUser36id());
        return record;
    }

    public String getApposid() {
        return apposid;
    }

    public void setApposid(String apposid) {
        this.apposid = apposid;
    }

    public boolean emptyApptoken() {
        return this.apptoken == null || this.apptoken.isEmpty();
    }

    public boolean emptySessionid() {
        return this.sessionid == null || this.sessionid.isEmpty();
    }

    public boolean emptyCookieinfo() {
        return this.cookieinfo == null || this.cookieinfo.isEmpty();
    }

    public String getAppos() {
        return appos;
    }

    public void setAppos(String appos) {
        if (appos != null) {
            this.appos = appos.trim().toLowerCase();
        }
    }

    public String getApptoken() {
        return apptoken;
    }

    public void setApptoken(String apptoken) {
        if (apptoken != null) {
            this.apptoken = apptoken.trim();
        }
    }

    public String getCookieinfo() {
        return cookieinfo;
    }

    public void setCookieinfo(String cookieinfo) {
        this.cookieinfo = cookieinfo;
    }

    public String getLoginagent() {
        return loginagent;
    }

    public void setLoginagent(String loginagent) {
        if (loginagent != null) {
            if (loginagent.length() > 128) loginagent = loginagent.substring(0, 127);
            this.loginagent = loginagent;
        }
    }

    public String getLoginaddr() {
        return loginaddr;
    }

    public void setLoginaddr(String loginaddr) {
        this.loginaddr = loginaddr;
    }

    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    public String getNetmode() {
        return netmode;
    }

    public void setNetmode(String netmode) {
        if (netmode != null) {
            this.netmode = netmode.trim().toLowerCase();
        }
    }

    public boolean isAutologin() {
        return autologin;
    }

    public void setAutologin(boolean autologin) {
        this.autologin = autologin;
    }

}
