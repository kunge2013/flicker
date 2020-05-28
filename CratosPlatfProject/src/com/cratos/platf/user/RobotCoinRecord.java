package com.cratos.platf.user;

import com.cratos.platf.base.BaseEntity;
import java.io.Serializable;
import javax.persistence.*;
import org.redkale.convert.json.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "机器人充值流水记录表")
@DistributeTable(strategy = RobotCoinRecord.TableStrategy.class)
public class RobotCoinRecord extends BaseEntity {

    @Id
    @Column(length = 32, comment = "主键 user36id(6位)+'-'+create36time(9位)")
    private String robotcoinid = "";

    @Column(comment = "机器人用户ID")
    private int userid;

    @Column(comment = "金币数")
    private long coins;

    @Column(comment = "操作人ID")
    private int memberid;

    @Column(length = 255, comment = "备注描述")
    private String remark = "";

    @Column(updatable = false, comment = "交易时间")
    private long createtime;

    public void setRobotcoinid(String robotcoinid) {
        this.robotcoinid = robotcoinid;
    }

    public String getRobotcoinid() {
        return this.robotcoinid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public long getCoins() {
        return this.coins;
    }

    public int getMemberid() {
        return memberid;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }
    
    public static class TableStrategy implements DistributeTableStrategy<RobotCoinRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, RobotCoinRecord bean) {
            return table + "_" + String.format(format, bean.getCreatetime());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getSingleTable(table, Long.parseLong(id.substring(id.lastIndexOf('-') + 1), 36));
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
