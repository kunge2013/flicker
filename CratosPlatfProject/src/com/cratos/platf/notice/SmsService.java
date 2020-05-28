/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.notice;

import com.cratos.platf.base.BaseService;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.boot.Application;
import org.redkale.convert.json.JsonConvert;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 * 发送短信服务
 *
 *
 * @author zhangjx
 */
@Comment("短信服务")
public class SmsService extends BaseService {

    @Resource(name = "platf")
    protected DataSource noticeSource;

    @Resource(name = "property.sms.sendurl")
    private String smssendurl = "http://smssh1.253.com/msg/send/json";

    @Resource(name = "property.sms.account")
    private String smsaccount = "N2720622";

    @Resource(name = "property.sms.password")
    private String smspassword = "EVTXhUYdAKa02c";

    @Resource(name = "property.sms.appname")
    private String appname = "皇家科技";

    private Map<String, String> headers = Utility.ofMap("Content-type", "application/json");

    public boolean isDebug() {
        return this.smsaccount.isEmpty();
    }

    public boolean sendRandomSmsCode(short smstype, String mobile, int randomSmsCode) {
        return sendSmsRecord(smstype, mobile, "【" + appname + "】验证码为: " + randomSmsCode + "");
    }

    public static void main(String[] args) throws Throwable {
        Application.singleton(SmsService.class); //logger
        SmsService service = new SmsService();
        System.out.println(service.sendRandomSmsCode(SmsRecord.CODETYPE_REG, "15136881378", 456894));
    }

    public boolean sendSmsRecord(short smstype, String mobile, String content0) {
        if (smsaccount.isEmpty()) return true; //测试使用
        final SmsRecord message = new SmsRecord(smstype, mobile, content0);
        String content = message.getContent();
        String resultdesc = "encode_content_error";
        boolean ok = false;
        try {
            //content = URLEncoder.encode(content, "UTF-8");
            String body = JsonConvert.root().convertTo(Utility.ofMap("account", this.smsaccount, "password", this.smspassword, "msg", content, "phone", mobile));
            resultdesc = "send_timeout";
            resultdesc = Utility.postHttpContent(this.smssendurl, headers, body);
            if (winos) logger.log(Level.FINEST, "smssend.body=" + body + ", result=" + resultdesc);
            Map<String, String> rs = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, resultdesc);
            ok = "0".equals(rs.get("code"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        message.setResultdesc(resultdesc);
        if (noticeSource != null) {
            message.setStatus(ok ? SmsRecord.SMSSTATUS_SENDOK : SmsRecord.SMSSTATUS_SENDNO);
            message.setSmsid(Utility.format36time(message.getCreatetime()) + "-" + Utility.uuid());
            noticeSource.insert(message);
        }
        return ok;
    }

    @Comment("查询短信记录列表")
    public Sheet<SmsRecord> querySmsRecord(@Comment("过滤条件") SmsBean bean, @Comment("翻页对象") Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return noticeSource.querySheet(SmsRecord.class, flipper, bean);
    }
}
