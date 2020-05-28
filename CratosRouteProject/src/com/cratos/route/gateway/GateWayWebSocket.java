/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.route.gateway;

import com.cratos.route.captcha.CaptchaService;
import java.lang.reflect.Type;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.annotation.Resource;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@RestWebSocket(name = "wsgame", catalog = "ws", wsmaxconns = 10000, comment = "WebSocket服务", repair = false)
public class GateWayWebSocket extends WebSocket<Integer, Object> {

    protected static final byte[] LINE = new byte[]{'\r', '\n'};

    public static class SimpleUser {

        public int userid;

        public String username = "";

        public String currgame = "";  //用户当前游戏
    }

    private static final Type RET_USER_TYPE = new TypeToken<RetResult<SimpleUser>>() {
    }.getType();

    protected HttpClient client;

    protected HttpContext context;

    protected int userid;

    protected Map<String, String> userHeader;

    @Resource
    protected GateWayService service;

    @Resource
    protected CaptchaService captchaService;

    protected String loginRetResult;

    protected String currgame = ""; //当前用户正在玩的游戏

    //key为模块名
    protected Map<String, ModuleNodeAddress> myModuleNodes = new ConcurrentHashMap<>();

    protected CompletableFuture<String> httpAsync(String url) {
        return httpAsync(url, 10_000);
    }

    protected CompletableFuture<String> httpAsync(String url, int timeoutmills) {
        return httpAsync(url, this.userHeader, 10_000);
    }

    protected CompletableFuture<String> httpAsync(String url, Map<String, String> headers, int timeoutmills) {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(timeoutmills));
        headers.forEach((name, value) -> builder.header(name, value));
        return client.sendAsync(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(resp -> resp.body());
    }

    @Override
    protected CompletableFuture<String> onOpen(final HttpRequest request) {
        this.context = request.getContext();
        this.client = HttpClient.newHttpClient();
        final boolean finest = getLogger().isLoggable(Level.FINEST);
        if (finest) getLogger().finest("WebSocket.req = " + request);
        String url = null;
        try {
            final ModuleNodeAddress mnode = this.service.loadHttpAddress("platf", "0", myModuleNodes);
            InetSocketAddress addr = mnode.address;
            url = "http://" + addr.getHostString() + ":" + addr.getPort() + "/pipes/user/"
                + request.getParameter("type", "") + "login?bean=" + URLEncoder.encode(request.getParameter("bean"), "utf8");

            long s = System.currentTimeMillis();
            this.userHeader = Utility.ofMap(
                "App-Agent", request.getParameter("appagent", request.getHeader("App-Agent", request.getHeader("User-Agent", "None"))),
                "X-RemoteHost", request.getHost(),
                "X-RemoteAddress", request.getRemoteAddr(),
                "WS-SncpAddress", JsonConvert.root().convertTo(getSncpAddress())
            );
            //this.userHeader.put("User-Agent", this.userHeader.get("App-Agent"));
            if (finest) getLogger().finest("login.http.url = " + url);
            mnode.semaphore.acquire();
            final String uri = url;
            return httpAsync(url).thenCompose(resptext -> {
                loginRetResult = resptext;
                if (finest) getLogger().finest("login.http.url.result = " + resptext);
                RetResult<SimpleUser> ret = JsonConvert.root().convertFrom(RET_USER_TYPE, resptext);
                String sessionid = null;
                if (ret.isSuccess()) {
                    userid = ret.getResult().userid;
                    currgame = ret.getResult().currgame;
                    sessionid = ret.getAttach().get("sessionid");
                    userHeader.put("Route-Userid", "" + userid);
                }
                long e = System.currentTimeMillis() - s;
                if (e > 6000) getLogger().finer("获取登陆数据耗时过长: " + request.getParameter("bean"));
                if (!ret.isSuccess()) return send("{\"onLoginFailMessage\":" + loginRetResult + "}").thenApply(x -> null);
                return CompletableFuture.completedFuture(sessionid);
            }).exceptionally(t -> {
                getLogger().log(Level.WARNING, uri + " sendasync error", t);
                return send("{\"onLoginFailMessage\":{\"retcode\":30010001,\"retinfo\":\"系统通讯错误\"}}").thenApply(x -> (String) null).join();
            }).whenComplete((r, t) -> mnode.semaphore.release());
        } catch (Exception e) {
            getLogger().log(Level.WARNING, url + " send error", e);
            return send("{\"onLoginFailMessage\":{\"retcode\":30010001,\"retinfo\":\"系统通讯错误\"}}").thenApply(x -> null);
        }
    }

