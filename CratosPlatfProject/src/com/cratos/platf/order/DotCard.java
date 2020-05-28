package com.cratos.platf.order;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.ConvertDisabled;
import org.redkale.convert.json.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "未使用点卡信息表")
public class DotCard extends BaseEntity {

    private static final Reproduce<DotCardHis, DotCard> reproduce = Reproduce.create(DotCardHis.class, DotCard.class);

    @Comment("未使用")
    public static final short CARD_STATUS_UNUSE = 10;

    @Comment("已使用")
    public static final short CARD_STATUS_USED = 30;

    @Comment("已取消")
    public static final short CARD_STATUS_CANCEL = 95;

    @Comment("邀请卡")
    public static final short CARD_TYPE_YAOQING = 10;

    @Comment("点卡")
    public static final short CARD_TYPE_DIANKA = 20;

    @Id
    @Column(length = 32, comment = "点卡ID ，随机值16位")
    private String cardid = "";

    @Column(comment = "点卡组ID")
    private int cardgroupid;

    @Column(comment = "状态; 10:未使用; 30:已使用;95:已取消; ")
    private short cardstatus = 10;

    @Column(comment = "类型; 10:邀请卡; 20:点卡; ")
    private short cardtype = 10;

    @Column(comment = "金币面值")
    private long coins;

    @Column(comment = "晶石面值")
    private long diamonds;

    @Column(comment = "奖券面值")
    private long coupons;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public DotCardHis createGoodsCardHis(int userid, short cardstatus, int agencyid, long time) {
        DotCardHis his = reproduce.apply(new DotCardHis(), this);
        his.setUserid(userid);
        his.setAgencyid(agencyid);
        his.setCardstatus(cardstatus);
        his.setMovetime(time);
        return his;
    }

    @ConvertDisabled
    public GoodsItem[] getGoodsItems() {
        GoodsItem[] items = new GoodsItem[0];
        if (this.coins > 0) items = Utility.append(items, new GoodsItem(GoodsInfo.GOODS_TYPE_COIN, (int) this.coins));
        if (this.diamonds > 0) items = Utility.append(items, new GoodsItem(GoodsInfo.GOODS_TYPE_DIAMOND, (int) this.diamonds));
        if (this.coupons > 0) items = Utility.append(items, new GoodsItem(GoodsInfo.GOODS_TYPE_COUPON, (int) this.coupons));
        return items;
    }

    public void setCardid(String cardid) {
        this.cardid = cardid;
    }

    public String getCardid() {
        return this.cardid;
    }

    public void setCardgroupid(int cardgroupid) {
        this.cardgroupid = cardgroupid;
    }

    public int getCardgroupid() {
        return this.cardgroupid;
    }

    public void setCardstatus(short cardstatus) {
        this.cardstatus = cardstatus;
    }

    public short getCardstatus() {
        return this.cardstatus;
    }

    public short getCardtype() {
        return cardtype;
    }

    public void setCardtype(short cardtype) {
        this.cardtype = cardtype;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public long getCoins() {
        return this.coins;
    }

    public long getDiamonds() {
        return diamonds;
    }

    public void setDiamonds(long diamonds) {
        this.diamonds = diamonds;
    }

    public long getCoupons() {
        return coupons;
    }

    public void setCoupons(long coupons) {
        this.coupons = coupons;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }
}
