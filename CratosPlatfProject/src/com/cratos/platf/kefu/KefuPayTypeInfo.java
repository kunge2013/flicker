package com.cratos.platf.kefu;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author Dell
 */
@Table(comment = "im客服支付方式信息表")
public class KefuPayTypeInfo extends BaseEntity {

    //正常
    public static final short STATUS_NORMAL = 10;
    
    //删除
    public static final short STATUS_DELETED = 80;
    
    @Id
    @Column(length = 64, comment = "主键 type + 三位序号")
    private String paytypeid = "";

    @Column(length = 64, comment = "支付方式;微信支付:wxpay;支付宝:alipay;银行卡:bankpay")
    private String type = "";

    @Column(length = 64, comment = "跳转链接")
    private String url = "";

    @Column(length = 64, comment = "二维码链接")
    private String qrcode = "";

    @Column(length = 2048, comment = "银行卡账号信息")
    private String bankjson = "";

    @Column(comment = "[状态]: 10:正常;80:删除;")
    private short status;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setPaytypeid(String paytypeid) {
        this.paytypeid = paytypeid;
    }

    public String getPaytypeid() {
        return this.paytypeid;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    public String getQrcode() {
        return this.qrcode;
    }

    public void setBankjson(String bankjson) {
        this.bankjson = bankjson;
    }

    public String getBankjson() {
        return this.bankjson;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public short getStatus() {
        return this.status;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }
}
