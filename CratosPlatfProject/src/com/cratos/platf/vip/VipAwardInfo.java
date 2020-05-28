/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.vip;

import com.cratos.platf.info.AwardInfo;
import javax.persistence.Table;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
@Table(comment = "签到幸运摇奖配置")
public class VipAwardInfo extends AwardInfo {

    private static final Reproduce<VipAwardRecord, VipAwardInfo> reproduce = Reproduce.create(VipAwardRecord.class, VipAwardInfo.class);

    public VipAwardRecord createAwardRecord(int userid, int intday, int index, long createtime) {
        VipAwardRecord rs = reproduce.apply(new VipAwardRecord(), this);
        rs.setUserid(userid);
        rs.setIntday(intday);
        rs.setAwardindex(index);
        rs.setCreatetime(createtime);
        rs.setAwardrecordid(userid + "-" + intday + "-" + index);
        return rs;
    }
}
