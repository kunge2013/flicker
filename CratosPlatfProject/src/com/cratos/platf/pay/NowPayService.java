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
import static org.redkalex.pay.Pays.*;

/**
 *
 * @author zhangjx
 */
@DIYPayService(paytype = NowPayService.PAYTYPE_NOWPAY)
public class NowPayService extends AbstractPayService {

    //现在支付
    public static final short PAYTYPE_NOWPAY = 51;

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
    protected Map<String, NowPayElement> elements = new HashMap<>();

    @Resource(name = "property.pay.nowpay.conf") //支付配置文件路径
    protected String conf = "config.properties";

    @Resource(name = "APP_HOME")
    protected File home;

    @Resource
    protected JsonConvert convert;

    @Override
    public void init(AnyValue config) {
        if (this.convert == null) this.convert = JsonConvert.root();
        this.reloadConfig(PAYTYPE_NOWPAY);
    }

    @Override
    @Comment("判断是否支持指定支付类型")
    public boolean supportPayType(final short paytype) {
        return paytype == PAYTYPE_NOWPAY && !elements.isEmpty();
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
                this.elements = NowPayElement.create(logger, properties, home);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "init nowpay conf error", e);
            }
        }
    }

    @Override
    public void destroy(AnyValue config) {
    }

    public static void main(String[] args) throws Throwable {
        NowPayService servie = Application.singleton(NowPayService.class);
        NowPayElement element = new NowPayElement();
        element.appid = "150527094260664";
        element.signkey = "1QNyLKebS9EkXQmvCKlcWFA20xVx5NtV";
        element.signkeymd5 = Utility.binToHexString(md5.digest(element.signkey.getBytes()));
        element.notifyurl = "http://39.108.79.238:6001/pipes/pay/nowpay/notify";
        element.returnurl = "http://39.108.79.238:6001/pay/index.html";
        servie.elements.put("", element);
        servie.elements.put(element.appid, element);

        PayPreRequest req = new PayPreRequest();
        req.setAppid("");
        req.setPaymoney(1);
        req.setClientAddr("127.0.0.1");
        req.setPayno("1000" + System.currentTimeMillis());
        req.setPaytitle("一分钱的东西");
        req.setPaybody("一分钱的东西内容");
        req.setPaytype(PAYTYPE_NOWPAY);
        req.setPayway(PAYWAY_APP);
        System.out.println(req);
        System.out.println(servie.prepay(req));
    }

    @Override
    public PayPreResponse prepay(PayPreRequest request) {
        request.checkVaild();
        final PayPreResponse result = new PayPreResponse();
        try {
            final NowPayElement element = elements.get(request.getAppid());
            if (element == null) return result.retcode(RETPAY_CONF_ERROR);
            result.setAppid(element.appid);
            String notifyurl = (request.getNotifyurl() != null && !request.getNotifyurl().isEmpty()) ? request.getNotifyurl() : element.notifyurl;
            if (!notifyurl.startsWith("http")) notifyurl = "http://" + request.getClienthost() + notifyurl;
            String returnurl = request.getAttach("returnurl", element.returnurl);
            if (!returnurl.startsWith("http")) returnurl = "http://" + request.getClienthost() + returnurl;

            final TreeMap<String, String> map = new TreeMap<>();
            if (request.getAttach() != null) map.putAll(request.getAttach());
            map.put("funcode", "WP001");
            map.put("version", "1.0.0");
            map.put("appId", element.appid);
            map.put("mhtOrderNo", request.getPayno());
            map.put("mhtOrderName", request.getPaytitle());
            map.put("mhtOrderType", "01");
            map.put("mhtCurrencyType", "156");
            map.put("mhtOrderAmt", "" + request.getPaymoney());
            map.put("mhtOrderDetail", request.getPaybody());
            map.put("mhtOrderStartTime", String.format(format, System.currentTimeMillis()));
            map.put("notifyUrl", notifyurl);
            map.put("frontNotifyUrl", returnurl);
            map.put("mhtCharset", "UTF-8");
            map.put("deviceType", "0601");
            map.put("payChannelType", "13"); //银联：11 支付宝：12 微信：13
            map.put("outputType", "2");
            map.put("mhtSignType", "MD5");
            map.put("mhtSignature", createSign(element, map));
            if (logger.isLoggable(Level.FINEST)) logger.finest("nowpay.req.map = " + map);
            final String responseText = Utility.postHttpContent("https://pay.inowpay.cn", joinToString(map));
            result.setResponsetext(responseText);

            Map<String, String> resultmap = parseToMap(responseText);

            if (!"A001".equals(resultmap.get("responseCode"))) return result.retcode(RETPAY_PAY_ERROR);
            if (!checkSign(element, resultmap)) return result.retcode(RETPAY_FALSIFY_ERROR);

            result.setThirdpayno(resultmap.getOrDefault("nowPayOrderNo", ""));
            final Map<String, String> rmap = new TreeMap<>();
            rmap.put("payno", request.getPayno());
            rmap.put("content", resultmap.getOrDefault("tn", ""));
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
        final String rstext = "success=Y";
        Map<String, String> map = request.getAttach();
        String appid = request.getAppid();
        if (appid == null || appid.isEmpty()) appid = map.getOrDefault("appid", "");
        final NowPayElement element = elements.get(appid);
        if (element == null) return result.retcode(RETPAY_CONF_ERROR);
        result.setPayno(map.getOrDefault("mhtOrderNo", ""));
        result.setThirdpayno(map.getOrDefault("nowPayOrderNo", ""));
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        if (!checkSign(element, map)) return result.retcode(RETPAY_FALSIFY_ERROR).notifytext("success=N");
        if (!"A001".equals(map.get("transStatus"))) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        if (Long.parseLong(map.getOrDefault("mhtOrderAmt", "0")) < 1) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        result.setPayedmoney(Long.parseLong(map.getOrDefault("mhtOrderAmt", "0")));
        return result.notifytext(rstext);
    }

    protected String joinToString(Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        Stream<String> stream = map.entrySet().stream().map((e -> e.getKey() + "=" + urlEncodeUTF8(e.getValue())));
        return stream.collect(Collectors.joining("&"));
    }

    protected static Map<String, String> parseToMap(String text) throws IOException {
        TreeMap<String, String> map = new TreeMap<>();
        for (String str : text.split("&")) {
            int pos = str.indexOf('=');
            if (pos < 0) continue;
            map.put(str.substring(0, pos), URLDecoder.decode(str.substring(pos + 1), "UTF-8"));
        }
        return map;
    }

    @Override
    protected String createSign(PayElement element, Map<String, ?> map) throws Exception {
        final StringBuilder sb = new StringBuilder();
        map.forEach((x, y) -> {
            if (!((String) y).isEmpty()) sb.append(x).append('=').append(y).append('&');
        });
        sb.append(((NowPayElement) element).signkeymd5);
        return Utility.binToHexString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes()));
    }

    @Override
    protected boolean checkSign(PayElement element, Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        String sign = (String) map.remove("signature");
        final StringBuilder sb = new StringBuilder();
        map.forEach((x, y) -> {
            if (!((String) y).isEmpty()) sb.append(x).append('=').append(y).append('&');
        });
        sb.append(((NowPayElement) element).signkeymd5);
        try {
            return sign.equals(Utility.binToHexString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes())));
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
    public NowPayElement getPayElement(String appid) {
        return elements.get(appid);
    }

    public static class NowPayElement extends PayElement {

        // pay.nowpay.[x].appid
        public String appid = "";  //APP应用ID

        // pay.nowpay.[x].signkey
        public String signkey = ""; //签名算法需要用到的密钥

        public String signkeymd5 = ""; //签名算法需要用到的密钥的MD5值

        // pay.nowpay.[x].returnurl
        public String returnurl = ""; //前台跳转页面

        public static Map<String, NowPayElement> create(Logger logger, Properties properties, File home) {
            String def_appid = properties.getProperty("pay.nowpay.appid", "").trim();
            String def_signkey = properties.getProperty("pay.nowpay.signkey", "").trim();
            String def_notifyurl = properties.getProperty("pay.nowpay.notifyurl", "").trim();
            String def_returnurl = properties.getProperty("pay.nowpay.returnurl", "").trim();

            final Map<String, NowPayElement> map = new HashMap<>();
            properties.keySet().stream().filter(x -> x.toString().startsWith("pay.nowpay.") && x.toString().endsWith(".appid")).forEach(appid_key -> {
                final String prefix = appid_key.toString().substring(0, appid_key.toString().length() - ".appid".length());

                String appid = properties.getProperty(prefix + ".appid", def_appid).trim();
                String signkey = properties.getProperty(prefix + ".signkey", def_signkey).trim();
                String notifyurl = properties.getProperty(prefix + ".notifyurl", def_notifyurl).trim();
                String returnurl = properties.getProperty(prefix + ".returnurl", def_returnurl).trim();

                if (appid.isEmpty() || signkey.isEmpty()) {
                    logger.log(Level.WARNING, properties + "; has illegal inowpay conf by prefix" + prefix);
                    return;
                }
                NowPayElement element = new NowPayElement();
                element.appid = appid;
                element.signkey = signkey;
                element.signkeymd5 = Utility.binToHexString(md5.digest(signkey.getBytes()));
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
