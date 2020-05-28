/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.BaseEntity;
import java.io.Serializable;
import javax.persistence.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "用户金币记录表")
@DistributeTable(strategy = UserDayRecord.TableStrategy.class)
public class UserDayRecord extends BaseEntity {

    @Id
    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "日期; 20190909")
    private int intday;

    @Column(comment = "总赢金币数")
    private long wincoins;

    @Column(comment = "总成本金币数")
    private long costcoins;

    @Column(comment = "总活跃度")
    private long liveness;

    @Column(comment = "别人给自己转的金币总数")
    private long bankincoins;

    @Column(comment = "自己转给别人的金币总数")
    private long bankoutcoins;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public UserDayRecord() {
    }

    public UserDayRecord(int userid, long wincoins, long costcoins, long liveness) {
        this.userid = userid;
        this.intday = Utility.today();
        this.wincoins = wincoins;
        this.costcoins = costcoins;
        this.liveness = liveness;
        this.createtime = System.currentTimeMillis();
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public int getIntday() {
        return intday;
    }

    public void setIntday(int intday) {
        this.intday = intday;
    }

    public long getLiveness() {
        return liveness;
    }

    public void setLiveness(long liveness) {
        this.liveness = liveness;
    }

    public long getWincoins() {
        return wincoins;
    }

    public void setWincoins(long wincoins) {
        this.wincoins = wincoins;
    }

    public long getCostcoins() {
        return costcoins;
    }

    public void setCostcoins(long costcoins) {
        this.costcoins = costcoins;
    }

    public long getBankincoins() {
        return bankincoins;
    }

    public void setBankincoins(long bankincoins) {
        this.bankincoins = bankincoins;
    }

    public long getBankoutcoins() {
        return bankoutcoins;
    }

    public void setBankoutcoins(long bankoutcoins) {
        this.bankoutcoins = bankoutcoins;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<UserDayRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, UserDayRecord bean) {
            return table + "_" + bean.getIntday();
        }

        @Override
        public String getTable(String table, Serializable primary) {
            return getSingleTable(table, System.currentTimeMillis());
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Object intday = node.findValue("intday");
            if (intday != null && intday instanceof Integer) {
                int pos = table.indexOf('.');
                return table.substring(pos + 1) + "_" + intday;
            }
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
