/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.*;
import com.cratos.platf.info.*;
import com.cratos.platf.notice.RandomCode;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpCookie;
import java.util.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.convert.json.*;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.*;
import org.redkalex.weixin.WeiXinMPService;

/**
 * 用户模块的Servlet
 *
 * @author zhangjx
 */
@WebServlet({"/user/*"})
public class UserServlet extends BaseServlet {

    private static final Type TYPE_RETRESULT_PLAYER = new TypeToken<RetResult<Player>>() {
    }.getType();

    @Resource
    private UserService service;

    @Resource
    private DictService dictService;

    //用于微信登录
    @Resource
    private WeiXinMPService wxService;

    @Resource
    private JsonConvert userDetailConvert;

    @Resource
    private JsonConvert loginConvert;

    @Override
    public void init(HttpContext context, AnyValue config) {
        JsonFactory factory = JsonFactory.root().createChild();
        //当前用户查看自己的用户信息时允许输出隐私信息
        factory.register(UserDetail.class, false, "mobile", "email", "apptoken");
        userDetailConvert = factory.getConvert();

        //用户登录信息
        factory = JsonFactory.create();
        factory.register(UserInfo.class, false, "currgame", "mobile2", "shenfenno2", "shenfenname2");
        factory.register(UserDetail.class, false, "currgame", "mobile2", "shenfenno2", "shenfenname2");
        loginConvert = factory.getConvert();

        super.init(context, config);
    }

    @Override
    public void postStart(HttpContext context, AnyValue config) {
        service.postStart();
    }

