package com.cratos.platf.liveness;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import java.io.Serializable;
import org.redkale.convert.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "活跃度记录表")
@DistributeTable(strategy = LivenessRewardDayRecord.TableStrategy.class)
public class LivenessRewardDayRecord extends BaseEntity {

    private static final Reproduce<LivenessRewardDayRecord, LivenessRewardInfo> reproduce = Reproduce.create(LivenessRewardDayRecord.class, LivenessRewardInfo.class);

    //任务状态  
    public static final short LIVENESS_STATUS_REACH = 10;  //可领取

    public static final short LIVENESS_STATUS_DOING = 20;  //未完成

    public static final short LIVENESS_STATUS_FINISH = 30;  //已领取

    @Id
    @Column(length = 128, comment = "任务记录ID,值=livenessrewardid+'-'+userid+'-'+intday")
    private String livenessrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "日期; 20190909")
    private int intday;

    @Column(comment = "签到奖励ID， 从101开始")
    private int livenessrewardid = 101;

    @Column(comment = "序号，从1开始")
    private int rewardindex;

    @Column(comment = "达标的活跃度值")
    private long reachliveness;

    @Column(comment = "任务状态; 10:可领取; 20:未完成; 30:已领取")
    private short livenessstatus;

    @Column(length = 4096, nullable = false, comment = "奖励的复合商品, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(length = 1024, comment = "备注")
    private String remark = "";

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    private long createtime;

    @Column(comment = "达标时间")
    private long reachtime;

    @Column(comment = "完成时间")
    private long finishtime;

    public LivenessRewardDayRecord() {
    }

    public LivenessRewardDayRecord(LivenessRewardInfo info, int userid, long createtime) {
        reproduce.apply(this, info);
        this.userid = userid;
        this.intday = Utility.yyyyMMdd(createtime);
        this.createtime = createtime;
        this.livenessstatus = LIVENESS_STATUS_DOING;
        this.livenessrecordid = this.livenessrewardid + "-" + this.userid + "-" + this.intday;
    }

    public void setLivenessrecordid(String livenessrecordid) {
        this.livenessrecordid = livenessrecordid;
    }

    public String getLivenessrecordid() {
        return this.livenessrecordid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
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

    public void setLivenessrewardid(int livenessrewardid) {
        this.livenessrewardid = livenessrewardid;
    }

    public int getLivenessrewardid() {
        return this.livenessrewardid;
    }

    public void setRewardindex(int rewardindex) {
        this.rewardindex = rewardindex;
    }

    public int getRewardindex() {
        return this.rewardindex;
    }

    public void setReachliveness(long reachliveness) {
        this.reachliveness = reachliveness;
    }

    public long getReachliveness() {
        return this.reachliveness;
    }

    public void setLivenessstatus(short livenessstatus) {
        this.livenessstatus = livenessstatus;
    }

    public short getLivenessstatus() {
        return this.livenessstatus;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public GoodsItem[] getGoodsitems() {
        return this.goodsitems;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRemark() {
        return this.remark;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }

    public void setReachtime(long reachtime) {
        this.reachtime = reachtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getReachtime() {
        return this.reachtime;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getFinishtime() {
        return this.finishtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<LivenessRewardDayRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, LivenessRewardDayRecord bean) {
            return table + "_" + bean.getIntday();
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + id.substring(id.lastIndexOf('-') + 1);
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
