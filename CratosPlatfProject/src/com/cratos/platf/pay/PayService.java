/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.pay;

import com.cratos.platf.base.BaseService;
import com.cratos.platf.order.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.convert.json.*;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;
import org.redkalex.pay.*;
import static org.redkalex.pay.PayRetCodes.*;

/**
 *
 * @author zhangjx
 */
@Comment("支付服务")
public class PayService extends BaseService {

    private static final JsonConvert convert = JsonFactory.create().skipAllIgnore(true).getConvert();

    @Resource(name = "platf")
    protected DataSource paySource;

    @Resource
    protected OrderService orderService;

    @Resource
    private org.redkalex.pay.PayService payService;

    protected ScheduledThreadPoolExecutor scheduler;

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, "OrderService-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkOrderRecord();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void destroy(AnyValue config) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void checkOrderRecord() {
        FilterNode statunode = FilterNode.create("paystatus", Pays.PAYSTATUS_UNPAY).or("paystatus", Pays.PAYSTATUS_UNREFUND);
        FilterNode node = FilterNode.create("createtime", FilterExpress.LESSTHAN, (System.currentTimeMillis() - 5 * 60 * 1000)).and(statunode);
        Flipper flipper = new Flipper();
        Sheet<PayRecord> sheet = paySource.querySheet(PayRecord.class, flipper, node);
        while (!sheet.isEmpty()) {
            for (PayRecord pay : sheet) {
                checkPay(pay);
            }
            sheet = paySource.querySheet(PayRecord.class, flipper.next(), node);
        }
    }

    @Comment("判断是否支持指定支付类型")
    public boolean supportPayType(final short paytype) {
        return payService.supportPayType(paytype);
    }

    public WeiXinPayService getWeiXinPayService() {
        return payService.getWeiXinPayService();
    }

    public UnionPayService getUnionPayService() {
        return payService.getUnionPayService();
    }

    public AliPayService getAliPayService() {
        return payService.getAliPayService();
    }

    public EhkingPayService getEhkingPayService() {
        return payService.getEhkingPayService();
    }

    @Comment("主动查询支付结果, 页面调用")
    public RetResult<PayRecord> checkPay(String payno) {
        return checkPay(findPayRecord(payno));
    }

    @Comment("固定返回支付成功结果， 用于调试")
    public RetResult<PayRecord> testCheckPay(String payno) {
        PayRecord pay = findPayRecord(payno);
        if (pay == null) return RetResult.success();
        return testCheckPay(pay);
    }

    @Comment("固定返回支付成功结果， 用于调试")
    public RetResult<PayRecord> testCheckPay(PayRecord pay) {
        pay.setPayedmoney(pay.getOrdermoney());
        pay.setResponsetext("test");
        pay.setPaystatus(Pays.PAYSTATUS_PAYOK);
        pay.setFinishtime(System.currentTimeMillis());
        paySource.updateColumn(pay, "paystatus", "payedmoney", "finishtime");
        return new RetResult<>(pay);
    }

    @Comment("固定返回支付成功结果， 用于调试")
    public RetResult<Map<String, String>> testPrepay(final PayRecord pay) {
        pay.setCreatetime(System.currentTimeMillis());
        pay.createPayno();
        pay.setPaybody("test");
        PayPreRequest req = pay.createPayPreRequest();
        pay.setRequestjson(convert.convertTo(req));
        paySource.insert(pay);
        PayPreResponse rr = new PayPreResponse();
        if (!rr.isSuccess()) {
            pay.setPaystatus(Pays.PAYSTATUS_PAYNO);
            pay.setResponsetext(String.valueOf(rr));
            pay.setFinishtime(System.currentTimeMillis());
            paySource.updateColumn(pay, "paystatus", "responsetext", "finishtime");
        }
        if (rr.isSuccess()) {
            rr.setRetinfo(pay.getPayno());
            testCheckPay(pay);
            rr.result(Utility.ofMap("content", "http://baidu.com"));
        }
        rr.setResponsetext(""); //请求内容，防止信息暴露给外部接口
        return rr;
    }

    @Comment("主动查询支付结果, 定时任务调用")
    public RetResult<PayRecord> checkPay(PayRecord pay) {
        if (pay == null) return RetResult.success();
        if (pay.getPaystatus() != Pays.PAYSTATUS_UNPAY && pay.getPaystatus() != Pays.PAYSTATUS_UNREFUND) return RetResult.success();//已经更新过了
        PayRequest request = new PayRequest(pay.getAppid(), pay.getPaytype(), pay.getPayno());
        final boolean expired = pay.getCreatetime() + 15 * 60 * 1000 < System.currentTimeMillis();//超过15分钟视为支付失败
        PayQueryResponse resp = payService.query(request);
        if (expired && resp == null) resp = new PayQueryResponse().retcode(RETPAY_PAY_EXPIRED);
        if (resp == null) return RetResult.success(); //不支持定时查询
        PayAction payact = new PayAction();
        payact.setActurl(Pays.PAYACTION_QUERY);
        payact.setCreatetime(System.currentTimeMillis());
        payact.setPayno(pay.getPayno());
        payact.setPaytype(pay.getPaytype());
        payact.setRequestjson(convert.convertTo(request));
        payact.setResponsetext(convert.convertTo(resp));
        payact.setPayactid("a" + Utility.format36time(payact.getCreatetime()) + "-" + pay.getPayno());
        paySource.insertAsync(payact);
        if (resp.isSuccess()) { //查询结果成功，并不表示支付成功
            if (resp.getPaystatus() != Pays.PAYSTATUS_UNPAY //不能将未支付状态更新到pay中， 否则notify发现是未支付状态会跳过pay的更新
                && resp.getPaystatus() != Pays.PAYSTATUS_UNREFUND) {
                pay.setPaystatus(resp.getPaystatus());
            }
            if (pay.isPayok()) pay.setPayedmoney(pay.getOrdermoney());
            pay.setThirdpayno(resp.getThirdpayno());
            pay.setFinishtime(payact.getCreatetime());
            pay.setResponsetext(payact.getResponsetext());
        } else if (expired) {
            pay.setPaystatus(Pays.PAYSTATUS_CLOSED);
            pay.setThirdpayno(resp.getThirdpayno());
            pay.setFinishtime(payact.getCreatetime());
            pay.setResponsetext(payact.getResponsetext());
            pay.setPayedmoney(0);
        }
        paySource.updateColumn(pay, "paystatus", "payedmoney", "thirdpayno", "responsetext", "finishtime");
        orderService.updateOrder(pay);
        return new RetResult(resp.getRetcode(), resp.getRetinfo()).result(pay);
    }

    @Comment("手机支付回调")
    public RetResult<PayRecord> notify(final PayNotifyRequest request) {
        final PayNotifyResponse resp = payService.notify(request);
        PayRecord pay = findPayRecord(resp.getPayno());
        PayAction payact = new PayAction();
        payact.setActurl(Pays.PAYACTION_NOTIFY);
        payact.setCreatetime(System.currentTimeMillis());
        payact.setPayno(resp.getPayno());
        payact.setPaytype(resp.getPaytype());
        payact.setRequestjson(convert.convertTo(request));
        payact.setResponsetext(convert.convertTo(resp));
        payact.setPayactid(Utility.format36time(payact.getCreatetime()) + "-" + pay.getPayno());
        paySource.insert(payact);
        if (pay.getPaystatus() != Pays.PAYSTATUS_UNPAY && pay.getPaystatus() != Pays.PAYSTATUS_UNREFUND) { //已经更新过了
            logger.log(Level.WARNING, "pay (" + pay + ") status error, req = " + request + ", resp = " + resp);
            return new RetResult(PayRetCodes.RETPAY_STATUS_ERROR, resp.getNotifytext()).result(pay);
        }
        boolean refund = pay.getPaystatus() == Pays.PAYSTATUS_UNREFUND;
        if (resp.isSuccess()) { //支付或退款成功
            pay.setPayedmoney(resp.getPayedmoney());
            pay.setPaystatus(refund ? Pays.PAYSTATUS_REFUNDOK : Pays.PAYSTATUS_PAYOK);
            pay.setThirdpayno(resp.getThirdpayno());
            pay.setFinishtime(System.currentTimeMillis());
            pay.setResponsetext(payact.getResponsetext());
            paySource.updateColumn(pay, "payedmoney", "paystatus", "thirdpayno", "responsetext", "finishtime");
        } else if (resp.getRetcode() != RETPAY_FALSIFY_ERROR && resp.getRetcode() != RETPAY_PAY_WAITING) {
            pay.setPaystatus(refund ? Pays.PAYSTATUS_REFUNDNO : Pays.PAYSTATUS_PAYNO);
            pay.setThirdpayno(resp.getThirdpayno());
            pay.setFinishtime(System.currentTimeMillis());
            pay.setResponsetext(payact.getResponsetext());
            paySource.updateColumn(pay, "paystatus", "thirdpayno", "responsetext", "finishtime");
        }
        orderService.updateOrder(pay);
        if (!resp.isSuccess()) return new RetResult(resp.getRetcode(), resp.getNotifytext()).result(pay);
        return new RetResult<>(pay).retinfo(resp.getNotifytext());   //支付的回调参数处理完必须输出success字样
    }

    @Comment("微信公众号、手机支付时调用")
    public RetResult<Map<String, String>> prepay(final PayRecord pay) {
        pay.setCreatetime(System.currentTimeMillis());
        pay.createPayno();
        final String oldappid = pay.getAppid();
        pay.setNotifyurl(payService.getNotifyurl(pay.getPaytype(), pay.getAppid()));
        PayPreRequest req = pay.createPayPreRequest();
        pay.setRequestjson(convert.convertTo(req));
        paySource.insert(pay);
        PayPreResponse rr = payService.prepay(req);
        if (!rr.getAppid().isEmpty()) pay.setAppid(rr.getAppid());
        if (!rr.isSuccess()) {
            pay.setPaystatus(Pays.PAYSTATUS_PAYNO);
            pay.setResponsetext(String.valueOf(rr));
            pay.setFinishtime(System.currentTimeMillis());
            paySource.updateColumn(pay, "appid", "paystatus", "responsetext", "finishtime");
        } else if (oldappid.isEmpty()) {
            paySource.updateColumn(pay, "appid");
        }
        if (rr.isSuccess()) rr.setRetinfo(pay.getPayno());
        rr.setResponsetext(""); //请求内容，防止信息暴露给外部接口
        return rr;
    }

    @Comment("退款调用")
    public RetResult<Map<String, String>> prefund(final PayRecord pay) {
        pay.setCreatetime(System.currentTimeMillis());
        pay.createPayno();
        pay.setNotifyurl(payService.getNotifyurl(pay.getPaytype(), pay.getAppid()));
        PayRefundRequest req = pay.createPayRefundRequest();
        pay.setRequestjson(convert.convertTo(req));
        paySource.insert(pay);
        PayRefundResponse rr = payService.refund(req);
        //if (!rr.getAppid().isEmpty()) pay.setAppid(rr.getAppid());
        if (!rr.isSuccess()) {
            pay.setPaystatus(Pays.PAYSTATUS_REFUNDNO);
            pay.setResponsetext(String.valueOf(rr));
            pay.setFinishtime(System.currentTimeMillis());
            paySource.updateColumn(pay, "appid", "paystatus", "responsetext", "finishtime");
        }
        if (rr.isSuccess()) rr.setRetinfo(pay.getPayno());
        rr.setResponsetext(""); //请求内容，防止信息暴露给外部接口
        return rr;
    }

    @Comment("根据payno查找单个PayRecord")
    public PayRecord findPayRecord(String payno) {
        return paySource.find(PayRecord.class, payno);
    }

}
