package com.cratos.platf.kefu;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.util.LogLevel;

/**
 *
 * @author Dell
 */
@LogLevel("FINER")
@Cacheable(interval = 60)
@Table(comment = "客服自动回复配置表")
public class KefuAutoRecovery extends BaseEntity {
    
    public static final short TYPE_HELLO = 30; //HELLO消息自动回复

    public static final short TYPE_PAYURL = 40; //支付链接消息

    public static final short TYPE_PAYSUCCESS = 50; //支付成功消息
    
    @Id
    @Column(comment = "type + 两位序号")
    private int recoveryid;

    @Column(comment = "[类型]: 30:HELLO消息自动回复;40:支付链接消息;50:支付成功消息;")
    private short type = 30;

    @Column(length = 255, comment = "自动回复内容")
    private String value = "";

    @Column(comment = "排序，值小靠前")
    private int display = 1;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setRecoveryid(int recoveryid) {
        this.recoveryid = recoveryid;
    }

    public int getRecoveryid() {
        return this.recoveryid;
    }

    public void setType(short type) {
        this.type = type;
    }

    public short getType() {
        return this.type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    public int getDisplay() {
        return this.display;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
