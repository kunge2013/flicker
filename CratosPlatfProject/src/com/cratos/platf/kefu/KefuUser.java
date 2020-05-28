package com.cratos.platf.kefu;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zhangjx
 */
@Table(comment = "客服用户信息表")
@Cacheable(interval = 60)
public class KefuUser extends BaseEntity {

    @Id
    @Column(comment = "[客服ID] 值从280_0001 至 289_9999 ")
    private int kefuid;

    @Column(length = 128, comment = "[用户昵称]")
    private String username = "";

    @Column(comment = "[状态]: 10:正常;40:冻结;")
    private short status;

    @Column(length = 255, comment = "用户头像")
    private String face = "";

    @Column(comment = "[性别]：2：男； 4:女；")
    private short gender = 4;

    @Column(length = 127, comment = "[个人介绍]")
    private String intro = "";

    @Column(comment = "月评价，五颗星，值0-50")
    private int star;

    @Column(comment = "月销售量")
    private long sales;

    @Column(length = 255, comment = "[备注]")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    @Transient
    private String payievipTypes = "";

    public void setKefuid(int kefuid) {
        this.kefuid = kefuid;
    }

    public int getKefuid() {
        return this.kefuid;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public String getFace() {
        return this.face;
    }

    public void setGender(short gender) {
        this.gender = gender;
    }

    public short getGender() {
        return this.gender;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getIntro() {
        return this.intro;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public int getStar() {
        return this.star;
    }

    public void setSales(long sales) {
        this.sales = sales;
    }

    public long getSales() {
        return this.sales;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public String getPayievipTypes() {
        return payievipTypes;
    }

    public void setPayievipTypes(String payievipTypes) {
        this.payievipTypes = payievipTypes;
    }

}
