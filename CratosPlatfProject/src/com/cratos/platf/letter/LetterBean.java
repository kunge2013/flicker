/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.letter;

import com.cratos.platf.base.BaseBean;
import org.redkale.source.*;
import org.redkale.source.Range.IntRange;
import org.redkale.source.Range.LongRange;

/**
 *
 * @author zhangjx
 */
public class LetterBean extends BaseBean implements FilterBean {

    @FilterColumn(comment = "邮件发送组ID")
    private String letterdraftid = "";

    @FilterColumn(comment = "邮件ID")
    private String letterid = "";

    @FilterColumn(name = "letterid", comment = "多个邮件ID")
    private String[] letterids;

    @FilterColumn(comment = "邮件接收方用户ID")
    private int userid;

    @FilterColumn(comment = "邮件发送方用户ID, 为0表示系统发送")
    private int fromuserid;

    @FilterColumn(comment = "邮件类型: 10:金钻赠送或奖励;")
    private short lettertype;

    @FilterColumn(comment = "赠送金币数")
    private IntRange coins;

    @FilterColumn(comment = "赠送钻石数")
    private IntRange diamonds;

    @FilterColumn(comment = "状态; 10:未读; 20:已读; 30:过期;")
    private short[] status = new short[]{LetterRecord.LETTER_STATUS_UNREAD};

    @FilterColumn(express = FilterExpress.LIKE, comment = "邮件标题")
    @FilterGroup("[OR]content")
    private String title = "";

    @FilterColumn(express = FilterExpress.LIKE, comment = "邮件内容")
    @FilterGroup("[OR]content")
    private String content = "";

    @FilterColumn(comment = "预设时间，单位毫秒, 为0表示及时生效")
    private LongRange starttime;

    @FilterColumn(comment = "创建时间，单位毫秒")
    private LongRange createtime;

    @FilterColumn(comment = "移动时间，单位毫秒")
    private LongRange movetime;

    public String getLetterdraftid() {
        return letterdraftid;
    }

    public void setLetterdraftid(String letterdraftid) {
        this.letterdraftid = letterdraftid;
    }

    public String getLetterid() {
        return letterid;
    }

    public void setLetterid(String letterid) {
        this.letterid = letterid;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getFromuserid() {
        return fromuserid;
    }

    public void setFromuserid(int fromuserid) {
        this.fromuserid = fromuserid;
    }

    public short getLettertype() {
        return lettertype;
    }

    public void setLettertype(short lettertype) {
        this.lettertype = lettertype;
    }

    public IntRange getCoins() {
        return coins;
    }

    public void setCoins(IntRange coins) {
        this.coins = coins;
    }

    public IntRange getDiamonds() {
        return diamonds;
    }

    public void setDiamonds(IntRange diamonds) {
        this.diamonds = diamonds;
    }

    public short[] getStatus() {
        return status;
    }

    public void setStatus(short[] status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LongRange getStarttime() {
        return starttime;
    }

    public void setStarttime(LongRange starttime) {
        this.starttime = starttime;
    }

    public LongRange getCreatetime() {
        return createtime;
    }

    public void setCreatetime(LongRange createtime) {
        this.createtime = createtime;
    }

    public LongRange getMovetime() {
        return movetime;
    }

    public void setMovetime(LongRange movetime) {
        this.movetime = movetime;
    }

    public String[] getLetterids() {
        return letterids;
    }

    public void setLetterids(String[] letterids) {
        this.letterids = letterids;
    }

}
