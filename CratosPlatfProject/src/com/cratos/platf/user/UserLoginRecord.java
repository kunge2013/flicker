package com.cratos.platf.user;

import com.cratos.platf.base.BaseEntity;
import java.io.Serializable;
import javax.persistence.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "用户登陆日志表")
@DistributeTable(strategy = UserLoginRecord.TableStrategy.class)
public class UserLoginRecord extends BaseEntity {

    @Id
    @Column(length = 32, comment = "[登陆ID] create36time(9位)+'-'+user36id(5位)")
    private String loginid = "";

    @Column(comment = "[用户ID]")
    private int userid;

    @Column(comment = "在线总时长，单位:秒")
    private long onlineseconds;

    @Column(length = 64, comment = "[会话ID],可能重复")
    private String sessionid = "";

    @Column(length = 64, comment = "登录网络类型; wifi/4g/3g")
    private String netmode = "";

    @Column(length = 64, comment = "APP的设备系统(小写); android/ios/web/wap")
    private String appos = "";

    @Column(length = 255, comment = "APP设备唯一标识")
    private String apptoken = "";

    @Column(length = 255, comment = "[终端信息]")
    private String loginagent = "";

    @Column(length = 64, comment = "[登陆IP]")
    private String loginaddr = "";

    @Column(comment = "登录经度")
    protected double longitude;

    @Column(comment = "登录纬度")
    protected double latitude;

    @Column(length = 255, comment = "登录街道")
    protected String street = "";

    @Column(updatable = false, comment = "[创建时间]")
    private long createtime;

    @Column(comment = "[退出时间]")
    private long logouttime;

    public void setLoginid(String loginid) {
        this.loginid = loginid;
    }

    public String getLoginid() {
        return this.loginid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setOnlineseconds(long onlineseconds) {
        this.onlineseconds = onlineseconds;
    }

    public long getOnlineseconds() {
        return this.onlineseconds;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    public String getSessionid() {
        return this.sessionid;
    }

    public void setNetmode(String netmode) {
        this.netmode = netmode;
    }

    public String getNetmode() {
        return this.netmode;
    }

    public void setAppos(String appos) {
        this.appos = appos;
    }

    public String getAppos() {
        return this.appos;
    }

    public void setApptoken(String apptoken) {
        this.apptoken = apptoken;
    }

    public String getApptoken() {
        return this.apptoken;
    }

    public void setLoginagent(String loginagent) {
        this.loginagent = loginagent;
    }

    public String getLoginagent() {
        return this.loginagent;
    }

    public void setLoginaddr(String loginaddr) {
        this.loginaddr = loginaddr;
    }

    public String getLoginaddr() {
        return this.loginaddr;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public void setLogouttime(long logouttime) {
        this.logouttime = logouttime;
    }

    public long getLogouttime() {
        return this.logouttime;
    }

    public static class TableStrategy implements DistributeTableStrategy<UserLoginRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, UserLoginRecord bean) {
            return table + "_" + String.format(format, bean.getCreatetime());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getSingleTable(table, Long.parseLong(id.substring(0, id.lastIndexOf('-')), 36));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Object time = node.findValue("createtime");
            if (time == null) time = node.findValue("#createtime");
            if (time instanceof Long) return getSingleTable(table, (Long) time);
            Range.LongRange createtime = (Range.LongRange) time;
            return getSingleTable(table, createtime.getMin());
        }

        private String getSingleTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + String.format(format, createtime);
        }
    }
}
