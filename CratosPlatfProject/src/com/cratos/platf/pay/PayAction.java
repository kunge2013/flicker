package com.cratos.platf.pay;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkalex.pay.Pays;

/**
 *
 * @author zhangjx
 */
@Table(comment = "支付接口结果表")
public class PayAction extends BaseEntity {

    @Id
    @Column(length = 64, comment = "记录ID 值=create36time(9位)+UUID(32位)")
    private String payactid = "";

    @Column(length = 128, comment = "支付编号")
    private String payno = "";

    @Column(comment = "支付类型:  10: 信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;")
    private short paytype = Pays.PAYTYPE_CREDIT;

    @Column(length = 1024, comment = "请求的URL")
    private String acturl = "";

    @Column(length = 2048, comment = "支付接口请求对象")
    private String requestjson = "";

    @Column(length = 5120, comment = "支付接口返回的原始结果")
    private String responsetext = "";

    @Column(updatable = false, comment = "创建时间，单位毫秒")
    private long createtime;

    public void setPayactid(String payactid) {
        this.payactid = payactid;
    }

    public String getPayactid() {
        return this.payactid;
    }

    public void setPayno(String payno) {
        this.payno = payno;
    }

    public String getPayno() {
        return this.payno;
    }

    public void setPaytype(short paytype) {
        this.paytype = paytype;
    }

    public short getPaytype() {
        return this.paytype;
    }

    public void setActurl(String acturl) {
        this.acturl = acturl;
    }

    public String getActurl() {
        return this.acturl;
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

}
