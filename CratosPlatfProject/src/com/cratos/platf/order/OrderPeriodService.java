/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.letter.LetterService;
import com.cratos.platf.letter.LetterRecord;
import com.cratos.platf.base.BaseService;
import com.cratos.platf.user.UserService;
import com.cratos.platf.util.QueueTask;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.persistence.Transient;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class OrderPeriodService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected LetterService letterService;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //MissionUpdateEntry 队列
    protected final QueueTask<Integer> letterQueue = new QueueTask<>(1);

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "-Task-Thread");
            t.setDaemon(true);
            return t;
        });

        final long midDelay = 24 * 60 * 60 * 1000 + Utility.midnight() - System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Flipper flipper = new Flipper(500);
                int today = Utility.today();
                Sheet<OrderPeriod> sheet = source.querySheet(OrderPeriod.class, flipper, (FilterNode) null);
                while (!sheet.isEmpty()) {
                    for (OrderPeriod order : sheet) {
                        checkOrderPeriod(order, today);
                    }
                    sheet = source.querySheet(OrderPeriod.class, flipper.next(), (FilterNode) null);
                }
            } catch (Throwable e) {
                logger.log(Level.SEVERE, OrderPeriodService.class.getSimpleName() + " scheduleAtFixedRate error", e);
            }
        }, midDelay, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        letterQueue.init(logger, (queue, userid) -> {
            taskOrderPeriod(userid);
        });
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
        letterQueue.destroy();
    }

    public void letterOrderPeriod(int userid) {
        letterQueue.add(userid);
    }

    private void taskOrderPeriod(int userid) {
        int today = Utility.today();
        List<OrderPeriod> list = source.queryList(OrderPeriod.class, FilterNode.create("userid", userid).and("doneday", FilterExpress.LESSTHAN, today));
        if (list == null || list.isEmpty()) return;
        for (OrderPeriod order : list) {
            checkOrderPeriod(order, today);
        }
    }

    protected void checkOrderPeriod(OrderPeriod order, int today) {
        try {
            if (order.getDoneday() >= order.getEndday()) {  //周期结束的移除到his
                source.insert(order.createOrderPeriodHis(System.currentTimeMillis()));
                source.delete(order);
                LetterRecord letter = new LetterRecord();
                letter.setUserid(order.getUserid());
                letter.setLettertype(LetterRecord.LETTER_TYPE_NOTICE);
                if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_WEEKCARD) {
                    letter.setTitle(letterService.bundleResourceValue("orderperiod.weekcardexpire.title"));
                    letter.setContent(letterService.bundleResourceValue("orderperiod.weekcardexpire.content"));
                } else {
                    letter.setTitle(letterService.bundleResourceValue("orderperiod.monthcardexpire.title"));
                    letter.setContent(letterService.bundleResourceValue("orderperiod.monthcardexpire.content"));
                }
                letterService.createLetterRecord(letter);
                return;
            }
            if (order.getDoneday() >= today) return;
            LetterRecord letter = new LetterRecord();
            letter.setUserid(order.getUserid());
            letter.setLettertype(LetterRecord.LETTER_TYPE_PERIOD);
            if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_WEEKCARD) {
                letter.setModule("weekcard");
                letter.setRemark("周卡; orderno=" + order.getOrderno());
                letter.setTitle(letterService.bundleResourceValue("orderperiod.weekcarding.title"));
                letter.setContent(letterService.bundleResourceValue("orderperiod.weekcarding.content"));
            } else {
                letter.setModule("monthcard");
                letter.setRemark("月卡; orderno=" + order.getOrderno());
                letter.setTitle(letterService.bundleResourceValue("orderperiod.monthcarding.title"));
                letter.setContent(letterService.bundleResourceValue("orderperiod.monthcarding.content"));
            }
            letter.setGoodsitems(Utility.append(order.getGoodsitems(), order.getGiftitems()));
            letterService.createLetterRecord(letter);
            order.setDoneday(today);
            source.updateColumn(order, "doneday");
        } catch (Exception e) {
            logger.log(Level.SEVERE, order + " checkOrderPeriod error", e);
        }
    }

    public void insertOrderPeriod(OrderRecord order) {
        if (order.getGoodstype() != GoodsInfo.GOODS_TYPE_WEEKCARD
            && order.getGoodstype() != GoodsInfo.GOODS_TYPE_MONTHCARD) {
            throw new IllegalArgumentException("goodstype error, order=" + order);
        }
        OrderPeriod period = new OrderPeriod(order);
        source.insert(period);
        letterOrderPeriod(order.getUserid());
    }
}
