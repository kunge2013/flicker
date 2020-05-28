/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.base;

import javax.persistence.*;

/**
 *
 * @author zhangjx
 */
public class Player extends BaseEntity {

    @Id
    @Column(comment = "[用户ID] 值从200_0001开始; 36进制固定长度为5位")
    protected int userid;  //用户ID

    @Column(length = 128, comment = "[用户昵称]")
    protected String username = "";  //用户昵称

    @Column(length = 255, comment = "用户头像")
    protected String face = "";  //用户头像

    @Column(comment = "[性别]：2：男； 4:女；")
    protected short gender; //性别; 2:男;  4:女;

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
        this.username = username == null ? "" : username.trim();
    }

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public short getGender() {
        return gender;
    }

    public void setGender(short gender) {
        this.gender = gender;
    }

}
