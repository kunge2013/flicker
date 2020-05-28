package com.cratos.platf.duty;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "签到奖励表")
public class DutyReward extends BaseEntity {

    //连续签到
    public static final int DUTY_TYPE_SERIE = 10;

    //累计签到
    public static final int DUTY_TYPE_TOTAL = 20;

    @Id
    @Column(comment = "签到奖励ID， 从100001开始")
    private int dutyrewardid = 100001;

    @Column(comment = "签到序号，从1开始")
    private int dutyindex;

    @Column(length = 4096, nullable = false, comment = "签到领取的复合商品, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(length = 1024, comment = "关联多个其他模块或游戏，以;隔开")
    private String modules = "";

    @Column(length = 1024, comment = "备注")
    private String remark = "";

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    private long createtime;

    public void setDutyrewardid(int dutyrewardid) {
        this.dutyrewardid = dutyrewardid;
    }

    public int getDutyrewardid() {
        return this.dutyrewardid;
    }

    public void setDutyindex(int dutyindex) {
        this.dutyindex = dutyindex;
    }

    public int getDutyindex() {
        return this.dutyindex;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public GoodsItem[] getGoodsitems() {
        return this.goodsitems;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getModules() {
        return modules;
    }

    public void setModules(String modules) {
        this.modules = modules;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRemark() {
        return this.remark;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return this.createtime;
    }
}
