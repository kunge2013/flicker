/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.*;
import com.cratos.platf.info.ModuleAddressService;
import com.cratos.platf.user.UserService;
import java.util.logging.Level;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Resource;
import javax.persistence.Transient;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("商品服务")
public class GoodsService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected ModuleAddressService moduleService;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //签到奖励列表
    protected List<GoodsIntro> goodsIntros;

    @Override
    public void init(AnyValue config) {
        this.reloadConfig();
        this.scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, "GoodsService-task-Thread");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                this.reloadConfig();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate reloadConfig error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
        source.queryList(GoodsInfo.class);
    }

    @Override
    public void destroy(AnyValue config) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    protected void reloadConfig() {
        this.goodsIntros = source.queryList(GoodsIntro.class);
    }

    public List<GoodsIntro> queryGoodsIntro() {
        return this.goodsIntros;
    }

    @Comment("查询单个商品")
    public GoodsInfo findGoods(@Comment("商品ID") int goodsid) {
        return source.find(GoodsInfo.class, goodsid);
    }

    @Comment("查询金币商品列表")
    public Sheet<GoodsInfo> queryGoodsInfo(@Comment("查询过滤条件") GoodsBean bean, @Comment("翻页信息") Flipper flipper) {
        Flipper.sortIfAbsent(flipper, (bean != null && bean.getStatus() > 0) ? "display" : "status, display");
        return source.querySheet(GoodsInfo.class, flipper, bean);
    }

    @Comment("新增/修改单个金币商品对象")
    public RetResult updateGoodsInfo(@Comment("商品对象") GoodsInfo info, @Comment("更新字段") String... columns) {
        if (info.getGoodsid() > 0) {
            source.updateColumn(info, columns);
        } else {
            Number maxid = source.getNumberResult(GoodsInfo.class, FilterFunc.MAX, 0, "goodsid", FilterNode.create("buytype", info.getBuytype()).and("goodstype", info.getGoodstype()));
            if (maxid.longValue() < 10_000_000) maxid = info.getBuytype() * 1000000 + info.getGoodstype() * 1000;
            info.setGoodsid(maxid.intValue() + 1);
            info.setCreatetime(System.currentTimeMillis());
            source.insert(info);
        }
        return RetResult.success();
    }

    @Comment("领取复合商品")
    public RetResult receiveGoodsItems(final int userid, final short goodstype, final int goodscount, final long time, final String module, final String remark, final GoodsItem... items) {
        if (items == null || items.length == 0) return RetResult.success();
        long gcoin = 0;
        long gdiamond = 0;
        long gcoupon = 0;
        long gliveness = 0;
        final int buycount = Math.max(1, goodscount);
        final Map<String, List<GoodsItem>> gameidmap = new LinkedHashMap<>();
        for (GoodsItem item : items) {
            if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_COIN) {
                gcoin += buycount * item.getGoodscount();
            } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_DIAMOND) {
                gdiamond += buycount * item.getGoodscount();
            } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_COUPON) {
                gcoupon += buycount * item.getGoodscount();
            } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_LIVENESS) {
                gliveness += buycount * item.getGoodscount();
            } else if (item.getGameid() != null && !item.getGameid().isEmpty()) {
                gameidmap.computeIfAbsent(item.getGameid(), t -> new ArrayList<>()).add(item);
            } else {
                throw new RuntimeException(item + " not found gameid");
            }
        }
        if (!gameidmap.isEmpty()) {
            for (Map.Entry<String, List<GoodsItem>> en : gameidmap.entrySet()) {
                RetResult rs = moduleService.remoteGameModule(userid, en.getKey(), "notifyGoodsInfo", Utility.ofMap("goodstype", goodstype, "goodscount", goodscount, "items", en.getValue()));
                if (!rs.isSuccess()) return rs;
            }
        }
        if (gcoin > 0 || gdiamond > 0 || gcoupon > 0) {
            RetResult rs = userService.updatePlatfUserCoinDiamondCoupons(userid, gcoin, gdiamond, gcoupon, time, module, remark);
            if (!rs.isSuccess()) return rs;
        }
        RetResult rs = new RetResult();
        if (gliveness > 0) {
            try {
                long userliveness = userService.updateGameUserLiveness(userid, gliveness, time, "platf", module, remark);
                rs.attach("userliveness", userliveness);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "updateGameUserLiveness error, userid=" + userid + ", gliveness=" + gliveness, e);
                return RetCodes.retResult(RetCodes.RET_INNER_ILLEGAL);
            }
        }
        return rs;
    }
}
