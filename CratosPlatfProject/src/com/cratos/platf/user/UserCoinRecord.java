package com.cratos.platf.user;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.util.ShuffleRandom;
import java.io.Serializable;
import java.security.SecureRandom;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "用户金币记录表")
@DistributeTable(strategy = UserCoinRecord.TableStrategy.class)
public class UserCoinRecord extends BaseEntity {

    private static final SecureRandom numberGenerator = ShuffleRandom.createRandom();

    @Id
    @Column(length = 32, comment = "记录ID 值=user36id+'-'+create36time(9位)")
    private String coinrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "入场序列，1-4")
    private int roomlevel = 0;

    @Column(comment = "金币数")
    private long coins;

    @Column(comment = "成本金币数")
    private long costcoin;

    @Column(comment = "玩家更新后的金币数")
    private long newusercoins;

    @Column(length = 32, comment = "游戏模块，平台为:platf")
    private String game = "";

    @Column(length = 32, comment = "子模块")
    private String module = "";

    @Column(length = 1024, comment = "记录描述")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public UserCoinRecord() {
    }

    public UserCoinRecord(int userid, int roomlevel, long coin, long costcoin, long newusercoin, long createtime, String game, String module, String remark) {
        this.userid = userid;
        this.roomlevel = roomlevel;
        this.coins = coin;
        this.costcoin = costcoin;
        this.newusercoins = newusercoin;
        this.game = game == null ? "" : game;
        this.module = module == null ? "" : module;
        this.remark = remark == null ? "" : remark;
        this.createtime = createtime;
        byte[] bs = new byte[6];
        numberGenerator.nextBytes(bs);
        this.coinrecordid = Integer.toString(userid, 36) + "-" + Utility.binToHexString(bs) + "-" + Utility.format36time(this.createtime);
    }

    public void setCoinrecordid(String coinrecordid) {
        this.coinrecordid = coinrecordid;
    }

    public String getCoinrecordid() {
        return this.coinrecordid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public long getCoins() {
        return this.coins;
    }

    public long getCostcoin() {
        return costcoin;
    }

    public void setCostcoin(long costcoin) {
        this.costcoin = costcoin;
    }

    public long getNewusercoins() {
        return newusercoins;
    }

    public void setNewusercoins(long newusercoins) {
        this.newusercoins = newusercoins;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getGame() {
        return this.game;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getModule() {
        return this.module;
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

    public static class TableStrategy implements DistributeTableStrategy<UserCoinRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, UserCoinRecord bean) {
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
