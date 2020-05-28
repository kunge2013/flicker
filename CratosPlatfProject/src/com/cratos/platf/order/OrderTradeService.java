/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.*;
import static com.cratos.platf.base.RetCodes.*;
import com.cratos.platf.info.*;
import static com.cratos.platf.order.CardInfo.*;
import com.cratos.platf.user.UserService;
import java.util.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.boot.Application;
import org.redkale.convert.json.JsonConvert;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class OrderTradeService extends BaseService {

    private final Object dealTradeLock = new Object();

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected DictService dictService;

    @Resource
    protected GoodsService goodsService;

    public static void main(String[] args) throws Throwable {
        OrderTradeService service = Application.singleton(OrderTradeService.class);
        OrderTradeRecord bean = new OrderTradeRecord();
        bean.setUserid(3000001);
        bean.setGoodsid(100002);
        bean.setGoodstype((short) 101);
        Map<String, String> map = new HashMap<>();
        map.put("userBankName", "招商银行");
        map.put("userRealName", "张先生");
        map.put("userMoney", "3000");
        bean.setTradejson(JsonConvert.root().convertTo(map));
        RetResult<String> rs = service.tradeOrder(bean);
        System.out.println(rs);
        System.out.println(service.dealTrade(rs.getResult(), TRADE_STATUS_DONEOK, "成功", 0));
        Thread.sleep(3000);
    }

    //提交充值证明
    public RetResult<String> tradeOrder(OrderTradeRecord bean) {
        if (bean == null || bean.getUserid() < 1 || bean.getTradejson() == null || bean.getTradejson().isEmpty()) {
            return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        }
        final UserInfo user = userService.findUserInfo(bean.getUserid());
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (user.isMate()) return RetCodes.retResult(RET_USER_MATE_AUTHILLEGAL);

        if (bean.getGoodstype() != GoodsInfo.GOODS_TYPE_COIN && bean.getGoodstype() != GoodsInfo.GOODS_TYPE_XCOIN
            && bean.getGoodstype() != GoodsInfo.GOODS_TYPE_DIAMOND && bean.getGoodstype() != GoodsInfo.GOODS_TYPE_XDIAMOND) {
            return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        }
        if (source.exists(OrderTradeRecord.class, FilterNode.create("userid", bean.getUserid()).and("tradestatus", TRADE_STATUS_PENDING))) {
            return RetCodes.retResult(RetCodes.RET_USER_TRADE_TRADING);
        }
        if (bean.getGoodsid() > 0) {
            final GoodsInfo goods = goodsService.findGoods(bean.getGoodsid());
            if (goods == null) return RetCodes.retResult(RET_ORDER_GOODS_NOTEXISTS);
            if (goods.getEndtime() > 0 && goods.getEndtime() < System.currentTimeMillis()) {
                return RetCodes.retResult(RET_ORDER_GOODS_EXPIRED);
            }
            bean.setMoney(goods.getPrice());
            bean.setBuytype(goods.getBuytype());
            bean.setGoodscount(goods.getGoodscount());
            bean.setGoodsitems(goods.getGoodsitems());
            bean.setGiftitems(goods.getGiftitems());
        } else if (bean.getGoodstype() == GoodsInfo.GOODS_TYPE_XCOIN) {
            if (bean.getMoney() < 100) return RetCodes.retResult(RET_USER_MONEY_TOOSMALL);
            bean.setGoodscount((int) (bean.getMoney() * GoodsInfo.EXCHANGE_RMB_COIN / 100));
            bean.setBuytype(GoodsInfo.GOODS_BUY_RMB);
        } else if (bean.getGoodstype() == GoodsInfo.GOODS_TYPE_XDIAMOND) {
            if (bean.getMoney() < 100) return RetCodes.retResult(RET_USER_MONEY_TOOSMALL);
            bean.setGoodscount((int) (bean.getMoney() * GoodsInfo.EXCHANGE_RMB_DIAMOND / 100));
            bean.setBuytype(GoodsInfo.GOODS_BUY_RMB);
        } else {
            return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        }
        Map<String, String> tradejsonmap = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, bean.getTradejson());
        if (!tradejsonmap.containsKey("userBankName") || !tradejsonmap.containsKey("userRealName") || !tradejsonmap.containsKey("userMoney")) {
            return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        }
        final long now = System.currentTimeMillis();
        Map<String, String> map = new HashMap<>();
        map.put("orderTradeBankAccount", dictService.findDictValue(DictInfo.PLATF_ORDERTRADE_BANKACCOUNT, ""));
        map.put("orderTradeBankUser", dictService.findDictValue(DictInfo.PLATF_ORDERTRADE_BANKUSER, ""));
        map.put("orderTradeBankName", dictService.findDictValue(DictInfo.PLATF_ORDERTRADE_BANKNAME, ""));
        bean.setOsbankjson(JsonConvert.root().convertTo(map));
        bean.setCreatetime(now);
        bean.setRemark("");
        final String tradeid = Integer.toString(bean.getUserid(), 36) + "-" + Utility.format36time(now);
        bean.setTradeid(tradeid);
        bean.setTradestatus(TRADE_STATUS_PENDING);
        source.insert(new OrderTradeRecordHis(bean));
        source.insert(bean);
        return new RetResult(tradeid);
    }

    public Sheet<OrderTradeRecord> queryOrderTradeRecord(OrderTradeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(OrderTradeRecord.class, flipper, bean);
    }

    public Sheet<OrderTradeRecordHis> queryOrderTradeRecordHis(OrderTradeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(OrderTradeRecordHis.class, flipper, bean);
    }

    public RetResult dealTrade(String tradeid, short tradestatus, String remark, int memberid) {
        if (tradeid == null) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (tradestatus != TRADE_STATUS_DONEOK && tradestatus != TRADE_STATUS_DONENO) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        synchronized (dealTradeLock) {
            long now = System.currentTimeMillis();
            OrderTradeRecord record = source.find(OrderTradeRecord.class, tradeid);
            if (record == null) return RetCodes.retResult(RetCodes.RET_USER_TRADE_NOT_EXISTS);
            if (record.getTradestatus() != TRADE_STATUS_PENDING) return RetCodes.retResult(RetCodes.RET_USER_TRADE_FINISHED);
            int rs = source.updateColumn(OrderTradeRecordHis.class, tradeid, ColumnValue.mov("tradestatus", tradestatus),
                ColumnValue.mov("finishtime", now), ColumnValue.mov("memberid", memberid), ColumnValue.mov("remark", remark == null ? "" : remark));
            if (rs != 1) return RetCodes.retResult(RetCodes.RET_USER_TRADE_NOT_EXISTS);
            if (tradestatus == TRADE_STATUS_DONEOK) { //收款验证通过
                try {
                    if (record.getGoodstype() == GoodsInfo.GOODS_TYPE_COIN || record.getGoodstype() == GoodsInfo.GOODS_TYPE_XCOIN) {
                        userService.rechargeUserCoins(record.getUserid(), record.getMoney(), record.getGoodscount(), now, "打款充值;ordertradeid=" + record.getTradeid());
                    } else if (record.getGoodstype() == GoodsInfo.GOODS_TYPE_DIAMOND || record.getGoodstype() == GoodsInfo.GOODS_TYPE_XDIAMOND) {
                        userService.rechargeUserDiamonds(record.getUserid(), record.getMoney(), record.getGoodscount(), now, "打款充值;ordertradeid=" + record.getTradeid());
                    }
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
