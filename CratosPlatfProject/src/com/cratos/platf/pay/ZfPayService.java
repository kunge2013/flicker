/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.pay;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;
import javax.annotation.Resource;
import org.redkale.boot.Application;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.*;
import org.redkalex.pay.*;
import static org.redkalex.pay.PayRetCodes.*;
import static org.redkalex.pay.Pays.PAYWAY_APP;

/**
 * 众付支付
 * http://154.204.33.132:8082/zf/platform/order.html
 *
 * @author zhangjx
 */
@DIYPayService(paytype = ZfPayService.PAYTYPE_ZFPAY)
public class ZfPayService extends AbstractPayService {

    public static final short PAYTYPE_ZFPAY = 53;

    protected static final String format = "%1$tY%1$tm%1$td%1$tH%1$tM%1$tS"; //yyyyMMddHHmmss

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
    protected Map<String, ZfPayElement> elements = new HashMap<>();

    @Resource(name = "property.pay.zfpay.conf") //支付配置文件路径
    protected String conf = "config.properties";

    @Resource(name = "APP_HOME")
    protected File home;

    @Resource
    protected JsonConvert convert;

    @Override
    public void init(AnyValue config) {
        if (this.convert == null) this.convert = JsonConvert.root();
        this.reloadConfig(PAYTYPE_ZFPAY);
    }

