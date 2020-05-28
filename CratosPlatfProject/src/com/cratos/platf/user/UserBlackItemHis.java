package com.cratos.platf.user;

import javax.persistence.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "限制设置历史表")
public class UserBlackItemHis extends UserBlackItem {

    @Id
    @Column(length = 64, comment = "限制ID 值=create36time(9位)+UUID(32位)")
    private String blackitemid = "";

    @Column(comment = "迁移时间")
    private long movetime;

    @Override
    public void setBlackitemid(String blackitemid) {
        this.blackitemid = blackitemid;
    }

    @Override
    public String getBlackitemid() {
        return this.blackitemid;
    }

    public void setMovetime(long movetime) {
        this.movetime = movetime;
    }

    public long getMovetime() {
        return this.movetime;
    }
}
