/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.line;

import com.cratos.platf.base.UserInfo;
import com.cratos.platf.game.*;
import static com.cratos.platf.util.Utils.HEADNAME_WS_SNCP_ADDRESS;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 * 连线游戏
 *
 * @author zhangjx
 */
@AutoLoad(false)
public abstract class LineCoinGameService extends CoinGameService<GameTable, LineGamePlayer, GameTableBean> {

    @Override
    protected LineGamePlayer loadGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean, LineGamePlayer oldPlayer) {
        if (oldPlayer != null) return oldPlayer.copyFromUserAndOnline(user, clientAddr, sncpAddress);
        return new LineGamePlayer(user, clientAddr, sncpAddress, roomlevel, 0);
    }

    @Override
    @RestMapping(auth = true, comment = "进入游戏")
    public <T> RetResult<T> enterGame(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, Map<String, String> bean) {
        if (this.serviceClosed) return GameRetCodes.retResult(GameRetCodes.RET_GAME_SHUTDOWN);
        LineGameAccount account = loadAccount(userid);
        int roomlevel;
        long linecoin;
        int linenum = 1;
        long pool2coins = 0;
        if (account.isGameing()) {
            if (account.getCurrRoomlevel() > 0) {
                roomlevel = account.getCurrRoomlevel();
                linenum = account.getCurrLinenum();
                linecoin = account.getCurrLinecoin();
                pool2coins = openDataCoinPool2() ? getDataCoinPool2Value(account.getCurrRoomlevel()) : 0;
            } else {
                roomlevel = Integer.parseInt(bean.get("roomlevel"));
                linecoin = roomBaseCoins[roomlevel - 1];
            }
            userEnterGame(userid);
            putLivingPlayer(userid, loadGamePlayer(findUserInfo(userid), clientAddr, sncpAddress, roomlevel, bean, findLivingPlayer(userid)));
        } else {
            roomlevel = Integer.parseInt(bean.get("roomlevel"));
            if (roomlevel == 0) {
                roomlevel = account.getCurrRoomlevel();
                bean.put("roomlevel", "" + roomlevel);
            }
            linecoin = roomBaseCoins[roomlevel - 1];
            RetResult rs = super.enterGame(userid, clientAddr, sncpAddress, bean);
            if (!rs.isSuccess()) return rs;
        }
        Map map = Utility.ofMap("roomlevel", roomlevel, "linenum", linenum, "linecoin", linecoin);
        if (openDataCoinPool2()) map.put("pool2coins", Math.max(0, pool2coins));
        return new RetResult(map);
    }

    @RestMapping(auth = true, comment = "加载Table信息")
    public <T> RetResult<T> loadTable(int userid, Map<String, String> bean) {
        LineGameAccount account = loadAccount(userid);
        if (!account.isGameing()) return RetResult.success();
        Map map = Utility.ofMap(
            "roomlevel", account.getCurrRoomlevel(),
            "currlinenum", account.getCurrLinenum(),
            "currlinecoin", account.getCurrLinecoin()
        );
        Map entrymap = account.newLoadTableEntryMap();
        if (entrymap != null) map.putAll(entrymap);

        if (bean != null && bean.containsKey("byload")) return new RetResult(map);
        sendMap(findLivingPlayer(userid), "onPlayerDoChouJiangMessage", new RetResult(map));
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = true, comment = "玩家离线")
    public <T> RetResult<T> offlineGame(int userid, Map<String, String> bean) {
        if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "offlineGame " + gameId() + ": userid=" + userid);
        LineGameAccount account = loadAccount(userid);
        leaveGame(userid, account.isGameing());
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = true, comment = "离开游戏")
    public <T> RetResult<T> leaveGame(int userid, Map<String, String> bean) {
        LineGameAccount account = loadAccount(userid);
        if (account.isGameing()) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_CANNOTLEAVE_GAMING);
        return super.leaveGame(userid, bean);
    }

    @Override
    @RestMapping(auth = true, comment = "获取玩家正在玩的场次，为-1表示当前没有在玩, 为0表示玩家在此游戏内，但不分场次")
    public int getPlayingRoomLevel(int userid) {
        GamePlayer player = findLivingPlayer(userid);
        if (player != null) return player.getRoomlevel();
        LineGameAccount account = loadAccount(userid);
        if (account == null) return -1;
        return account.isGameing() ? account.getCurrRoomlevel() : -1;
    }

    @RestMapping(auth = true, comment = "请求游戏")
    public abstract RetResult<LineGameRound> runRound(final int userid, LineGameRoundBean bean);

    @Override
    protected final int roomLevelSize() {
        return 4;
    }

    public static void runTest(LineCoinGameService service) throws Throwable {
        final String clientAddr = "127.0.0.1";
        final InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8800);
        service.testrun = true;
        System.setProperty(DataSources.DATASOURCE_CONFPATH, new File("conf/persistence.xml").getCanonicalPath());
        DataSource source = DataSources.createDataSource(service.gameId());
        List<GameConfig> configs = source.queryList(GameConfig.class);
        Map<String, Number> map = new HashMap<>();
        for (GameConfig conf : configs) {
            map.put(conf.getKeyname(), conf.getNumvalue());
            System.out.println("加载配置: " + conf.getKeyname() + " = " + conf.getNumvalue());
        }
        service.reloadConfig(GameConfigFunc.createFromMap(map));
        service.confCoinPoolTaxPermillage = 100;
        service.confCoinPool2Permillage = service.openDataCoinPool2() ? 100 : 0;
        service.confCoinPool3Permillage = 0;
        service.confLimitLosCoinStages = new long[]{0L};
        service.confLimitWinCoinStages = new long[]{5000_00L};
        service.confRoomCoinStages = new Range.LongRange[]{new Range.LongRange(1000L, -1L)};

        UserInfo user = new UserInfo();
        user.setUserid(UserInfo.USERID_SYSTEM);
        user.setCoins(1000_0000_0000L);
        LineGamePlayer player = service.loadGamePlayer(user, clientAddr, addr, 1, null, null);
        service.putLivingPlayer(player.getUserid(), player);
        LineGameRoundBean bean = new LineGameRoundBean();
        bean.setLinenum(1);
        int count = 10_0000; //总运行次数
        AtomicLong minPool = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxPool = new AtomicLong(Long.MIN_VALUE);
        AtomicLong minPool2 = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxPool2 = new AtomicLong(Long.MIN_VALUE);
        AtomicLong winCoins = new AtomicLong(0);
        AtomicLong freeCount = new AtomicLong(0);
        LineGameAccount account = service.loadAccount(user.getUserid());
        for (int i = 0; i < count; i++) {
            RetResult<LineGameRound> rs = service.runRound(user.getUserid(), bean);
            if (!rs.isSuccess()) {
                System.err.println("异常了: " + rs);
                break;
            }
            winCoins.addAndGet(rs.getResult().getWincoin());
            int free = rs.getResult().getFreecount();
            if (free >= 0) {
                LineGameAccount.LineEntry entry = account.getCurrLineEntry();
                if (entry != null) entry.currFreecount += free;
            }
            freeCount.addAndGet(free);
            long pool = service.getDataCoinPoolValue(1);
            if (count <= 100) System.out.println("奖池当前值: " + pool);
            if (pool > maxPool.get()) maxPool.set(pool);
            if (pool < minPool.get()) minPool.set(pool);
            if (service.openDataCoinPool2()) {
                long pool2 = service.getDataCoinPool2Value(1);
                if (count <= 100) System.out.println("奖池2当前值: " + pool2);
                if (pool2 > maxPool2.get()) maxPool2.set(pool2);
                if (pool2 < minPool2.get()) minPool2.set(pool2);
            }
            if (pool < service.confLimitLosCoinStages[0]) throw new RuntimeException((i + 1) + "--奖池击穿底线: " + pool);
        }
        System.out.println("");
        System.out.println("----------------------");
        System.out.println("奖池下限值: " + service.confLimitLosCoinStages[0]);
        System.out.println("奖池最低值: " + minPool);
        System.out.println("奖池上限值: " + service.confLimitWinCoinStages[0]);
        System.out.println("奖池最大值: " + maxPool);
        System.out.println("奖池当前值: " + service.getDataCoinPoolValue(1));
        if (service.openDataCoinPool2()) {
            System.out.println("奖池2最低值: " + minPool2);
            System.out.println("奖池2最大值: " + maxPool2);
            System.out.println("奖池2当前值: " + service.getDataCoinPool2Value(1));
        }
        System.out.println("玩家总输赢: " + winCoins);
        System.out.println("总免费次数: " + freeCount);
    }
}
