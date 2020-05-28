/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.kefu;

import com.cratos.platf.base.*;
import java.util.*;
import javax.annotation.Resource;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import static org.redkale.util.Utility.ofMap;

/**
 *
 * @author admin
 */
@RestService(name = "kefu")
public class KefuService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    @Resource(name = "wsgame")
    protected WebSocketNode webSocketNode;

    public List<KefuUser> queryOnLineKefu() {
        if (webSocketNode == null) return new ArrayList<>();
        final List<KefuUser> list = source.queryList(KefuUser.class, FilterNode.create("status", BaseEntity.STATUS_NORMAL));
        final List<KefuUser> rs = new ArrayList<>();
        for (KefuUser kefu : list) {
            if (webSocketNode.existsWebSocket(kefu.getKefuid()).join()) {
                rs.add(kefu);
            }
        }
        if (rs.isEmpty()) {
            KefuUser defKefu = new KefuUser();
            defKefu.setKefuid(UserInfo.USERID_MINKEFU + 1);
            defKefu.setUsername("充值客服1号");
            defKefu.setStar(Math.min(50, 46 + (int) (Math.random() * 5)));
            defKefu.setSales(Math.max(1000, (int) (Math.random() * 10000)));
            rs.add(defKefu);
        }
        return rs;
    }

    @RestMapping(auth = true, comment = "进入客服界面")
    public RetResult<Integer> enterKefuPanel(final int userid, int kefuid) {
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "离开客服界面")
    public RetResult<Integer> leaveKefuPanel(final int userid, int kefuid) {
        return RetResult.success();
    }

    //请求微信充值消息   msgtype: MSGTYPE_PAYURL,  msgcontent:  {"subpaytype":"iewxpay"}
    //请求支付宝充值消息 msgtype: MSGTYPE_PAYURL,  msgcontent:  {"subpaytype":"iealipay"}
    //请求银联充值消息   msgtype: MSGTYPE_PAYURL,  msgcontent:  {"subpaytype":"iebankpay"}
    @RestMapping(auth = true, comment = "给在线客服发送消息")
    public RetResult<String> sendToKefu(int userid, final KefuChatMessage bean) {
        bean.setSrcuserid(userid);
        bean.setCreatetime(System.currentTimeMillis());
        if (!UserInfo.isKefu(bean.getDestuserid())) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        bean.creatKefuchatid();
        webSocketNode.sendMessage(bean, bean.getDestuserid()).whenComplete((rs, e) -> {
            if (bean.getMsgtype() == KefuChatMessage.MSGTYPE_HELLO) {
                List<KefuAutoRecovery> list = source.queryList(KefuAutoRecovery.class, FilterNode.create("type", KefuAutoRecovery.TYPE_HELLO));
                for (KefuAutoRecovery rec : list) {
                    KefuChatMessage automsg = getAutomsg(bean, rec);
                    source.insert(automsg);
                    webSocketNode.sendMessage(ofMap("onKefuChatMessage", automsg), automsg.getDestuserid());//玩家
                    webSocketNode.sendMessage(automsg, automsg.getSrcuserid());//客服
                }
            } else if (bean.getMsgtype() == KefuChatMessage.MSGTYPE_PAYTYPE) {
                List<KefuAutoRecovery> list = source.queryList(KefuAutoRecovery.class, FilterNode.create("type", KefuAutoRecovery.TYPE_PAYURL));
                for (KefuAutoRecovery rec : list) {
                    KefuChatMessage automsg = getAutomsg(bean, rec);
                    if (rec.getDisplay() == 1) {
                        automsg.setMsgtype(KefuChatMessage.MSGTYPE_PAYURL);
                        Map<String, Object> map = new HashMap<>();
                        Map<String, String> msg = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, bean.getMsgcontent());
                        List<KefuPayTypeInfo> info = source.queryList(KefuPayTypeInfo.class, FilterNode.create("type", msg.get("subpaytype")).and("status", FilterExpress.EQUAL, KefuPayTypeInfo.STATUS_NORMAL));
                        map.put("type", info.get(0).getType());
                        map.put("url", info.get(0).getUrl());
                        map.put("qrcode", info.get(0).getQrcode());
                        Map<String, String> bankinfo = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, info.get(0).getBankjson());
                        map.put("bankinfo", bankinfo);
                        automsg.setMsgcontent(JsonConvert.root().convertTo(map));
                    }
                    source.insert(automsg);
                    webSocketNode.sendMessage(ofMap("onKefuChatMessage", automsg), automsg.getDestuserid());//玩家
                    webSocketNode.sendMessage(automsg, automsg.getSrcuserid());//客服
                }
            }
            if (bean.getMsgtype() == KefuChatMessage.MSGTYPE_HELLO || (e == null && rs == 0)) { //hello消息固定已读
                bean.setMsgstatus(KefuChatMessage.MSGSTATUS_READED);
                source.insert(bean);
            } else {
                bean.setMsgstatus(KefuChatMessage.MSGSTATUS_UNREAD);
                source.insert(bean);
            }
        });
        return RetResult.success();
    }

    private KefuChatMessage getAutomsg(KefuChatMessage bean, KefuAutoRecovery rec) {
        await(1);
        KefuChatMessage automsg = new KefuChatMessage();
        automsg.setSrcuserid(bean.getDestuserid()); //kefuid
        automsg.setDestuserid(bean.getSrcuserid()); //playerid
        automsg.setMsgtype(KefuChatMessage.MSGTYPE_TEXT);
        automsg.setMsgstatus(KefuChatMessage.MSGSTATUS_READED);
        automsg.setCreatetime(System.currentTimeMillis());
        automsg.creatKefuchatid();
        automsg.setMsgcontent(rec.getValue());
        return automsg;
    }

    @RestMapping(auth = true, comment = "给在线客服发送消息")
    public RetResult<String> readMessage(int userid, final String kefuchatid) {
        source.updateColumn(KefuChatMessage.class, kefuchatid, ColumnValue.mov("msgstatus", KefuChatMessage.MSGSTATUS_READED));
        return RetResult.success();
    }
}
