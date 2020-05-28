/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.base;

import javax.persistence.Column;

/**
 *
 * @author zhangjx
 */
public class IntroPlayer extends Player {

    @Column(length = 127, comment = "[个人介绍]")
    protected String intro = ""; //备注

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

}
