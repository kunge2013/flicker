/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.hundred;

import java.io.Serializable;
import javax.persistence.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(name = "hundredtable", comment = "百人游戏房间信息表")
@DistributeTable(strategy = HundredGameTableHis.TableStrategy.class)
public class HundredGameTableHis extends HundredGameTable<HundredGameRound> {

    public static class TableStrategy implements DistributeTableStrategy<HundredGameTableHis> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, HundredGameTableHis bean) {
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
