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
 * 每分钟在线玩家数记录
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(name = "playinguserrecord")
@DistributeTable(strategy = PlayingUserRecord.TableStrategy.class)
public class PlayingUserRecord extends BaseEntity {

    @Id
    @Column(comment = "时间KEY; 格式:201909091200")
    protected long timekey;

    @Column(comment = "当前游戏总在线人数")
    protected long playingcounts;

    @Column(comment = "场次1的在线人数")
    protected long playingcount1;

    @Column(comment = "场次2的在线人数")
    protected long playingcount2;

    @Column(comment = "场次3的在线人数")
    protected long playingcount3;

    @Column(comment = "场次4的在线人数")
    protected long playingcount4;

    @Column(comment = "场次4的在线人数")
    protected long playingcount5;

    @Column(comment = "创建时间")
    protected long createtime;

    public long getTimekey() {
        return timekey;
    }

    public void setTimekey(long timekey) {
        this.timekey = timekey;
    }

    public long getPlayingcounts() {
        return playingcounts;
    }

    public void setPlayingcounts(long playingcounts) {
        this.playingcounts = playingcounts;
    }

    public long getPlayingcount1() {
        return playingcount1;
    }

    public void setPlayingcount1(long playingcount1) {
        this.playingcount1 = playingcount1;
    }

    public long getPlayingcount2() {
        return playingcount2;
    }

    public void setPlayingcount2(long playingcount2) {
        this.playingcount2 = playingcount2;
    }

    public long getPlayingcount3() {
        return playingcount3;
    }

    public void setPlayingcount3(long playingcount3) {
        this.playingcount3 = playingcount3;
    }

    public long getPlayingcount4() {
        return playingcount4;
    }

    public void setPlayingcount4(long playingcount4) {
        this.playingcount4 = playingcount4;
    }

    public long getPlayingcount5() {
        return playingcount5;
    }

    public void setPlayingcount5(long playingcount5) {
        this.playingcount5 = playingcount5;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<PlayingUserRecord> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, PlayingUserRecord bean) {
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
