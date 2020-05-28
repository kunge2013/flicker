/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.kefu.KefuUser;
import com.cratos.platf.kefu.KefuService;
import com.cratos.platf.base.*;
import com.cratos.platf.info.*;
import static com.cratos.platf.order.GoodsInfo.*;
import java.io.*;
import java.util.*;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.source.Flipper;

/**
 *
 * @author zhangjx
 */
@WebServlet({"/goods/*"})
public class GoodsServlet extends BaseServlet {

    @Resource
    private DictService dictService;

    @Resource
    private GoodsService goodsService;

    @Resource
    private KefuService kefuService;

    @HttpMapping(url = "/goods/intros", auth = false, comment = "获取商品描述列表")
    public void intros(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(goodsService.queryGoodsIntro());
    }

    @HttpMapping(url = "/goods/coins", auth = true, comment = "获取金币商品列表")
    public void coins(HttpRequest req, HttpResponse resp) throws IOException {
        final GoodsBean bean = new GoodsBean();
        bean.setStatus(BaseEntity.STATUS_NORMAL);
        bean.goodstype(GOODS_TYPE_COIN);
        long now = System.currentTimeMillis();
        bean.setStarttime(now);
        bean.setEndtime(now);
        resp.finishJson(goodsService.queryGoodsInfo(bean, new Flipper()));
    }

    @HttpMapping(url = "/goods/diamonds", auth = true, comment = "获取钻石商品列表")
    public void diamonds(HttpRequest req, HttpResponse resp) throws IOException {
        final GoodsBean bean = new GoodsBean();
        bean.setStatus(BaseEntity.STATUS_NORMAL);
        bean.goodstype(GOODS_TYPE_DIAMOND);
        long now = System.currentTimeMillis();
        bean.setStarttime(now);
        bean.setEndtime(now);
        resp.finishJson(goodsService.queryGoodsInfo(bean, new Flipper()));
    }

    @HttpMapping(url = "/goods/query", auth = true, comment = "获取商品列表")
    public void query(HttpRequest req, HttpResponse resp) throws IOException {
        GoodsBean parambean = req.getJsonParameter(GoodsBean.class, "bean");
        final GoodsBean bean = parambean == null ? new GoodsBean() : parambean;
        bean.setStatus(BaseEntity.STATUS_NORMAL);
        long now = System.currentTimeMillis();
        bean.setStarttime(now);
        bean.setEndtime(now);
        Flipper flipper = new Flipper("display ASC");
        Map<String, List> map = new LinkedHashMap<>();
        bean.setBuytype(GOODS_BUY_COIN);
        {
            List<GoodsInfo> list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("coinbuy", list);
        }
        bean.setBuytype(GOODS_BUY_DIAMOND);
        {
            List<GoodsInfo> list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("diamondbuy", list);
        }
        bean.setBuytype(GOODS_BUY_COUPON);
        {
            List<GoodsInfo> list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("couponbuy", list);
        }
        bean.setBuytype(GOODS_BUY_RMB);
        {
            bean.goodstype(GOODS_TYPE_COIN);
            List<GoodsInfo> list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("rmbcoins", list);
            bean.goodstype(GOODS_TYPE_DIAMOND);
            list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("rmbdiamonds", list);
            bean.goodstype(GOODS_TYPE_COUPON);
            list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("rmbcoupons", list);
            bean.goodstype(GOODS_TYPE_PACKETS, GOODS_TYPE_DAYPACKET);
            list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("rmbpackets", list);
            bean.goodstype(GOODS_TYPE_WEEKCARD, GOODS_TYPE_MONTHCARD);
            list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("rmbcards", list);
            bean.goodstype(GOODS_TYPE_ONCEPACKET);
            list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("rmbonces", list);
            bean.goodstype(GOODS_TYPE_ACTIPACKET);
            list = goodsService.queryGoodsInfo(bean, flipper).list();
            if (!list.isEmpty()) map.put("rmbactis", list);
        }
        resp.finishJson(map);
    }

    @HttpMapping(url = "/goods/paylist", auth = true, cacheseconds = 30, comment = "获取支付以及充值客服列表")
    public void payTypes(HttpRequest req, HttpResponse resp) throws IOException {
        Map<String, Object> map = new HashMap<>();
        //支付类型;定额微信: dewxpay;  
        //定额支付宝: dealipay;  
        //微信充值: iewxpay;  
        //支付宝充值: iealipay;  
        //云闪付: ieysfpay;  
        //银行卡充值:iebankpay;  
        //专享闪付:ievippay;
        map.put("paySubTypes", dictService.findDictValue(DictInfo.PLATF_PAY_SUBTYPES, ""));
        String payievipTypes = dictService.findDictValue(DictInfo.PLATF_PAY_IEVIPPAY_TYPES, "");
        map.put("dewxpayItems", dictService.findDictValue(DictInfo.PLATF_PAY_DEWXPAY_ITEMS, ""));
        map.put("dealipayItems", dictService.findDictValue(DictInfo.PLATF_PAY_DEALIPAY_ITEMS, ""));
        map.put("iewxpayItems", dictService.findDictValue(DictInfo.PLATF_PAY_IEWXPAY_ITEMS, ""));
        map.put("iealipayItems", dictService.findDictValue(DictInfo.PLATF_PAY_IEALIPAY_ITEMS, ""));
        map.put("iebankpayItems", dictService.findDictValue(DictInfo.PLATF_PAY_IEBANKPAY_ITEMS, ""));
        map.put("ieysfpayItems", dictService.findDictValue(DictInfo.PLATF_PAY_IEYSFPAY_ITEMS, ""));
        List<KefuUser> kefuList = kefuService.queryOnLineKefu();
        for (KefuUser kefu : kefuList) {
            kefu.setPayievipTypes(payievipTypes);
        }
        map.put("kefuList", kefuList);
        resp.finishJson(map);
    }
}
