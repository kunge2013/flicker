/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.*;
import com.cratos.platf.pay.PayRecord;
import com.cratos.platf.user.UserService;
import java.util.Map;
import javax.annotation.Resource;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;
import org.redkalex.pay.*;
import static com.cratos.platf.base.RetCodes.*;
import com.cratos.platf.info.*;
import com.cratos.platf.letter.*;
import com.cratos.platf.util.ShuffleRandom;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import org.redkale.convert.json.JsonConvert;
import static org.redkalex.pay.Pays.PAYSTATUS_PAYOK;

/**
 *
 * @author zhangjx
 */
@Comment("订单服务")
public class OrderService extends BaseService {

    protected static final Type MAP_STRING_MAP_STRINGSTRING = new TypeToken<Map<String, Map<String, String>>>() {
    }.getType();

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    @Resource(name = "property.weixin.mp.appid") //微信APPID
    protected String weinxin_mp_appid = "";

    protected Map<String, PayChannel> confPayChannels = new HashMap<>();

    @Resource
    @Comment("全服参数配置服务")

    protected DictService dictService;

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected LetterService letterService;

    @Resource
    protected OrderPeriodService orderPeriodService;

    @Resource
    protected com.cratos.platf.pay.PayService payService;

    @Resource
    protected GoodsService goodsService;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Override
    public void init(AnyValue config) {
        this.refreshPayConfig(true);
        this.scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, "OrderService-task-Thread");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                this.refreshPayConfig(true);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate refreshPayConfig error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void destroy(AnyValue config) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    public Sheet<OrderRecord> queryOrderRecord(OrderBean bean, Flipper flipper) {
        return source.querySheet(OrderRecord.class, flipper, bean);
    }

