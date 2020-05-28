/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.route.gateway;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.logging.*;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.util.Utility;

/**
 * 无需登录态的对外接口
 *
 * @author zhangjx
 */
@WebServlet({"/user/*", "/pay/*", "/appinfo/*"})
public class GateWayServlet extends HttpServlet {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    @Resource
    protected GateWayService service;

    //@Resource
    //protected CaptchaService captchaService;
    //用户非登录态请求
    @HttpMapping(url = "/user/ping", auth = false)
    public void ping(HttpRequest req, HttpResponse resp) throws IOException {
        resp.setContentType("application/json; charset=utf-8").finish("{\"success\":true}");
    }

    //用户非登录态请求
    @HttpMapping(url = "/pay/", auth = false)
    public void payAjax(HttpRequest req, HttpResponse resp) throws IOException {
        ajax("platf_pay", req, resp);
    }

    //用户非登录态请求
    @HttpMapping(url = "/user/", auth = false)
    public void userAjax(HttpRequest req, HttpResponse resp) throws IOException {
        ajax("platf_user", req, resp);
    }

    //用户非登录态请求
    @HttpMapping(url = "/appinfo/", auth = false)
    public void appinfoAjax(HttpRequest req, HttpResponse resp) throws IOException {
        ajax("platf_info", req, resp);
    }

    private void ajax(final String moduleName, HttpRequest req, HttpResponse resp) throws IOException {
        if (logger.isLoggable(Level.FINEST)) logger.finest("" + req);
        String requri = req.getRequestURI();
//        String captchakey = req.getParameter("captchakey");
//        boolean needcaptcha = !requri.contains("/checkmobcode")
//            && !requri.contains("/kefu") && !requri.contains("/rank");
//        if (!winos && needcaptcha && captchakey == null) {
//            resp.finish(CaptchaService.CAPTCHA_RET_JSON);
//            return;
//        }
//        if (!winos && needcaptcha && !captchaService.check(captchakey, req.getParameter("captchacode")).join()) {
//            resp.finish(CaptchaService.CAPTCHA_RET_JSON);
//            return;
//        }
        final ModuleNodeAddress mnode = this.service.loadHttpAddress(moduleName, "0", null);
        InetSocketAddress addr = mnode.address;
        String url = "http://" + addr.getHostString() + ":" + addr.getPort() + requri + req.getParametersToString("?");
        if (logger.isLoggable(Level.FINEST)) logger.finest("platf.url = " + url);
        String content = null;
        String body = req.getBodyUTF8();
        if (body != null && body.isEmpty()) body = null;
        try {
            mnode.semaphore.acquire();
        } catch (InterruptedException ie) {
        }
        try {
            Map<String, String> headers = Utility.ofMap("App-Agent", req.getHeader("App-Agent", req.getHeader("User-Agent", "Unknown Agent")));
            if (req.getHost() != null) headers.put("X-RemoteHost", req.getHost());
            if (req.getRemoteAddr() != null) headers.put("X-RemoteAddress", req.getRemoteAddr());
            if (req.getContentType() != null) headers.put("Content-Type", req.getContentType());
            content = Utility.getHttpContent(url, 6000, headers, body);
        } finally {
            mnode.semaphore.release();
        }
        resp.finish(content);
    }

}
