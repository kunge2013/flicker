package com.cratos.platf.profit;

import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
@Table(comment = "用户提取收益记录表")
public class ProfitTradeRecordHis extends ProfitTradeRecord {

    private static final Reproduce<ProfitTradeRecordHis, ProfitTradeRecord> reproduce = Reproduce.create(ProfitTradeRecordHis.class, ProfitTradeRecord.class);

    @Id
    @Column(length = 64, comment = "主键 user36id(6位)+'-'+create36time(9位)")
    private String tradeid = "";

    @Column(comment = "结束时间")
    private long finishtime;

    @Column(comment = "操作人ID")
    private int memberid;

    @Transient
    @Column(comment = "操作人")
    private String membername = "";

    public ProfitTradeRecordHis() {
    }

    public ProfitTradeRecordHis(ProfitTradeRecord record) {
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

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getMemberid() {
        return memberid;
    }

    public void setMemberid(int memberid) {
        this.memberid = memberid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getMembername() {
        return membername;
    }

    public void setMembername(String membername) {
        this.membername = membername;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public long getFinishtime() {
        return this.finishtime;
    }
}
