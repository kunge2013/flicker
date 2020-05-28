/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.Column;

/**
 *
 * @author zhangjx
 */
public class VideoPlayer extends BaseEntity {

    @Column(comment = "用户ID")
    protected int userid;

    @Column(length = 128, comment = "用户昵称")
    protected String username;

    @Column(length = 255, comment = "用户头像")
    protected String face = "";

    @Column(comment = "[性别]：2：男； 4:女；")
    protected short gender; //性别; 2:男;  4:女;

    @Column(comment = "整局总战绩")
    protected int tableScore;

    @Column(comment = "玩家胡法")
    protected long huType;

    @Column(comment = "单回合战绩")
    protected int roundScore;

    public VideoPlayer() {
    }

    public VideoPlayer(GamePlayer player, int tableScore) {
        this.userid = player.getUserid();
        this.username = player.getUsername();
        this.face = player.getFace();
        this.gender = player.gender;
        this.tableScore = tableScore;
    }

    public VideoPlayer(GamePlayer player, long huType, int roundScore) {
        this.userid = player.getUserid();
        this.username = player.getUsername();
        this.face = player.getFace();
        this.gender = player.gender;
        this.huType = huType;
        this.roundScore = roundScore;
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

    public int getTableScore() {
        return tableScore;
    }

    public void setTableScore(int tableScore) {
        this.tableScore = tableScore;
    }

    public long getHuType() {
        return huType;
    }

    public void setHuType(long huType) {
        this.huType = huType;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public void setRoundScore(int roundScore) {
        this.roundScore = roundScore;
    }

}
