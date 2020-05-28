package com.cratos.platf.order;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.convert.*;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "用户银行信息表")
public class CardInfo extends BaseEntity {

    @Comment("提现方式: 银行卡")
    public static final short TRADE_TYPE_BANK = 10;

    @Comment("提现方式: 支付宝")
    public static final short TRADE_TYPE_ALIPAY = 20;

    @Comment("处理中")
    public static final short TRADE_STATUS_PENDING = 10;

    @Comment("处理成功")
    public static final short TRADE_STATUS_DONEOK = 20;

    @Comment("处理失败")
    public static final short TRADE_STATUS_DONENO = 30;
    
    @Comment("未验证")
    public static final short CARDSTATUS_UNCHECK = 10;

    @Comment("已验证")
    public static final short CARDSTATUS_CHECKOK = 20;

    @Comment("验证失败")
    public static final short CARDSTATUS_CHECKNO = 30;

    @Id
    @Column(comment = "用户ID")
    private int userid;

    @Column(length = 32, comment = "银行账号")
    private String cardaccount = "";

    @Column(length = 16, comment = "持卡人真实姓名")
    private String cardrealname = "";

    @Column(length = 16, comment = "卡类型; {DC: '储蓄卡', CC: '信用卡', SCC: '准贷记卡', PC: '预付费卡'}")
    private String cardtype = "";

    @Column(length = 16, comment = "卡所属银行ID; 'CDB': '国家开发银行', 'ICBC': '中国工商银行', ... ")
    private String cardbanktype = "";

    @Column(length = 32, comment = "卡所属银行名称; 如: 中国工商银行")
    private String cardbankname = "";

    @Column(length = 64, comment = "银行支行名称")
    private String cardsubbranch = "";

    @Column(length = 32, comment = "卡所属省份; 如: 湖北")
    private String cardprovince = "";

    @Column(length = 32, comment = "卡所属城市; 如: 武汉")
    private String cardcity = "";

    @Column(comment = "卡的验证状态; 10:未验证;20:已验证;30:验证失败;")
    private short cardstatus;

    @Column(length = 64, comment = "支付宝账号")
    private String alipayaccount = "";

    @Column(length = 32, comment = "支付宝真实姓名")
    private String alipayrealname = "";

    @Column(comment = "支付宝账号的验证状态; 10:未验证;20:已验证;30:验证失败;")
    private short alipaystatus;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public String toCardJson() {
        return JsonConvert.root().convertTo(Utility.ofMap(
            "cardaccount", this.cardaccount,
            "cardrealname", this.cardrealname,
            "cardtype", this.cardtype,
            "cardbanktype", this.cardbanktype,
            "cardbankname", this.cardbankname,
            "cardsubbranch", this.cardsubbranch,
            "cardprovince", this.cardprovince,
            "cardcity", this.cardcity,
            "cardstatus", this.cardstatus));
    }

    public String toAlipayJson() {
        return JsonConvert.root().convertTo(Utility.ofMap(
            "alipayaccount", this.alipayaccount,
            "alipayrealname", this.alipayrealname,
            "alipaystatus", this.alipaystatus));
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setCardaccount(String cardaccount) {
        this.cardaccount = cardaccount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getCardaccount() {
        return this.cardaccount;
    }

    public String getCardaccount2() {
        if (this.cardaccount == null || this.cardaccount.isEmpty()) return "";
        if (this.cardaccount.length() <= 8) return this.cardaccount;
        String s1 = this.cardaccount.substring(0, 4);
        String s2 = this.cardaccount.substring(this.cardaccount.length() - 4);
        StringBuilder s3 = new StringBuilder();
        for (int i = 0; i < this.cardaccount.length() - 8; i++) {
            s3.append("*");
        }
        return s1 + s3 + s2;
    }

    public void setCardrealname(String cardrealname) {
        this.cardrealname = cardrealname;
    }

    public String getCardrealname() {
        return this.cardrealname;
    }

    public void setCardtype(String cardtype) {
        this.cardtype = cardtype;
    }

    public String getCardtype() {
        return this.cardtype;
    }

    public void setCardbanktype(String cardbanktype) {
        this.cardbanktype = cardbanktype;
    }

    public String getCardbanktype() {
        return this.cardbanktype;
    }

    public void setCardbankname(String cardbankname) {
        this.cardbankname = cardbankname;
    }

    public String getCardbankname() {
        return this.cardbankname;
    }

    public void setCardsubbranch(String cardsubbranch) {
        this.cardsubbranch = cardsubbranch;
    }

    public String getCardsubbranch() {
        return this.cardsubbranch;
    }

    public void setCardprovince(String cardprovince) {
        this.cardprovince = cardprovince;
    }

    public String getCardprovince() {
        return this.cardprovince;
    }

    public void setCardcity(String cardcity) {
        this.cardcity = cardcity;
    }

    public String getCardcity() {
        return this.cardcity;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getCardstatus() {
        return cardstatus;
    }

    public void setCardstatus(short cardstatus) {
        this.cardstatus = cardstatus;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getAlipaystatus() {
        return alipaystatus;
    }

    public void setAlipaystatus(short alipaystatus) {
        this.alipaystatus = alipaystatus;
    }

    public void setAlipayaccount(String alipayaccount) {
        this.alipayaccount = alipayaccount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getAlipayaccount() {
        return this.alipayaccount;
    }

    public String getAlipayaccount2() {
        if (this.alipayaccount == null || this.alipayaccount.isEmpty()) return "";
        if (this.alipayaccount.length() <= 6) return this.alipayaccount;
        String s1 = this.alipayaccount.substring(0, 3);
        String s2 = this.alipayaccount.substring(this.alipayaccount.length() - 3);
        StringBuilder s3 = new StringBuilder();
        for (int i = 0; i < this.alipayaccount.length() - 6; i++) {
            s3.append("*");
        }
        return s1 + s3 + s2;
    }

    public void setAlipayrealname(String alipayrealname) {
        this.alipayrealname = alipayrealname;
    }

    public String getAlipayrealname() {
        return this.alipayrealname;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }
}
