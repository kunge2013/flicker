package com.cratos.platf.duty;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import java.io.Serializable;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "每日签到记录表")
@DistributeTable(strategy = DutyRecord.TableStrategy.class)
public class DutyRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "记录ID= userid+'-'+intday")
    private String dutyrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "日期")
    private int intday;

    @Column(comment = "签到序号，从1开始")
    private int dutyindex;

    @Column(comment = "签到ID")
    private int dutyrewardid;

    @Column(length = 4096, comment = "签到领取的复合商品, GoodsItem[]数组")
    private GoodsItem[] dutyitems;

    @Column(comment = "签到类型: 10:签到;20:补签")
    private short dtype;

    @Column(comment = "7/14天领奖标记")
    private short dflag;

    @Column(comment = "补签的金币成本， dtype=20时才有值")
    private long costcoin;

    @Column(length = 1024, comment = "备注")
    private String remark = "";

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    private long createtime;

    public void setDutyrecordid(String dutyrecordid) {
        this.dutyrecordid = dutyrecordid;
    }

    public String getDutyrecordid() {
        return this.dutyrecordid;
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

    public int getDutyindex() {
        return dutyindex;
    }

    public void setDutyindex(int dutyindex) {
        this.dutyindex = dutyindex;
    }

    public void setDutyrewardid(int dutyrewardid) {
        this.dutyrewardid = dutyrewardid;
    }

    public int getDutyrewardid() {
        return this.dutyrewardid;
    }

    public void setDutyitems(GoodsItem[] dutyitems) {
        this.dutyitems = dutyitems;
    }

    public GoodsItem[] getDutyitems() {
        return this.dutyitems;
    }

    public void setDtype(short dtype) {
        this.dtype = dtype;
    }

    public short getDtype() {
        return this.dtype;
    }

    public void setDflag(short dflag) {
        this.dflag = dflag;
    }

    public short getDflag() {
        return this.dflag;
    }

    public void setCostcoin(long costcoin) {
        this.costcoin = costcoin;
    }

    public long getCostcoin() {
        return this.costcoin;
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

    public static class TableStrategy implements DistributeTableStrategy<DutyRecord> {

        @Override
        public String getTable(String table, DutyRecord bean) {
            return getTable(table, (Serializable) bean.getDutyrecordid());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            int pos = table.indexOf('.');
            int d = id.indexOf('-') + 1;
            return table.substring(pos + 1) + "_" + id.substring(d, d + 6);
        }

        @Override
        public String getTable(String table, FilterNode node) {
            return getSingleTable(table, (Integer) node.findValue("intday"));
        }

        private String getSingleTable(String table, int intday) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + intday / 100;
        }
    }
}
