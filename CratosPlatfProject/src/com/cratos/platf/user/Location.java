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
public class Location extends BaseBean {

    @Comment("定位经度")
    protected Double longitude;

    @Comment("定位纬度")
    protected Double latitude;

    @Comment("定位街道")
    protected String street;

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

}
