/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.profit;

import com.cratos.platf.base.*;
import com.cratos.platf.order.CardInfo;
import com.cratos.platf.order.CardService;
import com.cratos.platf.order.*;
import static com.cratos.platf.order.CardInfo.*;
import com.cratos.platf.user.UserDayRecord;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 * 用户收益服务
 *
 * @author zhangjx
 */
public class ProfitService extends BaseService {

    @Comment("收益金币千分比")
    public static final int PROFIT_COIN_PERMILLAGE = 15;

    @Comment("其他子级返利千分比")
    public static final int SUBCHILD_PERMILLAGE = 300;

    private final Object dealTradeLock = new Object();

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected CardService cardService;

    protected ScheduledThreadPoolExecutor scheduler;

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, "ProfitService-Task-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> { //凌晨过10秒执行
            try {
                updateDayRecord();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, Utility.midnight() + 24 * 60 * 60 * 1000L - System.currentTimeMillis() + 10_000L, 24 * 60 * 60 * 1000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue config) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void updateDayRecord() {
        final long now = System.currentTimeMillis() / 60_000 * 60_000;
        final int intday = Utility.yesterday();
        final long time = Utility.midnight() - 300_000L;
        {
            ProfitDayRecord r = new ProfitDayRecord();
            r.setUserid(1);
            r.setIntday(intday);
            r.setCreatetime(time);
            try {
                source.insert(r);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "insert ProfitDayRecord(userid=1)", e);
            }
            source.delete(ProfitDayRecord.class, FilterNode.create("createtime", r.getCreatetime()).and("userid", r.getUserid()));
        }
        final Map<Integer, Integer> userToAgencyMap = source.queryColumnMap(UserInfo.class, "userid", null, "agencyid", FilterNode.create("userid", FilterExpress.GREATERTHAN, UserInfo.MIN_NORMAL_USERID));
        final Map<Integer, AtomicLong> childmap = new HashMap<>();
        userToAgencyMap.values().stream().filter(x -> x > 0).forEach(key -> childmap.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet());
        //生成玩家数信息
        genDayRecord(userToAgencyMap, intday, childmap, now);
        //更新玩家返利信息
        updateDayRecord(userToAgencyMap, intday, childmap, now);

    }

    //更新玩家返利信息
    private void updateDayRecord(final Map<Integer, Integer> userToAgencyMap, final int intday, final Map<Integer, AtomicLong> childmap, final long now) {
        FilterNode node = FilterNode.create("intday", intday).and("costcoins", FilterExpress.GREATERTHAN, 0);
        Flipper flipper = new Flipper(100);
        Sheet<ProfitDayRecord> sheet = source.querySheet(ProfitDayRecord.class, flipper, node);
        while (!sheet.isEmpty()) {
            for (ProfitDayRecord record : sheet) {
                updateOneDayRecord(record.getUserid(), record.getCostcoins(), "childcostcoins", userToAgencyMap, intday, childmap, now);
            }
            sheet = source.querySheet(ProfitDayRecord.class, flipper.next(), node);
        }
        //更新profitcoins字段
        node = FilterNode.create("intday", intday).and(FilterNode.create("childcostcoins", FilterExpress.GREATERTHAN, 0L).or("subchildcostcoins", FilterExpress.GREATERTHAN, 0L));
        flipper = new Flipper(100);
        sheet = source.querySheet(ProfitDayRecord.class, flipper, node);
        while (!sheet.isEmpty()) {
            for (ProfitDayRecord record : sheet) {
                record.setChildprofitcoins(record.getChildcostcoins() * PROFIT_COIN_PERMILLAGE / 1000);
                record.setSubchildprofitcoins(record.getSubchildcostcoins() * PROFIT_COIN_PERMILLAGE / 1000);
                record.setChildprofitmoney((long) (record.getChildprofitcoins() * 100 / GoodsInfo.EXCHANGE_RMB_COIN)); //100金币=1元=100分
                record.setSubchildprofitmoney((long) (record.getSubchildprofitcoins() * 100 / GoodsInfo.EXCHANGE_RMB_COIN)); //100金币=1元=100分
                source.updateColumn(record, "childprofitcoins", "subchildprofitcoins", "childprofitmoney", "subchildprofitmoney");
                long money = record.getChildprofitmoney() + record.getSubchildprofitmoney();
                if (money > 0) {
                    if (source.updateColumn(ProfitInfo.class, record.getUserid(), ColumnValue.inc("allprofitmoney", money),
                        ColumnValue.inc("remainmoney", money), ColumnValue.inc("updatetime", now)) == 0) {
                        ProfitInfo info = new ProfitInfo();
                        info.setUserid(record.getUserid());
                        info.setCreatetime(now);
                        info.setUpdatetime(now);
                        info.setAllprofitmoney(money);
                        info.setRemainmoney(money);
                        source.insert(info);
                    }
                }
            }
            sheet = source.querySheet(ProfitDayRecord.class, flipper.next(), node);
        }
    }

    private void updateOneDayRecord(final int userid, final long costcoins, final String column, final Map<Integer, Integer> userToAgencyMap,
        final int intday, final Map<Integer, AtomicLong> childmap, final long now) {
        if (costcoins < 1L) return;
        int agencyid = userToAgencyMap.getOrDefault(userid, 0);
        if (agencyid == 0) return; //无上级
        long childcostcoins = costcoins;
        if (userToAgencyMap.getOrDefault(agencyid, 0) == 0) { //无上上级
            source.updateColumn(ProfitDayRecord.class, FilterNode.create("intday", intday).and("userid", agencyid), ColumnValue.inc(column, childcostcoins));
        } else {
            childcostcoins = costcoins - costcoins * SUBCHILD_PERMILLAGE / 1000;
            source.updateColumn(ProfitDayRecord.class, FilterNode.create("intday", intday).and("userid", agencyid), ColumnValue.inc(column, childcostcoins));
            updateOneDayRecord(agencyid, costcoins - childcostcoins, "subchildcostcoins", userToAgencyMap, intday, childmap, now);
        }
    }

    //生成代理子级玩家数信息
    private void genDayRecord(final Map<Integer, Integer> userToAgencyMap, final int intday, final Map<Integer, AtomicLong> childmap, final long now) {
        final AtomicLong emptyCount = new AtomicLong(0);
        for (final Map.Entry<Integer, Integer> en : userToAgencyMap.entrySet()) {
            ProfitDayRecord record = new ProfitDayRecord();
            record.setUserid(en.getKey());
            record.setIntday(intday);
            record.setCreatetime(now);
            record.setChildcount(childmap.getOrDefault(en.getKey(), emptyCount).longValue());
            AtomicLong subChildCount = new AtomicLong();
            allChildCount(0, userToAgencyMap, intday, childmap, subChildCount, record.getUserid());
            record.setSubchildcount(subChildCount.longValue());
            UserDayRecord old = source.find(UserDayRecord.class, FilterNode.create("userid", record.getUserid()).and("intday", intday));
            if (old != null) record.setCostcoins(old.getCostcoins());
            source.insert(record);
        }
    }

    private AtomicLong allChildCount(int index, Map<Integer, Integer> userToAgencyMap, final int intday, Map<Integer, AtomicLong> childmap,
        AtomicLong childCounter, int userid) {
        AtomicLong child = childmap.get(userid);
        if (child == null || child.get() < 1) return childCounter;
        if (index > 0) childCounter.addAndGet(child.get());
        int newindex = index + 1;
        userToAgencyMap.forEach((x, y) -> {
            if (y != userid) return;
            allChildCount(newindex, userToAgencyMap, intday, childmap, childCounter, x);
        });
        return childCounter;
    }

    public ProfitInfo getMyProfitInfo(int userid) {
        return source.find(ProfitInfo.class, userid);
    }

    public ProfitDayRecord getMyProfitDayRecord(int userid) {
        int intday = Utility.yesterday();
        return source.find(ProfitDayRecord.class, FilterNode.create("intday", intday).and("userid", userid));
    }

    public RetResult<Long> tradeProfit(ProfitTradeRecord bean) {
        if (bean == null || bean.getUserid() < 1 || bean.getMoney() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
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
        if (source.exists(ProfitTradeRecord.class, FilterNode.create("userid", bean.getUserid()).and("tradestatus", TRADE_STATUS_PENDING))) {
            return RetCodes.retResult(RetCodes.RET_USER_TRADE_TRADING);
        }

        long newmoney;
        synchronized (this) {
            ProfitInfo info = source.find(ProfitInfo.class, bean.getUserid());
            if (info == null || info.getRemainmoney() < 1) return RetCodes.retResult(RetCodes.RET_USER_PROFIT_MONEY_ILLEGAL);
            if (bean.getMoney() > info.getRemainmoney()) return RetCodes.retResult(RetCodes.RET_USER_PROFIT_MONEY_ILLEGAL);
            ProfitTradeRecord record = new ProfitTradeRecord();
            record.setUserid(bean.getUserid());
            record.setTradetype(bean.getTradetype());
            record.setMoney(bean.getMoney());
            record.setCreatetime(System.currentTimeMillis());
            if (bean.getTradetype() == TRADE_TYPE_BANK) {
                record.setTradeaccount(card.getCardaccount());
                record.setTradejson(card.toCardJson());
            } else {
                record.setTradeaccount(card.getAlipayaccount());
                record.setTradejson(card.toAlipayJson());
            }
            record.setTradestatus(TRADE_STATUS_PENDING);
            record.setTradeid(Integer.toString(record.getUserid(), 36) + "-" + Utility.format36time(record.getCreatetime()));
            source.insert(new ProfitTradeRecordHis(record));
            source.insert(record);
            newmoney = info.getRemainmoney() - bean.getMoney();
            source.updateColumn(ProfitInfo.class, record.getUserid(), ColumnValue.inc("remainmoney", -bean.getMoney()), ColumnValue.inc("updatetime", record.getCreatetime()));

        }
        return new RetResult().result(newmoney);
    }

    public Sheet<ProfitTradeRecord> queryProfitTradeRecord(ProfitTradeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(ProfitTradeRecord.class, flipper, bean);
    }

    public Sheet<ProfitTradeRecordHis> queryProfitTradeRecordHis(ProfitTradeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(ProfitTradeRecordHis.class, flipper, bean);
    }

    public RetResult dealTrade(String tradeid, short tradestatus, String remark, int memberid) {
        if (tradeid == null) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (tradestatus != TRADE_STATUS_DONEOK && tradestatus != TRADE_STATUS_DONENO) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        synchronized (dealTradeLock) {
            long now = System.currentTimeMillis();
            ProfitTradeRecord record = source.find(ProfitTradeRecord.class, tradeid);
            if (record == null) return RetCodes.retResult(RetCodes.RET_USER_TRADE_NOT_EXISTS);
            if (record.getTradestatus() != TRADE_STATUS_PENDING) return RetCodes.retResult(RetCodes.RET_USER_TRADE_FINISHED);
            int rs = source.updateColumn(ProfitTradeRecordHis.class, tradeid, ColumnValue.mov("tradestatus", tradestatus),
                ColumnValue.mov("finishtime", now), ColumnValue.mov("memberid", memberid), ColumnValue.mov("remark", remark == null ? "" : remark));
            if (rs != 1) return RetCodes.retResult(RetCodes.RET_USER_TRADE_NOT_EXISTS);
            if (tradestatus == TRADE_STATUS_DONENO) { //退还
                source.updateColumn(ProfitInfo.class, record.getUserid(), ColumnValue.inc("remainmoney", record.getMoney()), ColumnValue.inc("updatetime", now));
            }
            source.delete(record);
            return RetResult.success();
        }
    }
}
