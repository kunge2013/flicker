/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.info.AwardInfo;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
public class SkywarAwardInfo extends AwardInfo {

    private static final Reproduce<SkywarAwardRecord, SkywarAwardInfo> reproduce = Reproduce.create(SkywarAwardRecord.class, SkywarAwardInfo.class);

    public SkywarAwardRecord createAwardRecord(int userid, int intday, int index, long createtime) {
        SkywarAwardRecord rs = reproduce.apply(new SkywarAwardRecord(), this);
        rs.setUserid(userid);
        rs.setIntday(intday);
        rs.setAwardindex(index);
        rs.setCreatetime(createtime);
        rs.setAwardrecordid(userid + "-" + intday + "-" + index);
        return rs;
    }
}
