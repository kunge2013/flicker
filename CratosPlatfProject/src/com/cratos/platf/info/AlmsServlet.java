/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.*;
import com.cratos.platf.user.*;
import java.io.IOException;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.Utility;
import static org.redkale.util.Utility.ofMap;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/alms/*"})
public class AlmsServlet extends BaseServlet {

    @Resource
    private UserService userService;

    @Resource
    private DictService dictService;

    @Resource
    private AlmsService almsService;

    @HttpMapping(url = "/alms/almsinfo", auth = true, comment = "获取领取救济金信息")
    public void almsInfo(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        int maxcount = (int) dictService.findDictValue(DictInfo.PLATF_BENEFIT_ALMS_DAY_COUNT, 1L);
        int getcoin = (int) dictService.findDictValue(DictInfo.PLATF_BENEFIT_ALMS_DAY_GETCOIN, 1000);
        int lesscoin = (int) dictService.findDictValue(DictInfo.PLATF_BENEFIT_ALMS_DAY_LESSCOIN, 2000);
        AlmsRecord record = almsService.findAlmsRecord(userid, Utility.today());
        resp.finishJson(new RetResult(ofMap("almscount", maxcount, "almsgetcoin", getcoin, "almslesscoin", lesscoin, "gotcount", record == null ? 0 : record.getAlmscount())));
    }

    @HttpMapping(url = "/alms/runalms", auth = true, comment = "领取福利救济金")
    public void runAlms(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = userService.findUserInfo(currentUserid(req));
        resp.finishJson(almsService.runAlms(user));
    }

}
