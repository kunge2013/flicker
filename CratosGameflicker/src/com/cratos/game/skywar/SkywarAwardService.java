/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.base.*;
import com.cratos.platf.order.*;
import com.cratos.platf.util.Utils;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.persistence.Transient;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class SkywarAwardService extends BaseService {

    @Resource(name = "skywar")
    protected DataSource source;

    @Comment("定时任务")
    protected ScheduledThreadPoolExecutor scheduler;

    @Transient //摇奖的选项
    protected List<SkywarAwardInfo>[] awardInfos;

    @Transient //摇奖的权重
    protected int[][] awardWeights = new int[100][1];

    @Transient  //摇奖门槛要求
    protected int[] awardScores = new int[]{1_0000, 10_0000, 50_0000, 100_0000, 500_0000, 2000_0000};

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
                logger.log(Level.SEVERE, SkywarAwardInfo.class.getSimpleName() + " scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    protected void reloadConfig() {
        //摇奖选项配置
        List<SkywarAwardInfo> alllist = source.queryList(SkywarAwardInfo.class, new Flipper(100, "awardlevel ASC, display ASC, awardid ASC"), (FilterNode) null);
        Map<Short, List<SkywarAwardInfo>> map = new LinkedHashMap<>();
        for (SkywarAwardInfo info : alllist) {
            map.computeIfAbsent(info.getAwardlevel(), (r) -> new ArrayList()).add(info);
        }
        List<SkywarAwardInfo>[] array = map.values().toArray(new ArrayList[map.size()]);
        int[][] ws = new int[array.length][];
        for (int i = 0; i < ws.length; i++) {
            ws[i] = Utils.calcIndexWeights(array[i].stream());
        }
        this.awardInfos = array;
        this.awardWeights = ws;
    }

    public List<SkywarAwardInfo>[] queryAwardInfo() {
        return this.awardInfos;
    }

    public int[] queryAwardScore() {
        return this.awardScores;
    }

    public RetResult<Map> runAward(final SkywarService serivce, final SkywarAccount account, final SkywarPlayer player, int awardlevel) {
        final long now = System.currentTimeMillis();
        final int intday = Utility.yyyyMMdd(now);
        final int userid = account.getUserid();
        if (awardlevel < 1 || awardlevel > this.awardInfos.length) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        synchronized (account) {
            if (account.getCurrAwardScore() < this.awardScores[awardlevel - 1]) return RetCodes.retResult(RetCodes.RET_AWARD_NOEVENT);
            final int[] weights = this.awardWeights[awardlevel - 1];
            final List<SkywarAwardInfo> awards = this.awardInfos[awardlevel - 1];
            SkywarAwardInfo rsinfo = awards.get(weights[serivce.gameRandom().nextInt(weights.length)]);
            int count = source.getNumberResult(SkywarAwardRecord.class, FilterFunc.COUNT, 0, null, FilterNode.create("userid", userid).and("intday", intday)).intValue();
            SkywarAwardRecord rsrecord = rsinfo.createAwardRecord(userid, intday, count + 1, now);
            GoodsItem item = rsinfo.createGoodsItem();
            RetResult rs = serivce.receiveGoodsItems(userid, 1, now, "gameaward", "夺奖券摇奖", Utility.ofArray(item));
            if (!rs.isSuccess()) return rs;
            account.setCurrAwardScore(0);
            source.insert(rsrecord);
            Map<String, Object> map = new HashMap();
            map.put("goodsitems", new GoodsItem[]{item});
            map.put("awardid", rsinfo.getAwardid());
            map.put("currawardscore", account.getCurrAwardScore());
            map.put("usercoins", player.getCoins());
            map.put("userdiamonds", player.getDiamonds());
            map.put("usercoupons", player.getCoupons());
            return new RetResult(map);
        }
    }

}
