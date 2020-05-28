/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.duty;

import com.cratos.platf.base.*;
import com.cratos.platf.info.*;
import com.cratos.platf.order.*;
import com.cratos.platf.user.UserService;
import com.cratos.platf.util.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.persistence.Transient;
import org.redkale.boot.Application;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class DutyService extends BaseService {

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected DictService dictService;

    @Resource
    protected GoodsService goodsService;

    @Resource
    protected ModuleAddressService moduleService;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //签到奖励列表
    protected List<DutyReward> rewards;

    @Transient //摇奖的选项
    protected List<DutyAwardInfo> awardInfos;

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
                logger.log(Level.SEVERE, DutyAwardInfo.class.getSimpleName() + " scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    protected void reloadConfig() {
        this.rewards = source.queryList(DutyReward.class, new Flipper(100, "dutyindex ASC"), (FilterNode) null);
        //摇奖选项配置
        List<DutyAwardInfo> list = source.queryList(DutyAwardInfo.class, new Flipper(100, "display ASC, awardid ASC"), (FilterNode) null);
        int[] ws = Utils.calcIndexWeights(list.stream());
        this.awardInfos = list;
        this.weights = ws;
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
            final int jiangcount = user.getLoginseries() >= 3 ? 2 : 1;
            int jiangs = getCountDutyAwardRecord(userid, intday);
            if (jiangs >= jiangcount) return RetCodes.retResult(RetCodes.RET_AWARD_NOEVENT);
            final int factor = jiangcount > 1 && user.getViplevel() >= 1 ? (user.getViplevel() >= 3 ? 3 : 2) : 1;
            DutyAwardInfo firstinfo = awardInfos.get(weights[random.nextInt(weights.length)]);
            DutyAwardInfo rsinfo = firstinfo;
            if (firstinfo.getGoodstype() == GoodsInfo.GOODS_TYPE_ITEM_PROP && firstinfo.getGoodsobjid() == 608) { //百宝箱
                while ((rsinfo.getGoodstype() == GoodsInfo.GOODS_TYPE_ITEM_PROP && rsinfo.getGoodsobjid() == 608)) {
                    rsinfo = awardInfos.get(weights[random.nextInt(weights.length)]);
                }
            }
            DutyAwardRecord rsrecord = rsinfo.createAwardRecord(userid, intday, jiangs + 1, now);
            if (firstinfo != rsinfo) {
                rsrecord.setRemark("选中百宝箱; awardid=" + firstinfo.getAwardid() + ", goodstype=" + firstinfo.getGoodstype() + ", goodsobjid=" + firstinfo.getGoodsobjid());
            }
            GoodsItem item = rsinfo.createGoodsItem();
            item.setGoodscount(item.getGoodscount() * factor);
            RetResult rs = goodsService.receiveGoodsItems(userid, rsinfo.getGoodstype(), factor, now, "dutyaward", "签到摇奖;factor=" + factor, item);
            if (!rs.isSuccess()) return rs;
            source.insert(rsrecord);
            Map<String, Object> map = new HashMap();
            map.put("goodsitems", new GoodsItem[]{item});
            map.put("awardid", firstinfo.getAwardid());
            map.put("goodstype", firstinfo.getGoodstype());
            map.put("goodsobjid", firstinfo.getGoodsobjid());
            map.put("factor", factor);
            UserInfo rsuser = userService.findUserInfo(userid);
            map.put("usercoins", rsuser.getCoins());
            map.put("userdiamonds", rsuser.getDiamonds());
            map.put("usercoupons", rsuser.getCoupons());
            return new RetResult(map);
        }
    }

    public int getCountDutyAwardRecord(int userid, int intday) {
        return source.getNumberResult(DutyAwardRecord.class, FilterFunc.COUNT, 0, null, FilterNode.create("userid", userid).and("intday", intday)).intValue();
    }

    public List<DutyAwardInfo> queryDutyAwardInfo() {
        return this.awardInfos;
    }

    public List<DutyReward> queryDutyReward() {
        return this.rewards;
    }

    public DutyReward getLastDutyReward() {
        List<DutyReward> list = this.rewards;
        return (list == null || list.isEmpty()) ? null : list.get(list.size() - 1);
    }

    public DutyAccount findDutyAccount(int userid) {
        return source.find(DutyAccount.class, userid);
    }

    public static void main(String[] args) throws Throwable {
        DutyService service = Application.singleton(DutyService.class);
        DutyReward reward = new DutyReward();
        reward.setDutyrewardid(100001);
        reward.setDutyindex(1);
        reward.setGoodsitems(Utility.ofArray(new GoodsItem(GoodsInfo.GOODS_TYPE_COIN, 2000)));
        reward.setCreatetime(0);
        service.source.insert(reward);
        reward.setDutyrewardid(100002);
        reward.setDutyindex(2);
        reward.setGoodsitems(Utility.ofArray(new GoodsItem(GoodsInfo.GOODS_TYPE_ITEM_SKIN, "skywar", 102, 1, 7 * 24 * 60 * 60)));
        service.source.insert(reward);
        reward.setDutyrewardid(100003);
        reward.setDutyindex(3);
        reward.setGoodsitems(Utility.ofArray(new GoodsItem(GoodsInfo.GOODS_TYPE_COIN, 10000)));
        service.source.insert(reward);
        reward.setDutyrewardid(100004);
        reward.setDutyindex(4);
        reward.setGoodsitems(Utility.ofArray(new GoodsItem(GoodsInfo.GOODS_TYPE_COIN, 10000)));
        service.source.insert(reward);
        reward.setDutyrewardid(100005);
        reward.setDutyindex(5);
        reward.setGoodsitems(Utility.ofArray(new GoodsItem(GoodsInfo.GOODS_TYPE_COUPON, 1000)));
        service.source.insert(reward);
        reward.setDutyrewardid(100006);
        reward.setDutyindex(6);
        reward.setGoodsitems(Utility.ofArray(new GoodsItem(GoodsInfo.GOODS_TYPE_COIN, 20000)));
        service.source.insert(reward);
        reward.setDutyrewardid(100007);
        reward.setDutyindex(7);
        reward.setGoodsitems(Utility.ofArray(new GoodsItem(GoodsInfo.GOODS_TYPE_ITEM_SKIN, "skywar", 102, 1, 0)));
        service.source.insert(reward);

    }

    public RetResult<Integer> checkToday(int userid) {
        final DutyReward lastReward = getLastDutyReward();
        if (lastReward == null) return new RetResult().result(1);
        DutyAccount account = findDutyAccount(userid);
        if (account == null) return new RetResult().result(0);
        if (account.getLastdutyseries() >= lastReward.getDutyindex()) {
            return new RetResult().result(1);
        }
        if (account.getLastdutyday() >= Utility.today()) {
            return new RetResult().result(1);
        }
        return new RetResult().result(0);
    }

    public RetResult bookToday(final int userid) {
        final int dutyType = dictService.findDictValue(DictInfo.PLATF_DUTY_TYPE, DutyReward.DUTY_TYPE_SERIE);
        synchronized (this) {
            final long now = System.currentTimeMillis();
            final List<DutyReward> list = queryDutyReward();
            DutyAccount account = findDutyAccount(userid);
            if (account != null && account.getLastdutyseries() >= list.get(list.size() - 1).getDutyindex()) {
                return RetCodes.retResult(RetCodes.RET_DUTY_ILLEGAL);
            }
            if (account == null) {
                account = new DutyAccount();
                account.setUserid(userid);
                account.setCreatetime(now);
                account.setUpdatetime(now);
            }
            final int today = Utility.today();
            DutyRecord old = source.find(DutyRecord.class, userid + "-" + today);
            if (old != null) return RetCodes.retResult(RetCodes.RET_DUTY_REPEAT);
            if (dutyType == DutyReward.DUTY_TYPE_SERIE) {
                if (account.getLastdutyday() == Utility.yesterday()) {
                    account.increLastdutyseries();
                } else {
                    account.setLastdutyseries(1);
                }
            } else if (dutyType == DutyReward.DUTY_TYPE_TOTAL) {
                account.increLastdutyseries();
            } else {
                return RetCodes.retResult(RetCodes.RET_CONFIG_ILLEGAL);
            }
            account.setLastdutyday(today);
            account.setUpdatetime(now);
            DutyReward reward = null;
            for (DutyReward one : list) {
                if (one.getDutyindex() == account.getLastdutyseries()) {
                    reward = one;
                    break;
                }
            }
            if (reward == null) return RetCodes.retResult(RetCodes.RET_DUTY_REPEAT);
            DutyRecord record = new DutyRecord();
            record.setUserid(userid);
            record.setIntday(today);
            record.setDutyindex(reward.getDutyindex());
            record.setDutyrewardid(reward.getDutyrewardid());
            record.setDutyitems(reward.getGoodsitems());
            record.setCreatetime(now);
            record.setDutyrecordid(record.getUserid() + "-" + record.getIntday());
            GoodsItem[] items = reward.getGoodsitems();
            if (items != null) {
                long gcoin = 0;
                long gdiamond = 0;
                long gcoupon = 0;
                final Map<String, List<GoodsItem>> gameidmap = new LinkedHashMap<>();
                for (GoodsItem item : items) {
                    if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_COIN) {
                        gcoin = item.getGoodscount();
                    } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_DIAMOND) {
                        gdiamond = item.getGoodscount();
                    } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_COUPON) {
                        gcoupon = item.getGoodscount();
                    } else if (item.getGameid() != null && !item.getGameid().isEmpty()) {
                        gameidmap.computeIfAbsent(item.getGameid(), t -> new ArrayList<>()).add(item);
                    } else {
                        throw new RuntimeException(item + " not found gameid");
                    }
                }
                if (!gameidmap.isEmpty()) {
                    for (Map.Entry<String, List<GoodsItem>> en : gameidmap.entrySet()) {
                        RetResult rs = moduleService.remoteGameModule(userid, en.getKey(), "notifyGoodsInfo", Utility.ofMap("items", en.getValue()));
                        if (!rs.isSuccess()) return rs;
                    }
                }
                if (gcoin > 0 || gdiamond > 0 || gcoupon > 0) {
                    RetResult rs = userService.updatePlatfUserCoinDiamondCoupons(userid, gcoin, gdiamond, gcoupon, now, "duty", "签到日:" + today + ",连续签到数:" + account.getLastdutyseries());
                    if (!rs.isSuccess()) return rs;
                }
                account.setDutycoins(account.getDutycoins() + gcoin);
                account.setDutydiamonds(account.getDutydiamonds() + gdiamond);
                account.setDutycoupons(account.getDutycoupons() + gcoupon);
            }
            if (source.update(account) < 1) source.insert(account);
            source.insert(record);
            if (reward.getModules() != null && !reward.getModules().isEmpty()) {
                for (String gameid : reward.getModules().split(";")) {
                    if (gameid.isEmpty()) continue;
                    RetResult rs = moduleService.remoteGameModule(userid, gameid, "notifyDutyRecord", Utility.ofMap("duty", record));
                    if (!rs.isSuccess()) logger.log(Level.SEVERE, record + " notifyDutyRecord error, rs = " + rs);
                }
            }
            return RetResult.success(reward);
        }
    }
}
