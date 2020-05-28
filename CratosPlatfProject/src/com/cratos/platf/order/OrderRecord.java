package com.cratos.platf.order;

import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.pay.PayRecord;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.*;
import org.redkale.util.*;
import org.redkalex.pay.Pays;

/**
 *
 * @author zhangjx
 */
@Table(comment = "购买商品订单表")
public class OrderRecord extends BaseEntity {

    private static final Reproduce<PayRecord, OrderRecord> orderToPayCopyer = Reproduce.create(PayRecord.class, OrderRecord.class);

    @Comment("待处理")
    public static final short ORDER_STATUS_PENDING = 10;

    @Comment("处理成功")
    public static final short ORDER_STATUS_OK = 20;

    @Comment("处理失败")
    public static final short ORDER_STATUS_NO = 30;

    @Id
    @Column(length = 64, comment = "订单编号; 值=类型(3位)+user36id(6位)+create36time(9位)")
    private String orderno = "";

    @Column(updatable = false, comment = "付款人用户ID")
    private int userid;

    @Column(updatable = false, comment = "订单日期， 格式为: 20200101")
    private int orderday;

    @Column(updatable = false, comment = "订单金额，单位: 人民币分")
    private int ordermoney;

    @Column(updatable = false, comment = "商品ID; 不同的goodstype则商品表不同")
    private int goodsid;
    
    @Column(updatable = false, length = 255, comment = "商品名称")
    private String goodsname = "";

    @Column(updatable = false, comment = "购买货币; 10:RMB;20:金币兑换;30:钻石兑换;40:奖券兑换;")
    private short buytype;

    @Column(updatable = false, comment = "商品类型(必须三位数); 101:购买金币;105:购买钻石;")
    private short goodstype;

    @Column(updatable = false, comment = "商品价值; 不同的goodstype表示不同的商品价值")
    private int goodscount;

    @Column(length = 4096, nullable = false, comment = "商品复合值")
    private GoodsItem[] goodsitems;

    @Column(length = 2048, nullable = false, comment = "商品扩展值")
    private GoodsItem[] giftitems;

    //----------------可更改字段---------------
    @Column(comment = "订单状态; 10:待处理; 20:处理成功; 30:处理失败;")
    private short orderstatus = ORDER_STATUS_PENDING;

    @Column(comment = "支付状态; 10:待支付;20:支付中;30:已支付;40:支付失败;50:待退款;60退款中;70:已退款;80:退款失败;90:已关闭;95:已取消; ")
    private short paystatus = Pays.PAYSTATUS_UNPAY;

    @Column(comment = "关闭时间，单位毫秒")
    private long finishtime;
    //-----------------------------------------

    @Column(updatable = false, comment = "支付类型:  10:信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;(人工支付将不产生payrecord记录)")
    private short paytype = 11;

    @Column(length = 64, comment = "子支付类型;")
    private String subpaytype = "";

    @Column(updatable = false, comment = "支付渠道:  10:信用/虚拟支付; 20:人工支付; 30:APP支付; 40:网页支付; 50:机器支付;")
    private short payway = 10;

    @Column(updatable = false, comment = "开始时间，单位毫秒")
    private long createtime;

    @Column(updatable = false, length = 64, comment = "客户端请求的HOST")
    private String clienthost = "";

    @Column(updatable = false, length = 128, comment = "客户端生成时的IP")
    private String clientaddr = "";

    public PayRecord createPayRecord(String appid) {
        PayRecord record = orderToPayCopyer.apply(new PayRecord(), this);
        record.setPaytitle((this.ordermoney < 100 ? this.ordermoney / 100.0 : this.ordermoney / 100) + "元购买" + this.goodscount + (this.goodstype == GoodsInfo.GOODS_TYPE_DIAMOND ? "钻石" : "金币"));
        record.setPaybody(record.getPaytitle());
        record.setAppid(appid);
        if (this.getSubpaytype().contains("alipay")) {
            record.setSubpaytype(Pays.PAYTYPE_ALIPAY);
        } else if (this.getSubpaytype().contains("wxpay")) {
            record.setSubpaytype(Pays.PAYTYPE_WEIXIN);
        }
        record.setClienthost(this.getClienthost());
        record.setClientaddr(this.getClientaddr());
        return record;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public String getOrderno() {
        return this.orderno;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public short getBuytype() {
        return buytype;
    }

    public void setBuytype(short buytype) {
        this.buytype = buytype;
    }

    public void setGoodstype(short goodstype) {
        this.goodstype = goodstype;
    }

    public short getGoodstype() {
        return this.goodstype;
    }

    public void setOrdermoney(int ordermoney) {
        this.ordermoney = ordermoney;
    }

    public int getOrdermoney() {
        return this.ordermoney;
    }

    public void setGoodsid(int goodsid) {
        this.goodsid = goodsid;
    }

    public int getGoodsid() {
        return this.goodsid;
    }

    public String getGoodsname() {
        return goodsname;
    }

    public void setGoodsname(String goodsname) {
        this.goodsname = goodsname;
    }

    public int getOrderday() {
        return orderday;
    }

    public void setOrderday(int orderday) {
        this.orderday = orderday;
    }

    public void setGoodscount(int goodscount) {
        this.goodscount = goodscount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getGoodscount() {
        return this.goodscount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public GoodsItem[] getGoodsitems() {
        return goodsitems;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public GoodsItem[] getGiftitems() {
        return giftitems;
    }

    public void setGiftitems(GoodsItem[] giftitems) {
        this.giftitems = giftitems;
    }

    public void setOrderstatus(short orderstatus) {
        this.orderstatus = orderstatus;
    }

    public short getOrderstatus() {
        return this.orderstatus;
    }

    public void setPaytype(short paytype) {
        this.paytype = paytype;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getPaytype() {
        return this.paytype;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getSubpaytype() {
        return subpaytype;
    }

    public void setSubpaytype(String subpaytype) {
        this.subpaytype = subpaytype;
    }

    public void setPaystatus(short paystatus) {
        this.paystatus = paystatus;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getPaystatus() {
        return this.paystatus;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getPayway() {
        return payway;
    }

    public void setPayway(short payway) {
        this.payway = payway;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public long getFinishtime() {
        return this.finishtime;
    }

    public void setClientaddr(String clientaddr) {
        this.clientaddr = clientaddr;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getClientaddr() {
        return this.clientaddr;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getClienthost() {
        return clienthost;
    }

    public void setClienthost(String clienthost) {
        this.clienthost = clienthost;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }
}
