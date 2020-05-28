package com.cratos.platf.order;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "用户兑换金币未处理的记录表")
public class CoinTradeRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "主键 user36id(6位)+'-'+create36time(9位)")
    private String tradeid = "";

    @Column(comment = "用户ID")
    private int userid;

    @Column(comment = "兑换的金币数")
    private long coin;

    @Column(comment = "兑换的金额，单位:分")
    private long money;

    @Column(comment = "提取方式; 10:银行卡; 20:支付宝")
    private short tradetype;

    @Column(length = 64, comment = "提取账号")
    private String tradeaccount = "";

    @Column(length = 2048, comment = "提取账号其他信息")
    private String tradejson = "";

    @Column(comment = "提取状态; 10:处理中; 20:已完成;30:失败;")
    private short tradestatus;

    @Column(length = 1024, comment = "备注信息")
    private String remark = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setTradeid(String tradeid) {
        this.tradeid = tradeid;
    }

    public String getTradeid() {
        return this.tradeid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getUserid() {
        return this.userid;
    }

    public void setCoin(long coin) {
        this.coin = coin;
    }

    public long getCoin() {
        return this.coin;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getMoney() {
        return this.money;
    }

    public void setTradetype(short tradetype) {
        this.tradetype = tradetype;
    }

    public short getTradetype() {
        return this.tradetype;
    }

    public void setTradeaccount(String tradeaccount) {
        this.tradeaccount = tradeaccount;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getTradeaccount() {
        return this.tradeaccount;
    }

    public String getTradeaccount2() {
        if (this.tradeaccount == null || this.tradeaccount.isEmpty()) return "";
        if (this.tradeaccount.length() <= 8) return this.tradeaccount;
        String s1 = this.tradeaccount.substring(0, 4);
        String s2 = this.tradeaccount.substring(this.tradeaccount.length() - 4);
        StringBuilder s3 = new StringBuilder();
        for (int i = 0; i < this.tradeaccount.length() - 8; i++) {
            s3.append("*");
        }
        return s1 + s3 + s2;
    }

    public void setTradejson(String tradejson) {
        this.tradejson = tradejson;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getTradejson() {
        return this.tradejson;
    }

    public void setTradestatus(short tradestatus) {
        this.tradestatus = tradestatus;
    }

    public short getTradestatus() {
        return this.tradestatus;
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
}
