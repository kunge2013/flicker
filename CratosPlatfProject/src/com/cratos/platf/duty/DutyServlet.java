/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.duty;

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
@WebServlet({"/duty/*"})
public class DutyServlet extends BaseServlet {

    @Resource
    protected DutyService service;

    @Resource
    protected UserService userService;

    @HttpMapping(url = "/duty/query", auth = true, comment = "签到奖励表")
    public void query(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final List<DutyReward> rewards = service.queryDutyReward();
        int mydutyseries = 0;
        if (!rewards.isEmpty()) {
            DutyAccount account = service.findDutyAccount(userid);
            if (account != null) {
                if (account.getLastdutyday() == Utility.today() || account.getLastdutyday() == Utility.yesterday()) {
                    mydutyseries = account.getLastdutyseries();
                } else if (account.getLastdutyday() >= rewards.get(rewards.size() - 1).getDutyindex()) {
                    mydutyseries = account.getLastdutyseries();
                }
            }
        }
        resp.finishJson(new RetResult(Utility.ofMap("rewards", rewards, "mydutyseries", mydutyseries)));
    }

    @HttpMapping(url = "/duty/book", auth = true, comment = "签到")
    public void book(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        resp.finishJson(service.bookToday(userid));
    }

    @HttpMapping(url = "/duty/checktoday", auth = true, comment = "检测当日是否可签到")
    public void checkToday(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        resp.finishJson(service.checkToday(userid));
    }

    @HttpMapping(url = "/duty/awardlist", auth = true, comment = "转盘奖励选项")
    public void awardList(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final List<DutyAwardInfo> awards = service.queryDutyAwardInfo();
        final UserInfo user = userService.findUserInfo(userid);
        final int jiangcount = user.getLoginseries() > 3 ? 2 : 1;
        final int intday = Utility.today();
        int jiangs = service.getCountDutyAwardRecord(userid, intday);
        resp.finishJson(new RetResult(Utility.ofMap("awards", awards, "runcount", jiangcount, "runremain", (jiangcount > jiangs ? (jiangcount - jiangs) : 0), "myloginseries", user.getLoginseries())));
    }

    @HttpMapping(url = "/duty/runaward", auth = true, comment = "转盘摇奖")
    public void runAward(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        resp.finishJson(service.runAward(userid));
    }

}
