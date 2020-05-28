/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.*;
import java.io.IOException;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.Sheet;

/**
 * 玩家兑换接口
 *
 * @author zhangjx
 */
@WebServlet({"/cointrade/*"})
public class CoinTradeServlet extends BaseServlet {

    @Resource
    private CoinTradeService service;

    //兑换
    @HttpMapping(url = "/cointrade/trade", auth = true)
    public void tradeCoin(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        CoinTradeRecord bean = req.getJsonParameter(CoinTradeRecord.class, "bean");
        if (bean == null || userid == 0) {
            resp.finishJson(RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL));
            return;
        }
        bean.setUserid(userid);
        resp.finishJson(service.tradeCoin(bean));
    }

    //查询兑换记录
    @HttpMapping(url = "/cointrade/queryrecords", auth = true)
    public void queryRecords(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        CoinTradeBean bean = req.getJsonParameter(CoinTradeBean.class, "bean");
        if (bean == null || userid == 0) {
            resp.finishJson(new RetResult(new Sheet()));
            return;
        }
        bean.setUserid(userid);
        resp.finishJson(new RetResult(service.queryCoinTradeRecordHis(bean, req.getFlipper(50))));
    }

}