    public RetResult refreshPayConfig(boolean schedule) {
        final Map<String, PayChannel> payChannels = new HashMap<>();
        final Map<String, Map<String, String>> allChannels = JsonConvert.root().convertFrom(MAP_STRING_MAP_STRINGSTRING, dictService.findDictValue(DictInfo.PLATF_PAY_ALLCHANNELS, ""));
        final Map<String, int[]> allItems = new HashMap<>();
        allChannels.forEach((paytype, submap) -> {
            String[] itemstrs = submap.get("items").split(";");
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < itemstrs.length; i++) {
                if (itemstrs[i].isEmpty()) continue;
                if (itemstrs[i].startsWith(">=")) {
                    list.add(-Integer.parseInt(itemstrs[i].substring(2)));
                } else {
                    list.add(Integer.parseInt(itemstrs[i]));
                }
            }
            allItems.put(paytype, list.stream().mapToInt(x -> x).toArray());
        });
        String dewxpayChannels = dictService.findDictValue(DictInfo.PLATF_PAY_DEWXPAY_CHANNELS, "");
        if (!dewxpayChannels.isEmpty()) {
            payChannels.put("dewxpay", createPayChannel(allItems, dewxpayChannels));
        }
        String iewxpayChannels = dictService.findDictValue(DictInfo.PLATF_PAY_IEWXPAY_CHANNELS, "");
        if (!iewxpayChannels.isEmpty()) {
            payChannels.put("iewxpay", createPayChannel(allItems, iewxpayChannels));
        }
        String dealipayChannels = dictService.findDictValue(DictInfo.PLATF_PAY_DEALIPAY_CHANNELS, "");
        if (!dealipayChannels.isEmpty()) {
            payChannels.put("dealipay", createPayChannel(allItems, dealipayChannels));
        }
        String iealipayChannels = dictService.findDictValue(DictInfo.PLATF_PAY_IEALIPAY_CHANNELS, "");
        if (!iealipayChannels.isEmpty()) {
            payChannels.put("iealipay", createPayChannel(allItems, iealipayChannels));
        }
        String iebankpayChannels = dictService.findDictValue(DictInfo.PLATF_PAY_IEBANKPAY_CHANNELS, "");
        if (!iebankpayChannels.isEmpty()) {
            payChannels.put("iebankpay", createPayChannel(allItems, iebankpayChannels));
        }
        String ieysfpayChannels = dictService.findDictValue(DictInfo.PLATF_PAY_IEYSFPAY_CHANNELS, "");
        if (!ieysfpayChannels.isEmpty()) {
            payChannels.put("ieysfpay", createPayChannel(allItems, ieysfpayChannels));
        }
        this.confPayChannels = payChannels;
        return RetResult.success();
    }

    protected PayChannel createPayChannel(final Map<String, int[]> allItems, String channelmapstr) {
        Map<String, String> map = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, channelmapstr);
        short[] subpaytypes = new short[map.size()];
        int[] weights = new int[map.size()];
        int[][] items = new int[map.size()][];
        final AtomicInteger index = new AtomicInteger();
        map.forEach((pstr, weis) -> {
            subpaytypes[index.get()] = Short.parseShort(pstr);
            weights[index.get()] = Integer.parseInt(weis);
            items[index.get()] = allItems.get(pstr);
            index.incrementAndGet();
        });
        PayChannel channel = new PayChannel();
        channel.subpaytypes = subpaytypes;
        channel.weights = weights;
        channel.items = items;
        return channel;
    }

    //支付完成后更新状态
    public RetResult<String> checkAge(final int userid, final int price) {
        UserInfo user = userService.findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (price < 1) return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        boolean indulge = dictService.findDictValue(DictInfo.PLATF_INDULGE_ACTIVATE, 0) == 10;
        if (!indulge) return RetResult.success();
        int less16OnceMaxPrice = dictService.findDictValue(DictInfo.PLATF_INDULGE_PAY_16ONCE_MAXMONEY, 0);
        int less16MonthMaxPrice = dictService.findDictValue(DictInfo.PLATF_INDULGE_PAY_16MONTH_MAXMONEY, 0);
        int less18OnceMaxPrice = dictService.findDictValue(DictInfo.PLATF_INDULGE_PAY_18ONCE_MAXMONEY, 0);
        int less18MonthMaxPrice = dictService.findDictValue(DictInfo.PLATF_INDULGE_PAY_18MONTH_MAXMONEY, 0);
        int age = user.getAge();
        if (age >= 18) return RetResult.success();
        int minage = dictService.findDictValue(DictInfo.PLATF_INDULGE_PAY_MINAGE, 0);
        if (minage > 0 && age < minage) return RetCodes.retResult(RET_ORDER_AGE_LESS08_ILLEGAL);
        long monthFirstDay = Utility.monthFirstDay(System.currentTimeMillis());
        if (age < 16) {
            if (less16OnceMaxPrice > 0 && price > less16OnceMaxPrice) return RetCodes.retResult(RET_ORDER_AGE_LESS16_ONE_ILLEGAL, less16OnceMaxPrice / 100);
            if (less16MonthMaxPrice > 0) {
                long monthpayed = source.getNumberResult(OrderRecord.class, FilterFunc.SUM, 0L, "ordermoney", FilterNode.create("userid", userid).and("paystatus", PAYSTATUS_PAYOK).and("finishtime", FilterExpress.GREATERTHAN, monthFirstDay)).longValue();
                if (monthpayed + price > less16MonthMaxPrice) return RetCodes.retResult(RET_ORDER_AGE_LESS16_MONTH_ILLEGAL, less16MonthMaxPrice / 100);
            }
        }
        if (age < 18) {
            if (less18OnceMaxPrice > 0 && price > less18OnceMaxPrice) return RetCodes.retResult(RET_ORDER_AGE_LESS18_ONE_ILLEGAL, less18OnceMaxPrice / 100);
            if (less18MonthMaxPrice > 0) {
                long monthpayed = source.getNumberResult(OrderRecord.class, FilterFunc.SUM, 0L, "ordermoney", FilterNode.create("userid", userid).and("paystatus", PAYSTATUS_PAYOK).and("finishtime", FilterExpress.GREATERTHAN, monthFirstDay)).longValue();
                if (monthpayed + price > less18MonthMaxPrice) return RetCodes.retResult(RET_ORDER_AGE_LESS18_MONTH_ILLEGAL, less18MonthMaxPrice / 100);
            }
        }
        return RetResult.success();
    }

    //支付完成后更新状态
    public RetResult<OrderRecord> updateOrder(final PayRecord pay) {
        final OrderRecord order = source.find(OrderRecord.class, pay.getOrderno());
        if (order == null) return RetCodes.retResult(RET_ORDER_NOT_EXISTS);
        if (order.getPaystatus() != Pays.PAYSTATUS_UNPAY && order.getPaystatus() != Pays.PAYSTATUS_UNREFUND) {
            return RetCodes.retResult(RET_ORDER_STATUS_ILLEGAL); //已经更新过了
        }
        if (!pay.isPayok() && !pay.isRefundok()) {
            boolean refundno = pay.getPaystatus() == Pays.PAYSTATUS_REFUNDNO;
            logger.fine((refundno ? "退款" : "支付") + "没有成功: " + pay);
            order.setOrderstatus(OrderRecord.ORDER_STATUS_OK);
            order.setPaystatus(pay.getPaystatus());
            order.setFinishtime(pay.getFinishtime());
            source.updateColumn(order, "orderstatus", "paystatus", "finishtime");
            return RetCodes.retResult(refundno ? PayRetCodes.RETPAY_REFUND_ERROR : PayRetCodes.RETPAY_PAY_ERROR);
        }
        order.setOrderstatus(OrderRecord.ORDER_STATUS_OK);
        order.setPaystatus(pay.getPaystatus());
        order.setFinishtime(pay.getFinishtime());
        source.updateColumn(order, "orderstatus", "paystatus", "finishtime");
        if (pay.isPayok() && (order.getGoodstype() == GoodsInfo.GOODS_TYPE_WEEKCARD
            || order.getGoodstype() == GoodsInfo.GOODS_TYPE_MONTHCARD)) {
            orderPeriodService.insertOrderPeriod(order);
        }
        //用户账号进行逻辑处理
        if (pay.isPayok()) return updateOrderOK(order, pay.getFinishtime());
        return new RetResult<>(order);
    }

    //支付成功后更新状态
    public RetResult<OrderRecord> updateOrderOK(final OrderRecord order, final long finishtime) {
        final int userid = order.getUserid();
        boolean done = false;
        if (order.getBuytype() == GoodsInfo.GOODS_BUY_RMB) {
            long gcoin = 0;
            long gdiamond = 0;
            long gcoupon = 0;
            if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_COIN || order.getGoodstype() == GoodsInfo.GOODS_TYPE_XCOIN) {  //金币充值
                gcoin += order.getGoodscount();
            } else if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_DIAMOND || order.getGoodstype() == GoodsInfo.GOODS_TYPE_XDIAMOND) {  //钻石充值
                gdiamond += order.getGoodscount();
            } else if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_COUPON || order.getGoodstype() == GoodsInfo.GOODS_TYPE_XCOUPON) {  //奖券充值
                gcoupon += order.getGoodscount();
            }
            List<GoodsItem> otheritems = new ArrayList<>();
            GoodsItem[] items = Utility.append(order.getGoodsitems(), order.getGiftitems());
            if (items != null && items.length > 0) {
                for (GoodsItem item : items) {
                    if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_COIN) {
                        gcoin += item.getGoodscount();
                    } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_DIAMOND) {
                        gdiamond += item.getGoodscount();
                    } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_COUPON) {
                        gcoupon += item.getGoodscount();
                    } else {
                        otheritems.add(item);
                    }
                }
            }
            if (gcoin > 0 || gdiamond > 0 || gcoupon > 0) {
                RetResult rs = userService.rechargeUserCoinDiamondCoupons(userid, order.getOrdermoney(), gcoin, gdiamond, gcoupon, order.getFinishtime(), "用户充值;orderno=" + order.getOrderno());
                if (!rs.isSuccess()) return rs;
            }
            if (!otheritems.isEmpty()) {
                RetResult rs = goodsService.receiveGoodsItems(userid, order.getGoodstype(), 1, order.getFinishtime(), "recharge", "用户充值;orderno=" + order.getOrderno(), otheritems.toArray(new GoodsItem[otheritems.size()]));
                if (!rs.isSuccess()) return rs;
            }
            LetterRecord letter = new LetterRecord();
            letter.setUserid(order.getUserid());
            letter.setLettertype(LetterRecord.LETTER_TYPE_NOTICE);
            letter.setTitle(letterService.bundleResourceValue("order.payok.title"));
            String goodsname = order.getGoodsname();
            if (goodsname == null || goodsname.isEmpty()) goodsname = "商品";
            letter.setContent(letterService.bundleResourceValue("order.payok.content", Utility.formatTime(finishtime), order.getOrdermoney() / 100, goodsname));
            letterService.createLetterRecord(letter);
            return new RetResult<>(order);
        } else {
            GoodsItem buyitem = null;
            if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_COIN) {
                buyitem = GoodsItem.createCoin(order.getGoodscount());
            } else if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_DIAMOND) {
                buyitem = GoodsItem.createDiamond(order.getGoodscount());
            } else if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_COUPON) {
                buyitem = GoodsItem.createCoupon(order.getGoodscount());
            }
            //可以和上面并存
            if (buyitem != null || (order.getGoodsitems() != null && order.getGoodsitems().length > 0)
                || (order.getGiftitems() != null && order.getGiftitems().length > 0)) {
                GoodsItem[] items = buyitem == null ? new GoodsItem[0] : Utility.ofArray(buyitem);
                items = Utility.append(items, order.getGoodsitems());
                items = Utility.append(items, order.getGiftitems());
                RetResult rs = goodsService.receiveGoodsItems(userid, order.getGoodstype(), buyitem == null ? order.getGoodscount() : 1, order.getFinishtime(), "recharge", "用户充值;orderno=" + order.getOrderno(), items);
                if (!rs.isSuccess()) return rs;
            } else if (!done) {
                throw new RuntimeException("order.getGoodstype() error, order = " + order);
            }
            return new RetResult<>(order);
        }
    }

    //支付请求
    public RetResult<Map<String, Object>> prepay(final UserInfo user, final OrderRecord bean, final boolean test) {
        if (bean == null) return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        if (user.isMate()) return RetCodes.retResult(RET_USER_MATE_AUTHILLEGAL);
        bean.setUserid(user.getUserid());
        GoodsInfo goods = null;
        final long now = System.currentTimeMillis();
        if (bean.getGoodstype() == GoodsInfo.GOODS_TYPE_XCOIN || bean.getGoodstype() == 102) { //102是兼容旧的
            if (bean.getOrdermoney() < 100) return RetCodes.retResult(RET_USER_MONEY_TOOSMALL);
            bean.setGoodscount((int) (bean.getOrdermoney() * GoodsInfo.EXCHANGE_RMB_COIN / 100));
            bean.setBuytype(GoodsInfo.GOODS_BUY_RMB);
        } else if (bean.getGoodstype() == GoodsInfo.GOODS_TYPE_XDIAMOND) {
            if (bean.getOrdermoney() < 100) return RetCodes.retResult(RET_USER_MONEY_TOOSMALL);
            bean.setGoodscount((int) (bean.getOrdermoney() * GoodsInfo.EXCHANGE_RMB_DIAMOND / 100));
            bean.setBuytype(GoodsInfo.GOODS_BUY_RMB);
        } else {
            goods = goodsService.findGoods(bean.getGoodsid());
            if (goods == null) return RetCodes.retResult(RET_ORDER_GOODS_NOTEXISTS);
            if (goods.getEndtime() > 0 && goods.getEndtime() < System.currentTimeMillis()) {
                return RetCodes.retResult(RET_ORDER_GOODS_EXPIRED);
            }
            bean.setBuytype(goods.getBuytype());
            bean.setGoodstype(goods.getGoodstype());
            bean.setGoodsname(goods.getGoodsname());
            bean.setGoodsitems(goods.getGoodsitems());
            bean.setGiftitems(goods.getGiftitems());
            final int buycount = Math.max(1, bean.getGoodscount());
            bean.setOrdermoney(buycount * goods.getPrice());
            if (bean.getGoodscount() < 1 && goods.getGoodscount() > 0) { //必须在setOrdermoney之后调用
                bean.setGoodscount(goods.getGoodscount());
            }
        }

        if (bean.getBuytype() == GoodsInfo.GOODS_BUY_RMB && bean.getPaytype() == Pays.PAYTYPE_CREDIT) {
            final PayChannel channel = confPayChannels.get(bean.getSubpaytype());
            if (channel == null) {
                logger.log(Level.WARNING, "prepay subpaytype error: bean = " + bean);
                return RetCodes.retResult(RET_ORDER_GOODS_PAYTYPEILLEGAL);
            }
            Map<Short, Integer> paytypeWeights = new LinkedHashMap<>();
            for (int i = 0; i < channel.subpaytypes.length; i++) {
                int[] items = channel.items[i];
                if (Utility.contains(items, (int) bean.getOrdermoney())) {
                    paytypeWeights.put(channel.subpaytypes[i], channel.weights[i]);
                } else {
                    boolean flag = false;
                    for (int k = 0; k < items.length; k++) {
                        if (items[k] < 0 && bean.getOrdermoney() >= Math.abs(items[i])) {
                            flag = true;
                            break;
                        }
                    }
                    if (flag) paytypeWeights.put(channel.subpaytypes[i], channel.weights[i]);
                }
            }
            if (paytypeWeights.isEmpty()) {
                logger.log(Level.WARNING, "prepay channel error: bean = " + bean + ", channel = " + JsonConvert.root().convertTo(channel));
                return RetCodes.retResult(RET_ORDER_PAYCHANNELILLEGAL);
            }
            if (paytypeWeights.size() > 1) {
                int count = paytypeWeights.values().stream().mapToInt(x -> x).sum();
                final short[] ptypes = new short[count];
                final AtomicInteger pindex = new AtomicInteger();
                paytypeWeights.forEach((t, w) -> {
                    for (int j = 0; j < w; j++) {
                        ptypes[pindex.getAndIncrement()] = t;
                    }
                });
                bean.setPaytype(ptypes[random.nextInt(count)]);
            } else {
                bean.setPaytype(new ArrayList<>(paytypeWeights.keySet()).get(0));
            }
        }
        if (bean.getGoodstype() == GoodsInfo.GOODS_TYPE_ONCEPACKET) {
            if (source.exists(OrderRecord.class, FilterNode.create("userid", bean.getUserid())
                .and("goodsid", bean.getGoodsid()).and("orderstatus", OrderRecord.ORDER_STATUS_OK))) {
                return RetCodes.retResult(RET_ORDER_BUY_ONCE);
            }
        } else if (bean.getGoodstype() == GoodsInfo.GOODS_TYPE_DAYPACKET) {
            int count = source.getNumberResult(OrderRecord.class, FilterFunc.COUNT, 0, null, FilterNode.create("userid", bean.getUserid())
                .and("goodsid", bean.getGoodsid()).and("orderday", Utility.yyyyMMdd(now))
                .and("orderstatus", OrderRecord.ORDER_STATUS_OK)).intValue();
            if (goods != null && count >= goods.getDaylimit()) {
                return RetCodes.retResult(RET_ORDER_BUY_DAYONCE);
            }
        }
        if (bean.getBuytype() == GoodsInfo.GOODS_BUY_RMB) { //RMB充值
            bean.setOrderstatus(OrderRecord.ORDER_STATUS_PENDING);
            bean.setPaystatus(Pays.PAYSTATUS_UNPAY);
            bean.setCreatetime(now);
            bean.setOrderday(Utility.yyyyMMdd(now));
            bean.setOrderno("d" + bean.getGoodstype() + Integer.toString(bean.getUserid(), 36) + Utility.format36time(bean.getCreatetime()));
            source.insert(bean);
            String appid = "";
            if (bean.getPaytype() == Pays.PAYTYPE_WEIXIN) {
                appid = weinxin_mp_appid;
            }
            PayRecord pay = bean.createPayRecord(appid);
            if (test) {
                RetResult<Map<String, String>> testrs = payService.testPrepay(pay);
                if (testrs.isSuccess()) updateOrder(pay);
                if ((bean.getGoodsitems() != null && bean.getGoodsitems().length > 0)
                    || (bean.getGiftitems() != null && bean.getGiftitems().length > 0)) {
                    GoodsItem[] goodsitems = generateGoodsItems(bean);
                    return goodsitems == null || goodsitems.length < 1 ? RetResult.success() : new RetResult(Utility.ofMap("goodsitems", goodsitems));
                }
                return (RetResult) testrs;
            }
            RetResult<Map<String, String>> rs = payService.prepay(pay);
            if (!rs.isSuccess()) {
                bean.setOrderstatus(OrderRecord.ORDER_STATUS_NO);
                bean.setPaystatus(pay.getPaystatus());
                bean.setFinishtime(pay.getFinishtime());
                source.updateColumn(bean, "orderstatus", "paystatus", "finishtime");
            }
            return (RetResult) rs;
        } else {
            return payVirtualOrder(user, bean, now);
        }
    }

    //非RMB购买订单
    public RetResult<OrderRecord> payVirtualOrder(final int userid, OrderRecord bean, final long now) {
        RetResult rs = payVirtualOrder(userService.findUserInfo(userid), bean, now);
        if (!rs.isSuccess() || rs.getResult() == null) return rs;
        Map map = (Map) rs.getResult();
        OrderRecord order = new OrderRecord();
        order.setGoodsitems((GoodsItem[]) map.get("goodsitems"));
        return new RetResult<>(order);
    }

    //非RMB购买订单
    private RetResult<Map<String, Object>> payVirtualOrder(final UserInfo user, OrderRecord bean, final long now) {
        if (bean.getBuytype() == GoodsInfo.GOODS_BUY_COIN) {
            if (user.getCoins() < bean.getOrdermoney()) return RetCodes.retResult(RET_USER_COINS_NOTENOUGH);
        } else if (bean.getBuytype() == GoodsInfo.GOODS_BUY_DIAMOND) {
            if (user.getDiamonds() < bean.getOrdermoney()) return RetCodes.retResult(RET_USER_DOMAINDS_NOTENOUGH);
        } else if (bean.getBuytype() == GoodsInfo.GOODS_BUY_COUPON) {
            if (user.getCoupons() < bean.getOrdermoney()) return RetCodes.retResult(RET_USER_COUPONS_NOTENOUGH);
        } else {
            return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        }
        bean.setOrderstatus(OrderRecord.ORDER_STATUS_OK);
        bean.setPaystatus(Pays.PAYSTATUS_PAYOK);
        bean.setCreatetime(now);
        bean.setFinishtime(now);
        bean.setOrderday(Utility.yyyyMMdd(now));
        bean.setOrderno("d" + bean.getGoodstype() + Integer.toString(bean.getUserid(), 36) + Utility.format36time(bean.getCreatetime()));

        if (bean.getBuytype() == GoodsInfo.GOODS_BUY_COIN) {
            RetResult rs = userService.costGameUserCoins(user.getUserid(), bean.getOrdermoney(), now, "platf", "order", (bean.getGoodsid() > 0 ? ("金币商场兑换; orderno=" + bean.getOrderno()) : "金币虚拟兑换"));
            if (!rs.isSuccess()) return rs;
        } else if (bean.getBuytype() == GoodsInfo.GOODS_BUY_DIAMOND) {
            RetResult rs = userService.costGameUserDiamonds(user.getUserid(), bean.getOrdermoney(), now, "platf", "order", (bean.getGoodsid() > 0 ? ("钻石商场兑换; orderno=" + bean.getOrderno()) : "钻石虚拟兑换"));
            if (!rs.isSuccess()) return rs;
        } else if (bean.getBuytype() == GoodsInfo.GOODS_BUY_COUPON) {
            RetResult rs = userService.costGameUserCoupons(user.getUserid(), bean.getOrdermoney(), now, "platf", "order", (bean.getGoodsid() > 0 ? ("奖券商场兑换; orderno=" + bean.getOrderno()) : "奖券虚拟兑换"));
            if (!rs.isSuccess()) return rs;
        }

        RetResult<OrderRecord> okrs = updateOrderOK(bean, now);
        if (!okrs.isSuccess()) {
            if (bean.getBuytype() == GoodsInfo.GOODS_BUY_COIN) {
                RetResult rs = userService.refundGameUserCoins(user.getUserid(), bean.getOrdermoney(), now, "platf", "order", (bean.getGoodsid() > 0 ? ("金币商场兑换; orderno=" + bean.getOrderno()) : "金币虚拟兑换"));
                if (!rs.isSuccess()) logger.log(Level.SEVERE, bean + "回退失败");
            } else if (bean.getBuytype() == GoodsInfo.GOODS_BUY_DIAMOND) {
                RetResult rs = userService.refundGameUserDiamonds(user.getUserid(), bean.getOrdermoney(), now, "platf", "order", (bean.getGoodsid() > 0 ? ("钻石商场兑换; orderno=" + bean.getOrderno()) : "钻石虚拟兑换"));
                if (!rs.isSuccess()) logger.log(Level.SEVERE, bean + "回退失败");
            } else if (bean.getBuytype() == GoodsInfo.GOODS_BUY_COUPON) {
                RetResult rs = userService.refundGameUserCoupons(user.getUserid(), bean.getOrdermoney(), now, "platf", "order", (bean.getGoodsid() > 0 ? ("奖券商场兑换; orderno=" + bean.getOrderno()) : "奖券虚拟兑换"));
                if (!rs.isSuccess()) logger.log(Level.SEVERE, bean + "回退失败");
            }
            bean.setOrderstatus(OrderRecord.ORDER_STATUS_NO);
            bean.setPaystatus(Pays.PAYSTATUS_PAYNO);
            if (bean.getGoodsid() > 0) source.insert(bean);    //虚拟的订单不入库
            return (RetResult) okrs;
        }
        if (bean.getGoodsid() > 0) source.insert(bean);   //虚拟的订单不入库
        // 
        GoodsItem[] goodsitems = generateGoodsItems(bean);
        return goodsitems == null || goodsitems.length < 1 ? RetResult.success() : new RetResult(Utility.ofMap("goodsitems", goodsitems));
    }

    protected GoodsItem[] generateGoodsItems(OrderRecord order) {
        GoodsItem buyitem = null;
        if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_COIN) {
            buyitem = GoodsItem.createCoin(order.getGoodscount());
        } else if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_DIAMOND) {
            buyitem = GoodsItem.createDiamond(order.getGoodscount());
        } else if (order.getGoodstype() == GoodsInfo.GOODS_TYPE_COUPON) {
            buyitem = GoodsItem.createCoupon(order.getGoodscount());
        }
        GoodsItem[] items = buyitem == null ? new GoodsItem[0] : Utility.ofArray(buyitem);
        items = Utility.append(items, order.getGoodsitems());
        items = Utility.append(items, order.getGiftitems());
        if (buyitem == null && order.getGoodscount() > 1) {
            GoodsItem[] newitems = new GoodsItem[items.length];
            for (int i = 0; i < items.length; i++) {
                newitems[i] = items[i].copy();
                newitems[i].setGoodscount(newitems[i].getGoodscount() * order.getGoodscount());
            }
            items = newitems;
        }
        return items;
    }

    protected static class PayChannel extends BaseBean {

        public short[] subpaytypes; //渠道id 

        public int[] weights; //权重

        public int[][] items; //金额选项, 负数表示>=

    }
}
