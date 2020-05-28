/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.duty.DutyRecord;
import com.cratos.platf.base.*;
import com.cratos.platf.game.*;

import static com.cratos.game.skywar.Skywars.*;
import static com.cratos.platf.game.GameRetCodes.*;
import com.cratos.platf.game.battle.*;
import com.cratos.platf.info.*;
import com.cratos.platf.letter.*;
import com.cratos.platf.order.*;
import com.cratos.platf.user.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.convert.ConvertField;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.source.Range.IntRange;
import org.redkale.source.Range.LongRange;
import org.redkale.util.*;
import static org.redkale.util.Utility.ofMap;

/**
 *
 * @author zhangjx
 */
@RestService(name = "skywar")
public class SkywarService extends BattleCoinGameService<SkywarTable, SkywarRound, SkywarPlayer, SkywarEnemyRecord, SkywarEnemyKind, GameTableBean> {

    @Resource(name = "property.skywar.name")
    protected String gameName = "飞机大战";

    @Resource(name = "skywar")
    protected DataSource source;

    @Resource(name = "platf")
    protected DataSource platfSource;

    @Resource
    protected UserService userService;

    @Resource
    protected OrderService orderService;

    @Resource
    protected LetterService letterService;

    @Resource
    protected SkywarAwardService awardService;

    protected List<SkywarSkinInfo> skinInfos;

    protected List<RankRecord> nuclearRanks = new ArrayList<>();

    protected List<RankRecord> couponsRanks = new ArrayList<>();

    //-------------------- 比赛场开始 ------------------------
    protected SkywarSportOnceRoom sportApplyRoom101;  //核弹大奖赛

    protected SkywarSportOnceRoom sportApplyRoom102;  //50万子弹大奖赛

    protected SkywarSportOnceRoom sportRunRoom101;  //核弹大奖赛

    protected SkywarSportOnceRoom sportRunRoom102;  //50万子弹大奖赛

    protected SkywarSportManyRoom sportRunRoom103;  //核弹积分赛

    protected GoodsItem[] confSportGoodsItems101 = Utility.ofArray(
        new GoodsItem(GoodsInfo.GOODS_TYPE_ITEM_PROP, gameId(), Skywars.PROPID_NUCLEAR, 2),
        GoodsItem.createCoin(250000), GoodsItem.createCoin(150000)
    );

    protected GoodsItem[] confSportGoodsItems102 = Utility.ofArray(
        GoodsItem.createCoin(500000), GoodsItem.createCoin(250000), GoodsItem.createCoin(150000)
    );

    protected GoodsItem[] confSportGoodsItems103 = Utility.ofArray(
        new GoodsItem(GoodsInfo.GOODS_TYPE_ITEM_PROP, gameId(), Skywars.PROPID_NUCLEAR, 2),
        GoodsItem.createCoin(300000), GoodsItem.createCoin(200000)
    );

    protected int confSport101StartMills = 73_800_000; //20:30

    protected int confSport102StartMills = 45_000_000; //12:30 

    protected IntRange[] confSport103StartRanges = {
        new IntRange(36_000_000, 46_800_000), //10:00 - 13:00
        new IntRange(46_800_000, 57_600_000), //13:00 - 16:00
        new IntRange(57_600_000, 68_400_000), //16:00 - 19:00
        new IntRange(68_400_000, 79_200_000), //19:00 - 22:00
    };

    //-------------------- 比赛场结束 ------------------------
    protected JsonConvert tableConvert = JsonConvert.root().newConvert(null, obj -> {
        if (!(obj instanceof SkywarTable)) return null;
        return ConvertField.ofArray("shotLevels", SHOT_LEVELS);
    });

