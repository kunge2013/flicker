/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

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
@DistributeTable(strategy = PoolDataRecord.TableStrategy.class)
public class PoolDataRecord extends BaseEntity {

    @Id
    @Column(length = 128, comment = "记录ID 值=user36id+'-'+roundid+'-'+module+'-'+create36time(9位)")
    private String poolrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "入场序列，1-4")
    private int roomlevel = 0;

    @Column(comment = "第几个奖池")
    private int poolindex = 0;

    @Column(comment = "回合ID")
    private String roundid = "";

    @Column(comment = "金币数")
    private long coins;

    @Column(comment = "手续费")
    private long taxcoin;

    @Column(comment = "奖池金币数")
    private long poolcoin;

    @Column(comment = "奖池更新后的金币数")
    private long newpoolcoins;

    @Column(length = 32, comment = "子模块")
    private String module = "";

    @Column(length = 127, comment = "记录描述")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public PoolDataRecord() {
    }

    public PoolDataRecord(String gameid, int userid, int roomlevel, int poolindex, String roundid, long coin, long taxcoin, long poolcoin, long newpoolcoins, long createtime, String module, String remark) {
        this.userid = userid;
        this.roomlevel = roomlevel;
        this.poolindex = poolindex;
        this.roundid = roundid;
        this.coins = coin;
        this.taxcoin = taxcoin;
        this.poolcoin = poolcoin;
        this.newpoolcoins = newpoolcoins;
        this.module = module == null || module.isEmpty() ? "" : module;
        this.remark = remark == null ? "" : remark;
        this.createtime = createtime;
        this.poolrecordid = gameid + "-" + Integer.toString(userid, 36) + "-" + poolindex + "-" + roundid + "-" + module + "-" + Utility.format36time(this.createtime);
    }

    public void refreshPoolrecordid() {
        this.poolrecordid = this.poolrecordid.substring(0, this.poolrecordid.lastIndexOf('-')) + "-" + Utility.format36time(this.createtime);
    }

    public String getPoolrecordid() {
        return poolrecordid;
    }

    public void setPoolrecordid(String poolrecordid) {
        this.poolrecordid = poolrecordid;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    public int getPoolindex() {
        return poolindex;
    }

    public void setPoolindex(int poolindex) {
        this.poolindex = poolindex;
    }

    public String getRoundid() {
        return roundid;
    }

    public void setRoundid(String roundid) {
        this.roundid = roundid;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public long getTaxcoin() {
        return taxcoin;
    }

    public void setTaxcoin(long taxcoin) {
        this.taxcoin = taxcoin;
    }

    public long getPoolcoin() {
        return poolcoin;
    }

    public void setPoolcoin(long poolcoin) {
        this.poolcoin = poolcoin;
    }

    public long getNewpoolcoins() {
        return newpoolcoins;
    }

    public void setNewpoolcoins(long newpoolcoins) {
        this.newpoolcoins = newpoolcoins;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<PoolDataRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, PoolDataRecord bean) {
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
