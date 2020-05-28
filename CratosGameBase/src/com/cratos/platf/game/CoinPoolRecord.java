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
import org.redkale.util.LogLevel;

/**
 *
 * 每分钟记录
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(name = "coinpoolrecord")
@DistributeTable(strategy = CoinPoolRecord.TableStrategy.class)
public class CoinPoolRecord extends BaseEntity {

    @Id
    @Column(comment = "时间KEY; 格式:201909091200")
    protected long timekey;

    @Column(comment = "GAMEDATA_XXX_COINPOOL_1值")
    protected long coin1val1;

    @Column(comment = "GAMEDATA_XXX_COINPOOL_2值")
    protected long coin1val2;

    @Column(comment = "GAMEDATA_XXX_COINPOOL_3值")
    protected long coin1val3;

    @Column(comment = "GAMEDATA_XXX_COINPOOL_4值")
    protected long coin1val4;

    @Column(comment = "GAMEDATA_XXX_COINPOOL_5值")
    protected long coin1val5;

    @Column(comment = "GAMEDATA_XXX_COINPOOL2_1值")
    protected long coin2val1;

    @Column(comment = "GAMEDATA_XXX_COINPOOL2_2值")
    protected long coin2val2;

    @Column(comment = "GAMEDATA_XXX_COINPOOL2_3值")
    protected long coin2val3;

    @Column(comment = "GAMEDATA_XXX_COINPOOL2_4值")
    protected long coin2val4;

    @Column(comment = "GAMEDATA_XXX_COINPOOL2_5值")
    protected long coin2val5;

    @Column(comment = "GAMEDATA_XXX_COINPOOL3_1值")
    protected long coin3val1;

    @Column(comment = "GAMEDATA_XXX_COINPOOL3_2值")
    protected long coin3val2;

    @Column(comment = "GAMEDATA_XXX_COINPOOL3_3值")
    protected long coin3val3;

    @Column(comment = "GAMEDATA_XXX_COINPOOL3_4值")
    protected long coin3val4;

    @Column(comment = "GAMEDATA_XXX_COINPOOL3_5值")
    protected long coin3val5;

    @Column(comment = "创建时间")
    protected long createtime;

    public static long checkCoinVal(long val) {
        return val == Long.MIN_VALUE ? 0 : val;
    }

    public long getTimekey() {
        return timekey;
    }

    public void setTimekey(long timekey) {
        this.timekey = timekey;
    }

    public long getCoin1val1() {
        return coin1val1;
    }

    public void setCoin1val1(long coin1val1) {
        this.coin1val1 = coin1val1;
    }

    public long getCoin1val2() {
        return coin1val2;
    }

    public void setCoin1val2(long coin1val2) {
        this.coin1val2 = coin1val2;
    }

    public long getCoin1val3() {
        return coin1val3;
    }

    public void setCoin1val3(long coin1val3) {
        this.coin1val3 = coin1val3;
    }

    public long getCoin1val4() {
        return coin1val4;
    }

    public void setCoin1val4(long coin1val4) {
        this.coin1val4 = coin1val4;
    }

    public long getCoin2val1() {
        return coin2val1;
    }

    public void setCoin2val1(long coin2val1) {
        this.coin2val1 = coin2val1;
    }

    public long getCoin2val2() {
        return coin2val2;
    }

    public void setCoin2val2(long coin2val2) {
        this.coin2val2 = coin2val2;
    }

    public long getCoin2val3() {
        return coin2val3;
    }

    public void setCoin2val3(long coin2val3) {
        this.coin2val3 = coin2val3;
    }

    public long getCoin2val4() {
        return coin2val4;
    }

    public void setCoin2val4(long coin2val4) {
        this.coin2val4 = coin2val4;
    }

    public long getCoin3val1() {
        return coin3val1;
    }

    public void setCoin3val1(long coin3val1) {
        this.coin3val1 = coin3val1;
    }

    public long getCoin3val2() {
        return coin3val2;
    }

    public void setCoin3val2(long coin3val2) {
        this.coin3val2 = coin3val2;
    }

    public long getCoin3val3() {
        return coin3val3;
    }

    public void setCoin3val3(long coin3val3) {
        this.coin3val3 = coin3val3;
    }

    public long getCoin3val4() {
        return coin3val4;
    }

    public void setCoin3val4(long coin3val4) {
        this.coin3val4 = coin3val4;
    }

    public long getCoin1val5() {
        return coin1val5;
    }

    public void setCoin1val5(long coin1val5) {
        this.coin1val5 = coin1val5;
    }

    public long getCoin2val5() {
        return coin2val5;
    }

    public void setCoin2val5(long coin2val5) {
        this.coin2val5 = coin2val5;
    }

    public long getCoin3val5() {
        return coin3val5;
    }

    public void setCoin3val5(long coin3val5) {
        this.coin3val5 = coin3val5;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<CoinPoolRecord> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, CoinPoolRecord bean) {
            return getTable(table, (Serializable) bean.getTimekey());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            Long id = (Long) primary;
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + id / 100_00_00;
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
