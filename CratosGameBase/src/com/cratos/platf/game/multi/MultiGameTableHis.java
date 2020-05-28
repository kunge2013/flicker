/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.base.BaseEntity;
import java.io.Serializable;
import javax.persistence.*;
import org.redkale.source.*;

/**
 * 暂时没用到
 *
 * @author zhangjx
 */
@Table(name = "multitablehis")
@DistributeTable(strategy = MultiGameTableHis.TableStrategy.class)
public class MultiGameTableHis extends BaseEntity {

    @Id
    @Column(comment = "房间ID; 值=finish36time+'-'+create36time+'-'+user36id+'-'+nodeid")
    protected String tableid;

    @Column(comment = "入场序列，1-4")
    protected int roomlevel = 0;

    @Column(comment = "本局系统输赢的总和金币数(排除电脑)")
    protected long oswincoins = 0;

    @Column(comment = "手续费")
    protected long taxcoin;

    @Column(length = 4096, comment = "玩家信息")
    protected String playersjson = "";

    @Column(comment = "创建时间")
    protected long createtime;

    @Column(comment = "一局开始时间")
    protected long starttime;

    @Column(comment = "关闭时间")
    protected long finishtime;

    public String getTableid() {
        return tableid;
    }

    public void setTableid(String tableid) {
        this.tableid = tableid;
    }

    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    public long getOswincoins() {
        return oswincoins;
    }

    public void setOswincoins(long oswincoins) {
        this.oswincoins = oswincoins;
    }

    public long getTaxcoin() {
        return taxcoin;
    }

    public void setTaxcoin(long taxcoin) {
        this.taxcoin = taxcoin;
    }

    public String getPlayersjson() {
        return playersjson;
    }

    public void setPlayersjson(String playersjson) {
        this.playersjson = playersjson;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getFinishtime() {
        return finishtime;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<MultiGameTableHis> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, MultiGameTableHis bean) {
            return getTable(table, (Serializable) bean.getTableid());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getSingleTable(table, Long.parseLong(id.substring(0, id.indexOf('-')), 36));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Object time = node.findValue("finishtime");
            if (time == null) time = node.findValue("#finishtime");
            if (time == null) time = node.findValue("createtime");
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