    //客服联系方式
    @HttpMapping(url = "/user/config", auth = false)
    public void config(HttpRequest req, HttpResponse resp) throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("kefuURL", dictService.findDictValue(DictInfo.PLATF_KEFU_URL, ""));
        map.put("kefuQQ", dictService.findDictValue(DictInfo.PLATF_KEFU_QQ, ""));
        map.put("kefuWX", dictService.findDictValue(DictInfo.PLATF_KEFU_WX, ""));
        map.put("kefuTEL", dictService.findDictValue(DictInfo.PLATF_KEFU_TEL, ""));
        map.put("apiHost", dictService.findDictValue(DictInfo.PLATF_API_HOST, ""));
        map.put("appHost", dictService.findDictValue(DictInfo.PLATF_APP_HOST, ""));
        map.put("impayURL", dictService.findDictValue(DictInfo.PLATF_IMPAY_URL, ""));
        map.put("payAllSubTypes", dictService.findDictValue(DictInfo.PLATF_PAY_ALLSUBTYPES, ""));
        map.put("paySubTypes", dictService.findDictValue(DictInfo.PLATF_PAY_SUBTYPES, ""));
        map.put("orderTradeBankAccount", dictService.findDictValue(DictInfo.PLATF_ORDERTRADE_BANKACCOUNT, ""));
        map.put("orderTradeBankUser", dictService.findDictValue(DictInfo.PLATF_ORDERTRADE_BANKUSER, ""));
        map.put("orderTradeBankName", dictService.findDictValue(DictInfo.PLATF_ORDERTRADE_BANKNAME, ""));
        resp.finishJson(map);
    }

    //用户注销
    @HttpMapping(url = "/user/logout", auth = false)
    public void logout(HttpRequest req, HttpResponse resp) throws IOException {
        String sessionid = req.getHeader("JSESSIONID");
        if (sessionid == null || sessionid.isEmpty()) sessionid = req.getParameter("JSESSIONID");
        if (sessionid != null) service.logout(sessionid);
        //if (clubService != null) clubService.leaveClubPanel(currentUserid(req));
        HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, "");
        cookie.setPath("/");
        cookie.setMaxAge(1);
        resp.addCookie(cookie);
        resp.finishJson(RET_SUCCESS);
    }

    @HttpMapping(url = "/user/updatewxid", auth = true)
    public void updateWxunionid(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag
        if (finest) logger.finest("/user/updatewxid :  " + code + "," + state);
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateWxunionid(user, code));
    }

    @HttpMapping(url = "/user/webupdatewxid", auth = true)
    public void webUpdateWxunionid(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag
        if (finest) logger.finest("/user/webupdatewxid :  " + code + "," + state);
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        service.updateWxunionid(user, code);
        resp.setHeader("Location", req.getParameter("url", "/"));
        resp.finish(302, null);
    }

    //需要在 “开发 - 接口权限 - 网页服务 - 网页帐号 - 网页授权获取用户基本信息”的配置选项中，修改授权回调域名
    @HttpMapping(url = "/user/webwxopenid", auth = false)
    public void webWxopenid(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        if (finest) logger.finest("/user/webwxopenid :  " + req);
        Map<String, String> rr = wxService.getMPUserTokenByCode(code);
        resp.setHeader("Location", req.getParameter("url", "/"));
        resp.finish(302, null);
    }

    @HttpMapping(url = "/user/check", auth = true)
    public void check(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(RET_SUCCESS);
    }

    /**
     * 微信登陆 https://open.weixin.qq.com/connect/qrconnect?appid=wx微信ID&redirect_uri=xxxxx&response_type=code&scope=snsapi_login&state=wx微信ID_1#wechat_redirect
     * 接收两种形式：
     * WEB端微信登录： /user/wxlogin?code=XXXXXX&state=wx微信ID_1&apptoken=XXX
     * APP端微信登录: /user/wxlogin?openid=XXXX&state=1&access_token=XXX&apptoken=XXX
     * <p>
     * @param req
     * @param resp
     *
     * @throws IOException
     */
    @HttpMapping(url = "/user/wxlogin", auth = false)
    public void wxlogin(HttpRequest req, HttpResponse resp) throws IOException {
        if (finest) logger.log(Level.FINEST, "微信登陆: req = " + req);
        Map<String, String> parambean = null;
        if (req.getParameter("bean") != null) parambean = req.getJsonParameter(JsonConvert.TYPE_MAP_STRING_STRING, "bean");
        String code = req.getParameter("code", parambean == null ? "" : parambean.getOrDefault("code", "")).replace("\"", "").replace("'", "");
        String state = req.getParameter("state", parambean == null ? "" : parambean.getOrDefault("state", ""));  //state值格式: appid_autoregflag

        String access_token = req.getParameter("access_token", parambean == null ? null : parambean.get("access_token"));
        String openid = req.getParameter("openid", parambean == null ? null : parambean.get("openid"));

        int pos = state.indexOf('_');
        String appid = pos > 0 ? state.substring(0, pos) : state;
        if (appid.length() < 2) appid = "";
        //final boolean wxbrowser = req.getHeader("User-Agent", "").contains("MicroMessenger");
        LoginWXBean bean = new LoginWXBean();
        bean.setCode(code);
        { //WEB方式
            bean.setAppid(appid);
        }
        { //APP方式
            bean.setAccesstoken(access_token);
            bean.setOpenid(openid);
        }
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        bean.setUserid(user == null ? 0 : user.getUserid());
        bean.setAutoreg(true);
        bean.setApposid(req.getParameter("apposid", parambean == null ? "" : parambean.getOrDefault("apposid", "")));
        bean.setNetmode(req.getParameter("netmode", parambean == null ? "" : parambean.getOrDefault("netmode", "")));
        bean.setAppos(req.getParameter("appos", parambean == null ? "" : parambean.getOrDefault("appos", "")));
        bean.setApptoken(req.getParameter("apptoken", parambean == null ? "" : parambean.getOrDefault("apptoken", "")));
        bean.setLoginaddr(req.getRemoteAddr());
        bean.setLoginagent(req.getHeader("App-Agent", req.getHeader("User-Agent")));
        if (bean.getApposid().isEmpty() && bean.getLoginagent().contains("/")) {
            bean.setApposid(bean.getLoginagent().substring(0, bean.getLoginagent().indexOf('/')));
        }
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> rr = service.wxlogin(bean);
        if (rr.isSuccess()) {
            UserInfo info = rr.getResult();
            int age = 3 * 24 * 60 * 60;
            String key = (bean.emptyApptoken() ? "" : (bean.getApptoken() + "#")) + info.getUser36id() + "$2" + info.getWxunionid() + "?" + age + "-" + System.currentTimeMillis();
            String unf = UserService.encryptAES(key);
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, unf);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(age);
            resp.addCookie(cookie);
            rr.attach("sessionid", bean.getSessionid()).attach("unf", unf);
        }
        resp.finishJson(loginConvert, rr);
