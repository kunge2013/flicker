/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class UserPwdBean extends BaseBean {

    @Comment("Session会话ID")
    private String sessionid;

    @Comment("随机码")
    private String randomcode = "";

    @Comment("用户账号")
    private String account = "";

    @Comment("验证码")
    private String vercode = "";

    @Comment("旧密码 MD5(密码明文)")
    private String oldpwd;  //HEX-MD5(密码明文)

    @Comment("新密码 MD5(密码明文)")
    private String newpwd;  //HEX-MD5(密码明文)

    private UserInfo user;

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    public String getOldpwd() {
        return oldpwd;
    }

    public void setOldpwd(String oldpwd) {
        this.oldpwd = oldpwd;
    }

    public String getNewpwd() {
        return newpwd;
    }

    public void setNewpwd(String newpwd) {
        this.newpwd = newpwd;
    }

    public String getRandomcode() {
        return randomcode;
    }

    public void setRandomcode(String randomcode) {
        this.randomcode = randomcode;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getVercode() {
        return vercode;
    }

    public void setVercode(String vercode) {
        this.vercode = vercode;
    }

}
