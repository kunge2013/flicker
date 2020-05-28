/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.*;
import static com.cratos.platf.base.RetCodes.*;
import com.cratos.platf.user.UserService;
import javax.annotation.Resource;
import org.redkale.service.RetResult;
import org.redkale.source.DataSource;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("福利救济金服务")
public class AlmsService extends BaseService {

    @Resource
    private UserService userService;

    @Resource
    private DictService dictService;

    @Resource(name = "platf")
    protected DataSource source;

    public AlmsRecord findAlmsRecord(int userid, int intday) {
        return source.find(AlmsRecord.class, userid + "-" + intday);
    }

    public RetResult<AlmsRecord> runAlms(UserInfo user) {
        int today = Utility.today();
        int lesscoin = (int) dictService.findDictValue(DictInfo.PLATF_BENEFIT_ALMS_DAY_LESSCOIN, 2000);
        if (lesscoin <= user.getCoins()) return RetCodes.retResult(RET_BENEFIT_ALMS_OPTIONILLEGAL);
        int maxcount = (int) dictService.findDictValue(DictInfo.PLATF_BENEFIT_ALMS_DAY_COUNT, 1L);
        synchronized (userLock(user.getUserid())) {
            AlmsRecord record = findAlmsRecord(user.getUserid(), today);
            if (record != null && record.getAlmscount() >= maxcount) return RetCodes.retResult(RET_BENEFIT_ALMS_GOTILLEGAL);
            int getcoin = (int) dictService.findDictValue(DictInfo.PLATF_BENEFIT_ALMS_DAY_GETCOIN, 1000);
            long now = System.currentTimeMillis();
            userService.increPlatfUserCoins(user.getUserid(), getcoin, now, "alms", "领取救济金");
            if (record == null) {
                record = new AlmsRecord();
                record.setAlmsid(user.getUserid() + "-" + today);
                record.setIntday(today);
                record.setUserid(user.getUserid());
                record.setAlmscount(1);
                record.setLastgotcoin(getcoin);
                record.setAlmscoins(getcoin);
                record.setCreatetime(now);
                source.insert(record);
            } else {
                record.setLastgotcoin(getcoin);
                record.setAlmscount(record.getAlmscount() + 1);
                record.setAlmscoins(record.getAlmscoins() + getcoin);
                source.update(record);
            }
            return new RetResult(record);
        }
    }
}
