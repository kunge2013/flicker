/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.pay;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.stream.*;
import java.util.logging.*;
import javax.annotation.Resource;
import org.redkale.boot.Application;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.*;
import static org.redkalex.pay.PayRetCodes.*;
import org.redkalex.pay.*;

/**
 * 众鑫易付
 * https://zxyf.zx100pay.com/merchant/home.php
 *
 * @author zhangjx
 */
@DIYPayService(paytype = ZxPayService.PAYTYPE_ZXPAY)
public class ZxPayService extends AbstractPayService {

    public static final short PAYTYPE_ZXPAY = 54;

    private static final MessageDigest md5;

    static {
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        md5 = d;
    }

    //配置集合
    protected Map<String, ZxPayElement> elements = new HashMap<>();

    @Resource(name = "property.pay.zxpay.conf") //支付配置文件路径
    protected String conf = "config.properties";

    @Resource(name = "APP_HOME")
    protected File home;

    @Resource
    protected JsonConvert convert;

    @Override
    public void init(AnyValue config) {
        if (this.convert == null) this.convert = JsonConvert.root();
        this.reloadConfig(PAYTYPE_ZXPAY);
    }

    @Override
    @Comment("判断是否支持指定支付类型")
    public boolean supportPayType(final short paytype) {
        return paytype == PAYTYPE_ZXPAY && !elements.isEmpty();
    }

