package com.cratos.platf.info;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.util.Weightable;
import org.redkale.convert.*;
import org.redkale.util.Comment;

/**
 * 积分摇奖
 *
 * @author zhangjx
 */
@Table(name = "awardinfo", comment = "幸运摇奖选项表")
public class ScoreAwardInfo extends BaseEntity implements Weightable {

    @Comment("白银盘")
    public static final short AWARD_LEVEL_1 = 1;

    @Comment("黄金盘")
    public static final short AWARD_LEVEL_2 = 2;

    @Comment("钻石盘")
    public static final short AWARD_LEVEL_3 = 3;

    @Comment("不中奖选项类型, 小写")
    public static final String AWARD_TYPE_NONE = "none";

    @Comment("金币选项类型, 小写")
    public static final String AWARD_TYPE_COIN = "coin";

    @Comment("钻石选项类型, 小写")
    public static final String AWARD_TYPE_DIAMOND = "diamond";

    @Id
    @Column(comment = "摇奖中奖项ID, 三位数;  awardlevel+两位序号; 序号从01开始")
    private int awardid = 11;

    @Column(comment = "中奖项等级; 1:白银盘;2:黄金盘;3:钻石盘;")
    private short awardlevel;

    @Column(length = 32, comment = "中奖项类型(字母小写); none:不中奖; coin:金币; diamond:钻石;")
    private String awardtype = "";

    @Column(comment = "中奖项的类型数量值")
    private long awardval;

    @Column(comment = "权重")
    private int weight = 0;

    @Column(comment = "排序顺序，值小靠前")
    private int display = 1000;

    @Column(comment = "操作人ID")
    private int memberid;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setAwardid(int awardid) {
        this.awardid = awardid;
    }

    public int getAwardid() {
        return this.awardid;
    }

    public void setAwardlevel(short awardlevel) {
        this.awardlevel = awardlevel;
    }

    public short getAwardlevel() {
        return this.awardlevel;
    }

    public void setAwardtype(String awardtype) {
        this.awardtype = awardtype;
    }

    public String getAwardtype() {
        return this.awardtype;
    }

    public void setAwardval(long awardval) {
        this.awardval = awardval;
    }

    public long getAwardval() {
        return this.awardval;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getDisplay() {
        return this.display;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMemberid() {
        return this.memberid;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }
}
