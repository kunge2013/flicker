package com.cratos.game.skywar;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "战机皮肤信息表")
public class SkywarSkinInfo extends BaseEntity {

    public static final int DEFAULT_SKINID = 700;

    @Id
    @Column(comment = "战机种类ID(3位)")
    private int skinid;

    @Column(length = 32, comment = "战机名称")
    private String skinname = "";

    @Column(comment = "[状态]: 10:正常;40:冻结;")
    private short status = 10;

    @Column(comment = "子弹兑换(永久), -1表示无法兑换")
    private int coin0price;

    @Column(comment = "子弹兑换(7天), -1表示无法兑换")
    private int coin7price;

    @Column(comment = "晶石兑换(永久), -1表示无法兑换")
    private int diamond0price;

    @Column(comment = "晶石兑换(7天), -1表示无法兑换")
    private int diamond7price;

    @Column(comment = "狂暴时间加持秒数")
    private int incfrenzyseconds;

    @Column(comment = "追踪时间加持秒数")
    private int inctrackseconds;

    @Column(comment = "长度，像素值")
    private int width;

    @Column(comment = "宽度，像素值")
    private int height;

    public void setSkinid(int skinid) {
        this.skinid = skinid;
    }

    public int getSkinid() {
        return this.skinid;
    }

    public void setSkinname(String skinname) {
        this.skinname = skinname;
    }

    public String getSkinname() {
        return this.skinname;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public short getStatus() {
        return this.status;
    }

    public void setCoin0price(int coin0price) {
        this.coin0price = coin0price;
    }

    public int getCoin0price() {
        return this.coin0price;
    }

    public void setCoin7price(int coin7price) {
        this.coin7price = coin7price;
    }

    public int getCoin7price() {
        return this.coin7price;
    }

    public void setDiamond0price(int diamond0price) {
        this.diamond0price = diamond0price;
    }

    public int getDiamond0price() {
        return this.diamond0price;
    }

    public void setDiamond7price(int diamond7price) {
        this.diamond7price = diamond7price;
    }

    public int getDiamond7price() {
        return this.diamond7price;
    }

    public void setIncfrenzyseconds(int incfrenzyseconds) {
        this.incfrenzyseconds = incfrenzyseconds;
    }

    public int getIncfrenzyseconds() {
        return this.incfrenzyseconds;
    }

    public void setInctrackseconds(int inctrackseconds) {
        this.inctrackseconds = inctrackseconds;
    }

    public int getInctrackseconds() {
        return this.inctrackseconds;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getWidth() {
        return this.width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getHeight() {
        return this.height;
    }

}
