package com.cratos.game.skywar;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import java.io.Serializable;
import org.redkale.source.*;
import org.redkale.util.LogLevel;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "用户比赛记录表")
@DistributeTable(strategy = SkywarSportUserRecord.TableStrategy.class)
public class SkywarSportUserRecord extends BaseEntity {

    @Id
    @Column(length = 32, comment = "记录ID 值=intday+'-'+userid+'-'+index")
    private String sportuserrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "比赛日期; 20190909")
    private int intday;

    @Column(comment = "入场序列，100+")
    private int roomlevel;

    @Column(comment = "报名序号，从1开始")
    private int applyindex;

    @Column(comment = "入场成本")
    private int applycost;

    @Column(comment = "结果排名，从1开始")
    private int resultindex;

    @Column(comment = "结果积分")
    private int resultscore;

    @Column(length = 4096, comment = "领取的复合商品, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(length = 1024, comment = "记录描述")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setSportuserrecordid(String sportuserrecordid) {
        this.sportuserrecordid = sportuserrecordid;
    }

    public String getSportuserrecordid() {
        return this.sportuserrecordid;
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

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    public int getRoomlevel() {
        return this.roomlevel;
    }

    public void setApplyindex(int applyindex) {
        this.applyindex = applyindex;
    }

    public int getApplyindex() {
        return this.applyindex;
    }

    public void setApplycost(int applycost) {
        this.applycost = applycost;
    }

    public int getApplycost() {
        return this.applycost;
    }

    public void setResultindex(int resultindex) {
        this.resultindex = resultindex;
    }

    public int getResultindex() {
        return this.resultindex;
    }

    public void setResultscore(int resultscore) {
        this.resultscore = resultscore;
    }

    public int getResultscore() {
        return this.resultscore;
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

    public String getRemark() {
        return this.remark;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<SkywarSportUserRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, SkywarSportUserRecord bean) {
            return table + "_" + bean.getIntday();
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            String intday = id.substring(0, id.indexOf('-'));
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + intday;
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