    @Override
    protected CompletableFuture<Integer> createUserid() {
        return CompletableFuture.completedFuture(userid);
    }

    @Override
    protected boolean predicate(WebSocketRange wsrange) {
        if (wsrange == null) return true;
        if ("userid".equals(wsrange.getWskey()) && wsrange.containsAttach("minuserid")) {
            String minuserid = wsrange.getAttach("minuserid");
            return this.userid >= Integer.parseInt(minuserid);
        }
        return true;
    }

    @Override
    @Comment("连接后事件响应")
    public CompletableFuture onConnected() {
        return send("{\"onUserLoginMessage\":" + loginRetResult + "}");
    }

    @Override
    @Comment("断线后事件响应")
    public CompletableFuture onClose(int code, String reason) {
        Logger logger = getLogger();
        final boolean finest = logger.isLoggable(Level.FINEST);
        if (finest) logger.finest("WebSocket.onClose: userid = " + userid + ", code = " + code + ", reason = " + reason);
        try {
            CompletableFuture future1 = null;
            if (!this.currgame.isEmpty()) {
                ModuleNodeAddress mnode = this.service.loadHttpAddress(this.currgame, "0", myModuleNodes);
                InetSocketAddress gameaddr = mnode.address;
                if (gameaddr != null) {
                    String url = "http://" + gameaddr.getHostString() + ":" + gameaddr.getPort() + "/pipes/" + this.currgame + "/offlineGame?userid=" + getUserid();
                    mnode.semaphore.acquire();
                    future1 = httpAsync(url, 3_000).whenComplete((r, t) -> {
                        mnode.semaphore.release();
                        if (t != null) getLogger().log(Level.WARNING, url + " send error", t);
                    });
                }
                this.currgame = "";
            }
            ModuleNodeAddress mnode = this.service.loadHttpAddress("platf", "0", myModuleNodes);
            InetSocketAddress addr = mnode.address;
            String url = "http://" + addr.getHostString() + ":" + addr.getPort() + "/pipes/user/logout?JSESSIONID=" + getSessionid();
            CompletableFuture future2 = httpAsync(url, 3_000).whenComplete((r, t) -> {
                mnode.semaphore.release();
                if (t != null) getLogger().log(Level.WARNING, url + " send error", t);
            });
            return future1 == null ? future2 : CompletableFuture.allOf(future1, future2);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, " onClose error", e);
        }
        return null;
    }

    @Comment("刷新用户登陆态")
    void checkUser() {
        ModuleNodeAddress mnode = this.service.loadHttpAddress("platf", "0", myModuleNodes);
        InetSocketAddress addr = mnode.address;
        final String url = "http://" + addr.getHostString() + ":" + addr.getPort() + "/pipes/user/check?JSESSIONID=" + getSessionid();
        try {
            mnode.semaphore.acquire();
            httpAsync(url).whenComplete((r, t) -> {
                mnode.semaphore.release();
                if (t != null) getLogger().log(Level.WARNING, url + " send error", t);
            });
        } catch (Exception e) {
            getLogger().log(Level.WARNING, url + " send error", e);
        }
    }

    @RestOnMessage(name = "message", comment = "接收WS通用消息")
    public void message(
        @Comment("前缀") String pipes,
        @Comment("模块名称") String module,
        @Comment("请求参数") String action,
        @Comment("HTTP请求唯一序号") String http_seqno,
        @Comment("HTTP请求头") Map<String, String> http_headers,
        @Comment("节点ID，只取最后两位") String nodeid,
        @Comment("验证码KEY") String captchakey,
        @Comment("验证码CODE") String captchacode,
        @Comment("请求参数, 格式: bean1={}&=name=xxx") String params) {
        InetSocketAddress addr = null;
        try {
            if (getLogger().isLoggable(Level.FINEST)) {
                getLogger().finest("module = " + module + ", action = " + action + ", userid = " + userid + ", nodeid = " + nodeid + ", params = " + URLDecoder.decode(params, "UTF-8"));
            }
            if ("team".equals(module)) { //必然有params；格式为: fromuserid=3000001&touserids=[3000002,3000003]&msgtype=1&chatkey=xxxx
//                app.sendSocketMessage({
//                    module: "team",
//                    sendChatMessage: {
//                        fromuserid:3000001,
//                        destuserid:3000002,  //可有可无
//                        touserids: [3000002,3000003],
//                        msgkey: "xxxx",
//                        msgtype: 3,  //1:表情选项；2:文字选项;3:攻击选项; 4:语音; 5:文本;
//                        extxxxkey : "xxxval" 
//
//                    }
//                });
                Map<String, String> map = new HashMap<>();
                for (String item : params.split("&")) {
                    int pos = item.indexOf('=');
                    if (pos < 0) continue;
                    String key = item.substring(0, pos);
                    String val = URLDecoder.decode(item.substring(pos + 1), "UTF-8");
                    map.put(key, val);
                }
                int[] touserids = JsonConvert.root().convertFrom(int[].class, map.remove("touserids"));
                map.put("action", action);
                String chatmsg = JsonConvert.root().convertTo(Utility.ofMap("onPlayerTeamMessage", map));
                for (int touserid : touserids) {
                    sendMessage(chatmsg, touserid);
                }
                return;
            }
            final ModuleNodeAddress mnode = this.service.loadHttpAddress(module, nodeid, myModuleNodes);
            addr = mnode.address;
            if (pipes == null || pipes.isEmpty()) pipes = "pipes";
            final int sub = module.lastIndexOf('_');
            String url = "http://" + addr.getHostString() + ":" + addr.getPort()
                + "/" + pipes + "/" + (sub > 0 ? module.substring(sub + 1) : module) + "/" + action + "?userid=" + getUserid();
            if (params != null && !params.isEmpty()) url += "&" + params;
            if (captchakey != null && !captchakey.isEmpty()) {
                if (!captchaService.check(captchakey, captchacode).join()) {
                    if (http_seqno != null && http_seqno.length() > 6) {
                        send("{\"onHttpResponseMessage\":{\"http_seqno\":\"" + http_seqno + "\",\"content\":" + CaptchaService.CAPTCHA_RET_JSON + "}}");
                    }
                    return;
                }
                if (http_headers == null) {
                    http_headers = Utility.ofMap("CAPTCHA_KEY", captchakey);
                } else {
                    http_headers.put("CAPTCHA_KEY", captchakey);
                }
            } else {
                if (http_headers != null) http_headers.remove("CAPTCHA_KEY"); //防止客户端造假
            }
            if (http_headers == null) {
                http_headers = userHeader;
            } else {
                http_headers.putAll(userHeader);
            }
            mnode.semaphore.acquire();
            final String uri = url;
            if (http_seqno != null && http_seqno.length() > 6) {
                try {
                    httpAsync(uri, http_headers, 6_000).thenAccept(content -> {
                        char ch = content.charAt(0);
                        boolean json = ch == '{' || ch == '[';
                        send("{\"onHttpResponseMessage\":{\"http_seqno\":\"" + http_seqno + "\",\"content\":" + (json ? "" : "\"") + content + (json ? "" : "\"") + "}}");
                        if (action.equalsIgnoreCase("enterGame")) { //进入游戏
                            if (content.contains("\"success\":true")) {
                                String game = module.indexOf('_') > 0 ? module.substring(module.indexOf('_') + 1) : module;
                                this.currgame = game;
                            }
                        } else if (action.equalsIgnoreCase("leaveGame")) {  //离开游戏
                            if (content.contains("\"success\":true")) {
                                this.currgame = "";
                            }
                        }
                    }).whenComplete((r, t) -> {
                        mnode.semaphore.release();
                        if (t != null) getLogger().log(Level.WARNING, uri + " send error", t);
                    });

                } catch (Exception e) {
                    getLogger().log(Level.WARNING, url + " send error", e);
                }
            } else {
                httpAsync(uri, http_headers, 6_000).whenComplete((r, t) -> {
                    mnode.semaphore.release();
                    if (t != null) getLogger().log(Level.WARNING, uri + " send error", t);
                });
            }
            //}
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "module:" + module + ", action:" + action + ", nodeid:" + nodeid + ", addr:" + addr + ", params:" + params + " send error", ex);
        }
    }
}
