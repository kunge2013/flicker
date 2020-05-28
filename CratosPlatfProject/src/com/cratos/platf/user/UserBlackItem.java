package com.cratos.platf.user;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "限制设置表")
public class UserBlackItem extends BaseEntity {

    private static final Reproduce<UserBlackItemHis, UserBlackItem> reproduce = Reproduce.create(UserBlackItemHis.class, UserBlackItem.class);

    public static final short BLACKTYPE_IP = 10;

    public static final short BLACKTYPE_APPTOKEN = 20;

    @Id
    @Column(length = 64, comment = "限制ID 值=create36time(9位)+UUID(32位)")
    protected String blackitemid = "";

    @Column(comment = "类型; 10:ip；20:apptoken；")
    protected short blacktype = 10;

    @Column(length = 1024, comment = "值")
    protected String blackvalue = "";

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    protected long createtime;

    @Column(comment = "操作人ID")
    protected int memberid;

    public UserBlackItemHis createUserBlackItemHis(int memberid) {
        UserBlackItemHis his = reproduce.apply(new UserBlackItemHis(), this);
        his.setMovetime(System.currentTimeMillis());
        his.memberid = memberid;
        return his;
    }

    public void setBlackitemid(String blackitemid) {
        this.blackitemid = blackitemid;
    }

    public String getBlackitemid() {
        return this.blackitemid;
    }

    public void setBlacktype(short blacktype) {
        this.blacktype = blacktype;
    }

    public short getBlacktype() {
        return this.blacktype;
    }

    public void setBlackvalue(String blackvalue) {
        this.blackvalue = blackvalue;
    }

    public String getBlackvalue() {
        return this.blackvalue;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public int getMemberid() {
        return memberid;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

}