    @Override
    @Comment("判断是否支持指定支付类型")
    public boolean supportPayType(final short paytype) {
        return paytype == PAYTYPE_ZFPAY && !elements.isEmpty();
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
                this.elements = ZfPayElement.create(logger, properties, home);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "init zfpay conf error", e);
            }
        }
    }

    @Override
    public void destroy(AnyValue config) {
    }

    public static void main(String[] args) throws Throwable {
        ZfPayService servie = Application.singleton(ZfPayService.class);
        ZfPayElement element = new ZfPayElement();
        element.appid = "20000102";
        element.signkey = "0fe68a52fb4ee5948040fed35f8fe74b";
        element.payapiurl = "http://202.60.240.28:8080/zfpay/create";
        element.notifyurl = "http://47.98.180.255/pipes/pay/zfpay/notify";
        element.returnurl = "http://47.98.180.255/index.html";
        servie.elements.put("", element);
        servie.elements.put(element.appid, element);

        PayPreRequest req = new PayPreRequest();
        req.setAppid("");
        req.setPaymoney(100_00);
        req.setClientAddr("127.0.0.1");
        req.setPayno("1000" + System.currentTimeMillis());
        req.setPaytitle("一毛钱的东西");
        req.setPaybody("一毛钱的东西内容");
        req.setPaytype(PAYTYPE_ZFPAY);
        req.setSubpaytype(Pays.PAYTYPE_ALIPAY);
        req.setPayway(PAYWAY_APP);
        System.out.println(req);
        System.out.println(servie.prepay(req));
    }

    @Override
    public PayPreResponse prepay(PayPreRequest request) {
        request.checkVaild();
        final PayPreResponse result = new PayPreResponse();
        try {
            final ZfPayElement element = elements.get(request.getAppid());
            if (element == null) return result.retcode(RETPAY_CONF_ERROR);
            result.setAppid(element.appid);
            String notifyurl = (request.getNotifyurl() != null && !request.getNotifyurl().isEmpty()) ? request.getNotifyurl() : element.notifyurl;
            if (!notifyurl.startsWith("http")) notifyurl = "http://" + request.getClienthost() + notifyurl;
            String returnurl = request.getAttach("returnurl", element.returnurl);
            if (!returnurl.isEmpty() && !returnurl.startsWith("http")) returnurl = "http://" + request.getClienthost() + returnurl;

            final String params = "merchant_id=" + element.appid + "&order_id=" + request.getPayno() + "&amount=" + request.getPaymoney() / 100;
            final String sign = Utility.binToHexString(MessageDigest.getInstance("MD5").digest((params + "&sign=" + element.signkey).getBytes()));
            final short subpaytype = request.getSubpaytype() == Pays.PAYTYPE_ALIPAY ? 1 : (request.getSubpaytype() == Pays.PAYTYPE_WEIXIN ? 2 : request.getSubpaytype());
            String url = element.payapiurl + "?" + params + "&sign=" + sign + "&pay_method=" + subpaytype + "&notify_url=" + urlEncodeUTF8(notifyurl);
            if (!returnurl.isEmpty()) url += "&return_url=" + urlEncodeUTF8(returnurl);
//            System.out.println("支付请求: " + url);
//            //{"code": "00000", "msg": "通道临时维护中，稍后开启", "resp":{}}
//            final String responseText = Utility.getHttpContent(url);
//            result.setResponsetext(responseText);
//            System.out.println("支付请求: " + url);
//            System.out.println("返回结果: " + responseText);
//
//            Map<String, String> resultmap = parseToMap(responseText);
//
//            if (!"0".equals(resultmap.get("code"))) return result.retcode(RETPAY_PAY_ERROR);
//            if (!checkSign(element, resultmap)) return result.retcode(RETPAY_FALSIFY_ERROR);
//
//            result.setThirdpayno(resultmap.getOrDefault("sign", ""));
            result.setResponsetext("");
            result.setThirdpayno(request.getPayno());
            final Map<String, String> rmap = new TreeMap<>();
            rmap.put("payno", request.getPayno());
            rmap.put("content", url);
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
        final String rstext = "success";
        Map<String, String> map = request.getAttach();
        String appid = request.getAppid();
        if (appid == null || appid.isEmpty()) appid = map.getOrDefault("appid", "");
        final ZfPayElement element = elements.get(appid);
        if (element == null) return result.retcode(RETPAY_CONF_ERROR);
        result.setPayno(map.getOrDefault("order_id", ""));
        result.setThirdpayno(result.getPayno());
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        if (!checkSign(element, map)) return result.retcode(RETPAY_FALSIFY_ERROR).notifytext("success");
        if (Long.parseLong(map.getOrDefault("amount", "0")) < 1) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        result.setPayedmoney(Long.parseLong(map.getOrDefault("amount", "0")) * 100);
        return result.notifytext(rstext);
    }

    protected String joinToString(Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        Stream<String> stream = map.entrySet().stream().map((e -> e.getKey() + "=" + encodeURL(e.getValue())));
        return stream.collect(Collectors.joining("&"));
    }

    protected String encodeURL(Object val) {
        try {
            return URLEncoder.encode(val.toString(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Map<String, String> parseToMap(final String text) throws IOException {
        if (text.indexOf('&') > 0 && text.indexOf('=') > 0) {
            TreeMap<String, String> map = new TreeMap<>();
            for (String str : text.split("&")) {
                int pos = str.indexOf('=');
                if (pos < 0) continue;
                map.put(str.substring(0, pos), URLDecoder.decode(str.substring(pos + 1), "UTF-8"));
            }
            return map;
        }
        final String key = "\"resp\"";
        final int pos = text.indexOf(key);
        if (pos > 0) {
            int pos1 = text.indexOf('{', pos + key.length());
            int pos2 = text.indexOf('}', pos1);
            String text1 = text.substring(0, pos).trim();
            String text2 = text.substring(pos2 + 1).trim();
            if (text1.endsWith(",") && text2.length() < 2) text1 = text1.substring(0, text1.length() - 1);
            Map<String, String> map1 = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, text1 + text2);
            Map<String, String> map2 = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, text.substring(pos1, pos2 + 1));
            map1.putAll(map2);
            return map1;
        } else {
            return JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, text);
        }
    }

    @Override
    protected String createSign(AbstractPayService.PayElement element, Map<String, ?> map) throws Exception {
        final StringBuilder sb = new StringBuilder();
        map.forEach((x, y) -> {
            if (!((String) y).isEmpty()) sb.append(x).append('=').append(y).append('&');
        });
        return Utility.binToHexString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes()));
    }

    @Override
    protected boolean checkSign(AbstractPayService.PayElement element, Map<String, ?> map) {
        String sign = (String) map.remove("sign");
        final String sb = "merchant_id=" + map.get("merchant_id") + "&order_id=" + map.get("order_id")
            + "&amount=" + map.get("amount") + "&sign=" + ((ZfPayElement) element).signkey;
        try {
            return sign.equals(Utility.binToHexString(MessageDigest.getInstance("MD5").digest(sb.getBytes())));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PayCreatResponse create(PayCreatRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PayQueryResponse query(PayRequest request) {
        return null;
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
    public ZfPayElement getPayElement(String appid) {
        return elements.get(appid);
    }

    public static class ZfPayElement extends AbstractPayService.PayElement {

        // pay.zfpay.[x].appid
        public String appid = "";  //APP应用ID

        // pay.zfpay.[x].payapiurl
        public String payapiurl = "";  //APP支付接口url

        // pay.zfpay.[x].signkey
        public String signkey = ""; //签名算法需要用到的密钥

        // pay.zfpay.[x].returnurl
        public String returnurl = ""; //前台跳转页面

        public static Map<String, ZfPayElement> create(Logger logger, Properties properties, File home) {
            String def_appid = properties.getProperty("pay.zfpay.appid", "").trim();
            String def_signkey = properties.getProperty("pay.zfpay.signkey", "").trim();
            String def_payapiurl = properties.getProperty("pay.zfpay.payapiurl", "").trim();
            String def_notifyurl = properties.getProperty("pay.zfpay.notifyurl", "").trim();
            String def_returnurl = properties.getProperty("pay.zfpay.returnurl", "").trim();

            final Map<String, ZfPayElement> map = new HashMap<>();
            properties.keySet().stream().filter(x -> x.toString().startsWith("pay.zfpay.") && x.toString().endsWith(".appid")).forEach(appid_key -> {
                final String prefix = appid_key.toString().substring(0, appid_key.toString().length() - ".appid".length());

                String appid = properties.getProperty(prefix + ".appid", def_appid).trim();
                String signkey = properties.getProperty(prefix + ".signkey", def_signkey).trim();
                String payapiurl = properties.getProperty(prefix + ".payapiurl", def_payapiurl).trim();
                String notifyurl = properties.getProperty(prefix + ".notifyurl", def_notifyurl).trim();
                String returnurl = properties.getProperty(prefix + ".returnurl", def_returnurl).trim();

                if (appid.isEmpty() || signkey.isEmpty()) {
                    logger.log(Level.WARNING, properties + "; has illegal zfpay conf by prefix" + prefix);
                    return;
                }
                ZfPayElement element = new ZfPayElement();
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
