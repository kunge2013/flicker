/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.*;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import org.redkale.convert.*;
import org.redkale.util.*;

/**
 * ACTID的规则筒GameRetCodes定义范围类似， 单个游戏的都使用6xx_xxx 开始
 *
 * @author zhangjx
 */
public class GameActionEvent extends BaseBean {

    @Comment("玩家ID")
    protected int userid;

    @Comment("操作ID")
    protected int actid;

    @Comment("倒计时结束时间差，-1表示不超时，0表示立即执行")
    protected long delaymillis;

    @Comment("操作开始计数时间，如果为-1表示从Table.addAction 开始重新计时")
    protected long starttime;

    protected Map<String, Object> attributes;

    //-----------------------------------------------------
    ScheduledFuture future;

    public GameActionEvent() {
    }

    //delaymillis < 0 表示永不超时
    public GameActionEvent(int userid, long delaymillis, int actionid) {
        this(userid, delaymillis, actionid, (Map<String, Object>) null);
    }

    public GameActionEvent(int userid, long delaymillis, int actionid, String name, Object value) {
        this(userid, delaymillis, actionid, (Map) Utility.ofMap(name, value));
    }

    public GameActionEvent(int userid, long delaymillis, int actionid, Object... params) {
        this(userid, delaymillis, actionid, (Map) (params == null || params.length < 2 ? null : Utility.ofMap(params)));
    }

    //delaymillis < 0 表示永不超时； 0:表示立即执行
    public GameActionEvent(int userid, long delaymillis, int actionid, Map attributes) {
        this.userid = userid;
        this.actid = actionid;
        this.attributes = attributes;
        this.starttime = System.currentTimeMillis();
        this.delaymillis = delaymillis;
    }

    //取消任务
    public void cancel() {
        ScheduledFuture f = this.future;
        if (f != null && !f.isCancelled()) {
            f.cancel(false);
            this.future = null;
        }
    }

    //重新计时
    public GameActionEvent reclocking() {
        this.starttime = System.currentTimeMillis();
        return this;
    }

    public int getRemains() {
        long now = System.currentTimeMillis();
        if (this.starttime > now) return -2;
        if (this.delaymillis < 0) return -1;
        long cha = (this.starttime + this.delaymillis) - now;
        return cha < 0 ? 0 : (int) cha / 1000;
    }

    @ConvertDisabled
    public boolean isStarted() {
        return System.currentTimeMillis() >= this.starttime;
    }

    @ConvertDisabled
    public boolean isExpired() {
        if (this.delaymillis == 0) return true;
        if (this.delaymillis < 0) return false;
        return System.currentTimeMillis() - this.starttime >= this.delaymillis;
    }

    @ConvertDisabled
    public boolean isRobot() {
        return UserInfo.isRobot(this.userid);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return attributes == null ? null : (T) attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, T defValue) {
        return attributes == null ? defValue : (T) attributes.getOrDefault(name, defValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(String name) {
        if (attributes == null) return null;
        return (T) attributes.remove(name);
    }

    public GameActionEvent addAttribute(String key, Object value) {
        if (this.attributes == null) this.attributes = new LinkedHashMap<>();
        this.attributes.put(key, value);
        return this;
    }

    public GameActionEvent attributes(Object... attributes) {
        this.attributes = Utility.ofMap(attributes);
        return this;
    }

    public GameActionEvent attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    //-------------------------------------------

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getActid() {
        return actid;
    }

    public void setActids(int actid) {
        this.actid = actid;
    }

    public boolean containsUserid(int userid) {
        return this.userid == userid;
    }

    public boolean containsActid(int actid) {
        return this.actid == actid;
    }

    public boolean contains(int userid, int actid) {
        return this.userid == userid && this.actid == actid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getDelaymillis() {
        return delaymillis;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

}