    @Override
    public void init(AnyValue config) {
        super.init(config);
        loadSkinInfo();
        this.initSportRoom();
        this.scheduler.scheduleAtFixedRate(() -> { //定时加载
            try {
                loadSkinInfo();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "loadSkinInfo() scheduleAtFixedRate error", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
        loadRank();
        this.scheduler.scheduleAtFixedRate(() -> { //定时加载
            try {
                loadRank();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "loadRank() scheduleAtFixedRate error", e);
            }
        }, 1, 5, TimeUnit.MINUTES);

        final long midDelay = 24 * 60 * 60 * 1000 + Utility.midnight() - System.currentTimeMillis();  //凌晨
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkExpireSkinRecord();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "checkExpireSkinRecord() scheduleAtFixedRate error", e);
            }
        }, midDelay, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
    }

    protected void initSportRoom() {
        int today = Utility.today();
        this.sportApplyRoom101 = new SkywarSportOnceRoom(101, "核弹大奖赛", today, this.confSportGoodsItems101);
        this.sportApplyRoom102 = new SkywarSportOnceRoom(102, "50万子弹大奖赛", today, this.confSportGoodsItems102);
        this.sportRunRoom103 = new SkywarSportManyRoom(103, "核弹积分赛", today, this.confSportGoodsItems103);

        //核弹大奖赛
        final long midDelay101 = Utility.midnight() + this.confSport101StartMills - System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                this.sportRunRoom101 = this.sportApplyRoom101;
                this.sportApplyRoom101 = new SkywarSportOnceRoom(101, "核弹大奖赛", Utility.tomorrow(), this.confSportGoodsItems101);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "sportRunRoom101() scheduleAtFixedRate error", e);
            }
        }, midDelay101, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

        //50万子弹大奖赛
        final long midDelay102 = Utility.midnight() + this.confSport102StartMills - System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                this.sportRunRoom102 = this.sportApplyRoom102;
                this.sportApplyRoom102 = new SkywarSportOnceRoom(102, "50万子弹大奖赛", Utility.tomorrow(), this.confSportGoodsItems102);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "sportRunRoom102() scheduleAtFixedRate error", e);
            }
        }, midDelay102, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
    }

    protected void checkExpireSkinRecord() {
        Flipper flipper = new Flipper(100);
        List<Integer> accountids = source.queryColumnList("userid", SkywarAccount.class, flipper, (FilterNode) null);
        while (!accountids.isEmpty()) {
            for (Integer userid : accountids) {
                if (playerAccounts.containsKey(userid)) {
                    SkywarAccount account = loadAccount(userid);
                    if (account.checkExpireSkinRecord()) {
                        SkywarPlayer player = findLivingPlayer(userid);
                        if (player != null) {
                            SkywarTable table = player.table();
                            if (table != null) {  //在游戏进行中进行切换皮肤
                                sendMap(table.onlinePlayers(userid), "onPlayerUpdateSkinMessage", new RetResult(ofMap("userid", userid, "skinid", account.getCurrSkinid())));
                            }
                        }
                    }
                } else {
                    SkywarAccount account = loadAccount(userid);
                    account.checkExpireSkinRecord();
                }
            }
            accountids = source.queryColumnList("userid", SkywarAccount.class,
                flipper.next(), (FilterNode) null);
        }
    }

    protected void loadRank() {
        int rankLimit = 100;
        List<SkywarAccount> naccounts = source.queryList(SkywarAccount.class,
            new Flipper(rankLimit, "currNuclears DESC"), FilterNode.create("currNuclears", FilterExpress.GREATERTHANOREQUALTO, 1));
        List<UserDetail> couponusers = platfSource.queryList(UserDetail.class,
            SelectColumn.includes("userid", "username", "face", "gender", "intro", "coupons"), new Flipper(100, "coupons DESC"), FilterNode.create("coupons", FilterExpress.GREATERTHANOREQUALTO, 20000));
        List<RankRecord> nuclears = new ArrayList<>();
        List<RankRecord> coupons = new ArrayList<>();
        for (SkywarAccount account : naccounts) {
            RankRecord record = new RankRecord();
            record.setUserid(account.getUserid());
            record.setRankvalue(account.getCurrNuclears());
            record.setRanktype(RankRecord.RANK_TYPE_EQUIP);
            record.setPlayer(userService.findIntroPlayer(account.getUserid()));
            nuclears.add(record);
        }
        int len = rankLimit - nuclears.size();
        for (int i = 0; i < len; i++) {
            UserInfo robot = userService.randomRobot();
            while (robot == null && i < 5) {
                await(2000);
                robot = userService.randomRobot();
            }
            if (robot == null) continue;
            RankRecord record = new RankRecord();
            record.setUserid(robot.getUserid());
            record.setPlayer(robot.createIntroPlayer());
            record.setRanktype(RankRecord.RANK_TYPE_WIN);
            record.setRankvalue(gameRandom.nextInt(100) + 1);
            nuclears.add(record);
        }
        RankRecord.sort(nuclears);

        for (UserDetail user : couponusers) {
            RankRecord record = new RankRecord();
            record.setUserid(user.getUserid());
            record.setRankvalue(user.getCoupons());
            record.setRanktype(RankRecord.RANK_TYPE_COUPON);
            record.setPlayer(user.createIntroPlayer());
            coupons.add(record);
        }
        len = rankLimit - coupons.size();
        for (int i = 0; i < len; i++) {
            UserInfo robot = userService.randomRobot();
            while (robot == null && i < 5) {
                await(2000);
                robot = userService.randomRobot();
            }
            if (robot == null) continue;
            RankRecord record = new RankRecord();
            record.setUserid(robot.getUserid());
            record.setPlayer(robot.createIntroPlayer());
            record.setRanktype(RankRecord.RANK_TYPE_WIN);
            record.setRankvalue(gameRandom.nextInt(10_0000) + 10000);
            coupons.add(record);
        }
        RankRecord.sort(coupons);

        this.nuclearRanks = nuclears;
        this.couponsRanks = coupons;
    }

    @Override
    protected JsonConvert createTableConvert(final SkywarTable table, final SkywarPlayer player) {
        return tableConvert;
    }

    @Override
    protected SkywarPlayer loadGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean, SkywarPlayer oldPlayer) {
        SkywarPlayer player = super.loadGamePlayer(user, clientAddr, sncpAddress, roomlevel, bean, oldPlayer);
        SkywarAccount account = loadAccount(user.getUserid());
        player.setShotradarendtime(account.getRadaringEndtime());
        if (account.getCurrBlood() < 1) {
            account.setCurrBlood(100);
        }
        return player;
    }

    protected ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return this.scheduler.schedule(command, delay, unit);
    }

    protected int checkBlood(SkywarAccount account, SkywarPlayer player) {
        int today = Utility.today();
        if (account.bloodDay != today) {
            account.bloodDay = today;
            account.bloodCount = 0;
            account.bloodTime = 0;
        }
        if (account.getCurrBlood() < 1) {
            account.setCurrBlood(100);
            account.bloodCount++;
            account.bloodTime = System.currentTimeMillis();
        }
        if (account.bloodCount < 1) return 30;
        return 30 + (account.getBloodCount() - 1) * 10;
    }

    @RestMapping(auth = true, comment = "报名比赛场")
    public RetResult applySport(int userid, int roomlevel) {
        SkywarSportOnceRoom sportRoom;
        if (roomlevel == 101) { //核弹大奖赛
            sportRoom = sportApplyRoom101;
            if (sportRoom == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
            if (sportRoom.containsApplyUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_APPLY_REPEAT);
            sportRoom.addApplyUserid(userid);
            this.sportRunRoom101 = sportApplyRoom101; //测试使用
        } else if (roomlevel == 102) { //50万子弹大奖赛
            sportRoom = sportApplyRoom102;
            if (sportRoom == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
            if (sportRoom.containsApplyUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_APPLY_REPEAT);
            sportRoom.addApplyUserid(userid);
            this.sportRunRoom102 = sportApplyRoom102; //测试使用
        } else {
            return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
        }
        return new RetResult(ofMap(
            "roomlevel", sportRoom.getRoomlevel(),
            "applycount", sportRoom.applyUserids.size()
        ));
    }

    @Override  //进入比赛场
    protected <T> RetResult<T> enterSportGame(UserInfo user, int roomlevel, String clientAddr, InetSocketAddress sncpAddress, Map<String, String> bean) {
        int userid = user.getUserid();
        if (roomlevel == 101) { //核弹大奖赛
            SkywarSportOnceRoom sportRoom = sportRunRoom101;
            if (sportRoom == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
            if (sportRoom.containsPlayedUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_PLAYED);
            if (!sportRoom.containsApplyUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_UNAPPLY);
        } else if (roomlevel == 102) { //50万子弹大奖赛
            SkywarSportOnceRoom sportRoom = sportRunRoom102;
            if (sportRoom == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
            if (sportRoom.containsPlayedUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_PLAYED);
            if (!sportRoom.containsApplyUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_UNAPPLY);
        } else if (roomlevel == 103) { //核弹积分赛
            if (sportRunRoom103 == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
        } else {
            return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
        }
        loadAccount(userid); //setLeaving
        putLivingPlayer(userid, loadGamePlayer(user, clientAddr, sncpAddress, roomlevel, bean, findLivingPlayer(userid)));
        return RetResult.success();
    }

    @Override  //进入比赛场
    protected RetResult<SkywarTable> joinSportTable(int userid, String clientAddr, InetSocketAddress sncpAddress, GameTableBean bean) {
        final SkywarPlayer player = findLivingPlayer(userid);
        int roomlevel = player.getRoomlevel();
        BattleSportRoom<SkywarTable> room = null;
        if (roomlevel == 101) { //核弹大奖赛
            SkywarSportOnceRoom sportRoom = sportRunRoom101;
            if (sportRoom == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
            synchronized (sportRoom) {
                if (sportRoom.containsPlayedUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_PLAYED);
                if (!sportRoom.containsApplyUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_UNAPPLY);
            }
            room = sportRoom;
        } else if (roomlevel == 102) { //50万子弹大奖赛
            SkywarSportOnceRoom sportRoom = sportRunRoom102;
            if (sportRoom == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
            synchronized (sportRoom) {
                if (sportRoom.containsPlayedUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_PLAYED);
                if (!sportRoom.containsApplyUserid(userid)) return GameRetCodes.retResult(RET_GAME_PLAYER_SPORT_UNAPPLY);
            }
            room = sportRoom;
        } else if (roomlevel == 103) { //核弹积分赛
            if (sportRunRoom103 == null) return GameRetCodes.retResult(RET_GAME_SPORT_UNSTART);
            room = sportRunRoom103;
        } else {
            return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
        }

        final SkywarTable table = createGameTable(player, bean);
        table.setGameid(gameId());
        table.setShotrate(this.confShotRate);
        table.paramBean(bean);
        table.initConfig(this);
        if (table.getCreatetime() < 1) table.setCreatetime(System.currentTimeMillis());
        table.setTableid(Utility.format36time(table.getCreatetime()) + "-" + Integer.toString(player.getUserid(), 36) + "-" + nodeid);
        RetResult rs = table.addPlayer(player, this);
        if (!rs.isSuccess()) return rs;
        player.online(player.getClientAddr(), player.sncpAddress());
        room.addTable(table);
        table.start(this);
        return joinNewTable(table, player, bean);
    }

    @RestMapping(auth = true, comment = "加载比赛场")
    public RetResult loadSport(int userid) {
        return new RetResult(ofMap(
            "sport101", ofMap(
                "roomlevel", sportApplyRoom101.getRoomlevel(),
                "goodsitems", confSportGoodsItems101,
                "startmills", confSport101StartMills,
                "applycount", sportApplyRoom101.applyUserids.size(),
                "myapplyed", sportApplyRoom101.containsApplyUserid(userid)
            ),
            "sport102", ofMap(
                "roomlevel", sportApplyRoom102.getRoomlevel(),
                "goodsitems", confSportGoodsItems102,
                "startmills", confSport102StartMills,
                "applycount", sportApplyRoom102.applyUserids.size(),
                "myapplyed", sportApplyRoom102.containsApplyUserid(userid)
            ),
            "sport103", ofMap("roomlevel", 103,
                "goodsitems", confSportGoodsItems103,
                "startranges", confSport103StartRanges
            )
        ));
    }

    @RestMapping(auth = true, comment = "加载夺奖券信息")
    public RetResult loadAward(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        return new RetResult(ofMap("awardinfos", awardService.queryAwardInfo(),
            "awardscores", awardService.queryAwardScore(),
            "currawardscore", account.getCurrAwardScore()));
    }

    @RestMapping(auth = true, comment = "夺奖券摇奖")
    public RetResult runAward(int userid, @RestParam(name = "#") int awardlevel) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        return awardService.runAward(this, account, player, awardlevel);
    }

    @RestMapping(auth = true, comment = "测试")
    public RetResult testAward(int userid, @RestParam(name = "#") long score) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        account.setCurrAwardScore(Math.max(10000, score));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "加载血量信息")
    public RetResult loadBlood(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        int cdseconds = checkBlood(account, player);
        long bloodcha = (System.currentTimeMillis() - account.bloodTime) / 1000;
        return new RetResult(ofMap("cdseconds", bloodcha >= cdseconds ? 0 : (cdseconds - bloodcha),
            "blood", account.getCurrBlood(),
            "wingunlocks", account.getWingUnlocks(),
            "wingpausing", account.wingPausing(),
            "wingrunnings", account.getWingRunnings(),
            "wingremains", account.wingRemains()));
    }

    @RestMapping(auth = true, comment = "补血领取")
    public RetResult giveBlood(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        if (account.bloodTime > 0 && System.currentTimeMillis() - account.bloodTime < checkBlood(account, player) * 1000) {
            return GameRetCodes.retResult(GameRetCodes.RET_GAME_CDTIME_ILLEGAL);
        }
        account.setCurrBlood(0);
        int cdseconds = checkBlood(account, player);
        GoodsItem[] items = Utility.ofArray(GoodsItem.createDiamond(Math.max(1, gameRandom.nextInt(5))));
        int propcount = gameRandom.nextInt(3);
        if (propcount > 0) {
            int[] propids = new int[]{Skywars.PROPID_RADAR, Skywars.PROPID_FRENZY, Skywars.PROPID_TRACK, Skywars.PROPID_TRANSMIT};
            int subpropcount = gameRandom.nextInt(3);
            if (subpropcount > 0) {
                items = Utility.append(items, new GoodsItem(GoodsInfo.GOODS_TYPE_ITEM_PROP, gameId(), propids[gameRandom.nextInt(propids.length)], subpropcount));
                if (propcount - subpropcount > 0) {
                    items = Utility.append(items, new GoodsItem(GoodsInfo.GOODS_TYPE_ITEM_PROP, gameId(), propids[gameRandom.nextInt(propids.length)], propcount - subpropcount));
                }
            } else {
                items = Utility.append(items, new GoodsItem(GoodsInfo.GOODS_TYPE_ITEM_PROP, gameId(), propids[gameRandom.nextInt(propids.length)], propcount));
            }
        }
        try {
            RetResult rs = receiveGoodsItems(userid, 1, System.currentTimeMillis(), "giveblood", "补血", items);
            if (!rs.isSuccess()) items = null;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "giveBlood.receiveGoodsItems error", ex);
            items = null;
        }
        return new RetResult(ofMap("cdseconds", cdseconds, "blood", account.getCurrBlood(), "goodsitems", items));
    }

    @RestMapping(auth = true, comment = "减血")
    public RetResult decreBlood(int userid, @RestParam(name = "#") int kindid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarEnemyKind kind = findEnemyKind(kindid);
        if (kind == null) return RetCodes.retResult(RetCodes.RET_REPEAT_ILLEGAL);
        SkywarAccount account = loadAccount(userid);
        account.decCurrBlood(kind.getShotblood());
        if (account.getCurrBlood() < 1) {
            SkywarTable table = player.table();
            if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
            long decdiamond = Math.max(1, Math.min(3, table.getRoomlevel()));
            if (player.getDiamonds() >= decdiamond) {
                userService.updatePlatfUserCoinDiamondCoupons(userid, 0L, -decdiamond, 0L, System.currentTimeMillis(), "dead", "血量为0");
                player.setDiamonds(player.getDiamonds() - decdiamond);
            }
            int cdseconds = checkBlood(account, player);
            sendMap(table.onlinePlayers(), "onPlayerDeadMessage", new RetResult(ofMap("userid", userid, "userdiamonds", player.getDiamonds(), "cdseconds", cdseconds, "blood", account.getCurrBlood())));
        }
        return new RetResult(ofMap("userid", userid, "blood", account.getCurrBlood()));
    }

    @RestMapping(auth = true, comment = "使用僚机")
    public RetResult useWing(int userid, @RestParam(name = "#") int wingcount) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        if (wingcount < 1) wingcount = 1;
        if (account.getWingUnlocks() > 0 && wingcount < account.getWingUnlocks()) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (account.getWingUnlocks() < 1 && wingcount != 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (account.getCurrWings() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);

        if (account.getWingUnlocks() > 0) {
            account.setWingRunnings(wingcount);
            account.wingPausing(false);
        } else {
            account.setWingRunnings(wingcount);
            account.setWingingEndTime(System.currentTimeMillis() + Skywars.WING_PERIOD_MILLS);
            account.wingPausing(false);
            scheduler.schedule(() -> {
                SkywarAccount a = loadAccount(userid);
                a.setWingRunnings(0);
                a.wingPausing(false);
                a.setWingingEndTime(0L);
            }, Skywars.WING_PERIOD_MILLS, TimeUnit.MINUTES);
        }
        int wingremains = account.wingRemains();
        sendMap(table.onlinePlayers(userid), "onPlayerUseWingMessage", new RetResult(ofMap("userid", userid, "wingrunnings", account.getWingRunnings(), "wingremains", wingremains)));
        return RetResult.success(ofMap("userid", userid, "wingrunnings", account.getWingRunnings(), "wingremains", wingremains));
    }

    @RestMapping(auth = true, comment = "暂停僚机")
    public RetResult pauseWing(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        account.wingPausing(true);
        int wingremains = account.wingRemains();
        sendMap(table.onlinePlayers(userid), "onPlayerPauseWingMessage", new RetResult(ofMap("userid", userid, "wingremains", wingremains)));
        return RetResult.success(ofMap("userid", userid, "wingremains", wingremains));
    }

    @RestMapping(auth = true, comment = "获取火力下级信息")
    public RetResult loadNextFireLevel(int userid) {
        SkywarAccount account = loadAccount(userid);
        int nextFireLevel = Skywars.nextFirelevel(account.getFireLevel());
        return new RetResult(ofMap("nextFireLevel", nextFireLevel,
            "nextLevelNeedDiamond", Skywars.fireLevelNeedDiamond(nextFireLevel),
            "nextLevelAwardCoin", Skywars.fireLevelAwardCoin(nextFireLevel)));
    }

    @RestMapping(auth = true, comment = "升级火力等级")
    public RetResult upgradeFireLevel(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        synchronized (account) {
            int nextFireLevel = Skywars.nextFirelevel(account.getFireLevel());
            long nextLevelNeedDiamond = Skywars.fireLevelNeedDiamond(nextFireLevel);
            if (player.getDiamonds() < nextLevelNeedDiamond) return RetCodes.retResult(RetCodes.RET_USER_DOMAINDS_NOTENOUGH);
            long nextLevelAwardCoin = Skywars.fireLevelAwardCoin(nextFireLevel);
            RetResult rs = userService.updatePlatfUserCoinDiamondCoupons(userid, nextLevelAwardCoin, -nextLevelNeedDiamond, 0L, System.currentTimeMillis(), "firelevel", "火力等级升级到" + nextFireLevel);
            if (!rs.isSuccess()) return rs;
            player.setDiamonds(player.getDiamonds() - nextLevelNeedDiamond);
            player.increCoin(nextLevelAwardCoin);
            account.setFireLevel(nextFireLevel);
        }
        int nextFireLevel = Skywars.nextFirelevel(account.getFireLevel());
        return new RetResult(ofMap("nextFireLevel", nextFireLevel,
            "nextLevelNeedDiamond", Skywars.fireLevelNeedDiamond(nextFireLevel),
            "nextLevelAwardCoin", Skywars.fireLevelAwardCoin(nextFireLevel),
            "fireLevel", account.getFireLevel(),
            "usercoins", player.getCoins(),
            "userdiamonds", player.getDiamonds(),
            "usercoupons", player.getCoupons()));
    }

    @RestMapping(auth = true, comment = "排行榜")
    public RetResult queryRank(int userid) {
        return new RetResult(ofMap("nuclears", this.nuclearRanks, "coupons", this.couponsRanks));
    }

    @RestMapping(auth = true, comment = "点赞排行榜")
    public RetResult<Map> zanRank(int userid) {
        SkywarAccount account = loadAccount(userid);
        synchronized (account) {
            int coin = 500;
            int today = Utility.today();
            if (account.getZanRankDay() == today) return RetCodes.retResult(RetCodes.RET_REPEAT_ILLEGAL);
            long usercoins = userService.increPlatfUserCoins(userid, coin, System.currentTimeMillis(), "zanrank", "点赞排行榜");
            account.setZanRankDay(today);
            return new RetResult<>(ofMap("usercoins", usercoins, "zanRankDay", today, "goodsitems", Utility.ofArray(GoodsItem.createCoin(coin))));
        }
    }

    @RestMapping(auth = false, comment = "获取战机皮肤信息列表")
    public RetResult<List<SkywarSkinInfo>> querySkinInfo() {
        return new RetResult(skinInfos);
    }

    @RestMapping(auth = false, comment = "获取敌机列表")
    public RetResult<List<SkywarEnemyKind>> queryEnemyKind() {
        return new RetResult(enemyKinds.values());
    }

    @RestMapping(auth = true, comment = "赠送道具")
    public RetResult giveProp(int userid, SkywarGiveBean bean) {
        if (bean.getPropcount() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (bean.getPropid() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (bean.getTouserid() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        UserInfo touser = findUserInfo(bean.getTouserid());
        if (touser == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        final int count = bean.getPropcount();
        SkywarAccount account = loadAccount(userid);
        SkywarAccount toaccount = loadAccount(bean.getTouserid());
        synchronized (account) {
            String goodsname = "道具";
            if (bean.getPropid() == PROPID_NUCLEAR) {
                goodsname = "核弹";
                if (account.getCurrNuclears() < count) return RetCodes.retResult(RetCodes.RET_COUNT_ILLEGAL);
                account.setCurrNuclears(account.getCurrNuclears() - count);
                toaccount.setCurrNuclears(toaccount.getCurrNuclears() + count);
            } else if (bean.getPropid() == PROPID_RADAR) {
                goodsname = "道具巡航";
                if (account.getCurrRadars() < count) return RetCodes.retResult(RetCodes.RET_COUNT_ILLEGAL);
                account.setCurrRadars(account.getCurrRadars() - count);
                toaccount.setCurrRadars(toaccount.getCurrRadars() + count);
            } else if (bean.getPropid() == PROPID_WING) {
                goodsname = "道具僚机体验卡";
                if (account.getCurrWings() < count) return RetCodes.retResult(RetCodes.RET_COUNT_ILLEGAL);
                account.setCurrWings(account.getCurrWings() - count);
                toaccount.setCurrWings(toaccount.getCurrWings() + count);
            } else if (bean.getPropid() == PROPID_FRENZY) {
                goodsname = "道具狂暴";
                if (account.getCurrFrenzys() < count) return RetCodes.retResult(RetCodes.RET_COUNT_ILLEGAL);
                account.setCurrFrenzys(account.getCurrFrenzys() - count);
                toaccount.setCurrFrenzys(toaccount.getCurrFrenzys() + count);
            } else if (bean.getPropid() == PROPID_TRACK) {
                goodsname = "道具追踪";
                if (account.getCurrTracks() < count) return RetCodes.retResult(RetCodes.RET_COUNT_ILLEGAL);
                account.setCurrTracks(account.getCurrTracks() - count);
                toaccount.setCurrTracks(toaccount.getCurrTracks() + count);
            } else if (bean.getPropid() == PROPID_TRANSMIT) {
                goodsname = "道具传送";
                if (account.getCurrTransmits() < count) return RetCodes.retResult(RetCodes.RET_COUNT_ILLEGAL);
                account.setCurrTransmits(account.getCurrTransmits() - count);
                toaccount.setCurrTransmits(toaccount.getCurrTransmits() + count);
            } else {
                return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
            }
            LetterRecord toletter = new LetterRecord();
            toletter.setUserid(userid);
            toletter.setLettertype(LetterRecord.LETTER_TYPE_NOTICE);
            toletter.setTitle(letterService.bundleResourceValue("prop.give.title"));
            toletter.setContent(letterService.bundleResourceValue("prop.give.content", touser.getUsername(), touser.getUserid(), goodsname, count));
            letterService.createLetterRecord(toletter);

            LetterRecord fromletter = new LetterRecord();
            fromletter.setFromuserid(userid);
            fromletter.setUserid(bean.getTouserid());
            fromletter.setLettertype(LetterRecord.LETTER_TYPE_NOTICE);
            fromletter.setTitle(letterService.bundleResourceValue("prop.receive.title"));
            fromletter.setContent(letterService.bundleResourceValue("prop.receive.content", touser.getUsername(), touser.getUserid(), goodsname, count));
            letterService.createLetterRecord(fromletter);
            return RetResult.success();
        }
    }

    @RestMapping(auth = true, comment = "获取个人账号信息")
    public RetResult<SkywarAccount> loadMyAccount(int userid) {
        SkywarAccount account = loadAccount(userid);
        if (account == null) return RetCodes.retResult(RetCodes.RET_USER_STATUS_ILLEGAL);
        if (account.bloodDay != Utility.today()) {
            account.bloodDay = Utility.today();
            account.bloodCount = 0;
            account.bloodTime = System.currentTimeMillis();
        }
        return new RetResult<>(account).attach("givablepropids", Utility.joining(GIVABLE_PROPIDS, ";"));
    }

    @RestMapping(auth = true, comment = "非自动开火模式下点击发射单个子弹")
    public RetResult<String> shotBullet(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        LongRange range = this.confRoomCoinStages[table.getRoomlevel() - 1];
        if (player.getCoins() < player.getShotlevel() || (range.getMax() > 0 && player.getCoins() > range.getMax())) {
            return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);
        }
        sendMap(table.onlinePlayers(userid), "onPlayerShotBulletMessage", new RetResult(ofMap("userid", userid, "shotlevel", player.getShotlevel())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家使用弹头")
    public RetResult<String> useDantou(int userid, @RestParam(name = "#") int propid) {
        if (propid != PROPID_ROCKET && propid != PROPID_MISSILE && propid != PROPID_NUCLEAR) {
            return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        }
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        synchronized (account) {
            if (propid == PROPID_ROCKET) {
                if (account.getCurrRockets() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
                account.decCurrRockets();
            } else if (propid == PROPID_MISSILE) {
                if (account.getCurrMissiles() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
                account.decCurrMissiles();
            } else if (propid == PROPID_NUCLEAR) {
                if (account.getCurrNuclears() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
                account.decCurrNuclears();
            }
        }
        account.useDantouid = Utility.uuid();
        account.useDantouTime = System.currentTimeMillis();
        sendMap(table.onlinePlayers(), "onPlayerUseDantouMessage", new RetResult(ofMap("userid", userid, "propid", propid)));
        SkywarKillBean bean = new SkywarKillBean();
        bean.setDantouid(account.useDantouid);
        bean.innerDantouKillPropid = propid;
        account.useDantouid = "";
        account.useDantouTime = 0;
        bean.setEnemyids(roomBaseCoins);
        killEnemy(userid, bean);
        return new RetResult(ofMap("dantouid", account.useDantouid));
    }

    @RestMapping(auth = true, comment = "玩家传送高倍敌机")
    public RetResult<String> shotTransmit(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        synchronized (account) {
            if (account.getCurrTransmits() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
            account.decCurrTransmits();
        }
        updateGameMissionUseProp(userid, Skywars.PROPID_TRANSMIT, 1, System.currentTimeMillis());
        return table.joinEnemyRecordImmediately(this, 1, (short) 4); //高倍敌机kindtype
    }

    @RestMapping(auth = true, comment = "玩家设置自动开火: 10:自动; 20:取消")
    public RetResult<String> shotAuto(int userid, @RestParam(name = "#") int flag) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        player.setShotauto(flag == 10);
        sendMap(table.onlinePlayers(userid), "onPlayerShotAutoMessage", new RetResult(ofMap("userid", userid, "auto", player.isShotauto())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家切换子弹等级")
    public RetResult<String> shotLevel(int userid, @RestParam(name = "#") int level) {
        if (level < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        player.setShotlevel(level);
        sendMap(table.onlinePlayers(userid), "onPlayerShotLevelMessage", new RetResult(ofMap("userid", userid, "shotlevel", player.getShotlevel())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家开启狂暴状态")
    public RetResult<String> shotFrenzy(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        if (player.getShotfrenzyremains() > 0) return RetCodes.retResult(RetCodes.RET_USER_STATUS_ILLEGAL);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        synchronized (account) {
            if (account.getCurrFrenzys() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
            account.decCurrFrenzys();
            player.startShotfrenzy();
        }
        updateGameMissionUseProp(userid, Skywars.PROPID_FRENZY, 1, System.currentTimeMillis());
        sendMap(table.onlinePlayers(userid), "onPlayerShotFrenzyMessage", new RetResult(ofMap("userid", userid, "shotfrenzyremains", player.getShotfrenzyremains())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家开启巡航")
    public RetResult<String> shotRadar(int userid, @RestParam(name = "#") int enemyid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        if (player.getShotradarendtime() > 0 && player.isRadarusing()) return RetCodes.retResult(RetCodes.RET_USER_STATUS_ILLEGAL);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        if (player.isRadarpause()) {
            player.setRadarstatus(SkywarPlayer.RADAR_STATUS_USING);
        } else {
            synchronized (account) {
                if (account.getCurrRadars() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
                account.decCurrRadars();
                account.setRadaringEndtime(player.startShotradar());
                player.setShottrackenemyid(enemyid);
            }
        }
        updateGameMissionUseProp(userid, Skywars.PROPID_RADAR, 1, System.currentTimeMillis());
        sendMap(table.onlinePlayers(userid), "onPlayerShotRadarMessage", new RetResult(ofMap("userid", userid, "shottrackenemyid", player.getShottrackenemyid(), "shotradarremains", player.getShotadarremains())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家暂停巡航")
    public RetResult<String> pauseRadar(int userid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        player.setRadarstatus(SkywarPlayer.RADAR_STATUS_PAUSE);
        sendMap(table.onlinePlayers(userid), "onPlayerPauseRadarMessage", new RetResult(ofMap("userid", userid, "shotradarremains", player.getShotadarremains())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家开启追踪状态")
    public RetResult<String> shotTrack(int userid, @RestParam(name = "#") int enemyid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        if (player.getShottrackremains() > 0) return RetCodes.retResult(RetCodes.RET_USER_STATUS_ILLEGAL);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        synchronized (account) {
            if (account.getCurrTracks() < 1) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
            account.decCurrTracks();
            player.startShottrack(enemyid);
        }
        updateGameMissionUseProp(userid, Skywars.PROPID_TRACK, 1, System.currentTimeMillis());
        sendMap(table.onlinePlayers(userid), "onPlayerShotTrackMessage", new RetResult(ofMap("userid", userid, "shottrackenemyid", player.getShottrackenemyid(), "shottrackremains", player.getShottrackremains())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家切换追踪敌机")
    public RetResult<String> trackEnemy(int userid, @RestParam(name = "#") int enemyid) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        if (player.getShottrackremains() < 1) return RetResult.success();
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        player.setShottrackenemyid(enemyid);
        sendMap(table.onlinePlayers(userid), "onPlayerTrackEnemyMessage", new RetResult(ofMap("userid", userid, "shottrackenemyid", player.getShottrackenemyid(), "shottrackremains", player.getShottrackremains())));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "购买战机皮肤; bean={buytype,skinid,expires:1/0}")
    public RetResult<Map<String, Object>> paySkin(int userid, Map<String, String> bean) {
        final boolean expires = "1".equals(bean.get("expires"));
        final SkywarAccount account = loadAccount(userid);
        final OrderRecord order = new OrderRecord();
        order.setUserid(userid);
        order.setBuytype(Short.parseShort(bean.get("buytype")));
        order.setGoodstype(GoodsInfo.GOODS_TYPE_PACKETS);
        SkywarSkinInfo skin = findSkinInfo(Integer.parseInt(bean.get("skinid")));
        if (skin == null) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (account.containsUnExpireSkinid(skin.getSkinid())) return RetCodes.retResult(RetCodes.RET_ORDER_SKIN_REPEAT);
        if (order.getBuytype() == GoodsInfo.GOODS_BUY_COIN) {
            order.setOrdermoney(expires ? skin.getCoin7price() : skin.getCoin0price());
        } else if (order.getBuytype() == GoodsInfo.GOODS_BUY_DIAMOND) {
            order.setOrdermoney(expires ? skin.getDiamond7price() : skin.getDiamond0price());
        } else {
            return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        }
        final GoodsItem item = new GoodsItem();
        item.setGameid(gameId());
        item.setGoodscount(1);
        item.setGoodsexpires(expires ? 604800 : 0);
        item.setGoodsobjid(skin.getSkinid());
        item.setGoodstype(GoodsInfo.GOODS_TYPE_ITEM_SKIN);
        order.setGoodsitems(new GoodsItem[]{item});
        RetResult rs = orderService.payVirtualOrder(userid, order, System.currentTimeMillis());
        if (!rs.isSuccess()) return rs;
        UserInfo user = userService.findUserInfo(userid);
        return new RetResult<>(ofMap("userid", userid, "coins", user.getCoins(),
            "diamonds", user.getDiamonds(), "coupons", user.getCoupons(),
            "skinRecords", account.getSkinRecords()));
    }

    @RestMapping(auth = true, comment = "玩家更换战机皮肤")
    public RetResult<String> updateSkin(int userid, @RestParam(name = "#") int skinid) {
        SkywarAccount account = loadAccount(userid);
        if (!account.containsSkinid(skinid)) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        account.setCurrSkinid(skinid);
        SkywarPlayer player = findLivingPlayer(userid);
        if (player != null) {  //==null表示在大厅更换战机
            player.setSkinid(skinid);
            SkywarTable table = player.table();
            if (table != null) {  //在游戏进行中进行切换皮肤
                sendMap(table.onlinePlayers(userid), "onPlayerUpdateSkinMessage", new RetResult(ofMap("userid", userid, "skinid", skinid)));
            }
        }
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家移动位置")
    public RetResult<String> updatePoint(int userid, @RestParam(name = "#x:") float x, @RestParam(name = "#y:") float y) {
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        player.setPoint(x, y);
        sendMap(table.onlinePlayers(userid), "onPlayerUpdatePointMessage", new RetResult(ofMap("userid", userid, "x", x, "y", y)));
        return RetResult.success();
    }

    @RestMapping(auth = true, comment = "玩家击中敌机")
    public RetResult<String> killEnemy(int userid, SkywarKillBean bean) {
        bean.setUserid(userid);
        SkywarPlayer player = findLivingPlayer(userid);
        if (player == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarTable table = player.table();
        if (table == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYER_NOTIN);
        SkywarAccount account = loadAccount(userid);
        account.setCurrRoomlevel(table.getRoomlevel());
        account.setLastgametime(System.currentTimeMillis());
        final boolean jiguang = player.getShotjiguangremains() > 0;
        final long costcoins = bean.innerDantouKillPropid > 0 || jiguang ? 0 : account.getFactor(player) * bean.getEnemyCount();
        LongRange range = this.confRoomCoinStages[table.getRoomlevel() - 1];
        if (player.getCoins() < costcoins || (range.getMax() > 0 && player.getCoins() > range.getMax())) {
            return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);
        }
        bean.setUsercoins(player.getCoins());
        RetResult<SkywarKillBean> rs;
        synchronized (lockDataCoinPool(table.getRoomlevel())) {
            if (player.getCoins() < costcoins) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);
            final long now = System.currentTimeMillis();
            player.increCoin(-costcoins);
            //扣掉成本
            player.costQueue.add(costcoins);
            if (player.data1Record == null) player.data1Record = new PoolDataRecord(gameId(), userid, table.getRoomlevel(), 1, table.getTableid(), 0, 0, 0, 0, now, "cost", "子弹成本");
            if (openDataCoinPool2() && player.data2Record == null) player.data2Record = new PoolDataRecord(gameId(), userid, table.getRoomlevel(), 2, table.getTableid(), 0, 0, 0, 0, now, "cost", "子弹成本");
            if (openDataCoinPool3() && player.data3Record == null) player.data3Record = new PoolDataRecord(gameId(), userid, table.getRoomlevel(), 3, table.getTableid(), 0, 0, 0, 0, now, "cost", "子弹成本");
            long taxcoin = updatePoolCoin(table.getRoomlevel(), userid, costcoins, table.getTableid(), now, "cost", "子弹成本", player.data1Record, player.data2Record, player.data3Record);
            table.increTaxcoin(taxcoin);
            table.increOswincoins(costcoins);
            rs = table.killEnemy(this, account, player, bean);
            if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "击杀敌机; bean=" + bean + ", rs=" + rs);

            if (rs.isSuccess()) {
                if (rs.getResult().getWincoin() > 0) {
                    long allwincoins = rs.getResult().getWincoin();
                    cleanCostCoins(table, account, player, now);
                    updateGameUserCoins(account, userid, table.getRoomlevel(), rs.getResult().getWincoin(), 0L, now, "", "普通击杀结算;(killedcount=" + rs.getResult().getKilledEnemyCount() + ")");
                    updatePoolCoin(table.getRoomlevel(), userid, -rs.getResult().getWincoin(), table.getTableid(), now, "settle", "击杀结算");//从奖池中扣除金币数
                    table.increOswincoins(-allwincoins);
                    player.increCoin(allwincoins);
                    rs.getResult().setUsercoins(player.getCoins());
                }
                if (rs.getResult().getWindiamond() > 0 || rs.getResult().getWincoupons() > 0) {
                    long allwindiamonds = rs.getResult().getWindiamond();
                    long allwincoupons = rs.getResult().getWincoupons();
                    userService.updatePlatfUserCoinDiamondCoupons(userid, 0L, allwindiamonds, allwincoupons, now, "", "普通击杀结算;(killedcount=" + rs.getResult().getKilledEnemyCount() + ")");
                    player.setDiamonds(player.getDiamonds() + allwindiamonds);
                    player.setCoupons(player.getCoupons() + allwincoupons);
                    rs.getResult().setUserdiamonds(player.getDiamonds());
                    rs.getResult().setUsercoupons(player.getCoupons());
                }
                if (rs.getResult().innerSpecialWinCoins > 0) { //特殊敌机的金币不能立即算到返回的usercoins中
                    long specialwincoins = rs.getResult().innerSpecialWinCoins;
                    cleanCostCoins(table, account, player, now);
                    updateGameUserCoins(account, userid, table.getRoomlevel(), specialwincoins, 0L, now, "specialenemy", "特殊敌机被击落:" + bean.specialmap.keySet());
                    updatePoolCoin(table.getRoomlevel(), userid, -specialwincoins, table.getTableid(), now, "specialenemy", "特殊敌机被击落:" + bean.specialmap.keySet());//从奖池中扣除金币数
                    table.increOswincoins(-specialwincoins);
                    player.increCoin(specialwincoins);
                }
                if (rs.getResult().innerSpecialWinDiamonds > 0) { //特殊敌机的奖券不能立即算到返回的userdiamonds中
                    long specialwindiamonds = rs.getResult().innerSpecialWinDiamonds;
                    cleanCostCoins(table, account, player, now);
                    updateGameUserDiamonds(account, userid, table.getRoomlevel(), specialwindiamonds, 0L, now, "specialenemy", "特殊敌机被击落:" + bean.specialmap.keySet());
                    player.increDiamond(specialwindiamonds);
                }
                if (rs.getResult().innerSpecialWinCoupons > 0) { //特殊敌机的奖券不能立即算到返回的usercoupons中
                    long specialwincoupons = rs.getResult().innerSpecialWinCoupons;
                    cleanCostCoins(table, account, player, now);
                    updateGameUserCoupons(account, userid, table.getRoomlevel(), specialwincoupons, 0L, now, "specialenemy", "特殊敌机被击落:" + bean.specialmap.keySet());
                    player.increCoupon(specialwincoupons);
                }
                if (bean.innerJiguangable) { //激光炮                    
                    schedule(() -> {
                        long time = System.currentTimeMillis();
                        long seconds = 10; //激光多少秒
                        player.setShotjiguangtime(time + seconds * 1000L); //激光10秒钟      
                        sendMap(table.onlinePlayers(), "onPlayerJiguangMessage", new RetResult(ofMap("userid", userid, "shotjiguangremains", player.getShotjiguangremains())));
                        schedule(() -> {
                            player.setShotjiguangtime(0);
                        }, seconds, TimeUnit.SECONDS);
                    }, 3, TimeUnit.SECONDS);
                }
            }
        }
        sendMap(table.onlinePlayers(), "onEnemyKilledMessage", new RetResult(bean));
        return (RetResult) rs.result(null);
    }

    @Override
    @RestMapping(auth = false, comment = "用户更新签到")
    public RetResult<String> notifyDutyRecord(final int userid, final DutyRecord duty) {
        if (duty == null) return RetResult.success();
        SkywarAccount account = loadAccount(userid);
        account.setLastDutyDay(duty.getIntday());
        account.setLastDutyIndex(duty.getDutyindex());
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = false, comment = "用户更新设备商品")
    public RetResult<String> notifyGoodsInfo(final int userid, final short goodstype, final int goodscount, final List<GoodsItem> items) {
        if (items == null) return RetResult.success();
        SkywarAccount account = loadAccount(userid);
        final int buycount = Math.max(1, goodscount);
        synchronized (account) {
            for (GoodsItem item : items) {
                if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_ITEM_SKIN) {
                    SkywarSkinInfo skin = findSkinInfo(item.getGoodsobjid());
                    if (skin != null) {
                        SkywarSkinRecord record = new SkywarSkinRecord();
                        record.setSkinid(skin.getSkinid());
                        record.setStarttime(System.currentTimeMillis());
                        if (item.getGoodsexpires() > 0) {
                            if (record.getEndtime() > 0) {
                                record.setEndtime(record.getEndtime() + item.getGoodsexpires() * buycount * 1000L);
                            } else {
                                record.setEndtime(Utility.midnight() + item.getGoodsexpires() * buycount * 1000L);
                            }
                        }
                        account.addSkinRecord(record);
                        if (account.getCurrSkinid() == SkywarSkinInfo.DEFAULT_SKINID) {
                            account.setCurrSkinid(record.getSkinid());
                        }
                    }
                } else if (item.getGoodstype() == GoodsInfo.GOODS_TYPE_ITEM_PROP) {
                    if (item.getGoodscount() > 0) {
                        if (item.getGoodsobjid() == Skywars.PROPID_FRENZY) {
                            account.setCurrFrenzys(account.getCurrFrenzys() + buycount * item.getGoodscount());
                        } else if (item.getGoodsobjid() == Skywars.PROPID_MISSILE) {
                            account.setCurrMissiles(account.getCurrMissiles() + buycount * item.getGoodscount());
                        } else if (item.getGoodsobjid() == Skywars.PROPID_NUCLEAR) {
                            account.setCurrNuclears(account.getCurrNuclears() + buycount * item.getGoodscount());
                        } else if (item.getGoodsobjid() == Skywars.PROPID_RADAR) {
                            account.setCurrRadars(account.getCurrRadars() + buycount * item.getGoodscount());
                        } else if (item.getGoodsobjid() == Skywars.PROPID_ROCKET) {
                            account.setCurrRockets(account.getCurrRockets() + buycount * item.getGoodscount());
                        } else if (item.getGoodsobjid() == Skywars.PROPID_TRACK) {
                            account.setCurrTracks(account.getCurrTracks() + buycount * item.getGoodscount());
                        } else if (item.getGoodsobjid() == Skywars.PROPID_TRANSMIT) {
                            account.setCurrTransmits(account.getCurrTransmits() + buycount * item.getGoodscount());
                        } else if (item.getGoodsobjid() == Skywars.PROPID_WING) {
                            account.setCurrWings(account.getCurrWings() + buycount * item.getGoodscount());
                        }
                    }
                }
            }
            if (goodstype == GoodsInfo.GOODS_TYPE_ONCEPACKET) { //首充
                account.setOnceGoodsDay(Utility.today());
            }
        }
        return RetResult.success();
    }

    public SkywarSkinInfo findSkinInfo(int skinid) {
        for (SkywarSkinInfo skin : skinInfos) {
            if (skin.getSkinid() == skinid) return skin;
        }
        return null;
    }

    private void loadSkinInfo() {
        skinInfos = source.queryList(SkywarSkinInfo.class
        );
    }

    @Override
    protected SkywarPlayer createGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean) {
        SkywarPlayer player = new SkywarPlayer(user, clientAddr, sncpAddress, roomlevel);
        SkywarAccount account = loadAccount(user.getUserid());
        player.setSkinid(account.getCurrSkinid());
        player.skin = findSkinInfo(player.getSkinid());
        if (roomlevel > 100) {
            player.setShotlevel(10);  //固定10倍
        } else {
            int[] shotlevels = SHOT_LEVELS[roomlevel - 1];
            if (player.getShotlevel() < shotlevels[0]) {
                player.setShotlevel(shotlevels[0]);
            } else if (player.getShotlevel() > shotlevels[shotlevels.length - 1]) {
                player.setShotlevel(shotlevels[shotlevels.length - 1]);
            }
        }
        return player;
    }

    @Override
    protected SkywarTable createGameTable(SkywarPlayer player, GameTableBean bean) {
        final SkywarTable table = new SkywarTable();
        table.setGameid(gameId());
        if (bean != null) table.setExtmap(bean.getExtmap());
        table.setCreatetime(System.currentTimeMillis());
        table.setRoomlevel(player.getRoomlevel());
        table.setBaseBetCoin(player.getRoomlevel() > 100 ? 10 : BASECOIN_ITEMS[player.getRoomlevel() - 1]);
        return table;
    }

    @Override
    @RestMapping(auth = false, comment = "平台用户信息更新通知")
    public RetResult<String> notifyPlatfPlayer(final int userid, final Map<String, String> bean) {
        UserInfo user = findUserInfo(userid);
        SkywarPlayer player = findLivingPlayer(userid);
        if (player != null) player.copyFromUser(user);
        if (logger.isLoggable(Level.FINER)) logger.log(Level.FINER, "平台用户信息更新通知 notifyPlatfPlayer: " + user);
        if (user.getViplevel() > 0) {
            SkywarAccount account = loadAccount(userid);
            if (user.getViplevel() >= 9) {
                account.setWingUnlocks(4);
            } else if (user.getViplevel() >= 5) {
                account.setWingUnlocks(3);
            } else if (user.getViplevel() >= 2) {
                account.setWingUnlocks(2);
            } else {
                account.setWingUnlocks(1);
            }
            if (account.getWingRunnings() > account.getWingUnlocks()) {
                account.setWingRunnings(account.getWingUnlocks());
            }
            account.setWingingEndTime(0);
        }
        return RetResult.success();
    }

    @Override
    protected int roomLevelSize() {
        return 5;
    }

    @Override
    protected DataSource dataSource() {
        return source;
    }

    @Override
    protected Class<? extends GameAccount> accountClass() {
        return SkywarAccount.class;
    }

    @Override
    protected Class<SkywarEnemyKind> enemyKindClass() {
        return SkywarEnemyKind.class;
    }

    @Override
    public String gameId() {
        return "skywar";
    }

    @Override
    public String gameName() {
        return gameName;
    }

}
