/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import org.redkale.source.FilterBean;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public final class LoginBean extends LoginAbstractBean implements FilterBean {

    @Comment("登录账号")
    private String account;  //登录账号: 用户名、邮箱或者手机号码(为便于区别，用户名规则：不能以数字开头或者包含@)

    @Comment("用户类型")
    private short type; //用户类型

    @Comment("MD5(密码明文)")
    private String password = ""; //HEX-MD5(HEX-MD5(密码明文))

    @Comment("验证码")
    private String vercode = ""; //验证码

    @Comment("COOKIE缓存天数")
    private int cacheday; //COOKIE缓存天数

    public boolean emptyAccount() {
        return this.account == null || this.account.isEmpty();
    }

    public boolean emptyPassword() {
        return this.password == null || this.password.isEmpty();
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public String getAccount() {
        return account == null ? "" : account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVercode() {
        return vercode;
    }

    public void setVercode(String vercode) {
        this.vercode = vercode;
    }

    public int getCacheday() {
        return cacheday;
    }

    public void setCacheday(int cacheday) {
        this.cacheday = cacheday;
    }

}
