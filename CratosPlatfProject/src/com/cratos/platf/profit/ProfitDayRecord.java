package com.cratos.platf.profit;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import java.io.Serializable;
import org.redkale.source.*;
import org.redkale.util.LogLevel;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "用户每天收益记录表")
@DistributeTable(strategy = ProfitDayRecord.TableStrategy.class)
public class ProfitDayRecord extends BaseEntity {

    @Id
    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "日期; 20190909")
    private int intday;

    @Column(comment = "自己总成本金币数")
    private long costcoins;

    @Column(comment = "直属下级玩家数")
    private long childcount;

    @Column(comment = "直属下级总成本金币数")
    private long childcostcoins;

    @Column(comment = "直属下级总返利金币数")
    private long childprofitcoins;

    @Column(comment = "直属下级总返利元，单位:分")
    private long childprofitmoney;

    @Column(comment = "其他子级玩家数")
    private long subchildcount;

    @Column(comment = "其他子级总成本金币数")
    private long subchildcostcoins;

    @Column(comment = "其他子级总返利金币数")
    private long subchildprofitcoins;

    @Column(comment = "其他子级总返利元，单位:分")
    private long subchildprofitmoney;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setIntday(int intday) {
        this.intday = intday;
    }

    public int getIntday() {
        return this.intday;
    }

    public long getCostcoins() {
        return costcoins;
    }

    public void setCostcoins(long costcoins) {
        this.costcoins = costcoins;
    }

    public void setChildcount(long childcount) {
        this.childcount = childcount;
    }

    public long getChildcount() {
        return this.childcount;
    }

    public void setChildcostcoins(long childcostcoins) {
        this.childcostcoins = childcostcoins;
    }

    public long getChildcostcoins() {
        return this.childcostcoins;
    }

    public void setSubchildcount(long subchildcount) {
        this.subchildcount = subchildcount;
    }

    public long getSubchildcount() {
        return this.subchildcount;
    }

    public void setSubchildcostcoins(long subchildcostcoins) {
        this.subchildcostcoins = subchildcostcoins;
    }

    public long getSubchildcostcoins() {
        return this.subchildcostcoins;
    }

    public long getChildprofitcoins() {
        return childprofitcoins;
    }

    public void setChildprofitcoins(long childprofitcoins) {
        this.childprofitcoins = childprofitcoins;
    }

    public long getSubchildprofitcoins() {
        return subchildprofitcoins;
    }

    public void setSubchildprofitcoins(long subchildprofitcoins) {
        this.subchildprofitcoins = subchildprofitcoins;
    }

    public long getChildprofitmoney() {
        return childprofitmoney;
    }

    public void setChildprofitmoney(long childprofitmoney) {
        this.childprofitmoney = childprofitmoney;
    }

    public long getSubchildprofitmoney() {
        return subchildprofitmoney;
    }

    public void setSubchildprofitmoney(long subchildprofitmoney) {
        this.subchildprofitmoney = subchildprofitmoney;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<ProfitDayRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, ProfitDayRecord bean) {
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
