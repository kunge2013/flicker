/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

/**
 * QQ登录参数
 *
 * @author zhangjx
 */
public class LoginQQBean extends LoginAbstractBean {

    protected String accesstoken;

    protected String openid;

    public boolean emptyAccesstoken() {
        return this.accesstoken == null || this.accesstoken.isEmpty();
    }

    public String getAccesstoken() {
        return accesstoken;
    }

    public void setAccesstoken(String accesstoken) {
        this.accesstoken = accesstoken;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

}
