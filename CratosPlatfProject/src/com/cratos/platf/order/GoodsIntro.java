package com.cratos.platf.order;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "商品描述表")
public class GoodsIntro extends BaseEntity {

    @Id
    @Column(comment = "商品ID")
    private int goodsobjid;

    @Column(length = 128, comment = "商品名称")
    private String goodsname = "";

    @Column(length = 128, comment = "商品图标名称")
    private String goodsicon = "";

    @Column(length = 1024, comment = "商品备注")
    private String goodsremark = "";

    @Column(length = 2048, comment = "商品描述")
    private String goodsdesc = "";

    @Column(updatable = false, comment = "创建时间")
    private long createtime;

    public void setGoodsobjid(int goodsobjid) {
        this.goodsobjid = goodsobjid;
    }

    public int getGoodsobjid() {
        return this.goodsobjid;
    }

    public void setGoodsname(String goodsname) {
        this.goodsname = goodsname;
    }

    public String getGoodsname() {
        return this.goodsname;
    }

    public void setGoodsicon(String goodsicon) {
        this.goodsicon = goodsicon;
    }

    public String getGoodsicon() {
        return this.goodsicon;
    }

    public void setGoodsremark(String goodsremark) {
        this.goodsremark = goodsremark;
    }

    public String getGoodsremark() {
        return this.goodsremark;
    }

    public void setGoodsdesc(String goodsdesc) {
        this.goodsdesc = goodsdesc;
    }

    public String getGoodsdesc() {
        return this.goodsdesc;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }
}
