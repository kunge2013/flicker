package com.cratos.platf.info;

import com.cratos.platf.base.BaseEntity;
import java.io.Serializable;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "用户领取福利救济金记录表")
@DistributeTable(strategy = AlmsRecord.TableStrategy.class)
public class AlmsRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "福利救济金ID 值=userid+'-'+intday")
    private String almsid = "";

    @Column(updatable = false, comment = "用户ID")
    private int userid;

    @Column(updatable = false, comment = "日期;格式：20170707;")
    private int intday;

    @Column(comment = "领取次数")
    private int almscount = 1;

    @Column(comment = "最后一次领取的金币数")
    private long lastgotcoin;

    @Column(comment = "领取福利救济金的总金币数")
    private long almscoins;

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setAlmsid(String almsid) {
        this.almsid = almsid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getAlmsid() {
        return this.almsid;
    }

    public void setIntday(int intday) {
        this.intday = intday;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getIntday() {
        return this.intday;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getUserid() {
        return this.userid;
    }

    public void setAlmscount(int almscount) {
        this.almscount = almscount;
    }

    public int getAlmscount() {
        return this.almscount;
    }

    public long getLastgotcoin() {
        return lastgotcoin;
    }

    public void setLastgotcoin(long lastgotcoin) {
        this.lastgotcoin = lastgotcoin;
    }

    public void setAlmscoins(long almscoins) {
        this.almscoins = almscoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getAlmscoins() {
        return this.almscoins;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }

    public static class TableStrategy implements DistributeTableStrategy<AlmsRecord> {

        @Override
        public String getTable(String table, AlmsRecord bean) {
            return getSingleTable(table, bean.getIntday());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            int pos1 = id.indexOf('-');
            int pos2 = id.lastIndexOf('-');
            return getSingleTable(table, Integer.parseInt(pos1 != pos2 ? id.substring(pos1 + 1, pos2) : id.substring(pos1 + 1)));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            return getSingleTable(table, (Integer) node.findValue("intday"));
        }

        private String getSingleTable(String table, int initday) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + initday;
        }
    }
}
