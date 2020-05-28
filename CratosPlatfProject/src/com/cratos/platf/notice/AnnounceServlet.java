/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.notice;

import com.cratos.platf.base.*;
import java.io.IOException;
import java.util.*;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.source.Flipper;
import org.redkale.util.Sheet;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/announces/*"})
public class AnnounceServlet extends BaseServlet {

    private final Flipper onepage = new Flipper(20);

    @Resource
    private AnnounceService service;

    @HttpMapping(url = "/announces/query", auth = true, cacheseconds = 60)
    public void query(HttpRequest req, HttpResponse resp) throws IOException {
        AnnounceBean bean = new AnnounceBean();
        bean.setStatus(BaseEntity.STATUS_NORMAL);
        long now = System.currentTimeMillis();
        bean.setStarttime(now);
        bean.setEndtime(now);
        Sheet<Announcement> sheet = service.queryAnnouncement(bean, onepage);
        List<Announcement> rs = new ArrayList<>();
        Announcement loginAnnounce = service.getLoginAnnouncement();
        if (loginAnnounce != null) rs.add(loginAnnounce);
        sheet.forEach(announce -> {
            if (announce.getType() != Announcement.ANNOUNCE_TYPE_LOGIN) rs.add(announce);
        });
        resp.finishJson(Sheet.asSheet(rs));
    }

    @HttpMapping(url = "/announces/plains", auth = true)
    public void plains(HttpRequest req, HttpResponse resp) throws IOException {
        AnnounceBean bean = new AnnounceBean();
        bean.setStatus(BaseEntity.STATUS_NORMAL);
        bean.setType(Announcement.ANNOUNCE_TYPE_PLAIN);
        long now = System.currentTimeMillis();
        bean.setStarttime(now);
        bean.setEndtime(now);
        Sheet<Announcement> sheet = service.queryAnnouncement(bean, onepage);
        resp.finishJson(new RetResult(sheet));
    }
}
