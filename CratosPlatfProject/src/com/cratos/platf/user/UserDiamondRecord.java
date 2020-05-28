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
@Table(comment = "用户钻记录表")
@DistributeTable(strategy = UserDiamondRecord.TableStrategy.class)
public class UserDiamondRecord extends BaseEntity {

    private static final SecureRandom numberGenerator = ShuffleRandom.createRandom();

    @Id
    @Column(length = 32, comment = "记录ID 值=user36id+'-'+create36time(9位)")
    private String diamondrecordid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "钻数")
    private long diamonds;

    @Column(comment = "玩家更新后的钻数")
    private long newuserdiamonds;
    
    @Column(length = 32, comment = "游戏模块，平台为:platf")
    private String game = "";

    @Column(length = 32, comment = "子模块")
    private String module = "";

    @Column(length = 1024, comment = "记录描述")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public UserDiamondRecord() {
    }

    public UserDiamondRecord(int userid, long diamond, long newuserdiamond, long createtime, String game, String module, String remark) {
        this.userid = userid;
        this.diamonds = diamond;
        this.newuserdiamonds = newuserdiamond;
        this.game = game == null ? "" : game;
        this.module = module == null ? "" : module;
        this.remark = remark == null ? "" : remark;
        this.createtime = createtime;
        byte[] bs = new byte[6];
        numberGenerator.nextBytes(bs);
        this.diamondrecordid = Integer.toString(userid, 36) + "-" + Utility.binToHexString(bs) + "-" + Utility.format36time(this.createtime);
    }

    public void setDiamondrecordid(String diamondrecordid) {
        this.diamondrecordid = diamondrecordid;
    }

    public String getDiamondrecordid() {
        return this.diamondrecordid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setDiamonds(long diamonds) {
        this.diamonds = diamonds;
    }

    public long getDiamonds() {
        return this.diamonds;
    }

    public long getNewuserdiamonds() {
        return newuserdiamonds;
    }

    public void setNewuserdiamonds(long newuserdiamonds) {
        this.newuserdiamonds = newuserdiamonds;
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

    public static class TableStrategy implements DistributeTableStrategy<UserDiamondRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, UserDiamondRecord bean) {
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
