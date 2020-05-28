package com.cratos.platf.pay;

import javax.persistence.*;
import java.util.Map;
import com.cratos.platf.base.BaseEntity;
import org.redkale.util.Utility;
import org.redkalex.pay.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "支付表")
public class PayRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "支付编号; 值='pay'+orderno")
    private String payno = "";

    @Column(length = 128, comment = "第三方支付订单号")
    private String thirdpayno = "";

    @Column(length = 128, comment = "支付APP应用ID")
    private String appid = "";

    @Column(comment = "支付类型:  10: 信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;")
    private short paytype = Pays.PAYTYPE_CREDIT;

    @Column(comment = "子支付类型;")
    private short subpaytype;

    @Column(comment = "支付渠道:  10: 信用/虚拟支付; 20:人工支付; 30:APP支付; 40:网页支付; 50:机器支付;")
    private short payway = Pays.PAYWAY_CREDIT;

    @Column(comment = "付款人用户ID")
    private int userid;

    @Column(length = 128, comment = "订单标题")
    private String paytitle = "";

    @Column(length = 255, comment = "订单内容描述")
    private String paybody = "";

    @Column(length = 255, comment = "支付回调连接")
    private String notifyurl = "";

    @Column(length = 64, comment = "订单编号")
    private String orderno = "";

    @Column(comment = "支付状态; 10:待支付; 30:已支付;90:已关闭;95:已取消;")
    private short paystatus = 10;

    @Column(comment = "实际支付金额 单位：人民币分")
    private long payedmoney;

    @Column(comment = "订单金额，单位：人民币分")
    private long ordermoney;

    @Column(length = 1024, comment = "支付接口请求对象")
    private String requestjson = "";

    @Column(length = 10240, comment = "支付接口返回的原始结果")
    private String responsetext = "";

    @Column(updatable = false, comment = "支付开始时间，单位毫秒")
    private long createtime;

    @Column(comment = "支付结束时间，单位毫秒")
    private long finishtime;

    @Column(updatable = false, length = 64, comment = "客户端请求的HOST")
    private String clienthost = "";

    @Column(length = 128, comment = "客户端生成时的IP")
    private String clientaddr = "";

    @Transient
    private Map<String, String> attach; //扩展信息

    public PayRecord() {
    }

    public PayPreRequest createPayPreRequest() {
        PayPreRequest req = new PayPreRequest();
        req.setPayno(this.getPayno());
        req.setAttach(this.getAttach());
        req.setPaytype(this.getPaytype());
        req.setSubpaytype(this.getSubpaytype());
        req.setPayway(this.getPayway());
        req.setPaymoney(this.getOrdermoney());
        req.setAppid(this.getAppid());
        req.setClienthost(this.getClienthost());
        req.setClientAddr(this.getClientaddr());
        req.setPaytitle(this.getPaytitle());
        req.setPaybody(this.getPaybody());
        req.setNotifyurl(this.getNotifyurl());
        return req;
    }

    public PayRefundRequest createPayRefundRequest() {
        PayRefundRequest req = new PayRefundRequest();
        req.setPayno(this.getPayno());
        req.setRefundno(this.getPayno());
        req.setThirdpayno(this.getPayno());
        req.setAttach(this.getAttach());
        req.setPaytype(this.getPaytype());
        req.setSubpaytype(this.getSubpaytype());
        req.setPaymoney(this.getOrdermoney());
        req.setRefundmoney(this.getOrdermoney());
        req.setClienthost(this.getClienthost());
        req.setClientAddr(this.getClientaddr());
        req.setAppid(this.getAppid());
        return req;
    }

    public void createPayno() {
        this.payno = "p" + this.getOrderno() + "-" + Utility.format36time(this.createtime);
    }

    public boolean isPayok() {
        return this.paystatus == Pays.PAYSTATUS_PAYOK;
    }

    public boolean isRefundok() {
        return this.paystatus == Pays.PAYSTATUS_REFUNDOK;
    }

    public void setPayno(String payno) {
        this.payno = payno;
    }

    public String getPayno() {
        return this.payno;
    }

    public void setThirdpayno(String thirdpayno) {
        this.thirdpayno = thirdpayno;
    }

    public String getThirdpayno() {
        return this.thirdpayno;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppid() {
        return this.appid == null ? "" : this.appid;
    }

    public void setPaytype(short paytype) {
        this.paytype = paytype;
    }

    public short getPaytype() {
        return this.paytype;
    }

    public short getSubpaytype() {
        return subpaytype;
    }

    public void setSubpaytype(short subpaytype) {
        this.subpaytype = subpaytype;
    }

    public String getPaytypename() {
        if (this.paytype == Pays.PAYTYPE_UNION) return "union";
        if (this.paytype == Pays.PAYTYPE_WEIXIN) return "weixin";
        if (this.paytype == Pays.PAYTYPE_ALIPAY) return "alipay";
        return "xxx"; //不存在的类型
    }

    public void setPayway(short payway) {
        this.payway = payway;
    }

    public short getPayway() {
        return this.payway;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public String getPaytitle() {
        return paytitle;
    }

    public void setPaytitle(String paytitle) {
        this.paytitle = paytitle;
    }

    public String getPaybody() {
        return paybody;
    }

    public void setPaybody(String paybody) {
        this.paybody = paybody;
    }

    public String getNotifyurl() {
        return notifyurl;
    }

    public void setNotifyurl(String notifyurl) {
        this.notifyurl = notifyurl;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public String getOrderno() {
        return this.orderno;
    }

    public void setPaystatus(short paystatus) {
        this.paystatus = paystatus;
    }

    public short getPaystatus() {
        return this.paystatus;
    }

    public void setPayedmoney(long payedmoney) {
        this.payedmoney = payedmoney;
    }

    public long getPayedmoney() {
        return this.payedmoney;
    }

    public void setOrdermoney(long ordermoney) {
        this.ordermoney = ordermoney;
    }

    public long getOrdermoney() {
        return this.ordermoney;
    }

    public void setRequestjson(String requestjson) {
        this.requestjson = requestjson;
    }

    public String getRequestjson() {
        return this.requestjson;
    }

    public void setResponsetext(String responsetext) {
        this.responsetext = responsetext;
    }

    public String getResponsetext() {
        return this.responsetext;
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

    public String getClienthost() {
        return clienthost;
    }

    public void setClienthost(String clienthost) {
        this.clienthost = clienthost;
    }

    public void setClientaddr(String clientaddr) {
        this.clientaddr = clientaddr;
    }

    public String getClientaddr() {
        return this.clientaddr;
    }

    public Map<String, String> getAttach() {
        return attach;
    }

    public void setAttach(Map<String, String> attach) {
        this.attach = attach;
    }

}
