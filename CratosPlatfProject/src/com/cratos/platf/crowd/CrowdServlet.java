/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.crowd;

import com.cratos.platf.base.BaseServlet;
import static com.cratos.platf.crowd.CrowdApplyDayRecord.CROWD_APPLY_STATUS_DONE;
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
@WebServlet({"/crowd/*"})
public class CrowdServlet extends BaseServlet {

    @Resource
    protected CrowdService service;

    @Resource
    protected UserService userService;

    @HttpMapping(url = "/crowd/query", auth = true, comment = "全民打卡查询")
    public void query(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final CrowdPoolRecord todaydata = service.getTodayPoolRecord();
        int todaycount = service.getApplyCount(userid, Utility.today());
        List<Short> yesterStatuses = service.queryApplyStatus(userid, Utility.yesterday());
        boolean dakaed = yesterStatuses.size() > 0 && yesterStatuses.get(0) == CROWD_APPLY_STATUS_DONE;
        resp.finishJson(new RetResult(Utility.ofMap("poolrecord", todaydata, "todayapplycount", todaycount, "yesterdayapplycount", yesterStatuses.size(), "dakaed", dakaed)));
    }

    @HttpMapping(url = "/crowd/apply", auth = true, comment = "报名")
    public void apply(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        resp.finishJson(service.apply(userid));
    }

    @HttpMapping(url = "/crowd/daka", auth = true, comment = "打卡")
    public void daka(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        resp.finishJson(service.daka(userid));
    }
}
