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
 *
 * @author zhangjx
 */
@WebServlet({"/ordertrade/*"})
public class OrderTradeServlet extends BaseServlet {

    @Resource
    private OrderTradeService service;

    //银行卡打款充值验证申请
    @HttpMapping(url = "/ordertrade/trade", auth = true)
    public void tradeCoin(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        OrderTradeRecord bean = req.getJsonParameter(OrderTradeRecord.class, "bean");
        if (bean == null || userid == 0) {
            resp.finishJson(RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL));
            return;
        }
        bean.setUserid(userid);
        resp.finishJson(service.tradeOrder(bean));
    }

    //查询银行卡打款充值记录
    @HttpMapping(url = "/ordertrade/queryrecords", auth = true)
    public void queryRecords(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        OrderTradeBean bean = req.getJsonParameter(OrderTradeBean.class, "bean");
        if (bean == null || userid == 0) {
            resp.finishJson(new RetResult(new Sheet()));
            return;
        }
        bean.setUserid(userid);
        resp.finishJson(new RetResult(service.queryOrderTradeRecordHis(bean, req.getFlipper(50))));
    }

}
