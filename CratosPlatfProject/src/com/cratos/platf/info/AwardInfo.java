package com.cratos.platf.info;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import com.cratos.platf.util.Weightable;
import org.redkale.convert.*;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
@Table(comment = "摇奖配置基础类")
public abstract class AwardInfo extends BaseEntity implements Weightable {

    private static final Reproduce<GoodsItem, AwardInfo> reproduce = Reproduce.create(GoodsItem.class, AwardInfo.class);

    @Id
    @Column(comment = "摇奖项ID, 三位数; 序号从101开始")
    protected int awardid;

    @Column(comment = "中奖项等级; 1开始")
    protected short awardlevel;

    @Column(comment = "[商品类型]")
    protected short goodstype;

    @Column(comment = "商品值数量")
    protected int goodscount;

    @Column(comment = "实物ID， 根据goodstype来指定不同id")
    protected int goodsobjid;

    @Column(comment = "实物ID所属子游戏ID，为空表示平台")
    protected String gameid = "";

    @Column(comment = "商品过期秒数，为0表示不过期")
    protected long goodsexpires;

    @Column(comment = "权重")
    protected int weight;

    @Column(comment = "排序顺序，值小靠前")
    protected int display = 1000;

    @Column(comment = "操作人ID")
    protected int memberid;

    @Column(updatable = false, comment = "创建时间")
    protected long createtime;

    public GoodsItem createGoodsItem() {
        return reproduce.apply(new GoodsItem(), this);
    }

    public int getAwardid() {
        return awardid;
    }

    public void setAwardid(int awardid) {
        this.awardid = awardid;
    }

    public short getAwardlevel() {
        return awardlevel;
    }

    public void setAwardlevel(short awardlevel) {
        this.awardlevel = awardlevel;
    }

    public short getGoodstype() {
        return goodstype;
    }

    public void setGoodstype(short goodstype) {
        this.goodstype = goodstype;
    }

    public int getGoodscount() {
        return goodscount;
    }

    public void setGoodscount(int goodscount) {
        this.goodscount = goodscount;
    }

    public int getGoodsobjid() {
        return goodsobjid;
    }

    public void setGoodsobjid(int goodsobjid) {
        this.goodsobjid = goodsobjid;
    }

    public String getGameid() {
        return gameid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public long getGoodsexpires() {
        return goodsexpires;
    }

    public void setGoodsexpires(long goodsexpires) {
        this.goodsexpires = goodsexpires;
    }

    @Override
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getDisplay() {
        return display;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMemberid() {
        return memberid;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

}