    @Override
    @Comment("重新加载配置")
    public void reloadConfig(short paytype) {
        if (this.conf != null && !this.conf.isEmpty()) { //存在现在支付配置
            try {
                File file = (this.conf.indexOf('/') == 0 || this.conf.indexOf(':') > 0) ? new File(this.conf) : new File(home, "conf/" + this.conf);
                InputStream in = (file.isFile() && file.canRead()) ? new FileInputStream(file) : getClass().getResourceAsStream("/META-INF/" + this.conf);
                if (in == null) return;
                Properties properties = new Properties();
                properties.load(in);
                in.close();
                this.elements = ZxPayElement.create(logger, properties, home);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "init zxpay conf error", e);
            }
        }
    }

    @Override
    public void destroy(AnyValue config) {
    }

    public static void main(String[] args) throws Throwable {
        ZxPayService servie = Application.singleton(ZxPayService.class);
        ZxPayElement element = new ZxPayElement();
        element.appid = "ZX100566";
        element.signkey = "03a2ab4a2f01f77fa146a2391ac123b0";
        element.payapiurl = "https://ad.66shanxi.cn/api/v4/cashier.php";
        element.notifyurl = "http://116.204.186.140/pipes/pay/zxpay/notify";
        element.returnurl = "http://116.204.186.140/index.html";
        servie.elements.put("", element);
        servie.elements.put(element.appid, element);

//        PayPreRequest req = new PayPreRequest();
//        req.setAppid("");
//        req.setPaymoney(100);
//        req.setClientAddr("127.0.0.1");
//        req.setPayno("1000" + System.currentTimeMillis());
//        req.setPaytitle("十分钱的商品");
//        req.setPaybody("一分钱的东西内容");
//        req.setPaytype(PAYTYPE_ZXPAY);
//        req.setSubpaytype(Pays.PAYTYPE_WEIXIN);
//        req.setPayway(PAYWAY_APP);
//        System.out.println(req);
//        System.out.println(servie.prepay(req));
        String body = "customno=pd1024he4d0k556eiy0-0k556ej1r&merchant=ZX100566&money=1.00&orderno=01081844408142&paytime=1578480320&qrtype=ap&sendtime=1578480280&state=1&sign=2095748D54406982E5EF46481E147ABF";
        PayNotifyRequest notifyRequest = new PayNotifyRequest(ZxPayService.PAYTYPE_ZXPAY, parseToMap(body));
        System.out.println("notify.map = " + parseToMap(body));
        System.out.println("notify.result = " + servie.notify(notifyRequest));
    }

    @Override
    public PayPreResponse prepay(PayPreRequest request) {
        request.checkVaild();
        final PayPreResponse result = new PayPreResponse();
        try {
            final ZxPayElement element = elements.get(request.getAppid());
            if (element == null) return result.retcode(RETPAY_CONF_ERROR);
            result.setAppid(element.appid);
            String notifyurl = (request.getNotifyurl() != null && !request.getNotifyurl().isEmpty()) ? request.getNotifyurl() : element.notifyurl;
            if (!notifyurl.startsWith("http")) notifyurl = "http://" + request.getClienthost() + notifyurl;
            String returnurl = request.getAttach("returnurl", element.returnurl);
            if (!returnurl.startsWith("http")) returnurl = "http://" + request.getClienthost() + returnurl;

            final TreeMap<String, String> map = new TreeMap<>();
            if (request.getAttach() != null) map.putAll(request.getAttach());
            final String subpaytype = request.getSubpaytype() == Pays.PAYTYPE_ALIPAY ? "ap" : (request.getSubpaytype() == Pays.PAYTYPE_WEIXIN ? "wp" : "" + request.getSubpaytype());
            map.put("merchant", element.appid);
            map.put("qrtype", subpaytype);
            map.put("customno", request.getPayno());
            map.put("money", String.format("%.2f", request.getPaymoney() / 100.0f));
            map.put("sendtime", "" + System.currentTimeMillis() / 1000);
            map.put("notifyurl", notifyurl);
            map.put("backurl", returnurl);
            map.put("risklevel", "1");
            map.put("sign", createSign(element, map));
            final String url = element.payapiurl;
            final String body = joinToString(map);
            if (logger.isLoggable(Level.FINEST)) logger.finest("zxpay.req.url = " + url + ", body = " + body);

            final Map<String, String> rmap = new TreeMap<>();
            rmap.put("payno", request.getPayno());
            rmap.put("content", url);
            rmap.put("body", body);
            result.setResult(rmap);
        } catch (Exception e) {
            result.setRetcode(RETPAY_PAY_ERROR);
            logger.log(Level.WARNING, "prepay_pay_error req=" + request + ", resp=" + result.getResponsetext(), e);
        }
        return result;
    }

    @Override
    public PayNotifyResponse notify(PayNotifyRequest request) {
        request.checkVaild();
        final PayNotifyResponse result = new PayNotifyResponse();
        result.setPaytype(request.getPaytype());
        final String rstext = "OK";
        Map<String, String> map = request.getAttach();
        String appid = request.getAppid();
        if (appid == null || appid.isEmpty()) appid = map.getOrDefault("merchant", "");
        final ZxPayElement element = elements.get(appid);
        if (element == null) return result.retcode(RETPAY_CONF_ERROR);
        result.setPayno(map.getOrDefault("customno", ""));
        result.setThirdpayno(map.getOrDefault("orderno", ""));
        long amount = (long) (Double.parseDouble(map.getOrDefault("money", "0")) * 10000);
        result.setPayedmoney(amount / 100);
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        if (!checkSign(element, map)) return result.retcode(RETPAY_FALSIFY_ERROR).notifytext("NO");
        if (!"1".equals(map.get("state"))) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        if (amount < 1) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        return result.notifytext(rstext);
    }

    protected String joinToString(Map<String, ?> map) {
        Stream<String> stream = map.entrySet().stream().map((e -> e.getKey() + "=" + urlEncodeUTF8(e.getValue())));
        return stream.collect(Collectors.joining("&"));
    }

    protected static Map<String, String> parseToMap(final String text) throws IOException {
        Map<String, String> map = new TreeMap<>();
        for (String item : text.split("&")) {
            int pos = item.indexOf('=');
            map.put(item.substring(0, pos), item.substring(pos + 1));
        }
        return map;
    }

    @Override
    protected String createSign(AbstractPayService.PayElement element, Map<String, ?> map) throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("merchant=").append(map.get("merchant")).append("&qrtype=").append(map.get("qrtype"))
            .append("&customno=").append(map.get("customno")).append("&money=").append(map.get("money"))
            .append("&sendtime=").append(map.get("sendtime")).append("&notifyurl=").append(map.get("notifyurl"))
            .append("&backurl=").append(map.get("backurl")).append("&risklevel=").append(map.get("risklevel"));
        sb.append(((ZxPayElement) element).signkey);
        return Utility.binToHexString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes())).toLowerCase();
    }

    @Override
    protected boolean checkSign(AbstractPayService.PayElement element, Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        String sign = (String) map.remove("sign");
        final StringBuilder sb = new StringBuilder();
        map.forEach((x, y) -> {
            if (!((String) y).isEmpty()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(x).append('=').append(y);
            }
        });
        sb.append(((ZxPayElement) element).signkey);
        System.out.println(sb);
        try {
            return sign.equalsIgnoreCase(Utility.binToHexString(MessageDigest.getInstance("MD5").digest(Utility.binToHexString(MessageDigest.getInstance("SHA-1").digest(sb.toString().getBytes())).getBytes())));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PayQueryResponse query(final PayRequest request) {
        return null;
    }

    @Override
    public PayCreatResponse create(PayCreatRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PayResponse close(PayCloseRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PayRefundResponse refund(PayRefundRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PayRefundResponse queryRefund(PayRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ZxPayElement getPayElement(String appid) {
        return elements.get(appid);
    }

    public static class ZxPayElement extends AbstractPayService.PayElement {

        // pay.zxpay.[x].appid
        public String appid = "";  //APP应用ID

        // pay.zxpay.[x].signkey
        public String signkey = ""; //签名算法需要用到的密钥

        // pay.zfpay.[x].payapiurl
        public String payapiurl = "";  //APP支付接口host

        // pay.zxpay.[x].returnurl
        public String returnurl = ""; //前台跳转页面

        public static Map<String, ZxPayElement> create(Logger logger, Properties properties, File home) {
            String def_appid = properties.getProperty("pay.zxpay.appid", "").trim();
            String def_signkey = properties.getProperty("pay.zxpay.signkey", "").trim();
            String def_payapiurl = properties.getProperty("pay.zxpay.payapiurl", "").trim();
            String def_notifyurl = properties.getProperty("pay.zxpay.notifyurl", "").trim();
            String def_returnurl = properties.getProperty("pay.zxpay.returnurl", "").trim();

            final Map<String, ZxPayElement> map = new HashMap<>();
            properties.keySet().stream().filter(x -> x.toString().startsWith("pay.zxpay.") && x.toString().endsWith(".appid")).forEach(appid_key -> {
                final String prefix = appid_key.toString().substring(0, appid_key.toString().length() - ".appid".length());

                String appid = properties.getProperty(prefix + ".appid", def_appid).trim();
                String signkey = properties.getProperty(prefix + ".signkey", def_signkey).trim();
                String payapiurl = properties.getProperty(prefix + ".payapiurl", def_payapiurl).trim();
                String notifyurl = properties.getProperty(prefix + ".notifyurl", def_notifyurl).trim();
                String returnurl = properties.getProperty(prefix + ".returnurl", def_returnurl).trim();

                if (appid.isEmpty() || signkey.isEmpty()) {
                    logger.log(Level.WARNING, properties + "; has illegal zxpay conf by prefix" + prefix);
                    return;
                }
                ZxPayElement element = new ZxPayElement();
                element.appid = appid;
                element.signkey = signkey;
                element.payapiurl = payapiurl;
                element.notifyurl = notifyurl;
                element.returnurl = returnurl;
                if (element.initElement(logger, home)) {
                    map.put(appid, element);
                    if (def_appid.equals(appid)) map.put("", element);
                }
            });
            return map;
        }

        @Override
        public boolean initElement(Logger logger, File home) {
            return true;
        }

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }
    }
}
