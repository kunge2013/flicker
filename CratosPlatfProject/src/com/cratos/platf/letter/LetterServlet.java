/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.letter;

import com.cratos.platf.base.*;
import java.io.IOException;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/letter/*"})
public class LetterServlet extends BaseServlet {

    @Resource
    private LetterService letterService;

    @HttpMapping(url = "/letter/query", auth = true)
    public void query(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = currentUserid(req);
        LetterBean bean = new LetterBean();
        bean.setStatus(new short[]{LetterRecord.LETTER_STATUS_UNREAD, LetterRecord.LETTER_STATUS_READED});
        bean.setUserid(userid);
        resp.finishJson(new RetResult(letterService.queryLetterRecord(bean, req.getFlipper(true, 50).sort("status ASC,createtime DESC"))));
    }

    @HttpMapping(url = "/letter/query2", auth = true)
    public void query2(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = currentUserid(req);
        LetterBean bean = new LetterBean();
        bean.setStatus(new short[]{LetterRecord.LETTER_STATUS_UNREAD});
        bean.setUserid(userid);
        resp.finishJson(new RetResult(letterService.queryLetterRecord(bean, req.getFlipper(true, 50).sort("status ASC,createtime DESC"))));
    }

    @HttpMapping(url = "/letter/read/", auth = true)
    public void read(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = currentUserid(req);
        LetterBean bean = new LetterBean();
        bean.setStatus(new short[]{LetterRecord.LETTER_STATUS_UNREAD});
        bean.setUserid(userid);
        String id = req.getRequstURILastPath();
        if (id.contains(",")) {
            bean.setLetterids(id.split(","));
        } else if (id.contains(";")) {
            bean.setLetterids(id.split(";"));
        } else {
            bean.setLetterid(id);
        }
        resp.finishJson(letterService.readLetterRecord(bean));
    }

    @HttpMapping(url = "/letter/delete/", auth = true)
    public void delete(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = currentUserid(req);
        LetterBean bean = new LetterBean();
        bean.setStatus(new short[]{LetterRecord.LETTER_STATUS_READED});
        bean.setUserid(userid);
        String id = req.getRequstURILastPath();
        if (id.contains(",")) {
            bean.setLetterids(id.split(","));
        } else if (id.contains(";")) {
            bean.setLetterids(id.split(";"));
        } else {
            bean.setLetterid(id);
        }
        resp.finishJson(letterService.deleteLetterRecord(bean));
    }

    @HttpMapping(url = "/letter/unreadcount", auth = true)
    public void count(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = currentUserid(req);
        resp.finishJson(letterService.getUnreadLetterCount(userid));
    }
}
