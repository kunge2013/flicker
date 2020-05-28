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

/**
 *
 * @author zhangjx
 */
@WebServlet({"/card/*"})
public class CardServlet extends BaseServlet {

    @Resource
    private CardService cardService;

    //查询个人卡信息
    @HttpMapping(url = "/card/mycard", auth = true)
    public void mycard(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo curr = req.currentUser();
        resp.finishJson(new RetResult(cardService.findCardInfo(curr == null ? currentUserid(req) : curr.getUserid())));
    }

    //修改个人卡信息
    @HttpMapping(url = "/card/update", auth = true)
    public void updateCard(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo curr = req.currentUser();
        CardInfo bean = req.getJsonParameter(CardInfo.class, "bean");
        bean.setUserid(curr == null ? currentUserid(req) : curr.getUserid());
        resp.finishJson(cardService.updateCardInfo(bean));
    }
}
