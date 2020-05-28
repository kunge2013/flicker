/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.vip;

import com.cratos.platf.base.*;
import com.cratos.platf.info.DictService;
import com.cratos.platf.letter.*;
import com.cratos.platf.order.*;
import com.cratos.platf.user.*;
import com.cratos.platf.util.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
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
public class VipService extends BaseService {

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    @Resource
    protected UserService userService;

    @Resource
    protected DictService dictService;

    @Resource
    protected GoodsService goodsService;

    @Resource
    protected LetterService letterService;

    @Resource(name = "platf")
    protected DataSource source;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //VIP的选项
    protected List<VipInfo> vips;

    @Transient //摇奖的选项
    protected List<VipAwardInfo> awardInfos;

    @Transient //摇奖的权重
    protected int[] weights = new int[100];

    @Override
    public void init(AnyValue conf) {
        reloadConfig();
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        final long seconds = 1 * 60 * 1000L;
        final long delay = seconds - System.currentTimeMillis() % seconds; //每分钟执行
        scheduler.scheduleAtFixedRate(() -> {
            try {
                reloadConfig();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, VipAwardInfo.class.getSimpleName() + " scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    protected void reloadConfig() {
        this.vips = source.queryList(VipInfo.class, new Flipper(100, "vipid ASC"), (FilterNode) null);
        //摇奖选项配置
        List<VipAwardInfo> list = source.queryList(VipAwardInfo.class, new Flipper(100, "display ASC, awardid ASC"), (FilterNode) null);
        int[] ws = Utils.calcIndexWeights(list.stream());
        this.awardInfos = list;
        this.weights = ws;
    }

    public UserDetail checkUpgradeVipLevel(final UserDetail user) {
        final List<VipInfo> list = this.vips;
        if (vips == null) return user;
        final int oldviplevel = user.getViplevel();
        for (int i = list.size() - 1; i >= 0; i--) {
            VipInfo vip = list.get(i);
            if (vip.getPaymoneytotal() > 0 && user.getPaymoney() >= vip.getPaymoneytotal()
                && vip.getVipid() > oldviplevel) {
                user.setViplevel(vip.getVipid());
                final VipInfo vipinfo = vip;
                this.scheduler.schedule(() -> upgradeBatchVipLevel(user, list, vipinfo, oldviplevel), 1, TimeUnit.SECONDS);
                return user;
            }
        }
        return user;
    }

    private void upgradeBatchVipLevel(final UserDetail user, List<VipInfo> list, final VipInfo vip, int oldviplevel) {
        for (int i = 0; i < list.size(); i++) {
            VipInfo one = list.get(i);
            if (one.getVipid() <= oldviplevel) continue;
            if (one.getVipid() > vip.getVipid()) break;
            await(10);
            upgradeVipLevel(user.getUserid(), one);
        }
    }

    private void upgradeVipLevel(final int userid, final VipInfo vip) {
        GoodsItem[] items = vip.getGoodsitems();
        if (items != null && items.length > 0) {
            LetterRecord letter = new LetterRecord();
            letter.setUserid(userid);
            letter.setLettertype(LetterRecord.LETTER_TYPE_GIFT);
            letter.setTitle(letterService.bundleResourceValue("vip.upgradelevel.title", vip.getVipid()));
            letter.setContent(letterService.bundleResourceValue("vip.upgradelevel.content", vip.getVipid(), vip.getVipid()));
            letter.setGoodsitems(items);
            letter.setModule("vip");
            letter.setRemark("VIP等级为" + vip.getVipid());
            letterService.createLetterRecord(letter);
        }
    }

    public RetResult<Map> runAward(final int userid) {
        final long now = System.currentTimeMillis();
        final int intday = Utility.yyyyMMdd(now);
        synchronized (userLock(userid)) {
            UserInfo user = userService.findUserInfo(userid);
            if (user == null) {
                removeUserLock(userid);
                return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
            }
            final VipInfo vip = findVipInfo(user.getViplevel());
            final int runcount = vip == null ? 0 : vip.getAwardcount();
            int jiangs = getCountVipAwardRecord(userid, intday);
            if (jiangs >= runcount) return RetCodes.retResult(RetCodes.RET_AWARD_NOEVENT);
            final int goodscount = 1;
            VipAwardInfo firstinfo = awardInfos.get(weights[random.nextInt(weights.length)]);
            VipAwardInfo rsinfo = firstinfo;
            if (firstinfo.getGoodstype() == GoodsInfo.GOODS_TYPE_ITEM_PROP && firstinfo.getGoodsobjid() == 608) { //百宝箱
                while ((rsinfo.getGoodstype() == GoodsInfo.GOODS_TYPE_ITEM_PROP && rsinfo.getGoodsobjid() == 608)) {
                    rsinfo = awardInfos.get(weights[random.nextInt(weights.length)]);
                }
            }
            VipAwardRecord rsrecord = rsinfo.createAwardRecord(userid, intday, jiangs + 1, now);
            if (firstinfo != rsinfo) {
                rsrecord.setRemark("选中百宝箱; awardid=" + firstinfo.getAwardid() + ", goodstype=" + firstinfo.getGoodstype() + ", goodsobjid=" + firstinfo.getGoodsobjid());
            }
            GoodsItem item = rsinfo.createGoodsItem();
            RetResult rs = goodsService.receiveGoodsItems(userid, rsinfo.getGoodstype(), goodscount, now, "vipaward", "VIP摇奖;", item);
            if (!rs.isSuccess()) return rs;
            source.insert(rsrecord);
            Map<String, Object> map = new HashMap();
            map.put("goodsitems", new GoodsItem[]{item});
            map.put("awardid", firstinfo.getAwardid());
            UserInfo rsuser = userService.findUserInfo(userid);
            map.put("usercoins", rsuser.getCoins());
            map.put("userdiamonds", rsuser.getDiamonds());
            map.put("usercoupons", rsuser.getCoupons());
            return new RetResult(map);
        }
    }

    public RetResult<Integer> checkAward(int userid) {
        UserInfo user = userService.findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        if (user.getViplevel() < 1) return new RetResult().result(0);
        final int intday = Utility.today();
        final VipInfo vip = findVipInfo(user.getViplevel());
        final int runcount = vip == null ? 0 : vip.getAwardcount();
        int jiangs = getCountVipAwardRecord(userid, intday);
        return new RetResult().result(jiangs >= runcount ? 0 : (runcount - jiangs));
    }

    public int getCountVipAwardRecord(int userid, int intday) {
        return source.getNumberResult(VipAwardRecord.class, FilterFunc.COUNT, 0, null, FilterNode.create("userid", userid).and("intday", intday)).intValue();
    }

    public List<VipAwardInfo> queryVipAwardInfo() {
        return this.awardInfos;
    }

    public List<VipInfo> queryVipInfo() {
        return this.vips;
    }

    public VipInfo findVipInfo(int vipid) {
        List<VipInfo> list = this.vips;
        if (list == null) return null;
        for (VipInfo vip : list) {
            if (vip.getVipid() == vipid) return vip;
        }
        return null;
    }
}
