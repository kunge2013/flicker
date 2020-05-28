/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.profit;

import com.cratos.platf.base.*;
import java.io.IOException;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/profit/*"})
public class ProfitServlet extends BaseServlet {

    @Resource
    private ProfitService service;

    //查询
    @HttpMapping(url = "/profit/myprofit", auth = true)
    public void mycard(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        ProfitInfo info = service.getMyProfitInfo(userid);
        ProfitDayRecord record = service.getMyProfitDayRecord(userid);
        resp.finishJson(new RetResult(Utility.ofMap(
            "allprofitmoney", info == null ? 0L : info.getAllprofitmoney(),
            "remainmoney", info == null ? 0L : info.getRemainmoney(),
            "childcount", record == null ? 0L : record.getChildcount(),
            "childprofitmoney", record == null ? 0L : record.getChildprofitmoney(),
            "subchildcount", record == null ? 0L : record.getSubchildcount(),
            "subchildprofitmoney", record == null ? 0L : record.getSubchildprofitmoney())));
    }

    //查询领取奖励记录
    @HttpMapping(url = "/profit/queryrecords", auth = true)
    public void queryRecords(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        ProfitTradeBean bean = req.getJsonParameter(ProfitTradeBean.class, "bean");
        if (bean == null || userid == 0) {
            resp.finishJson(new RetResult(new Sheet()));
            return;
        }
        bean.setUserid(userid);
        resp.finishJson(new RetResult(service.queryProfitTradeRecordHis(bean, req.getFlipper(50))));
    }

    //领取奖励
    @HttpMapping(url = "/profit/trade", auth = true)
    public void tradeProfit(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        ProfitTradeRecord bean = req.getJsonParameter(ProfitTradeRecord.class, "bean");
        if (bean == null || userid == 0) {
            resp.finishJson(RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL));
            return;
        }
        bean.setUserid(userid);
        resp.finishJson(service.tradeProfit(bean));
    }
}
