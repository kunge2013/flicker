/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.mission;

import com.cratos.platf.base.*;
import com.cratos.platf.liveness.*;
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
@WebServlet({"/mission/*"})
public class MissionServlet extends BaseServlet {

    @Resource
    protected MissionService service;

    @Resource
    protected LivenessService livenessService;

    @Resource
    protected UserService userService;

    @HttpMapping(url = "/mission/check", auth = true, comment = "判断是否有领取的任务列表")
    public void check(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final int intday = Utility.today();
        int count = service.checkMissionDayRecord(userid, intday);
        if (count < 1) count = livenessService.checkLivenessRewardRecord(userid, intday);
        if (count < 1) count = service.checkMissionOnceRecord(userid);
        resp.finishJson(RetResult.success(count));
    }

    @HttpMapping(url = "/mission/reachday/", auth = true, comment = "领取活跃任务")
    public void reachday(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final int intday = Utility.today();
        final String recordid = req.getRequstURILastPath();
        if (!recordid.endsWith("-" + intday)) {
            resp.finishJson(RetCodes.retResult(RetCodes.RET_MISSION_STATUS_ILLEGAL));
        } else {
            resp.finishJson(service.reachMissionDayRecord(userid, recordid));
        }
    }

    @HttpMapping(url = "/mission/reachonce/", auth = true, comment = "领取新手任务")
    public void reachonce(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final String recordid = req.getRequstURILastPath();
        resp.finishJson(service.reachMissionOnceRecord(userid, recordid));
    }

    @HttpMapping(url = "/mission/livenessreward/", auth = true, comment = "领取活跃度奖励")
    public void livenessreward(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final int intday = Utility.today();
        final String recordid = req.getRequstURILastPath();
        if (!recordid.endsWith("-" + intday)) {
            resp.finishJson(RetCodes.retResult(RetCodes.RET_MISSION_STATUS_ILLEGAL));
        } else {
            resp.finishJson(livenessService.reachLivenessRewardRecord(userid, recordid));
        }
    }

    @HttpMapping(url = "/mission/records", auth = true, comment = "当前用户的任务列表")
    public void records(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        final int intday = Utility.today();
        int daycount = 0;
        List<MissionDayRecord> dayRecords = service.queryMissionDayRecord(userid, intday);
        for (MissionDayRecord r : dayRecords) {
            if (r.getMissionstatus() == MissionRecord.MISSION_STATUS_REACH) daycount++;
        }
        List<LivenessRewardDayRecord> rewards = livenessService.queryLivenessRewardRecord(userid, intday);
        for (LivenessRewardDayRecord r : rewards) {
            if (r.getLivenessstatus() == LivenessRewardDayRecord.LIVENESS_STATUS_REACH) daycount++;
        }
        int oncecount = 0;
        List<MissionOnceRecord> onceRecords = service.queryMissionOnceRecord(userid);
        for (MissionOnceRecord r : onceRecords) {
            if (r.getMissionstatus() == MissionRecord.MISSION_STATUS_REACH) oncecount++;
        }
        long userliveness = userService.findUserLiveness(userid);
        resp.finishJson(new RetResult(Utility.ofMap("daymissions", dayRecords,
            "dayreachcount", daycount,
            "oncemissions", onceRecords,
            "oncereachcount", oncecount,
            "rewards", rewards, "userliveness", userliveness)));
    }
}
