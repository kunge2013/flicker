package com.cratos.platf.kefu;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
@Table(comment = "玩家与客服IM消息表")
public class KefuChatMessage extends BaseEntity {

    public static final short MSGTYPE_TEXT = 10; //文本消息

    public static final short MSGTYPE_IMAGE = 20; //图片消息

    public static final short MSGTYPE_HELLO = 30; //HELLO消息

    public static final short MSGTYPE_PAYTYPE = 40; //支付方式消息

    public static final short MSGTYPE_PAYURL = 50; //支付链接消息

    public static final short MSGTYPE_PAYOK = 60; //支付成功消息

    public static final short MSGSTATUS_UNREAD = 10; //未读

    public static final short MSGSTATUS_READED = 20; //已读

    @Id
    @Column(length = 64, comment = "消息ID; 值=srcuser36id+'-'+destuser36id+'-'+create36time")
    private String kefuchatid = "";

    @Column(comment = "发送消息用户ID ")
    private int srcuserid;

    @Column(comment = "接收消息用户ID ")
    private int destuserid;

    @Column(comment = "[状态]: 10:文本消息;20:图片消息;30:HELLO消息;40:支付链接消息;50:支付成功消息;")
    private short msgtype;

    @Column(comment = "[状态]: 10:未读;20:已读;")
    private short msgstatus;

    @Column(length = 2048, comment = "消息内容")
    private String msgcontent = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public String creatKefuchatid() {
        this.kefuchatid = Integer.toString(this.getSrcuserid(), 36) + "-" + Integer.toString(this.getDestuserid(), 36) + "-" + Utility.format36time(this.getCreatetime());
        return this.kefuchatid;
    }

    public void setKefuchatid(String kefuchatid) {
        this.kefuchatid = kefuchatid;
    }

    public String getKefuchatid() {
        return this.kefuchatid;
    }

    public void setSrcuserid(int srcuserid) {
        this.srcuserid = srcuserid;
    }

    public int getSrcuserid() {
        return this.srcuserid;
    }

    public void setDestuserid(int destuserid) {
        this.destuserid = destuserid;
    }

    public int getDestuserid() {
        return this.destuserid;
    }

    public void setMsgtype(short msgtype) {
        this.msgtype = msgtype;
    }

    public short getMsgtype() {
        return this.msgtype;
    }

    public void setMsgstatus(short msgstatus) {
        this.msgstatus = msgstatus;
    }

    public short getMsgstatus() {
        return this.msgstatus;
    }

    public void setMsgcontent(String msgcontent) {
        this.msgcontent = msgcontent;
    }

    public String getMsgcontent() {
        return this.msgcontent;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
