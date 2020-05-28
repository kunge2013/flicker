/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.*;
import com.cratos.platf.user.*;
import com.cratos.platf.util.ShuffleRandom;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.convert.json.JsonConvert;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("排行榜")
public class RankService extends BaseService {

    protected static final SecureRandom random = ShuffleRandom.createRandom();

    public static final int RANKTOP_LIMIT = 50;

    protected ScheduledThreadPoolExecutor scheduler;

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected DictService dictService;

    protected List<RankRecord> yesterdayWinList = new ArrayList<>();

    protected List<RankRecord> yesterdayLuckList = new ArrayList<>();

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, "RankService-Task-Thread");
            t.setDaemon(true);
            return t;
        });
        loadRank();
        scheduler.scheduleAtFixedRate(() -> {
            updateRank();
            loadRank();
        }, Utility.midnight() + 24 * 60 * 60 * 1000L - System.currentTimeMillis() + 10_000L, 24 * 60 * 60 * 1000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }
    
    protected void loadRank() {
        int day = Utility.yesterday();
        List<RankRecord> records = source.queryList(RankRecord.class, new Flipper(RANKTOP_LIMIT * 2, "rankindex ASC"), FilterNode.create("intday", day));
        List<RankRecord> winRecords = new ArrayList<>();
        List<RankRecord> luckRecords = new ArrayList<>();
        Map<String, String> gameNames = JsonConvert.root().convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, dictService.findDictValue(DictInfo.PLATF_APP_GAMENAMES, "{}"));
        for (RankRecord record : records) {
            UserInfo user = userService.findUserInfo(record.getUserid());
            if (user != null) record.setPlayer(user.createIntroPlayer());
            if (record.getRanktype() == RankRecord.RANK_TYPE_WIN) {
                winRecords.add(record);
            } else {
                String gamename = gameNames.getOrDefault(record.getGameid(), "");
                if (gamename.isEmpty()) {
                    logger.log(Level.WARNING, record.getGameid() + " 没有找到对应的中文名称");
                }
                record.setGamename(gamename);
                luckRecords.add(record);
            }
        }
        long max = winRecords.isEmpty() ? 2000_00L : winRecords.get(0).getRankvalue();
        if (winRecords.size() < RANKTOP_LIMIT) {
            int len = RANKTOP_LIMIT - winRecords.size();
            for (int i = 0; i < len; i++) {
                UserInfo robot = userService.randomRobot();
                while (robot == null && i < 5) {
                    await(2000);
                    robot = userService.randomRobot();
                }
                if (robot == null) continue;
                max += random.nextInt(i > len / 2 ? 500_00 : 1000_00);
                RankRecord record = new RankRecord();
                record.setIntday(day);
                record.setUserid(robot.getUserid());
                record.setPlayer(robot.createIntroPlayer());
                record.setRanktype(RankRecord.RANK_TYPE_WIN);
                record.setRankvalue(max);
                record.setCreatetime(System.currentTimeMillis());
                winRecords.add(record);
            }
            RankRecord.sort(winRecords);
        }
        max = luckRecords.isEmpty() ? 2000_00L : luckRecords.get(0).getRankvalue();
        if (luckRecords.size() < RANKTOP_LIMIT) {
            List<String> gameids = new ArrayList<>(gameNames.keySet());
            int len = RANKTOP_LIMIT - luckRecords.size();
            for (int i = 0; i < len; i++) {
                UserInfo robot = userService.randomRobot();
                if (robot == null) continue;
                max += random.nextInt(i > len / 2 ? 500_00 : 1000_00);
                RankRecord record = new RankRecord();
                record.setIntday(day);
                record.setGameid(gameids.get(random.nextInt(gameids.size())));
                record.setGamename(gameNames.getOrDefault(record.getGameid(), ""));
                record.setPlaytime(Utility.midnight() - random.nextInt(24 * 60 * 60) * 1000);
                record.setUserid(robot.getUserid());
                record.setPlayer(robot.createIntroPlayer());
                record.setRanktype(RankRecord.RANK_TYPE_LUCK);
                record.setRankvalue(max);
                record.setCreatetime(System.currentTimeMillis());
                luckRecords.add(record);
            }
            RankRecord.sort(luckRecords);
        }
        this.yesterdayWinList = winRecords;
        this.yesterdayLuckList = luckRecords;
    }

    protected void updateRank() {
        int day = Utility.yesterday();
        long time = Utility.midnight() - 100L;
        FilterNode coinNode = FilterNode.create("createtime", time).and("game", FilterExpress.NOTEQUAL, "platf")
            .and("game", FilterExpress.NOTEQUAL, "").and("coins", FilterExpress.GREATERTHAN, 1000_00L);
        List<UserCoinRecord> coinRecords = source.queryList(UserCoinRecord.class, new Flipper(RANKTOP_LIMIT, "coins DESC"), coinNode);
        int rankindex = 0;
        long now = System.currentTimeMillis();
        for (UserCoinRecord record : coinRecords) {
            RankRecord rank = new RankRecord();
            rank.setCreatetime(now);
            rank.setRankindex(++rankindex);
            rank.setGameid(record.getGame());
            rank.setIntday(day);
            rank.setPlaytime(record.getCreatetime());
            rank.setUserid(record.getUserid());
            rank.setRankvalue(record.getCoins());
            rank.setRanktype(RankRecord.RANK_TYPE_LUCK);
            rank.createRankid();
            source.insert(rank);
        }
        rankindex = 0;
        List<UserDayRecord> dayRecords = source.queryList(UserDayRecord.class, new Flipper(RANKTOP_LIMIT, "wincoins DESC"), FilterNode.create("createtime", time).and("wincoins", FilterExpress.GREATERTHAN, 1000_00L));
        for (UserDayRecord record : dayRecords) {
            RankRecord rank = new RankRecord();
            rank.setCreatetime(now);
            rank.setRankindex(++rankindex);
            rank.setGameid("");
            rank.setIntday(day);
            rank.setPlaytime(0);
            rank.setUserid(record.getUserid());
            rank.setRankvalue(record.getWincoins());
            rank.setRanktype(RankRecord.RANK_TYPE_WIN);
            rank.createRankid();
            source.insert(rank);
        }
    }

    public List<RankRecord> queryRankWinTop() {
        return yesterdayWinList;
    }

    public List<RankRecord> queryRankLuckTop() {
        return yesterdayLuckList;
    }

}
