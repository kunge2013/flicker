package com.cratos.platf.crowd;

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
@Table(comment = "全民打卡玩家报名记录表")
@DistributeTable(strategy = CrowdApplyDayRecord.TableStrategy.class)
public class CrowdApplyDayRecord extends BaseEntity {

    public static final short CROWD_APPLY_STATUS_UNDO = 10; //未打卡

    public static final short CROWD_APPLY_STATUS_DONE = 20; //已打卡

    @Id
    @Column(length = 64, comment = "记录ID 值=intday+'-'+userid+'-'+index")
    private String crowdapplyrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "日期; 20190909")
    private int intday;

    @Column(comment = "报名序号，从1开始")
    private int applyindex;

    @Column(comment = "打卡状态; 10:未打卡;20:已打卡")
    private short status;

    @Column(comment = "报名金币数")
    private long applycoins;

    @Column(comment = "瓜分的金币数")
    private long wincoins;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    @Column(comment = "创建时间")
    private long finishtime;

    public void setCrowdapplyrecordid(String crowdapplyrecordid) {
        this.crowdapplyrecordid = crowdapplyrecordid;
    }

    public String getCrowdapplyrecordid() {
        return this.crowdapplyrecordid;
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

    public int getIntday() {
        return this.intday;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public void setApplyindex(int applyindex) {
        this.applyindex = applyindex;
    }

    public int getApplyindex() {
        return this.applyindex;
    }

    public void setApplycoins(long applycoins) {
        this.applycoins = applycoins;
    }

    public long getApplycoins() {
        return this.applycoins;
    }

    public void setWincoins(long wincoins) {
        this.wincoins = wincoins;
    }

    public long getWincoins() {
        return this.wincoins;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public long getFinishtime() {
        return this.finishtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<CrowdApplyDayRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, CrowdApplyDayRecord bean) {
            return table + "_" + bean.getIntday();
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + id.substring(0, id.indexOf('-'));
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
