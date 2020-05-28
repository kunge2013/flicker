package com.cratos.platf.liveness;

import javax.persistence.*;
import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.order.GoodsItem;

/**
 *
 * @author zhangjx
 */
@Table(comment = "活跃度奖励表")
public class LivenessRewardInfo extends BaseEntity {

    @Id
    @Column(comment = "签到奖励ID， 从101开始")
    private int livenessrewardid = 101;

    @Column(comment = "序号，从1开始")
    private int rewardindex;

    @Column(comment = "达标的活跃度值")
    private long reachliveness;

    @Column(length = 4096, nullable = false, comment = "奖励的复合商品, GoodsItem[]数组")
    private GoodsItem[] goodsitems;

    @Column(length = 1024, comment = "备注")
    private String remark = "";

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    private long createtime;

    public void setLivenessrewardid(int livenessrewardid) {
        this.livenessrewardid = livenessrewardid;
    }

    public int getLivenessrewardid() {
        return this.livenessrewardid;
    }

    public void setRewardindex(int rewardindex) {
        this.rewardindex = rewardindex;
    }

    public int getRewardindex() {
        return this.rewardindex;
    }

    public void setReachliveness(long reachliveness) {
        this.reachliveness = reachliveness;
    }

    public long getReachliveness() {
        return this.reachliveness;
    }

    public void setGoodsitems(GoodsItem[] goodsitems) {
        this.goodsitems = goodsitems;
    }

    public GoodsItem[] getGoodsitems() {
        return this.goodsitems;
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
