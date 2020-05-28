/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.pay;

import java.io.*;
import java.net.URLDecoder;
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
import static org.redkalex.pay.Pays.*;

/**
 * 金鑫支付
 * http://39.97.191.5:8088/user_Index_index.html
 *
 * @author zhangjx
 */
@DIYPayService(paytype = JxPayService.PAYTYPE_JXPAY)
public class JxPayService extends AbstractPayService {

    public static final short PAYTYPE_JXPAY = 52;

    protected static final String format = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS";

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
    protected Map<String, JxPayElement> elements = new HashMap<>();

    @Resource(name = "property.pay.jxpay.conf") //支付配置文件路径
    protected String conf = "config.properties";

    @Resource(name = "APP_HOME")
    protected File home;

    @Resource
    protected JsonConvert convert;

    @Override
    public void init(AnyValue config) {
        if (this.convert == null) this.convert = JsonConvert.root();
        this.reloadConfig(PAYTYPE_JXPAY);
    }

    @Override
    @Comment("判断是否支持指定支付类型")
    public boolean supportPayType(final short paytype) {
        return paytype == PAYTYPE_JXPAY && !elements.isEmpty();
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
                this.elements = JxPayElement.create(logger, properties, home);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "init jxpay conf error", e);
            }
        }
    }

    @Override
    public void destroy(AnyValue config) {
    }

    public static void main(String[] args) throws Throwable {
        JxPayService servie = Application.singleton(JxPayService.class);
        JxPayElement element = new JxPayElement();
        element.appid = "10008";
        element.signkey = "8cjlpxpz9e6mk7ejbxdbo3l6rbcsq9a2";
        element.payapihost = "http://39.97.191.5:8088";
        element.notifyurl = "http://47.98.180.255/pipes/pay/jxpay/notify";
        element.returnurl = "http://47.98.180.255/index.html";
        servie.elements.put("", element);
        servie.elements.put(element.appid, element);

        PayPreRequest req = new PayPreRequest();
        req.setAppid("");
        req.setPaymoney(100);
        req.setClientAddr("127.0.0.1");
        req.setPayno("1000" + System.currentTimeMillis());
        req.setPaytitle("十分钱的商品");
        req.setPaybody("一分钱的东西内容");
        req.setPaytype(PAYTYPE_JXPAY);
        req.setSubpaytype(Pays.PAYTYPE_WEIXIN);
        req.setPayway(PAYWAY_APP);
        System.out.println(req);
        //System.out.println(servie.prepay(req));

        PayRequest qryReq = new PayRequest();
        qryReq.setAppid(element.appid);
        qryReq.setPaytype(req.getPaytype());
        qryReq.setPayno(req.getPayno());
        qryReq.setPayno("10001568008901761");
        //System.out.println(servie.query(qryReq));

        String body = "memberid=10008&orderid=10001568008901761&transaction_id=UKJ190909140142545248&amount=1.0000&datetime=20190909140346&returncode=00&sign=A41A8A8036C4A1BC5CF9E62F023FD897&attach=";
        Map<String, String> map = parseToMap(body);
        PayNotifyRequest pnReq = new PayNotifyRequest(PAYTYPE_JXPAY, map);
        System.out.println(servie.notify(pnReq));
    }

    @Override
    public PayPreResponse prepay(PayPreRequest request) {
        request.checkVaild();
        final PayPreResponse result = new PayPreResponse();
        try {
            final JxPayElement element = elements.get(request.getAppid());
            if (element == null) return result.retcode(RETPAY_CONF_ERROR);
            result.setAppid(element.appid);
            String notifyurl = (request.getNotifyurl() != null && !request.getNotifyurl().isEmpty()) ? request.getNotifyurl() : element.notifyurl;
            if (!notifyurl.startsWith("http")) notifyurl = "http://" + request.getClienthost() + notifyurl;
            String returnurl = request.getAttach("returnurl", element.returnurl);
            if (!returnurl.startsWith("http")) returnurl = "http://" + request.getClienthost() + returnurl;

            final TreeMap<String, String> map = new TreeMap<>();
            if (request.getAttach() != null) map.putAll(request.getAttach());
            map.put("pay_memberid", element.appid);
            map.put("pay_orderid", request.getPayno());
            map.put("pay_amount", String.format("%.2f", request.getPaymoney() / 100.0f));
            map.put("pay_applydate", String.format(format, System.currentTimeMillis()));
            map.put("pay_bankcode", "914");
            map.put("pay_notifyurl", notifyurl);
            map.put("pay_callbackurl", returnurl);
            map.put("pay_type", "json");
            map.put("pay_md5sign", createSign(element, map));
            map.put("pay_productname", request.getPaytitle());
            final String url = element.payapihost + "/Pay_Index.html";
            final String body = joinToString(map);
            if (logger.isLoggable(Level.FINEST)) logger.finest("jxpay.req.url = " + url + ", body = " + body);
            final String responseText = Utility.postHttpContent(url, body);
            //{"status":"error","msg":"订单号不合法！","data":[]}
            //{"url":"http://39.97.191.5/person/payment/dopay/orderid/UKF19090222063720568497.html"}
            result.setResponsetext(responseText);
            Map<String, String> resultmap = parseToMap(responseText);

            if (resultmap.get("url") == null) return result.retcode(RETPAY_PAY_ERROR);
            String rsurl = resultmap.get("url");
            rsurl = rsurl.substring(rsurl.lastIndexOf('/') + 1);
            if (rsurl.contains(".htm")) rsurl = rsurl.substring(0, rsurl.indexOf(".htm"));
            result.setThirdpayno(rsurl);
            final Map<String, String> rmap = new TreeMap<>();
            rmap.put("payno", request.getPayno());
            rmap.put("content", resultmap.getOrDefault("url", ""));
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
        if (appid == null || appid.isEmpty()) appid = map.getOrDefault("appid", "");
        final JxPayElement element = elements.get(appid);
        if (element == null) return result.retcode(RETPAY_CONF_ERROR);
        result.setPayno(map.getOrDefault("orderid", ""));
        result.setThirdpayno(map.getOrDefault("transaction_id", ""));
        long amount = (long) (Double.parseDouble(map.getOrDefault("amount", "0")) * 10000);
        result.setPayedmoney(amount / 100);
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        if (!checkSign(element, map)) return result.retcode(RETPAY_FALSIFY_ERROR).notifytext("NO");
        if (!"00".equals(map.get("returncode"))) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        if (amount < 1) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        return result.notifytext(rstext);
    }

    protected String joinToString(Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        Stream<String> stream = map.entrySet().stream().map((e -> e.getKey() + "=" + urlEncodeUTF8(e.getValue())));
        return stream.collect(Collectors.joining("&"));
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
        final String key = "\"data\"";
        final int pos = text.indexOf(key);
        if (pos > 0) {
            int pos1 = text.indexOf('[', pos + key.length());
            int pos2 = text.indexOf(']', pos1);
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
        sb.append("key=").append(((JxPayElement) element).signkey);
        return Utility.binToHexString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes())).toUpperCase();
    }

    @Override
    protected boolean checkSign(AbstractPayService.PayElement element, Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        String sign = (String) map.remove("sign");
        final StringBuilder sb = new StringBuilder();
        map.forEach((x, y) -> {
            if (!((String) y).isEmpty()) sb.append(x).append('=').append(y).append('&');
        });
        sb.append("key=").append(((JxPayElement) element).signkey);
        try {
            return sign.equalsIgnoreCase(Utility.binToHexString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes())));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PayCreatResponse create(PayCreatRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PayQueryResponse query(final PayRequest request) {
        request.checkVaild();
        final PayQueryResponse result = new PayQueryResponse();
        try {
            final JxPayElement element = elements.get(request.getAppid());
            if (element == null) return result.retcode(RETPAY_CONF_ERROR);

            final TreeMap<String, String> map = new TreeMap<>();
            map.put("pay_memberid", element.appid);
            map.put("pay_orderid", request.getPayno());
            map.put("pay_md5sign", createSign(element, map));
            final String url = element.payapihost + "/Pay_Trade_query";
            final String body = joinToString(map);
            if (logger.isLoggable(Level.FINEST)) logger.finest("jxpay.req.url = " + url + ", body = " + body);
            final String responseText = Utility.postHttpContent(url, body);
            //{"memberid":"10008","orderid":"10001567491860487","amount":"0.0100","time_end":"1970-01-01 08:00:00","transaction_id":"UKJ190903142420529756","returncode":"00","trade_state":"NOTPAY","sign":"6CB59128463938717C764C524687D5F4"}
            result.setResponsetext(responseText);
            Map<String, String> resultmap = parseToMap(responseText);
            if (!checkSign(element, resultmap)) return result.retcode(RETPAY_FALSIFY_ERROR);

            short paystatus = "SUCCESS".equals(resultmap.get("trade_state")) ? PAYSTATUS_PAYOK : PAYSTATUS_PAYNO;
            result.setPaystatus(paystatus);
            result.setThirdpayno(resultmap.getOrDefault("transaction_id", ""));
            result.setPayedmoney((long) (Double.parseDouble(resultmap.getOrDefault("amount", "0")) * 100));
        } catch (Exception e) {
            result.setRetcode(RETPAY_PAY_ERROR);
            logger.log(Level.WARNING, "query_pay_error req=" + request + ", resp=" + result.getResponsetext(), e);
        }
        return result;
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
    public JxPayElement getPayElement(String appid) {
        return elements.get(appid);
    }

    public static class JxPayElement extends AbstractPayService.PayElement {

        // pay.jxpay.[x].appid
        public String appid = "";  //APP应用ID

        // pay.jxpay.[x].signkey
        public String signkey = ""; //签名算法需要用到的密钥

        // pay.zfpay.[x].payapihost
        public String payapihost = "";  //APP支付接口host

        // pay.jxpay.[x].returnurl
        public String returnurl = ""; //前台跳转页面

        public static Map<String, JxPayElement> create(Logger logger, Properties properties, File home) {
            String def_appid = properties.getProperty("pay.jxpay.appid", "").trim();
            String def_signkey = properties.getProperty("pay.jxpay.signkey", "").trim();
            String def_payapihost = properties.getProperty("pay.jxpay.payapihost", "").trim();
            String def_notifyurl = properties.getProperty("pay.jxpay.notifyurl", "").trim();
            String def_returnurl = properties.getProperty("pay.jxpay.returnurl", "").trim();

            final Map<String, JxPayElement> map = new HashMap<>();
            properties.keySet().stream().filter(x -> x.toString().startsWith("pay.jxpay.") && x.toString().endsWith(".appid")).forEach(appid_key -> {
                final String prefix = appid_key.toString().substring(0, appid_key.toString().length() - ".appid".length());

                String appid = properties.getProperty(prefix + ".appid", def_appid).trim();
                String signkey = properties.getProperty(prefix + ".signkey", def_signkey).trim();
                String payapihost = properties.getProperty(prefix + ".payapihost", def_payapihost).trim();
                String notifyurl = properties.getProperty(prefix + ".notifyurl", def_notifyurl).trim();
                String returnurl = properties.getProperty(prefix + ".returnurl", def_returnurl).trim();

                if (appid.isEmpty() || signkey.isEmpty()) {
                    logger.log(Level.WARNING, properties + "; has illegal ijxpay conf by prefix" + prefix);
                    return;
                }
                JxPayElement element = new JxPayElement();
                element.appid = appid;
                element.signkey = signkey;
                element.payapihost = payapihost;
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
