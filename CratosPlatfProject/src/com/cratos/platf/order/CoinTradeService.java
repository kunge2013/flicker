/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.*;
import static com.cratos.platf.order.CardInfo.*;
import com.cratos.platf.user.UserService;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 * 玩家兑换服务
 *
 * @author zhangjx
 */
public class CoinTradeService extends BaseService {

    private final Object dealTradeLock = new Object();

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected CardService cardService;

    //兑换
    public RetResult<Long> tradeCoin(CoinTradeRecord bean) {
        if (bean == null || bean.getUserid() < 1 || bean.getCoin() < 100) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (bean.getTradetype() != TRADE_TYPE_BANK && bean.getTradetype() != TRADE_TYPE_ALIPAY) {
            return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        }
        CardInfo card = cardService.findCardInfo(bean.getUserid());
        if (card == null) return RetCodes.retResult(RetCodes.RET_USER_PROFIT_CARD_ILLEGAL);
        if (bean.getTradetype() == TRADE_TYPE_BANK && card.getCardaccount().isEmpty()) {
            return RetCodes.retResult(RetCodes.RET_USER_PROFIT_CARD_ILLEGAL);
        }
        if (bean.getTradetype() == TRADE_TYPE_ALIPAY && card.getAlipayaccount().isEmpty()) {
            return RetCodes.retResult(RetCodes.RET_USER_PROFIT_CARD_ILLEGAL);
        }
        if (source.exists(CoinTradeRecord.class, FilterNode.create("userid", bean.getUserid()).and("tradestatus", TRADE_STATUS_PENDING))) {
            return RetCodes.retResult(RetCodes.RET_USER_TRADE_TRADING);
        }
        synchronized (this) {
            UserInfo user = userService.findUserInfo(bean.getUserid());
            if (user == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
            if (user.isMate()) return RetCodes.retResult(RetCodes.RET_USER_MATE_AUTHILLEGAL);
            if ((user.getCoins() + user.getBankcoins()) < bean.getCoin()) return RetCodes.retResult(RetCodes.RET_USER_COINS_NOTENOUGH);
            long money = (long) (bean.getCoin() * 100 / GoodsInfo.EXCHANGE_RMB_COIN);
            long now = System.currentTimeMillis();
            final String tradeid = Integer.toString(bean.getUserid(), 36) + "-" + Utility.format36time(now);
            try {
                userService.decrePlatfUserCoins(user.getUserid(), bean.getCoin(), now, "exchange", "money=" + money + ";tradeid=" + tradeid);
            } catch (RuntimeException ex) {
                logger.log(Level.SEVERE, "bean=" + bean + ", user=" + user + ", trade error", ex);
                return RetCodes.retResult(RetCodes.RET_INNER_ILLEGAL);
            }
            CoinTradeRecord record = new CoinTradeRecord();
            record.setUserid(bean.getUserid());
            record.setTradetype(bean.getTradetype());
            record.setCoin(bean.getCoin());
            record.setMoney(money);
            record.setCreatetime(now);
            if (bean.getTradetype() == TRADE_TYPE_BANK) {
                record.setTradeaccount(card.getCardaccount());
                record.setTradejson(card.toCardJson());
            } else {
                record.setTradeaccount(card.getAlipayaccount());
                record.setTradejson(card.toAlipayJson());
            }
            record.setTradestatus(TRADE_STATUS_PENDING);
            record.setTradeid(tradeid);
            source.insert(new CoinTradeRecordHis(record));
            source.insert(record);
        }
        return RetResult.success();
    }

    public Sheet<CoinTradeRecord> queryCoinTradeRecord(CoinTradeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(CoinTradeRecord.class, flipper, bean);
    }

    public Sheet<CoinTradeRecordHis> queryCoinTradeRecordHis(CoinTradeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(CoinTradeRecordHis.class, flipper, bean);
    }

    public RetResult dealTrade(String tradeid, short tradestatus, String remark, int memberid) {
        if (tradeid == null) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (tradestatus != TRADE_STATUS_DONEOK && tradestatus != TRADE_STATUS_DONENO) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        synchronized (dealTradeLock) {
            long now = System.currentTimeMillis();
            CoinTradeRecord record = source.find(CoinTradeRecord.class, tradeid);
            if (record == null) return RetCodes.retResult(RetCodes.RET_USER_TRADE_NOT_EXISTS);
            if (record.getTradestatus() != TRADE_STATUS_PENDING) return RetCodes.retResult(RetCodes.RET_USER_TRADE_FINISHED);
            int rs = source.updateColumn(CoinTradeRecordHis.class, tradeid, ColumnValue.mov("tradestatus", tradestatus),
                ColumnValue.mov("finishtime", now), ColumnValue.mov("memberid", memberid), ColumnValue.mov("remark", remark == null ? "" : remark));
            if (rs != 1) return RetCodes.retResult(RetCodes.RET_USER_TRADE_NOT_EXISTS);
            if (tradestatus == TRADE_STATUS_DONENO) { //退还
                try {
                    userService.increPlatfUserCoins(record.getUserid(), record.getCoin(), now, "exchange", "deal back;tradeid=" + record.getTradeid());
                } catch (RuntimeException ex) {
                    logger.log(Level.SEVERE, "tradeid=" + tradeid + ", record=" + record + ", dealTrade error", ex);
                    return RetCodes.retResult(RetCodes.RET_INNER_ILLEGAL);
                }
            }
            source.delete(record);
            return RetResult.success();
        }
    }
}
