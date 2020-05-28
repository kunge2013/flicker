package com.cratos.platf.order;

import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
@Table(comment = "银行卡打款充值记录表")
public class OrderTradeRecordHis extends OrderTradeRecord {

    private static final Reproduce<OrderTradeRecordHis, OrderTradeRecord> reproduce = Reproduce.create(OrderTradeRecordHis.class, OrderTradeRecord.class);

    @Id
    @Column(length = 64, comment = "主键 user36id(6位)+'-'+create36time(9位)")
    private String tradeid = "";

    @Column(comment = "结束时间")
    private long finishtime;

    @Column(comment = "操作人")
    private int memberid;

    @Transient
    @Column(comment = "操作人")
    private String membername = "";

    public OrderTradeRecordHis() {
    }

    public OrderTradeRecordHis(OrderTradeRecord record) {
        reproduce.apply(this, record);
    }

    @Override
    public void setTradeid(String tradeid) {
        this.tradeid = tradeid;
    }

    @Override
    public String getTradeid() {
        return this.tradeid;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public long getFinishtime() {
        return this.finishtime;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    public int getMemberid() {
        return this.memberid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getMembername() {
        return membername;
    }

    public void setMembername(String membername) {
        this.membername = membername;
    }

}
