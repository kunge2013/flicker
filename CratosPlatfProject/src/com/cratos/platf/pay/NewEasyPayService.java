/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.pay;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;
import javax.annotation.Resource;
import org.redkale.boot.Application;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.*;
import static org.redkalex.pay.PayRetCodes.*;
import org.redkalex.pay.*;
import static org.redkalex.pay.Pays.*;

/**
 *
 * @author zhangjx
 */
@DIYPayService(paytype = NewEasyPayService.PAYTYPE_NEWEASYPAY)
public class NewEasyPayService extends AbstractPayService {

    public static final short PAYTYPE_NEWEASYPAY = 55;

    //配置集合
    protected Map<String, NewEasyPayElement> elements = new HashMap<>();

    @Resource(name = "property.pay.neweasypay.conf") //支付配置文件路径
    protected String conf = "config.properties";

    @Resource(name = "APP_HOME")
    protected File home;

    @Resource
    protected JsonConvert convert;

    @Override
    public void init(AnyValue config) {
        if (this.convert == null) this.convert = JsonConvert.root();
        this.reloadConfig(PAYTYPE_NEWEASYPAY);
    }

    @Override
    @Comment("判断是否支持指定支付类型")
    public boolean supportPayType(final short paytype) {
        return paytype == PAYTYPE_NEWEASYPAY && !elements.isEmpty();
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
                this.elements = NewEasyPayElement.create(logger, properties, home);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "init neweasypay conf error", e);
            }
        }
    }

    @Override
    public void destroy(AnyValue config) {
    }

    public static void main(String[] args) throws Throwable {
        NewEasyPayService servie = Application.singleton(NewEasyPayService.class);
        NewEasyPayElement element = new NewEasyPayElement();
        element.appid = "201000200";
        element.signkey = "592672621B9F46F0A15D3F83F860D9EB";
        element.payapihost = "http://47.52.30.212";
        element.notifyurl = "http://47.98.180.255/pipes/pay/neweasypay/notify";
        element.returnurl = "http://47.98.180.255/index.html";
        servie.elements.put("", element);
        servie.elements.put(element.appid, element);

        PayPreRequest req = new PayPreRequest();
        req.setAppid("");
        req.setPaymoney(10000);
        req.setClientAddr("127.0.0.1");
        req.setPayno("1000" + System.currentTimeMillis());
        req.setPaytitle("50元的商品");
        req.setPaybody("50元的东西内容");
        req.setPaytype(PAYTYPE_NEWEASYPAY);
        req.setSubpaytype(Pays.PAYTYPE_ALIPAY);
        req.setPayway(PAYWAY_APP);
        System.out.println(req);
        System.out.println(servie.prepay(req));
        if (true) return;
        PayRequest qryReq = new PayRequest();
        qryReq.setAppid(element.appid);
        qryReq.setPaytype(req.getPaytype());
        qryReq.setPayno(req.getPayno());
        qryReq.setPayno("10001568008901761");
        //System.out.println(servie.query(qryReq));

        String body = "memberid=10008&orderid=10001568008901761&transaction_id=UKJ190909140142545248&amount=1.0000&datetime=20190909140346&returncode=00&sign=A41A8A8036C4A1BC5CF9E62F023FD897&attach=";
        Map<String, String> map = parseToMap(body);
        PayNotifyRequest pnReq = new PayNotifyRequest(PAYTYPE_NEWEASYPAY, map);
        System.out.println(servie.notify(pnReq));
    }

    @Override
    public PayPreResponse prepay(PayPreRequest request) {
        request.checkVaild();
        final PayPreResponse result = new PayPreResponse();
        try {
            final NewEasyPayElement element = elements.get(request.getAppid());
            if (element == null) return result.retcode(RETPAY_CONF_ERROR);
            result.setAppid(element.appid);
            String notifyurl = (request.getNotifyurl() != null && !request.getNotifyurl().isEmpty()) ? request.getNotifyurl() : element.notifyurl;
            if (!notifyurl.startsWith("http")) notifyurl = "http://" + request.getClienthost() + notifyurl;
            String returnurl = request.getAttach("returnurl", element.returnurl);
            if (!returnurl.startsWith("http")) returnurl = "http://" + request.getClienthost() + returnurl;

            final TreeMap<String, String> map = new TreeMap<>();
            if (request.getAttach() != null) map.putAll(request.getAttach());
            final String subpaytype = request.getSubpaytype() == Pays.PAYTYPE_ALIPAY ? "wap" : (request.getSubpaytype() == Pays.PAYTYPE_WEIXIN ? "wxwap" : "" + request.getSubpaytype());

            map.put("merchant", element.appid);
            map.put("amount", "" + request.getPaymoney());
            map.put("pay_type", subpaytype);
            map.put("order_no", request.getPayno());
            map.put("order_time", "" + System.currentTimeMillis());
            map.put("subject", request.getPaytitle());
            map.put("notify_url", notifyurl);
            map.put("callback_url", returnurl);
            map.put("sign", createSign(element, map));
            final String url = element.payapihost + "/api/addOrder";
            final String body = joinToString(map);
            if (logger.isLoggable(Level.FINEST)) logger.finest("neweasypay.req.url = " + url + ", body = " + body);
            final String responseText = Utility.postHttpContent(url, body);
            //{"code":"0000","success":true,"error":false,"errorType":null,"errorMsg":null,"result":{"orderNo":"10001580550096445","qrCode":"http://cashier.ekouqin.com/page/match?orderNo=202002011741365144879&amount=100.00","sign":"9305C958F5ED2A3BAABDE47FD49A1CB14D6B597D"},"data":{},"msg":"下单成功"}
            System.out.println("responseText=" + responseText);
            result.setResponsetext(responseText);
            if (!responseText.contains("\"code\":\"0000\"")) return result.retcode(RETPAY_PAY_ERROR);
            final String rskey = "\"result\":";
            String rstext = responseText.substring(responseText.indexOf(rskey) + rskey.length());
            rstext = rstext.substring(0, rstext.indexOf('}') + 1);
            Map<String, String> resultmap = parseToMap(rstext);
            result.setThirdpayno(resultmap.get("sign"));
            final Map<String, String> rmap = new TreeMap<>();
            rmap.put("payno", request.getPayno());
            rmap.put("content", resultmap.getOrDefault("qrCode", ""));
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
        if (appid == null || appid.isEmpty()) appid = map.getOrDefault("merchant", "");
        final NewEasyPayElement element = elements.get(appid);
        if (element == null) return result.retcode(RETPAY_CONF_ERROR);
        result.setPayno(map.getOrDefault("orderId", ""));
        result.setThirdpayno(map.getOrDefault("tranNo", ""));
        int amount = (int) (Double.parseDouble(map.getOrDefault("amount", "0")) * 100);
        result.setPayedmoney(amount);
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        if (!checkSign(element, map)) return result.retcode(RETPAY_FALSIFY_ERROR).notifytext("failed");
        if (amount < 1) return result.retcode(RETPAY_PAY_FAILED).notifytext(rstext);
        return result.notifytext(rstext);
    }

    protected String joinToString(Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        Stream<String> stream = map.entrySet().stream().map((e -> e.getKey() + "=" + urlEncodeUTF8(e.getValue())));
        return stream.collect(Collectors.joining("&"));
    }

    protected static Map<String, String> parseToMap(final String text) throws IOException {
        return JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, text);
    }

    @Override
    protected String createSign(AbstractPayService.PayElement element, Map<String, ?> map) throws Exception {
        final StringBuilder sb = new StringBuilder();
        map.forEach((x, y) -> {
            if (!((String) y).isEmpty()) sb.append(x).append('=').append(y).append('&');
        });
        sb.append("key=").append(((NewEasyPayElement) element).signkey);
        return Utility.binToHexString(MessageDigest.getInstance("SHA-1").digest(sb.toString().getBytes())).toLowerCase();
    }

    @Override
    protected boolean checkSign(AbstractPayService.PayElement element, Map<String, ?> map) {
        if (!(map instanceof SortedMap)) map = new TreeMap<>(map);
        String sign = (String) map.remove("sign");
        final StringBuilder sb = new StringBuilder();
        map.forEach((x, y) -> {
            if (!((String) y).isEmpty()) sb.append(x).append('=').append(y).append('&');
        });
        sb.append("key=").append(((NewEasyPayElement) element).signkey);
        try {
            return sign.equalsIgnoreCase(Utility.binToHexString(MessageDigest.getInstance("SHA-1").digest(sb.toString().getBytes())));
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
            final NewEasyPayElement element = elements.get(request.getAppid());
            if (element == null) return result.retcode(RETPAY_CONF_ERROR);

            final TreeMap<String, String> map = new TreeMap<>();
            map.put("pay_memberid", element.appid);
            map.put("pay_orderid", request.getPayno());
            map.put("pay_md5sign", createSign(element, map));
            final String url = element.payapihost + "/Pay_Trade_query";
            final String body = joinToString(map);
            if (logger.isLoggable(Level.FINEST)) logger.finest("neweasypay.req.url = " + url + ", body = " + body);
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
    public NewEasyPayElement getPayElement(String appid) {
        return elements.get(appid);
    }

    public static class NewEasyPayElement extends AbstractPayService.PayElement {

        // pay.neweasypay.[x].appid
        public String appid = "";  //APP应用ID

        // pay.neweasypay.[x].signkey
        public String signkey = ""; //签名算法需要用到的密钥

        // pay.zfpay.[x].payapihost
        public String payapihost = "";  //APP支付接口host

        // pay.neweasypay.[x].returnurl
        public String returnurl = ""; //前台跳转页面

        public static Map<String, NewEasyPayElement> create(Logger logger, Properties properties, File home) {
            String def_appid = properties.getProperty("pay.neweasypay.appid", "").trim();
            String def_signkey = properties.getProperty("pay.neweasypay.signkey", "").trim();
            String def_payapihost = properties.getProperty("pay.neweasypay.payapihost", "").trim();
            String def_notifyurl = properties.getProperty("pay.neweasypay.notifyurl", "").trim();
            String def_returnurl = properties.getProperty("pay.neweasypay.returnurl", "").trim();

            final Map<String, NewEasyPayElement> map = new HashMap<>();
            properties.keySet().stream().filter(x -> x.toString().startsWith("pay.neweasypay.") && x.toString().endsWith(".appid")).forEach(appid_key -> {
                final String prefix = appid_key.toString().substring(0, appid_key.toString().length() - ".appid".length());

                String appid = properties.getProperty(prefix + ".appid", def_appid).trim();
                String signkey = properties.getProperty(prefix + ".signkey", def_signkey).trim();
                String payapihost = properties.getProperty(prefix + ".payapihost", def_payapihost).trim();
                String notifyurl = properties.getProperty(prefix + ".notifyurl", def_notifyurl).trim();
                String returnurl = properties.getProperty(prefix + ".returnurl", def_returnurl).trim();

                if (appid.isEmpty() || signkey.isEmpty()) {
                    logger.log(Level.WARNING, properties + "; has illegal ineweasypay conf by prefix" + prefix);
                    return;
                }
                NewEasyPayElement element = new NewEasyPayElement();
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
