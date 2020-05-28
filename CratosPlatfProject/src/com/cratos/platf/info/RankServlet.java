/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.*;
import java.io.IOException;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/rank/*"})
public class RankServlet extends BaseServlet {

    @Resource
    protected RankService service;

    @HttpMapping(url = "/rank/toplucks", auth = false, comment = "幸运排名榜")
    public void rankLucks(HttpRequest req, HttpResponse resp) throws IOException {
//        UserInfo user = req.currentUser();
//        if (user == null) user = userService.findUserInfo(currentUserid(req));
        resp.finishJson(new RetResult(Utility.ofMap("tops", service.queryRankLuckTop(), "myrecord", null)));
    }

    @HttpMapping(url = "/rank/topwins", auth = false, comment = "赢家排名榜")
    public void rankWins(HttpRequest req, HttpResponse resp) throws IOException {
//        UserInfo user = req.currentUser();
//        if (user == null) user = userService.findUserInfo(currentUserid(req));
        resp.finishJson(new RetResult(Utility.ofMap("tops", service.queryRankWinTop(), "myrecord", null)));
    }

}
