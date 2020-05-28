/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.vip;

import com.cratos.platf.base.*;
import com.cratos.platf.user.UserService;
import java.io.IOException;
import java.util.List;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/vip/*"})
public class VipServlet extends BaseServlet {

    @Resource
    protected VipService service;

    @Resource
    protected UserService userService;

    @HttpMapping(url = "/vip/vipinfos", auth = true, comment = "VIP信息列表")
    public void vipinfos(HttpRequest req, HttpResponse resp) throws IOException {
        final List<VipInfo> vips = service.queryVipInfo();
        resp.finishJson(new RetResult(Utility.ofMap("vips", vips)));
    }

    @HttpMapping(url = "/vip/awardlist", auth = true, comment = "转盘奖励选项")
    public void awardList(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final List<VipAwardInfo> awards = service.queryVipAwardInfo();
        final UserInfo user = userService.findUserInfo(userid);
        final int intday = Utility.today();
        final int vipid = user.getViplevel();
        final int runcount = vipid > 0 ? service.findVipInfo(vipid).getAwardcount() : 0;
        int jiangs = service.getCountVipAwardRecord(userid, intday);
        resp.finishJson(new RetResult(Utility.ofMap("awards", awards, "runcount", runcount, "runremain", (runcount > jiangs ? (runcount - jiangs) : 0))));
    }

    @HttpMapping(url = "/vip/checkaward", auth = true, comment = "判断转盘摇奖剩余次数")
    public void checkAward(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        resp.finishJson(service.checkAward(userid));
    }

    @HttpMapping(url = "/vip/runaward", auth = true, comment = "转盘摇奖")
    public void runAward(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        resp.finishJson(service.runAward(userid));
    }

}
