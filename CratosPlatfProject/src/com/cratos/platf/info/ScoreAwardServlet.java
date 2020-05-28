/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.BaseServlet;
import com.cratos.platf.base.UserInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.Utility;

/**
 * 幸运摇奖接口
 *
 * @author zhangjx
 */
@WebServlet({"/award/*"})
public class ScoreAwardServlet extends BaseServlet {

    @Resource
    protected ScoreAwardService service;

    //返回的数据结构为:   new RetResult({infos:{1:[],2:[],3:[]},costscores:[1000,2000,3000], totalscore:当前用户总积分数; remainscore:当前可用积分数; })
    @HttpMapping(url = "/award/enterAwardPanel", auth = true, comment = "进入摇奖界面")
    public void enterAwardPanel(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        //最低积分要求
        int[] costscores = new int[3];
        costscores[0] = service.awardScoreLevel1;
        costscores[1] = service.awardScoreLevel2;
        costscores[2] = service.awardScoreLevel3;

        //配置项
        Map infos = new HashMap<>();
        infos.put("1", service.awardInfoList1);
        infos.put("2", service.awardInfoList2);
        infos.put("3", service.awardInfoList3);

        //积分数
        Map<String, Long> scoreMap = service.getUserRecord(userid, true);

        //进入游戏
        service.enterAwardPanel(userid);
        resp.finishJson(new RetResult(Utility.ofMap(
            "infos", infos,
            "costscores", costscores,
            "totalscore", scoreMap.get("totalscore"),
            "remainscore", scoreMap.get("remainscore"),
            "todayscore", scoreMap.get("todayscore")
        )));
    }

    @HttpMapping(url = "/award/leaveAwardPanel", auth = true, comment = "离开摇奖界面")
    public void leaveAwardPanel(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        service.leaveAwardPanel(userid);
        resp.finishJson(RET_SUCCESS);
    }

    @HttpMapping(url = "/award/canAward", auth = true, comment = "是否可以幸运摇奖，主要用于大厅的摇奖图标是否显示红点")
    public void canAward(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        resp.finishJson(new RetResult(service.canAward(userid)));
    }

    @HttpMapping(url = "/award/myRecords", auth = true, comment = "返回当前用户摇奖记录")
    public void myRecords(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        resp.finishJson(new RetResult(service.myRecords(userid)));
    }

    @HttpMapping(url = "/award/allRecords", auth = true, comment = "返回全员摇奖记录")
    public void allRecords(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(new RetResult(service.allRecords()));
    }

    @HttpMapping(url = "/award/runJiang", auth = true, comment = "摇奖")
    public void runJiang(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        short awardlevel = req.getShortParameter("awardlevel", 0);
        resp.finishJson(service.runJiang(userid, awardlevel));
    }
}
