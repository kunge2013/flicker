package com.cratos.platf.info;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import java.io.Serializable;
import org.redkale.convert.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "摇奖记录表")
@DistributeTable(strategy = AwardRecord.TableStrategy.class)
public abstract class AwardRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "记录ID= userid+'-'+intday+'-'+awardindex")
    protected String awardrecordid = "";

    @Column(comment = "用户ID")
    protected int userid;

    @Column(comment = "日期")
    protected int intday;

    @Column(comment = "摇奖项ID")
    protected int awardid;

    @Column(comment = "摇奖序号，从1开始")
    protected int awardindex;

    @Column(comment = "中奖项等级; 1开始")
    protected short awardlevel;

    @Column(comment = "[商品类型]")
    protected short goodstype;

    @Column(comment = "商品值数量")
    protected int goodscount;

    @Column(comment = "实物ID， 根据goodstype来指定不同id")
    protected int goodsobjid;

    @Column(length = 64, comment = "所属游戏ID")
    protected String gameid = "";

    @Column(comment = "商品过期秒数，为0表示不过期")
    protected long goodsexpires;

    @Column(length = 1024, comment = "备注")
    protected String remark = "";

    @Column(updatable = false, comment = "创建时间")
    protected long createtime;

    public void setAwardrecordid(String awardrecordid) {
        this.awardrecordid = awardrecordid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getAwardrecordid() {
        return this.awardrecordid;
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

    public int getAwardid() {
        return awardid;
    }

    public void setAwardid(int awardid) {
        this.awardid = awardid;
    }

    public void setAwardindex(int awardindex) {
        this.awardindex = awardindex;
    }

    public int getAwardindex() {
        return this.awardindex;
    }

    public void setAwardlevel(short awardlevel) {
        this.awardlevel = awardlevel;
    }

    public short getAwardlevel() {
        return this.awardlevel;
    }

    public void setGoodstype(short goodstype) {
        this.goodstype = goodstype;
    }

    public short getGoodstype() {
        return this.goodstype;
    }

    public void setGoodscount(int goodscount) {
        this.goodscount = goodscount;
    }

    public int getGoodscount() {
        return this.goodscount;
    }

    public void setGoodsobjid(int goodsobjid) {
        this.goodsobjid = goodsobjid;
    }

    public int getGoodsobjid() {
        return this.goodsobjid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public String getGameid() {
        return this.gameid;
    }

    public void setGoodsexpires(long goodsexpires) {
        this.goodsexpires = goodsexpires;
    }

    public long getGoodsexpires() {
        return this.goodsexpires;
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

    public static class TableStrategy implements DistributeTableStrategy<AwardRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, AwardRecord bean) {
            return getSingleTable(table, bean.getIntday());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            int pos1 = id.indexOf('-');
            int pos2 = id.lastIndexOf('-');
            return getSingleTable(table, Integer.parseInt(pos1 != pos2 ? id.substring(pos1 + 1, pos2) : id.substring(pos1 + 1)));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            return getSingleTable(table, (Integer) node.findValue("intday"));
        }

        private String getSingleTable(String table, int initday) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + initday;
        }
    }
}
