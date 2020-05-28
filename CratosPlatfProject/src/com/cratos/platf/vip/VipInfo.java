package com.cratos.platf.vip;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "VIP等级信息表")
public class VipInfo extends BaseEntity {

    @Id
    @Column(comment = "VIP ID, 等级")
    private int vipid;

    @Column(comment = "累计充值达到多少才升级VIP")
    private long paymoneytotal = -1;

    @Column(comment = "可摇奖次数")
    private int awardcount;

    @Column(comment = "领取救济金")
    private long almscoin;

    @Column(length = 4096, comment = "描述，多行用;隔开")
    private String vipintros = "";

    @Column(length = 4096, nullable = false, comment = "商品复合值, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setVipid(int vipid) {
        this.vipid = vipid;
    }

    public int getVipid() {
        return this.vipid;
    }

    public void setPaymoneytotal(long paymoneytotal) {
        this.paymoneytotal = paymoneytotal;
    }

    public long getPaymoneytotal() {
        return this.paymoneytotal;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getAwardcount() {
        return awardcount;
    }

    public void setAwardcount(int awardcount) {
        this.awardcount = awardcount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getAlmscoin() {
        return almscoin;
    }

    public void setAlmscoin(long almscoin) {
        this.almscoin = almscoin;
    }

    public void setVipintros(String vipintros) {
        this.vipintros = vipintros;
    }

    public String getVipintros() {
        return this.vipintros;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public GoodsItem[] getGoodsitems() {
        return this.goodsitems;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }
}
