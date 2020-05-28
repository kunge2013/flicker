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
import static com.cratos.platf.base.RetCodes.*;
import com.cratos.platf.user.UserService;
import java.util.logging.Level;
import org.redkale.service.RetResult;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/order/*"})
public class OrderServlet extends BaseServlet {

    @Resource
    private UserService userService;

    @Resource
    private DotCardService dotCardService;

    @Resource
    private OrderService orderService;

    //存款
    @HttpMapping(url = "/order/indepositcoins/")
    public void inDepositCoins(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo curr = req.currentUser();
        int userid = curr == null ? currentUserid(req) : curr.getUserid();
        resp.finishJson(userService.transferDepositCoins(userid, req.getRequstURILastPath(0L)));
    }

    //取款
    @HttpMapping(url = "/order/outdepositcoins/")
    public void outDepositCoins(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo curr = req.currentUser();
        int userid = curr == null ? currentUserid(req) : curr.getUserid();
        resp.finishJson(userService.transferDepositCoins(userid, -req.getRequstURILastPath(0L)));
    }

    //充值点卡
    @HttpMapping(url = "/order/usedotcard/")
    public void useGoodsCard(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo curr = req.currentUser();
        int userid = curr == null ? currentUserid(req) : curr.getUserid();
        resp.finishJson(dotCardService.useDotCard(userid, req.getRequstURILastPath().toLowerCase(), 0));
    }

    //检查充值资格
    @HttpMapping(url = "/order/checkage")
    public void checkage(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo curr = req.currentUser();
        int userid = curr == null ? currentUserid(req) : curr.getUserid();
        int price = req.getIntParameter("price", 10000_0000);
        resp.finishJson(orderService.checkAge(userid, price));
    }

    //定额微信：    dewxpay  传参数:  order:  {paytype:10, goodstype: 106, subpaytype:"dewxpay",  ordermoney: xxx00}
    //定额支付宝:  dealipay  传参数:  order:  {paytype:10, goodstype: 106, subpaytype:"dealipay", ordermoney: xxx00}
    //微信充值:     iewxpay  传参数:  order:  {paytype:10, goodstype: 106, subpaytype:"iewxpay",  ordermoney: xxx00}
    //支付宝充值:  iealipay  传参数:  order:  {paytype:10, goodstype: 106, subpaytype:"iealipay", ordermoney: xxx00}
    //云闪付       ieysfpay  传参数:  order:  {paytype:10, goodstype: 106, subpaytype:"ieysfpay", ordermoney: xxx00}
    //银行卡充值  iebankpay  传参数:  order:  {paytype:10, goodstype: 106, subpaytype:"iebankpay",ordermoney: xxx00}
    //专享闪付     ievippay 
    @HttpMapping(url = "/order/prepay", auth = true, comment = "APP支付前请求该连接, paytype,payway,goodsid,goodstype必须要有")
    public void prepay(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = req.currentUser();
        if (user == null) user = userService.findUserInfo(currentUserid(req));
        OrderRecord order = req.getJsonParameter(OrderRecord.class, "bean");
        if (order == null) order = req.getJsonParameter(OrderRecord.class, "order");
        if (order == null) {
            resp.finishJson(RetCodes.retResult(RET_PARAMS_ILLEGAL));
            return;
        }
        if (logger.isLoggable(Level.FINEST)) logger.finest("prepay.req = " + req);
        order.setClienthost(req.getHeader("X-RemoteHost", req.getHost()));
        order.setClientaddr(req.getRemoteAddr());
        boolean test = true; //正式环境需屏蔽下面两行
        //Map<String, String> maporder = req.getJsonParameter(JsonConvert.TYPE_MAP_STRING_STRING, "order");
        //test = maporder != null && "true".equalsIgnoreCase(maporder.get("test"));
        resp.finishJson(orderService.prepay(user, order, test));
    }

    //充值记录
    @HttpMapping(url = "/order/rmbbuyquery")
    public void rmbBuyQuery(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        OrderBean bean = req.getJsonParameter(OrderBean.class, "bean");
        if (bean == null) bean = new OrderBean();
        bean.setUserid(Math.max(1, userid));
        bean.buytype(GoodsInfo.GOODS_BUY_RMB);
        resp.finishJson(new RetResult(orderService.queryOrderRecord(bean, req.getFlipper())));
    }

    //商城兑换记录
    @HttpMapping(url = "/order/otherbuyquery")
    public void otherBuyQuery(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        OrderBean bean = req.getJsonParameter(OrderBean.class, "bean");
        if (bean == null) bean = new OrderBean();
        bean.setUserid(Math.max(1, userid));
        bean.buytype(GoodsInfo.GOODS_BUY_COIN, GoodsInfo.GOODS_BUY_DIAMOND, GoodsInfo.GOODS_BUY_COUPON);
        resp.finishJson(new RetResult(orderService.queryOrderRecord(bean, req.getFlipper())));
    }

    //金币兑换记录
    @HttpMapping(url = "/order/coinbuyquery")
    public void coinBuyQuery(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        OrderBean bean = req.getJsonParameter(OrderBean.class, "bean");
        if (bean == null) bean = new OrderBean();
        bean.setUserid(Math.max(1, userid));
        bean.buytype(GoodsInfo.GOODS_BUY_COIN);
        resp.finishJson(new RetResult(orderService.queryOrderRecord(bean, req.getFlipper())));
    }

    //晶石兑换记录
    @HttpMapping(url = "/order/diamondbuyquery")
    public void diamondBuyQuery(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        OrderBean bean = req.getJsonParameter(OrderBean.class, "bean");
        if (bean == null) bean = new OrderBean();
        bean.setUserid(Math.max(1, userid));
        bean.buytype(GoodsInfo.GOODS_BUY_DIAMOND);
        resp.finishJson(new RetResult(orderService.queryOrderRecord(bean, req.getFlipper())));
    }

    //奖券兑换记录
    @HttpMapping(url = "/order/couponbuyquery")
    public void couponBuyQuery(HttpRequest req, HttpResponse resp) throws IOException {
        final int userid = currentUserid(req);
        OrderBean bean = req.getJsonParameter(OrderBean.class, "bean");
        if (bean == null) bean = new OrderBean();
        bean.setUserid(Math.max(1, userid));
        bean.buytype(GoodsInfo.GOODS_BUY_COUPON);
        resp.finishJson(new RetResult(orderService.queryOrderRecord(bean, req.getFlipper())));
    }
}
