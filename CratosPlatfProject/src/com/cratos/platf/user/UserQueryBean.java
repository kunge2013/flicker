/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.BaseBean;
import org.redkale.source.*;
import static org.redkale.source.FilterExpress.LIKE;

/**
 *
 * @author zhangjx
 */
public class UserQueryBean extends BaseBean implements FilterBean {

    private String currgame = "";

    private int userid;

    @FilterColumn(express = LIKE)
    private String username = "";

    public String getCurrgame() {
        return currgame;
    }

    public void setCurrgame(String currgame) {
        this.currgame = currgame;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