//        if (access_token == null || access_token.isEmpty()) { //WEB登录
//            resp.setHeader("Location", req.getParameter("url", "/"));
//            resp.finish(302, null);
//        } else { //APP 模式
//            resp.finishJson(rr);
//        }
    }

    @HttpMapping(url = "/user/qqlogin", auth = false)
    public void qqlogin(HttpRequest req, HttpResponse resp) throws IOException {
        String access_token = req.getParameter("access_token");
        String openid = req.getParameter("openid");
        if (finest) logger.finest("/user/qqlogin :  " + openid + "," + access_token);
        LoginQQBean bean = new LoginQQBean();
        bean.setAccesstoken(access_token);
        bean.setOpenid(openid);
        bean.setLoginaddr(req.getRemoteAddr());
        bean.setLoginagent(req.getHeader("App-Agent", req.getHeader("User-Agent")));
        if (bean.getApposid().isEmpty() && bean.getLoginagent().contains("/")) {
            bean.setApposid(bean.getLoginagent().substring(0, bean.getLoginagent().indexOf('/')));
        }
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> rr = service.qqlogin(bean);
        if (rr.isSuccess()) {
            UserInfo info = rr.getResult();
            int age = 3 * 24 * 60 * 60;
            String key = (bean.emptyApptoken() ? "" : (bean.getApptoken() + "#")) + info.getUser36id() + "$3" + info.getQqunionid() + "?" + age + "-" + System.currentTimeMillis();
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, UserService.encryptAES(key));
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(age);
            resp.addCookie(cookie);
        }
        if (access_token == null || access_token.isEmpty()) {
            resp.setHeader("Location", req.getParameter("url", "/"));
            resp.finish(302, null);
        } else { //APP 模式
            resp.finishJson(loginConvert, rr);
        }
    }

    /**
     * 用户登陆
     *
     * @param req
     * @param resp
     *
     * @throws IOException
     */
    @HttpMapping(url = "/user/login", auth = false)
    public void login(HttpRequest req, HttpResponse resp) throws IOException {
        logger.log(Level.FINEST, "登陆信息: " + req.getParametersToString());
        LoginBean bean = req.getJsonParameter(LoginBean.class, "bean");
        if (bean == null) bean = new LoginBean();

        if (!bean.emptyPassword()) bean.setPassword(UserService.secondPasswordMD5(bean.getPassword()));
        bean.setLoginagent(req.getHeader("App-Agent", req.getHeader("User-Agent", "")));
        if (bean.getApposid().isEmpty() && bean.getLoginagent().contains("/")) {
            bean.setApposid(bean.getLoginagent().substring(0, bean.getLoginagent().indexOf('/')));
        }
        bean.setLoginaddr(req.getRemoteAddr());
        RetResult<UserInfo> result = null;
        String oldsessionid = req.getHeader("JSESSIONID");
        if (oldsessionid == null || oldsessionid.isEmpty()) oldsessionid = req.getParameter("JSESSIONID");
        if (bean.isAutologin()) oldsessionid = null;
        logger.log(Level.FINEST, "登陆参数: " + bean);
        if (oldsessionid != null && !oldsessionid.isEmpty()) {
            UserInfo user = service.current(oldsessionid);
            if (user != null) {
                bean.setSessionid(oldsessionid);
                result = new RetResult<>(user);
            }
        }
        if (bean.getSessionid() == null || bean.getSessionid().isEmpty()) {
            bean.setSessionid(req.changeSessionid());
        }
        String oldunf = req.getHeader(COOKIE_AUTOLOGIN, "");
        if (oldunf.indexOf(',') > 0) oldunf = oldunf.substring(0, oldunf.indexOf(','));

        if (bean.isAutologin() && !oldunf.isEmpty()) {
            String oldaccount = bean.getAccount();
            bean.setAccount("");
            bean.setCookieinfo(oldunf);
            result = service.login(bean);
            if (result.isSuccess()) {
                bean.setCookieinfo("");
            } else {
                bean.setAccount(oldaccount);
            }
        }
        if (result == null || !result.isSuccess()) {
            result = service.login(bean);
        }
        if (!bean.isAutologin() && !result.isSuccess()) {
            if (!oldunf.isEmpty()) {
                bean.setAccount("");
                bean.setCookieinfo(oldunf);
                result = service.login(bean);
                if (result.isSuccess()) bean.setCookieinfo("");
            }
        }
        String unf = "";
        if (result.isSuccess()) {
            UserInfo info = result.getResult();
            int age = 3 * 24 * 60 * 60;
            String idstr = "$0" + bean.getPassword();
            if (!info.getWxunionid().isEmpty()) {
                idstr = "$2" + info.getWxunionid();
            } else if (!info.getQqunionid().isEmpty()) {
                idstr = "$3" + info.getQqunionid();
            } else if (!info.getCxunionid().isEmpty()) {
                idstr = "$4" + info.getCxunionid();
            } else if (info.getAccount().length() > 32) {
                idstr = "$1" + info.getAccount();
            }
            String key = (bean.emptyApptoken() ? "" : (bean.getApptoken() + "#")) + info.getUser36id()
                + idstr + "?" + age + "-" + System.currentTimeMillis();
            unf = UserService.encryptAES(key);
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, unf);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(age);
            resp.addCookie(cookie);
        }
        if (result.isSuccess()) result.attach("sessionid", bean.getSessionid()).attach("unf", unf);
        logger.log(Level.FINEST, "登陆结果: " + loginConvert.convertTo(result));
        resp.finishJson(loginConvert, result);
    }

    @HttpMapping(url = "/user/signup", auth = false)
    public void signup(HttpRequest req, HttpResponse resp) throws IOException {
        long s = System.currentTimeMillis();
        Map<String, String> map = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, req.getParameter("bean"));
        RetResult<RandomCode> ret = null;
        String beanaccount;
        UserDetail bean = new UserDetail();
        boolean accountable = false;
        if (map.containsKey("mobile")) {
            bean.setMobile(map.get("mobile"));
            beanaccount = bean.getMobile();
            ret = service.checkRandomCode(bean.getMobile(), map.get("vercode"), RandomCode.TYPE_SMSREG);
            if (!ret.isSuccess()) {
                resp.finishJson(ret);
                return;
            }
        } else if (map.containsKey("email")) {
            bean.setEmail(map.get("email"));
            beanaccount = bean.getEmail();
        } else {
            beanaccount = map.getOrDefault("account", "");
            accountable = true;
        }
        if (!map.getOrDefault("account", "").isEmpty()) {
            beanaccount = map.getOrDefault("account", "");
            accountable = true;
        }
        if (accountable) bean.setAccount(beanaccount);
        bean.setGender(Short.parseShort(map.getOrDefault("gender", "0")));
        bean.setUsername(map.getOrDefault("username", ""));
        bean.setApposid(map.getOrDefault("apposid", ""));
        bean.setAppos(map.getOrDefault("appos", ""));
        bean.setApptoken(map.getOrDefault("apptoken", ""));
        bean.setPassword(map.getOrDefault("password", ""));
        bean.setRegaddr(req.getRemoteAddr());
        bean.setRegagent(req.getHeader("App-Agent", req.getHeader("User-Agent", "")));
        if (bean.getApposid().isEmpty() && bean.getRegagent().contains("/")) {
            bean.setApposid(bean.getRegagent().substring(0, bean.getRegagent().indexOf('/')));
        }
        if (logger.isLoggable(Level.FINEST)) logger.finest("signup.req = " + req + ", map = " + map + ", bean = " + bean);
        final String reqpwd = bean.getPassword();
        if (reqpwd != null && !reqpwd.isEmpty()) bean.setPassword(UserService.secondPasswordMD5(bean.getPassword()));
        RetResult<UserInfo> rr = service.register(bean, false);
        if (rr.isSuccess()) {
            if (ret != null) {
                ret.getResult().setUserid(rr.getResult().getUserid());
                service.removeRandomCode(ret.getResult());
            }
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(beanaccount);
            loginbean.setApposid(bean.getApposid());
            loginbean.setApptoken(bean.getApptoken());
            loginbean.setPassword(UserService.secondPasswordMD5(reqpwd));
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginagent(req.getHeader("App-Agent", req.getHeader("User-Agent")));
            if (map.containsKey("cacheday")) loginbean.setCacheday(Integer.parseInt(map.getOrDefault("cacheday", "0")));
            loginbean.setLoginaddr(req.getRemoteAddr());
            rr = service.login(loginbean);
        }
        long e = System.currentTimeMillis() - s;
        if (e > 2000) logger.warning("/user/signup cost " + e / 1000.0 + " seconds " + bean);
        resp.finishJson(rr);
    }

    /**
     * 忘记重置密码
     *
     * @param req
     * @param resp
     *
     * @throws IOException
     */
    @HttpMapping(url = "/user/resetpwd", auth = false)
    public void updatepwd2(HttpRequest req, HttpResponse resp) throws IOException {
        updatepwd(req, resp);
    }

    /**
     * 修改密码
     *
     * @param req
     * @param resp
     *
     * @throws IOException
     */
    @HttpMapping(url = "/user/updatepwd")
    public void updatepwd(HttpRequest req, HttpResponse resp) throws IOException {
        UserPwdBean bean = req.getJsonParameter(UserPwdBean.class, "bean");
        UserInfo curr = req.currentUser();
        if (curr == null) curr = service.findUserInfo(currentUserid(req));
        String sessionid = req.getHeader("JSESSIONID");
        if (sessionid == null || sessionid.isEmpty()) sessionid = req.getParameter("JSESSIONID");

        if (curr != null) bean.setSessionid(sessionid);
        bean.setUser(curr);
        RetResult<UserInfo> result = service.updatePwd(bean);
        if (result.isSuccess() && curr == null) { //找回的密码
            curr = result.getResult();
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(curr.getEmail().isEmpty() ? curr.getMobile() : curr.getEmail());
            loginbean.setPassword(UserService.secondPasswordMD5(bean.getNewpwd()));
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginagent(req.getHeader("App-Agent", req.getHeader("User-Agent")));
            loginbean.setLoginaddr(req.getRemoteAddr());
            if (loginbean.getApposid().isEmpty() && loginbean.getLoginagent().contains("/")) {
                loginbean.setApposid(loginbean.getLoginagent().substring(0, loginbean.getLoginagent().indexOf('/')));
            }
            result = service.login(loginbean);
        }
        String autologin = req.getCookie(COOKIE_AUTOLOGIN);
        if (result.isSuccess() && autologin != null) {
            autologin = UserService.decryptAES(autologin);
            if (autologin.contains("$0")) { //表示COOKIE_AUTOLOGIN 为密码类型存储
                String newpwd = UserService.secondPasswordMD5(bean.getNewpwd());
                int wen = autologin.indexOf('?');
                int mei = autologin.indexOf('$');
                String key = autologin.substring(0, mei + 2) + newpwd + autologin.substring(wen);
                HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, UserService.encryptAES(key));
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                String time = autologin.substring(wen + 1);
                int fen = time.indexOf('-');
                int age = Integer.parseInt(time.substring(0, fen)); //秒数
                long point = Long.parseLong(time.substring(fen + 1)); //毫秒数
                cookie.setMaxAge(age - (System.currentTimeMillis() - point) / 1000);
                resp.addCookie(cookie);
            }
        }
        resp.finishJson(result);
    }

    //校验虚拟银行密码
    @HttpMapping(url = "/user/checkbankpwd")
    public void checkBankPwd(HttpRequest req, HttpResponse resp) throws IOException {
        UserPwdBean bean = req.getJsonParameter(UserPwdBean.class, "bean");
        UserInfo curr = req.currentUser();
        if (curr == null) curr = service.findUserInfo(currentUserid(req));
        String sessionid = req.getHeader("JSESSIONID");
        if (sessionid == null || sessionid.isEmpty()) sessionid = req.getParameter("JSESSIONID");
        if (curr != null) bean.setSessionid(sessionid);
        bean.setUser(curr);
        resp.finishJson(service.checkBankPwd(bean));
    }

    //更新虚拟银行密码
    @HttpMapping(url = "/user/updatebankpwd")
    public void updateBankPwd(HttpRequest req, HttpResponse resp) throws IOException {
        UserPwdBean bean = req.getJsonParameter(UserPwdBean.class, "bean");
        UserInfo curr = req.currentUser();
        if (curr == null) curr = service.findUserInfo(currentUserid(req));
        String sessionid = req.getHeader("JSESSIONID");
        if (sessionid == null || sessionid.isEmpty()) sessionid = req.getParameter("JSESSIONID");
        if (curr != null) bean.setSessionid(sessionid);
        bean.setUser(curr);
        resp.finishJson(service.updateBankPwd(bean));
    }

    //更新用户手机号码
    @HttpMapping(url = "/user/updatemobile")
    public void updatemobile(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateMobile(user.getUserid(), req.getParameter("mobile"), req.getParameter("vercode"), req.getParameter("precode")));
    }

    @HttpMapping(url = "/user/updatestatus", auth = false)
    public void updatestatus(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.updateStatus(req.getIntParameter("userid", 100), req.getShortParameter("status", 10)));
    }

    //更新用户昵称
    @HttpMapping(url = "/user/updateusername", auth = true)
    public void updateUsername(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateUsername(user.getUserid(), req.getParameter("newusername")));
    }

    //更新用户头像
    @HttpMapping(url = "/user/updatefaceandgender", auth = true)
    public void updateFaceAndGender(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateFaceAndGender(user.getUserid(), req.getParameter("face"), req.getShortParameter("gender", 0)));
    }

    //更新用户头像
    @HttpMapping(url = "/user/updateface", auth = true)
    public void updateFace(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateFace(user.getUserid(), req.getParameter("face")));
    }

    //更新性别
    @HttpMapping(url = "/user/updategender/", auth = true)
    public void updateGender(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateGender(user.getUserid(), Short.parseShort(req.getRequstURILastPath())));
    }

    //更新用户介绍
    @HttpMapping(url = "/user/updateintro", auth = true)
    public void updateIntro(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateIntro(user.getUserid(), req.getParameter("intro")));
    }

    //更新用户实名制
    @HttpMapping(url = "/user/updateshenfen", auth = true)
    public void updateShenfen(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        Map<String, String> parambean = null;
        if (req.getParameter("bean") != null) parambean = req.getJsonParameter(JsonConvert.TYPE_MAP_STRING_STRING, "bean");
        if (parambean == null) parambean = new HashMap<>();
        resp.finishJson(service.updateShenfen(user.getUserid(), parambean.getOrDefault("shenfenname", req.getParameter("shenfenname")), parambean.getOrDefault("shenfenno", req.getParameter("shenfenno"))));
    }

    //更新设备ID
    @HttpMapping(url = "/user/updateapptoken", auth = true)
    public void updateApptoken(HttpRequest req, HttpResponse resp) throws IOException {
        String s = req.getRequstURILastPath();
        if ("updateapptoken".equalsIgnoreCase(s)) s = "";
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateApptoken(user.getUserid(), req.getParameter("appos", req.getRequstURIPath("appos:", "")), req.getParameter("apptoken", s)));
    }

    //更新代理
    @HttpMapping(url = "/user/updateagency/", auth = true)
    public void updateAgency(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(service.updateAgencyid(user.getUserid(), Integer.parseInt(req.getRequstURILastPath()), 0));
    }

    //发送手机注册验证码
    @HttpMapping(url = "/user/smsregcode", auth = false)
    public void smsreg(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSREG, req, resp);
    }

    //发送修改密码验证码
    @HttpMapping(url = "/user/smspwdcode", auth = true)
    public void smspwd(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSPWD, req, resp);
    }

    //发送忘记密码验证码
    @HttpMapping(url = "/user/smsfpwcode", auth = false)
    public void smsfot(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSPWD, req, resp);
    }

    //发送修改银行密码验证码
    @HttpMapping(url = "/user/smsbakcode", auth = true)
    public void smsbak(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSBAK, req, resp);
    }

    //发送手机修改验证码
    @HttpMapping(url = "/user/smsmobcode", auth = true)
    public void smsmob(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSMOB, req, resp);
    }

    //发送原手机验证码
    @HttpMapping(url = "/user/smsodmcode", auth = false)
    public void smsodm(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSODM, req, resp);
    }

    //发送手机登录验证码
    @HttpMapping(url = "/user/smslgncode", auth = true)
    public void smslgn(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSLGN, req, resp);
    }

    private void smsvercode(final short type, HttpRequest req, HttpResponse resp) throws IOException {
        String mobile = req.getRequstURIPath("mobile:", req.getParameter("mobile"));
        if (type == RandomCode.TYPE_SMSODM) { //给原手机号码发送验证短信
            UserInfo user = req.currentUser();
            if (user == null) user = service.findUserInfo(currentUserid(req));
            if (user != null) mobile = user.getMobile();
        }
        UserInfo user = req.currentUser();
        int userid = user == null ? currentUserid(req) : user.getUserid();
        RetResult rr = service.smscode(userid, type, mobile);
        if (finest) logger.finest(req.getRequestURI() + ", mobile = " + mobile + "---->" + rr);
        resp.finishJson(rr);
    }

    //检测账号是否有效, 返回t0表示可用.给新用户注册使用
    @HttpMapping(url = "/user/checkaccount/", auth = true)
    public void checkAccount(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(RetCodes.retResult(service.checkAccount(req.getRequstURILastPath())));
    }

    //检测手机号码是否有效, 返回0表示可用.给新用户注册使用
    @HttpMapping(url = "/user/checkmobile/", auth = true)
    public void checkMobile(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(RetCodes.retResult(service.checkMobile(req.getRequstURILastPath())));
    }

    //检测邮箱地址是否有效, 返回0表示可用.给新用户注册使用
    @HttpMapping(url = "/user/checkemail/", auth = true)
    public void checkEmail(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(RetCodes.retResult(service.checkEmail(req.getRequstURILastPath())));
    }

    //验证短信验证码
    @HttpMapping(url = "/user/checkcode", auth = true)
    public void checkcode(HttpRequest req, HttpResponse resp) throws IOException {
        String mobile = req.getRequstURIPath("mobile:", req.getParameter("mobile", ""));
        if (mobile.isEmpty()) {
            UserInfo user = req.currentUser();
            if (user == null) user = service.findUserInfo(currentUserid(req));
            if (user != null) mobile = user.getMobile();
        }
        String vercode = req.getRequstURIPath("vercode:", req.getParameter("vercode"));
        RetResult<RandomCode> ret = service.checkRandomCode(mobile, vercode, (short) 0);
        resp.finishJson(RetCodes.retResult(ret.getRetcode()));
    }

    //验证短信验证码
    @HttpMapping(url = "/user/checkmobcode", auth = false)
    public void checkmobcode(HttpRequest req, HttpResponse resp) throws IOException {
        String mobile = req.getRequstURIPath("mobile:", req.getParameter("mobile", ""));
        if (mobile.isEmpty()) {
            UserInfo user = req.currentUser();
            if (user == null) user = service.findUserInfo(currentUserid(req));
            if (user != null) mobile = user.getMobile();
        }
        String vercode = req.getRequstURIPath("vercode:", req.getParameter("vercode"));
        RetResult<RandomCode> ret = service.checkRandomCode(mobile, vercode, (short) 0);
        resp.finishJson(RetCodes.retResult(ret.getRetcode()));
    }

    //获取当前用户基本信息
    @HttpMapping(url = "/user/myinfo", auth = false)
    public void myinfo(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finishJson(loginConvert, user);
    }

    //获取指定用户基本信息
    @HttpMapping(url = "/user/player", auth = false)
    public void player(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = req.getIntParameter("userid", 1);
        UserInfo user = service.findUserInfo(userid);
        RetResult rs = user == null ? RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS) : new RetResult(user);
        resp.finishJson(TYPE_RETRESULT_PLAYER, rs);
    }

    //获取个人基本信息
    @HttpMapping(url = "/user/info", auth = false)
    public void info(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = req.getIntParameter("userid", 1);
        resp.finishJson(service.findUserInfo(userid));
    }

    @HttpMapping(url = "/user/reload/", auth = false)
    public void reload(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.reloadUserInfo(Integer.parseInt(req.getRequstURILastPath())));
    }

    //获取他人用户名
    @HttpMapping(url = "/user/otherinfo", auth = false)
    public void otherinfo(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo other = service.findUserInfo(req.getIntParameter("otheruid", 0));
        if (other == null) {
            resp.finishJson(RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS));
        } else {
            resp.finishJson(new RetResult(Utility.ofMap("userid", other.getUserid(), "username", other.getUsername(), "gender", other.getGender(), "face", other.getFace())));
        }
    }

    //获取当前用户详细信息
    @HttpMapping(url = "/user/mydetail", auth = false)
    public void mydetail(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = service.findUserInfo(currentUserid(req));
        resp.finish(userDetailConvert.convertTo(service.findUserDetail(user.getUserid())));
    }

}
