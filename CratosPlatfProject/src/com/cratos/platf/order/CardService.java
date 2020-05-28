/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.*;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.boot.Application;
import org.redkale.convert.json.JsonConvert;
import org.redkale.service.RetResult;
import org.redkale.source.DataSource;
import org.redkale.util.*;

/**
 * 银行卡等支付账号
 *
 * @author zhangjx
 */
public class CardService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    protected Map<String, String> bankTypes;

    @Override
    public void init(AnyValue conf) {
        try {
            InputStream in = this.getClass().getResourceAsStream("/" + this.getClass().getPackage().getName().replace('.', '/') + "/banktypes.json");
            this.bankTypes = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, new String(Utility.readBytesThenClose(in), "UTF-8"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "read banktypes.json error", e);
        }
    }

    public static void main(String[] args) throws Throwable {
        CardService service = Application.singleton(CardService.class);
        service.init(null);
        CardInfo card = new CardInfo();
        card.setUserid(3000001);
        card.setCardaccount("5545434");
        card.setCardcity("北京");
        card.setCardprovince("北京");
        card.setCardbankname("工商银行");
        card.setCardbanktype("ICBC");
        card.setCardtype("DC");
        card.setCardsubbranch("");
        card.setCardrealname("张先生");
        service.updateCardInfo(card);
    }

    public CardInfo findCardInfo(int userid) {
        return source.find(CardInfo.class, userid);
    }

    //更新银行卡或支付宝账号信息， 一次只能更新一种
    public RetResult<String> updateCardInfo(CardInfo bean) {
        if (bean == null || bean.getUserid() < 1) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        final CardInfo old = source.find(CardInfo.class, bean.getUserid());
        boolean alipay = !bean.getAlipayaccount().isEmpty();
        //支付宝修改
        if (alipay && bean.getAlipayrealname().isEmpty()) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (alipay && (!bean.getCardaccount().isEmpty() || !bean.getCardbankname().isEmpty()
            || !bean.getCardbanktype().isEmpty() || !bean.getCardprovince().isEmpty()
            || !bean.getCardcity().isEmpty() || !bean.getCardrealname().isEmpty()
            || !bean.getCardsubbranch().isEmpty() || !bean.getCardtype().isEmpty())) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        //银行卡修改
        if (!alipay && (!bean.getAlipayaccount().isEmpty()
            || !bean.getAlipayrealname().isEmpty())) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (!alipay && (bean.getCardaccount().isEmpty() || bean.getCardbankname().isEmpty()
            || bean.getCardbanktype().isEmpty() || bean.getCardprovince().isEmpty()
            || bean.getCardcity().isEmpty() || bean.getCardrealname().isEmpty()
            || bean.getCardtype().isEmpty())) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (!alipay) {
            if (this.bankTypes.get(bean.getCardbanktype()) == null) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
            bean.setCardbankname(this.bankTypes.get(bean.getCardbanktype()));
        }
        if (old == null) {
            if (alipay) {//支付宝修改
                bean.setCardstatus((short) 0);
                bean.setCreatetime(System.currentTimeMillis());
                bean.setAlipaystatus(CardInfo.CARDSTATUS_UNCHECK);
                source.insert(bean);
            } else {//银行卡修改
                bean.setAlipaystatus((short) 0);
                bean.setCreatetime(System.currentTimeMillis());
                bean.setCardstatus(CardInfo.CARDSTATUS_UNCHECK);
                source.insert(bean);
            }
            return RetResult.success();
        } else {
            if (alipay) {//支付宝修改
                if (!bean.getAlipayaccount().contains("*")) { //带星号视为没修改账号
                    old.setAlipayaccount(bean.getAlipayaccount());
                }
                old.setAlipayrealname(bean.getAlipayrealname());
                old.setAlipaystatus(CardInfo.CARDSTATUS_UNCHECK);
                source.updateColumn(old, "alipayaccount", "alipayrealname", "alipaystatus");
            } else {//银行卡修改
                if (!bean.getCardaccount().contains("*")) {//带星号视为没修改账号
                    old.setCardaccount(bean.getCardaccount());
                }
                old.setCardbanktype(bean.getCardbanktype());
                old.setCardbankname(bean.getCardbankname());
                old.setCardrealname(bean.getCardrealname());
                old.setCardtype(bean.getCardtype());
                old.setCardcity(bean.getCardcity());
                old.setCardprovince(bean.getCardprovince());
                old.setCardsubbranch(bean.getCardsubbranch());
                old.setCardstatus(CardInfo.CARDSTATUS_UNCHECK);
                source.updateColumn(old, "cardaccount", "cardbanktype", "cardbankname", "cardrealname", "cardtype", "cardcity", "cardprovince", "cardsubbranch", "cardstatus");
            }
            return RetResult.success();
        }
    }

}
