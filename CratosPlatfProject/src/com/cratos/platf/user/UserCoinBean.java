/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.BaseBean;
import javax.persistence.Column;
import org.redkale.source.*;
import org.redkale.source.Range.LongRange;

/**
 *
 * @author zhangjx
 */
public class UserCoinBean extends BaseBean implements FilterBean {

    @FilterColumn(comment = "用户ID")
    private int userid;

    @FilterColumn(comment = "游戏模块，平台为:platf")
    private String game = "";

    @FilterColumn(name = "game", express = FilterExpress.NOTEQUAL, comment = "排除的游戏模块")
    private String notgame = "";

    @FilterColumn(comment = "子模块")
    private String module = "";

    @FilterColumn(express = FilterExpress.LIKE, comment = "记录描述")
    private String remark = "";

    @Column(comment = "创建时间")
    private LongRange createtime;

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getNotgame() {
        return notgame;
    }

    public void setNotgame(String notgame) {
        this.notgame = notgame;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LongRange getCreatetime() {
        return createtime;
    }

    public void setCreatetime(LongRange createtime) {
        this.createtime = createtime;
    }

}
