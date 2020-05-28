/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.base;

import com.cratos.platf.user.*;
import java.io.*;
import java.util.logging.*;
import javax.annotation.*;
import org.redkale.convert.json.*;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@HttpUserType(UserInfo.class)
public class BaseServlet extends HttpServlet {

    protected static final RetResult RET_SUCCESS = RetResult.success();

    public static final String COOKIE_AUTOLOGIN = "UNF";

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected final boolean fine = logger.isLoggable(Level.FINE);

    protected final boolean finer = logger.isLoggable(Level.FINER);

    protected final boolean finest = logger.isLoggable(Level.FINEST);

    protected final boolean loginfo = logger.isLoggable(Level.INFO);

    protected static final RetResult RET_UNLOGIN = RetCodes.retResult(RetCodes.RET_USER_UNLOGIN);

    protected static final RetResult RET_AUTHILLEGAL = RetCodes.retResult(RetCodes.RET_USER_AUTH_ILLEGAL);

    @Resource
    protected JsonConvert convert;

    @Resource
    private UserService service;

    @Override
    public void init(HttpContext context, AnyValue config) {
        super.init(context, config);
    }

    /**
     * Servlet的入口判断，一般用于全局的基本校验和预处理
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     *
     * @throws IOException
     */
    @Override
    public void preExecute(final HttpRequest request, final HttpResponse response) throws IOException {
        if (finer) response.recycleListener((req, resp) -> {  //记录处理时间比较长的请求
                long e = System.currentTimeMillis() - ((HttpRequest) req).getCreatetime();
                if (e > 1000) logger.finer("[" + Thread.currentThread().getName() + "] http-execute-cost-time: " + e + " ms. request = " + req + ", response = " + resp.getOutput());
            });
        request.setCurrentUser(currentUser(service, request));
        response.nextEvent();
    }

    /**
     * 校验用户的登录态
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     *
     * @throws IOException
     */
    @Override
    public void authenticate(HttpRequest request, HttpResponse response) throws IOException {
        UserInfo info = request.currentUser();
        if (info == null) {
            if (request.getIntHeader("Route-Userid", 0) == 0) {
                response.finishJson(RET_UNLOGIN);
                return;
            }
        } else if (!info.checkAuth(request.getModuleid(), request.getActionid())) {
            response.finishJson(RET_AUTHILLEGAL);
            return;
        }
        response.nextEvent();
    }

    /**
     * 获取当前用户对象，没有返回null, 提供static方法便于WebSocket进行用户态判断
     *
     * @param service UserService
     * @param req     HTTP请求对象
     *
     * @return UserInfo
     */
    public static final UserInfo currentUser(UserService service, HttpRequest req) {
        UserInfo user = (UserInfo) req.currentUser();
        if (user != null) return user;
        String sessionid = req.getHeader("JSESSIONID");
        if (sessionid == null || sessionid.isEmpty()) sessionid = req.getParameter("JSESSIONID");
        if (sessionid != null && !sessionid.isEmpty()) user = service.current(sessionid);
        return user;
    }

    protected int currentUserid(HttpRequest req) {
        UserInfo user = (UserInfo) req.currentUser();
        if (user != null) return user.userid;
        int userid = req.getIntHeader("Route-Userid", 0);
        return userid == 0 ? req.getIntParameter("Route-Userid", 0) : userid;
    }
}
