/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.duty.DutyRecord;
import com.cratos.platf.base.*;
import static com.cratos.platf.game.GameRetCodes.RET_GAME_PLAYER_STATUS_ILLEGAL;
import com.cratos.platf.info.DictInfo;
import com.cratos.platf.notice.Announcement;
import com.cratos.platf.order.GoodsItem;
import com.cratos.platf.util.*;
import static com.cratos.platf.util.Utils.HEADNAME_WS_SNCP_ADDRESS;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.logging.Level;
import java.util.stream.*;
import javax.persistence.Transient;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.source.Range;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 * @param <Table>  GameTable
 * @param <P>      GamePlayer
 * @param <GTBean> GameTableBean
 */
@AutoLoad(false)
public abstract class CoinGameService<Table extends GameTable, P extends GamePlayer, GTBean extends GameTableBean> extends GameService<Table, P, GTBean> {

    @Transient //此游戏服务是否关闭
    protected boolean serviceClosed = false;

    @Transient //房间场次的底注，没有就是null，有则必须与confRoomCoinStages个数一样;
    protected int[] roomBaseCoins;

    @Transient //房间场次金币要求信息
    protected Range.LongRange[] confRoomCoinStages;

    @Transient //房间场次系统的下限额度
    protected long[] confLimitLosCoinStages;

    @Transient //房间场次系统的上限额度
    protected long[] confLimitWinCoinStages;

    @Transient //游戏的主奖池税收千分比
    protected int confCoinPoolTaxPermillage;

    @Transient //游戏的奖池2占比千分比
    protected int confCoinPool2Permillage;

    @Transient //游戏的奖池3占比千分比
    protected int confCoinPool3Permillage;

    @Transient //需要发公告的最低金币数， 为0表示无需对金币进行发公告
    protected long confAnnounceCoin;

    @Transient //需要发公告的最低倍数， 为0表示无需对倍数进行发公告
    protected int confAnnounceFactor;

    //四五个场次的主奖金池
    protected final AtomicLong[] dataCoinPoolArray = openDataCoinPool() ? createAtomicLongs(roomLevelSize()) : null;

    //四五个场次的临时主奖金池
    protected final AtomicLong[] tmpCoinPoolArray = openDataCoinPool() ? createAtomicLongs(roomLevelSize()) : null;

    //四五个场次的DB操作池
    protected final QueueTask<GameData>[] dataCoinPoolQueues = openDataCoinPool() ? createQueueTasks(roomLevelSize()) : null;

    //四五个场次的奖金池2
    protected final AtomicLong[] dataCoinPool2Array = openDataCoinPool2() ? createAtomicLongs(roomLevelSize()) : null;

    //四五个场次的临时奖金池2
    protected final AtomicLong[] tmpCoinPool2Array = openDataCoinPool2() ? createAtomicLongs(roomLevelSize()) : null;

    //四五个场次的DB操作池2
    protected final QueueTask<GameData>[] dataCoinPool2Queues = openDataCoinPool2() ? createQueueTasks(roomLevelSize()) : null;

    //四五个场次的奖金池3
    protected final AtomicLong[] dataCoinPool3Array = openDataCoinPool3() ? createAtomicLongs(roomLevelSize()) : null;

    //四五个场次的临时奖金池3
    protected final AtomicLong[] tmpCoinPool3Array = openDataCoinPool3() ? createAtomicLongs(roomLevelSize()) : null;

    //四五个场次的DB操作池3
    protected final QueueTask<GameData>[] dataCoinPool3Queues = openDataCoinPool3() ? createQueueTasks(roomLevelSize()) : null;

    //奖池变化
    protected final QueueTask<PoolDataRecord> coinPoolDataQueue = new QueueTask<>(1);

    @Override
    protected void initGame(int schedulePoolSize) {
        super.initGame(schedulePoolSize + 1);
        long deplay = 60 * 1000 - System.currentTimeMillis() % (60 * 1000);
        final int poolCount = roomLevelSize();
        if (poolCount > 0 && !winos) {
            this.scheduler.scheduleAtFixedRate(() -> {
                try {
                    final java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    long timekey = now.getYear() * 100_00_00_00L + now.getMonthValue() * 100_00_00
                        + now.getDayOfMonth() * 100_00 + now.getHour() * 100 + now.getMinute();
                    CoinPoolRecord record = new CoinPoolRecord();
                    record.setTimekey(timekey);
                    if (openDataCoinPool()) {
                        if (poolCount >= 1) record.setCoin1val1(CoinPoolRecord.checkCoinVal(dataCoinPoolArray[0].get()));
                        if (poolCount >= 2) record.setCoin1val2(CoinPoolRecord.checkCoinVal(dataCoinPoolArray[1].get()));
                        if (poolCount >= 3) record.setCoin1val3(CoinPoolRecord.checkCoinVal(dataCoinPoolArray[2].get()));
                        if (poolCount >= 4) record.setCoin1val4(CoinPoolRecord.checkCoinVal(dataCoinPoolArray[3].get()));
                        if (poolCount >= 5) record.setCoin1val5(CoinPoolRecord.checkCoinVal(dataCoinPoolArray[4].get()));
                    }
                    if (openDataCoinPool2()) {
                        if (poolCount >= 1) record.setCoin2val1(CoinPoolRecord.checkCoinVal(dataCoinPool2Array[0].get()));
                        if (poolCount >= 2) record.setCoin2val2(CoinPoolRecord.checkCoinVal(dataCoinPool2Array[1].get()));
                        if (poolCount >= 3) record.setCoin2val3(CoinPoolRecord.checkCoinVal(dataCoinPool2Array[2].get()));
                        if (poolCount >= 4) record.setCoin2val4(CoinPoolRecord.checkCoinVal(dataCoinPool2Array[3].get()));
                        if (poolCount >= 5) record.setCoin2val5(CoinPoolRecord.checkCoinVal(dataCoinPool2Array[4].get()));
                    }
                    if (openDataCoinPool3()) {
                        if (poolCount >= 1) record.setCoin3val1(CoinPoolRecord.checkCoinVal(dataCoinPool3Array[0].get()));
                        if (poolCount >= 2) record.setCoin3val2(CoinPoolRecord.checkCoinVal(dataCoinPool3Array[1].get()));
                        if (poolCount >= 3) record.setCoin3val3(CoinPoolRecord.checkCoinVal(dataCoinPool3Array[2].get()));
                        if (poolCount >= 4) record.setCoin3val4(CoinPoolRecord.checkCoinVal(dataCoinPool3Array[3].get()));
                        if (poolCount >= 5) record.setCoin3val5(CoinPoolRecord.checkCoinVal(dataCoinPool3Array[4].get()));
                    }
                    record.setCreatetime(System.currentTimeMillis() / 1000 * 1000);
                    dataSource().insert(record);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "scheduleAtFixedRate CoinPoolRecord error", e);
                }
            }, deplay, 60 * 1000, TimeUnit.MILLISECONDS);
        }
    }

    //多少个奖池
    protected abstract int roomLevelSize();

    @Local
    public boolean openDataCoinPool() {
        return true;
    }

    protected boolean openDataCoinPool2() {
        return false;
    }

    protected boolean openDataCoinPool3() {
        return false;
    }

    protected static AtomicLong[] createAtomicLongs(int size) {
        AtomicLong[] rs = new AtomicLong[size];
        for (int i = 0; i < rs.length; i++) {
            rs[i] = new AtomicLong();
        }
        return rs;
    }

    protected static QueueTask[] createQueueTasks(int size) {
        QueueTask[] rs = new QueueTask[size];
        for (int i = 0; i < rs.length; i++) {
            rs[i] = new QueueTask(1);
        }
        return rs;
    }

    @Override
    public void init(AnyValue config) {
        super.init(config);

        BiConsumer<BlockingQueue<GameData>, GameData> consumer = (queue, data) -> {
            int c = updateDataValue(data.getKeyname(), data.getNumvalue());
            if (c < 1 && !existsDataValue(data.getKeyname())) {
                GameData rdata = null;
                try {
                    rdata = (GameData) createGameData(data.getKeyname(), data.getNumvalue(), "", "");
                    rdata.setUpdatetime(System.currentTimeMillis());
                    insertGameData(rdata);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "insert GameData(" + rdata + ") error", e);
                }
            }
        };
        if (dataCoinPoolQueues != null) {
            for (QueueTask<GameData> queue : dataCoinPoolQueues) {
                queue.init(logger, consumer);
            }
        }
        if (dataCoinPool2Queues != null) {
            for (QueueTask<GameData> queue : dataCoinPool2Queues) {
                queue.init(logger, consumer);
            }
        }
        if (dataCoinPool3Queues != null) {
            for (QueueTask<GameData> queue : dataCoinPool3Queues) {
                queue.init(logger, consumer);
            }
        }
        coinPoolDataQueue.init(logger, new QueueTask.InsertBiConsumer(dataSource()));
    }

    @Override
    public void destroy(AnyValue conf) {
        final RetResult rs = RetResult.success();
        livingPlayers.values().stream().forEach(player -> {
            try {
                userLeaveGame(player.getUserid());
                leaveAccount(player.getUserid());
                afterLeaveGame(player);
                if (player.isRobot()) return;
                sendMap(player, "onGameShutdownMessage", rs).join();
            } catch (Exception ex) {
                logger.log(Level.INFO, this.getClass().getSimpleName() + " destroy error, player = " + player, ex);
            }
        });
        livingPlayers.clear();
        if (dataCoinPoolQueues != null) {
            for (QueueTask<GameData> queue : dataCoinPoolQueues) {
                queue.destroy();
            }
        }
        if (dataCoinPool2Queues != null) {
            for (QueueTask<GameData> queue : dataCoinPool2Queues) {
                queue.destroy();
            }
        }
        if (dataCoinPool3Queues != null) {
            for (QueueTask<GameData> queue : dataCoinPool3Queues) {
                queue.destroy();
            }
        }
        coinPoolDataQueue.destroy();
        super.destroy(conf);
    }

    protected void checkPoolCoin(int roomlevel, int poolindex) {
        long poolcoin = this.dataCoinPoolArray[roomlevel - 1].get();
        if (poolcoin < this.getConfLimitLosCoinValue(roomlevel)) {
            this.serviceClosed = true;
            logger.log(Level.SEVERE, "Game " + gameId() + " los coin limit " + poolcoin);
            new Thread() {
                @Override
                public void run() {
                    CoinGameService.this.destroy(null);
                }
            }.start();
        }
    }

    protected void updatePoolDataRecord(PoolDataRecord data1Record, PoolDataRecord data2Record, PoolDataRecord data3Record) {
        if (data1Record != null) {
            int roomlevel = data1Record.getRoomlevel();
            String gamekey = gameId().toUpperCase();
            if (gamekey.endsWith("MJ")) gamekey = "MJ";
            final String key1 = "GAMEDATA_" + gamekey + "_COINPOOL_" + roomlevel;
            final String key2 = "GAMEDATA_" + gamekey + "_COINPOOL2_" + roomlevel;
            final String key3 = "GAMEDATA_" + gamekey + "_COINPOOL3_" + roomlevel;
            if (this.dataCoinPoolArray.length == 1) roomlevel = 1;
            final AtomicLong storage = this.dataCoinPoolArray[roomlevel - 1];
            final AtomicLong storage2 = this.dataCoinPool2Array == null ? null : this.dataCoinPool2Array[roomlevel - 1];
            final AtomicLong storage3 = this.dataCoinPool3Array == null ? null : this.dataCoinPool3Array[roomlevel - 1];
            final QueueTask<GameData> dataCoinPool1Queue = this.dataCoinPoolQueues[roomlevel - 1];
            final QueueTask<GameData> dataCoinPool2Queue = this.dataCoinPool2Queues == null ? null : this.dataCoinPool2Queues[roomlevel - 1];
            final QueueTask<GameData> dataCoinPool3Queue = this.dataCoinPool3Queues == null ? null : this.dataCoinPool3Queues[roomlevel - 1];
            synchronized (storage) {
                if (dataCoinPool1Queue.size() > 1) dataCoinPool1Queue.poll();
                dataCoinPool1Queue.add(createGameData(key1, storage.get()));
                coinPoolDataQueue.add(data1Record);
                if (data2Record != null) {
                    if (dataCoinPool2Queue.size() > 1) dataCoinPool2Queue.poll();
                    dataCoinPool2Queue.add(createGameData(key2, storage2.get()));
                    coinPoolDataQueue.add(data2Record);
                }
                if (data3Record != null) {
                    if (dataCoinPool3Queue.size() > 1) dataCoinPool3Queue.poll();
                    dataCoinPool3Queue.add(createGameData(key3, storage3.get()));
                    coinPoolDataQueue.add(data3Record);
                }
            }
        }
    }

    protected long updatePoolCoin(int roomlevel, int userid, long coins, String roundid, long now, String module, String remark) { //负数为玩家赢钱
        return updatePoolCoin(roomlevel, userid, coins, roundid, now, module, remark, null, null, null);
    }

    protected long updatePoolCoin(int roomlevel, int userid, long coins, String roundid, long now, String module, String remark, PoolDataRecord data1Record, PoolDataRecord data2Record, PoolDataRecord data3Record) { //负数为玩家赢钱
        if (coins == 0) return 0;
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        if (this.dataCoinPoolArray.length == 1) roomlevel = 1;
        final String key1 = "GAMEDATA_" + gamekey + "_COINPOOL_" + roomlevel;
        final String key2 = "GAMEDATA_" + gamekey + "_COINPOOL2_" + roomlevel;
        final String key3 = "GAMEDATA_" + gamekey + "_COINPOOL3_" + roomlevel;
        final AtomicLong storage = this.dataCoinPoolArray[roomlevel - 1];
        final AtomicLong storage2 = this.dataCoinPool2Array == null ? null : this.dataCoinPool2Array[roomlevel - 1];
        final AtomicLong storage3 = this.dataCoinPool3Array == null ? null : this.dataCoinPool3Array[roomlevel - 1];
        final QueueTask<GameData> dataCoinPool1Queue = this.dataCoinPoolQueues[roomlevel - 1];
        final QueueTask<GameData> dataCoinPool2Queue = this.dataCoinPool2Queues == null ? null : this.dataCoinPool2Queues[roomlevel - 1];
        final QueueTask<GameData> dataCoinPool3Queue = this.dataCoinPool3Queues == null ? null : this.dataCoinPool3Queues[roomlevel - 1];
        synchronized (storage) {
            if (coins < 0) {  //玩家赢钱， 从池子里扣钱
                long val1 = storage.addAndGet(coins);
                if (data1Record == null) {
                    if (dataCoinPool1Queue.size() > 1) dataCoinPool1Queue.poll();
                    dataCoinPool1Queue.add(createGameData(key1, val1));
                    coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 1, roundid, coins, 0, coins, val1, now, module, remark));
                } else {
                    data1Record.setCoins(data1Record.getCoins() + coins);
                    data1Record.setPoolcoin(data1Record.getPoolcoin() + coins);
                    data1Record.setNewpoolcoins(val1);
                }
                checkPoolCoin(roomlevel, 1);
                return 0;
            }
            final AtomicLong tmpArray = this.tmpCoinPoolArray[roomlevel - 1];
            final long tmpCoins = tmpArray.addAndGet(coins);
            int tax1 = this.confCoinPoolTaxPermillage;
            int poolper2 = (storage2 != null ? this.confCoinPool2Permillage : 0);
            int poolper3 = (storage3 != null ? this.confCoinPool3Permillage : 0);
            int fenmu = 1000;
            if (tax1 % 100 == 0 && poolper2 % 100 == 0 && poolper3 % 100 == 0) {
                fenmu = 10;
                tax1 = tax1 / 100;
                poolper2 = poolper2 / 100;
                poolper3 = poolper3 / 100;
            } else if (tax1 % 10 == 0 && poolper2 % 10 == 0 && poolper3 % 10 == 0) {
                fenmu = 100;
                tax1 = tax1 / 10;
                poolper2 = poolper2 / 10;
                poolper3 = poolper3 / 10;
            }
            if (tmpCoins >= fenmu) {
                final long inc = tmpCoins / fenmu;
                if (inc > 0) {
                    long coinIncre = inc * (fenmu - tax1 - poolper2 - poolper3);
                    tmpArray.addAndGet(-inc * fenmu);
                    long val1 = storage.addAndGet(coinIncre);
                    if (coinIncre != 0) {
                        if (data1Record == null) {
                            if (dataCoinPool1Queue.size() > 1) dataCoinPool1Queue.poll();
                            dataCoinPool1Queue.add(createGameData(key1, val1));
                            coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 1, roundid, coins, inc * tax1, coinIncre, val1, now, module, remark));
                        } else {
                            data1Record.setCoins(data1Record.getCoins() + coins);
                            data1Record.setTaxcoin(data1Record.getTaxcoin() + inc * tax1);
                            data1Record.setPoolcoin(data1Record.getPoolcoin() + coinIncre);
                            data1Record.setNewpoolcoins(val1);
                        }
                    }
                    if (storage2 != null) {
                        long coinIncre2 = poolper2 * inc;
                        if (coinIncre2 > 0) storage2.addAndGet(coinIncre2);
                        if (coinIncre2 != 0) {
                            if (data2Record == null) {
                                if (dataCoinPool2Queue.size() > 1) dataCoinPool2Queue.poll();
                                dataCoinPool2Queue.add(createGameData(key2, storage2.get()));
                                coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 2, roundid, coins, 0, coinIncre2, storage2.get(), now, module, remark));
                            } else {
                                data2Record.setCoins(data2Record.getCoins() + coins);
                                data2Record.setPoolcoin(data2Record.getPoolcoin() + coinIncre2);
                                data2Record.setNewpoolcoins(storage2.get());
                            }
                        }
                    }
                    if (storage3 != null) {
                        long coinIncre3 = poolper3 * inc;
                        if (coinIncre3 > 0) storage3.addAndGet(coinIncre3);
                        if (coinIncre3 != 0) {
                            if (data3Record == null) {
                                if (dataCoinPool3Queue.size() > 1) dataCoinPool3Queue.poll();
                                dataCoinPool3Queue.add(createGameData(key3, storage3.get()));
                                coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 3, roundid, coins, 0, coinIncre3, storage3.get(), now, module, remark));
                            } else {
                                data3Record.setCoins(data3Record.getCoins() + coins);
                                data3Record.setPoolcoin(data3Record.getPoolcoin() + coinIncre3);
                                data3Record.setNewpoolcoins(storage3.get());
                            }
                        }
                    }
                }
                checkPoolCoin(roomlevel, 1);
                return inc * tax1;
            }
        }
        checkPoolCoin(roomlevel, 1);
        return 0;
    }

    protected long increUnlockPool1Coin(int roomlevel, int userid, long increcoin, String roundid, long now, String module, String remark) {
        return increUnlockPool1Coin(roomlevel, userid, increcoin, 0, increcoin, roundid, now, module, remark);
    }

    protected long increUnlockPool1Coin(int roomlevel, int userid, long coin, long taxcoin, long increcoin, String roundid, long now, String module, String remark) {
        if (this.tmpCoinPoolArray.length == 1) roomlevel = 1;
        final AtomicLong jiang = this.dataCoinPoolArray[roomlevel - 1];
        final QueueTask<GameData> queue = this.dataCoinPoolQueues[roomlevel - 1];
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        final String key = "GAMEDATA_" + gamekey + "_COINPOOL_" + roomlevel;
        long val = increcoin; //必须是正数
        if (val == 0) return val;
        long jv = jiang.addAndGet(val);
        if (queue.size() > 1) queue.poll();
        queue.add(createGameData(key, jv));
        coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 1, roundid, coin, taxcoin, increcoin, jv, now, module, remark));
        checkPoolCoin(roomlevel, 1);
        return val;
    }

    protected String decreUnlockPool1Coin(int roomlevel, int userid, long decrecoin, String roundid, long now, String module, String remark) {
        if (this.tmpCoinPoolArray.length == 1) roomlevel = 1;
        final AtomicLong jiang = this.dataCoinPoolArray[roomlevel - 1];
        final QueueTask<GameData> queue = this.dataCoinPoolQueues[roomlevel - 1];
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        final String key = "GAMEDATA_" + gamekey + "_COINPOOL_" + roomlevel;
        long val = decrecoin; //必须是正数
        if (val == 0) return "";
        long jv = jiang.addAndGet(-val);
        if (queue.size() > 1) queue.poll();
        queue.add(createGameData(key, jv));
        PoolDataRecord record = new PoolDataRecord(gameId(), userid, roomlevel, 1, roundid, -val, 0, -val, jv, now, module, remark);
        coinPoolDataQueue.add(record);
        checkPoolCoin(roomlevel, 1);
        return record.getPoolrecordid();
    }

    protected long increPool1Coin(int roomlevel, int userid, Function<AtomicLong, Long> func, String roundid, long now, String module, String remark) {
        if (this.tmpCoinPoolArray.length == 1) roomlevel = 1;
        final AtomicLong jiang = this.dataCoinPoolArray[roomlevel - 1];
        final QueueTask<GameData> queue = this.dataCoinPoolQueues[roomlevel - 1];
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        final String key = "GAMEDATA_" + gamekey + "_COINPOOL_" + roomlevel;
        synchronized (jiang) {
            long val = func.apply(jiang); //必须是正数
            if (val == 0) return val;
            long jv = jiang.addAndGet(val);
            if (queue.size() > 1) queue.poll();
            queue.add(createGameData(key, jv));
            coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 1, roundid, val, 0, val, jv, now, module, remark));
            checkPoolCoin(roomlevel, 1);
            return val;
        }
    }

    protected String decrePool1Coin(int roomlevel, int userid, Function<AtomicLong, Long> func, String roundid, long now, String module, String remark) {
        if (this.tmpCoinPoolArray.length == 1) roomlevel = 1;
        final AtomicLong jiang = this.dataCoinPoolArray[roomlevel - 1];
        final QueueTask<GameData> queue = this.dataCoinPoolQueues[roomlevel - 1];
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        final String key = "GAMEDATA_" + gamekey + "_COINPOOL_" + roomlevel;
        synchronized (jiang) {
            long val = func.apply(jiang); //必须是正数
            if (val == 0) return "";
            long jv = jiang.addAndGet(-val);
            if (queue.size() > 1) queue.poll();
            queue.add(createGameData(key, jv));
            PoolDataRecord record = new PoolDataRecord(gameId(), userid, roomlevel, 1, roundid, -val, 0, -val, jv, now, module, remark);
            coinPoolDataQueue.add(record);
            checkPoolCoin(roomlevel, 1);
            return record.getPoolrecordid();
        }
    }

    protected long decrePool2Coin(int roomlevel, int userid, Function<AtomicLong, Long> func, String roundid, long now, String module, String remark) {
        if (this.tmpCoinPool2Array.length == 1) roomlevel = 1;
        final AtomicLong jiang = this.dataCoinPool2Array[roomlevel - 1];
        final QueueTask<GameData> queue = this.dataCoinPool2Queues[roomlevel - 1];
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        final String key = "GAMEDATA_" + gamekey + "_COINPOOL2_" + roomlevel;
        synchronized (jiang) {
            long val = func.apply(jiang); //必须是正数
            if (val == 0) return val;
            long jv = jiang.addAndGet(-val);
            if (queue.size() > 1) queue.poll();
            queue.add(createGameData(key, jv));
            coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 2, roundid, -val, 0, -val, jv, now, module, remark));
            checkPoolCoin(roomlevel, 2);
            return val;
        }
    }

    protected long decrePool3Coin(int roomlevel, int userid, Function<AtomicLong, Long> func, String roundid, long now, String module, String remark) {
        if (this.tmpCoinPool3Array.length == 1) roomlevel = 1;
        final AtomicLong jiang = this.dataCoinPool3Array[roomlevel - 1];
        final QueueTask<GameData> queue = this.dataCoinPool3Queues[roomlevel - 1];
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        final String key = "GAMEDATA_" + gamekey + "_COINPOOL3_" + roomlevel;
        synchronized (jiang) {
            long val = func.apply(jiang); //必须是正数
            if (val == 0) return val;
            long jv = jiang.addAndGet(-val);
            if (queue.size() > 1) queue.poll();
            queue.add(createGameData(key, jv));
            coinPoolDataQueue.add(new PoolDataRecord(gameId(), userid, roomlevel, 3, roundid, -val, 0, -val, jv, now, module, remark));
            checkPoolCoin(roomlevel, 3);
            return val;
        }
    }

    @Override
    protected void loadData() {
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        if (this.dataCoinPoolArray != null) {
            for (int i = 0; i < this.dataCoinPoolArray.length; i++) {
                this.dataCoinPoolArray[i].set(findDataValue("GAMEDATA_" + gamekey + "_COINPOOL_" + (i + 1), 0L));
            }
        }
        if (this.dataCoinPool2Array != null) {
            for (int i = 0; i < this.dataCoinPool2Array.length; i++) {
                this.dataCoinPool2Array[i].set(findDataValue("GAMEDATA_" + gamekey + "_COINPOOL2_" + (i + 1), Long.MIN_VALUE));
            }
        }
        if (this.dataCoinPool3Array != null) {
            for (int i = 0; i < this.dataCoinPool3Array.length; i++) {
                this.dataCoinPool3Array[i].set(findDataValue("GAMEDATA_" + gamekey + "_COINPOOL3_" + (i + 1), Long.MIN_VALUE));
            }
        }
    }

    @Override
    protected void restoreData() {
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        if (this.dataCoinPoolArray != null) {
            for (int i = 0; i < this.dataCoinPoolArray.length; i++) {
                String key = "GAMEDATA_" + gamekey + "_COINPOOL_" + (i + 1);
                long value = this.dataCoinPoolArray[i].get();
                try {
                    updateDataValue(key, value);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "update dataCoinPoolArray error(key=" + key + ", value=" + value, e);
                }
            }
        }
        if (this.dataCoinPool2Array != null) {
            for (int i = 0; i < this.dataCoinPool2Array.length; i++) {
                String key = "GAMEDATA_" + gamekey + "_COINPOOL2_" + (i + 1);
                long value = this.dataCoinPool2Array[i].get();
                if (value == Long.MIN_VALUE) continue;
                try {
                    updateDataValue(key, value);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "update dataCoinPool2Array error(key=" + key + ", value=" + value, e);
                }
            }
        }

        if (this.dataCoinPool3Array != null) {
            for (int i = 0; i < this.dataCoinPool3Array.length; i++) {
                String key = "GAMEDATA_" + gamekey + "_COINPOOL3_" + (i + 1);
                long value = this.dataCoinPool3Array[i].get();
                if (value == Long.MIN_VALUE) continue;
                try {
                    updateDataValue(key, value);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "update dataCoinPool3Array error(key=" + key + ", value=" + value, e);
                }
            }
        }
    }

    @Override
    protected void reloadSuperConfig(GameConfigFunc func) {
        super.reloadSuperConfig(func);
        String gamekey = gameId().toUpperCase();
        if (gamekey.endsWith("MJ")) gamekey = "MJ";
        this.confCoinPoolTaxPermillage = func.getInt("GAME_" + gamekey + "_COINPOOL_TAXPERMILLAGE", 100);
        this.confCoinPool2Permillage = openDataCoinPool2() ? func.getInt("GAME_" + gamekey + "_COINPOOL2_PERMILLAGE", 100) : 0;
        this.confCoinPool3Permillage = openDataCoinPool3() ? func.getInt("GAME_" + gamekey + "_COINPOOL3_PERMILLAGE", 100) : 0;
        this.confAnnounceCoin = roomLevelSize() == 0 ? 0 : func.getInt("GAME_" + gamekey + "_ANNOUNCE_COIN", 10000000); //需要发公告的最低金币数,为0表示无需对金币进行发公告
        this.confAnnounceFactor = roomLevelSize() == 0 ? 0 : func.getInt("GAME_" + gamekey + "_ANNOUNCE_FACTOR", 300); //需要发公告的最低倍数,为0表示无需对倍数进行发公告
        //房间场次金币要求信息
        if (roomLevelSize() == 1) {
            this.confRoomCoinStages = new Range.LongRange[]{
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_1", 1000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_1", -1L))
            };
            this.confLimitWinCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_1", 0L)
            };
            this.confLimitLosCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_1", 0L)
            };
        } else if (roomLevelSize() == 2) {
            this.confRoomCoinStages = new Range.LongRange[]{
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_1", 1000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_1", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_2", 1_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_2", -1L))
            };
            this.confLimitWinCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_2", 0L)
            };
            this.confLimitLosCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_2", 0L)
            };
        } else if (roomLevelSize() == 3) {
            this.confRoomCoinStages = new Range.LongRange[]{
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_1", 1000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_1", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_2", 1_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_2", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_3", 10_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_3", -1L))
            };
            this.confLimitWinCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_2", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_3", 0L)
            };
            this.confLimitLosCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_2", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_3", 0L)
            };
        } else if (roomLevelSize() == 4) {
            this.confRoomCoinStages = new Range.LongRange[]{
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_1", 1000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_1", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_2", 1_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_2", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_3", 10_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_3", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_4", 100_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_4", -1L))
            };
            this.confLimitWinCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_2", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_3", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_4", 0L)
            };
            this.confLimitLosCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_2", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_3", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_4", 0L)
            };
        } else if (roomLevelSize() == 5) {
            this.confRoomCoinStages = new Range.LongRange[]{
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_1", 1000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_1", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_2", 1_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_2", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_3", 10_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_3", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_4", 100_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_4", -1L)),
                new Range.LongRange(func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMIN_5", 1000_0000L), func.getLong("GAME_" + gamekey + "_COINSTAGE_RANGEMAX_5", -1L))
            };
            this.confLimitWinCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_2", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_3", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_4", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITWIN_5", 0L)
            };
            this.confLimitLosCoinStages = new long[]{
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_1", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_2", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_3", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_4", 0L),
                func.getLong("GAME_" + gamekey + "_COINSTAGE_LIMITLOS_5", 0L)
            };
        } else {
            this.confRoomCoinStages = new Range.LongRange[0];
            this.confLimitWinCoinStages = new long[0];
            this.confLimitLosCoinStages = new long[0];
        }
    }

    //是否能发公告
    protected boolean canBroadcastAnnounce(long coin, int factor) {
        return canBroadcastAnnounce(coin, (float) factor);
    }

    //是否能发公告
    protected boolean canBroadcastAnnounce(long coin, float factor) {
        if (this.confAnnounceCoin > 0 && coin >= this.confAnnounceCoin) return true;
        if (this.confAnnounceFactor > 0 && factor >= this.confAnnounceFactor) return true;
        return false;
    }

    public void broadcastHrollAnnouncement(String game, int userid, String username, String gamename, long coins, int factor) {
        this.broadcastHrollAnnouncement(game, userid, username, gamename, "", coins, factor);
    }

    public void broadcastHrollAnnouncement(String game, int userid, String username, String gamename, long coins, float factor) {
        this.broadcastHrollAnnouncement(game, userid, username, gamename, "", coins, factor);
    }

    public void broadcastHrollAnnouncement(String game, int userid, String username, String gamename, String subgame, long coins, int factor) {
        broadcastHrollAnnouncement(game, userid, username, gamename, subgame, coins, (float) factor);
    }

    public void broadcastHrollAnnouncement(String game, int userid, String username, String gamename, String subgame, long coins, float factor) {
        if (this.confAnnounceCoin > 0 && coins >= this.confAnnounceCoin) {
            broadcastHrollAnnouncement(Announcement.createHrollAnnounceWinCoins(game, userid, username, gamename, subgame, coins));
        } else if (this.confAnnounceFactor > 0 && factor >= this.confAnnounceFactor) {
            broadcastHrollAnnouncement(Announcement.createHrollAnnounceWinFactor(game, userid, username, gamename, subgame, factor));
        }
    }

    //发送滚动公告
    @Override
    public void broadcastHrollAnnouncement(Announcement... announces) {
        //if (finest) logger.finest("广播公告: " + JsonConvert.root().convertTo(announces));
        if (announces == null || announces.length < 1) return;
        this.announceLastTime.set(System.currentTimeMillis());
        String gameid = announces[0].getGame();
        //if ("bba".equals(gameid) || "fqzs".equals(gameid) || "niu0".equals(gameid)) return;
        this.announceQueue.add(announces);
    }

    //发送滚动公告
    @Override
    public void broadcastHrollAnnouncement(List<Announcement> announces) {
        broadcastHrollAnnouncement(announces.toArray(new Announcement[announces.size()]));
    }

    //击穿底线
    public boolean breakLimitLosCoin(int roomlevel, long humanWinCoins) {
        if (humanWinCoins < 0) throw new RuntimeException("breakLimitLos: humanWinCoins must > 0, but humanWinCoins = " + humanWinCoins);
        long limitLos = this.confLimitLosCoinStages[roomlevel - 1];
        if (limitLos >= Integer.MAX_VALUE) return false;
        return (this.getDataCoinPoolValue(roomlevel) - humanWinCoins) <= limitLos;
    }

    //超过上限
    public boolean passLimitWinCoin(int roomlevel, long humanWinCoins) {
        long limitWin = this.confLimitWinCoinStages[roomlevel - 1];
        if (limitWin < 0) return false;
        return (this.getDataCoinPoolValue(roomlevel) - humanWinCoins) >= limitWin;
    }

    //击穿底线
    public boolean breakLimitLosCoin(int roomlevel, long humanWinCoins, long humanBetCoins) {
        if (humanWinCoins < 0) throw new RuntimeException("breakLimitLos: humanWinCoins must > 0, but humanWinCoins = " + humanWinCoins);
        if (humanBetCoins < 0) throw new RuntimeException("breakLimitLos: humanBetCoins must > 0, but humanBetCoins = " + humanBetCoins);
        long limitLos = this.confLimitLosCoinStages[roomlevel - 1];
        if (limitLos >= Integer.MAX_VALUE) return false;
        if ((this.tmpCoinPoolArray[roomlevel - 1].get() + humanBetCoins) < 1000) return (this.getDataCoinPoolValue(roomlevel) - humanWinCoins) <= limitLos;

        final AtomicLong storage2 = this.dataCoinPool2Array == null ? null : this.dataCoinPool2Array[roomlevel - 1];
        final AtomicLong storage3 = this.dataCoinPool3Array == null ? null : this.dataCoinPool3Array[roomlevel - 1];
        int tax1 = this.confCoinPoolTaxPermillage;
        int poolper2 = (storage2 != null ? this.confCoinPool2Permillage : 0);
        int poolper3 = (storage3 != null ? this.confCoinPool3Permillage : 0);
        return (this.getDataCoinPoolValue(roomlevel) + (humanBetCoins - (humanBetCoins * (tax1 + poolper2 + poolper3) + 999) / 1000) - humanWinCoins) <= limitLos;
    }

    //超过上限
    public boolean passLimitWinCoin(int roomlevel, long humanWinCoins, long humanBetCoins) {
        long limitWin = this.confLimitWinCoinStages[roomlevel - 1];
        if (limitWin < 0) return false;
        if (humanBetCoins == 0) return (this.getDataCoinPoolValue(roomlevel) - humanWinCoins) >= limitWin;

        final AtomicLong storage2 = this.dataCoinPool2Array == null ? null : this.dataCoinPool2Array[roomlevel - 1];
        final AtomicLong storage3 = this.dataCoinPool3Array == null ? null : this.dataCoinPool3Array[roomlevel - 1];
        int tax1 = this.confCoinPoolTaxPermillage;
        int poolper2 = (storage2 != null ? this.confCoinPool2Permillage : 0);
        int poolper3 = (storage3 != null ? this.confCoinPool3Permillage : 0);
        return (this.getDataCoinPoolValue(roomlevel) - (humanWinCoins - humanBetCoins * (tax1 + poolper2 + poolper3) / 1000)) >= limitWin;
    }

    //获取手续费千分比
    public int getConfCoinPoolTaxPermillage() {
        return confCoinPoolTaxPermillage;
    }

    public long getConfLimitWinCoinValue(int roomlevel) {
        return this.confLimitWinCoinStages[roomlevel - 1];
    }

    public long getConfLimitLosCoinValue(int roomlevel) {
        return this.confLimitLosCoinStages[roomlevel - 1];
    }

    public long getDataCoinPoolValue(int roomlevel) {
        if (this.dataCoinPoolArray.length == 1) return this.dataCoinPoolArray[0].get();
        return this.dataCoinPoolArray[roomlevel - 1].get();
    }

    protected Object lockDataCoinPool(int roomlevel) {
        if (this.dataCoinPoolArray == null) return new Object();
        if (this.dataCoinPoolArray.length == 1) return this.dataCoinPoolArray[0];
        return this.dataCoinPoolArray[roomlevel - 1];
    }

    public long getDataCoinPool2Value(int roomlevel) {
        if (this.dataCoinPool2Array.length == 1) return this.dataCoinPool2Array[0].get();
        return this.dataCoinPool2Array[roomlevel - 1].get();
    }

    protected Object lockDataCoinPool2(int roomlevel) {
        if (this.dataCoinPool2Array.length == 1) return this.dataCoinPool2Array[0];
        return this.dataCoinPool2Array[roomlevel - 1];
    }

    public long getDataCoinPool3Value(int roomlevel) {
        if (this.dataCoinPool3Array.length == 1) return this.dataCoinPool3Array[0].get();
        return this.dataCoinPool3Array[roomlevel - 1].get();
    }

    protected Object lockDataCoinPool3(int roomlevel) {
        if (this.dataCoinPool3Array.length == 1) return this.dataCoinPool3Array[0];
        return this.dataCoinPool3Array[roomlevel - 1];
    }

    @RestMapping(auth = false, comment = "获取房间场次的金币范围要求")
    public <T> RetResult<T> loadRoomCoinStages() {
        if (this.roomBaseCoins == null || this.roomBaseCoins.length < this.confRoomCoinStages.length) {
            return new RetResult(this.confRoomCoinStages);
        }
        Map<String, Long>[] maps = new Map[this.roomBaseCoins.length];
        for (int i = 0; i < this.confRoomCoinStages.length; i++) {
            Range.LongRange range = this.confRoomCoinStages[i];
            maps[i] = Utility.ofMap("min", range.getMin(), "max", range.getMax(), "basecoin", this.roomBaseCoins[i]);
        }
        return new RetResult(maps);
    }

    @Override
    @RestMapping(auth = true, comment = "进入游戏")
    public <T> RetResult<T> enterGame(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, Map<String, String> bean) {
        if (logger.isLoggable(Level.FINEST) && !UserInfo.isRobot(userid)) logger.log(Level.FINEST, "enterGame " + gameId() + ": userid=" + userid + ", sncpAddress=" + sncpAddress + ", bean=" + bean);
        if (this.serviceClosed) return GameRetCodes.retResult(GameRetCodes.RET_GAME_SHUTDOWN);
        final int roomlevel = bean == null ? 0 : Integer.parseInt(bean.getOrDefault("roomlevel", "0"));
        if (roomLevelSize() > 0) {
            if (roomlevel < 1 || (roomlevel > roomLevelSize() && roomlevel < 100)) {
                return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
            }
        }
        UserInfo user = findUserInfo(userid);
        if (user == null || !user.isNormal()) return GameRetCodes.retResult(RET_GAME_PLAYER_STATUS_ILLEGAL);
        if (roomLevelSize() > 0 && roomlevel < 100) {
            Range.LongRange range = this.confRoomCoinStages[roomlevel - 1];
            if (!range.test(user.getCoins())) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_COINRANGE_ILLEGAL);
        }

        if (this.confIndulgeActivate) { //防沉迷判断        
            RetResult rs = indulgeEnterGame(userid, user, bean);
            if (rs != null && !rs.isSuccess()) return rs;
        }
        if (roomlevel > 100) return enterSportGame(user, roomlevel, clientAddr, sncpAddress, bean);
        RetResult rs = authEnterGame(userid, user, bean);
        if (rs != null && !rs.isSuccess()) return rs;
        if (!gameId().equalsIgnoreCase(user.getCurrgame())) userEnterGame(userid);
        loadAccount(userid); //setLeaving
        putLivingPlayer(userid, loadGamePlayer(user, clientAddr, sncpAddress, roomlevel, bean, findLivingPlayer(userid)));
        return RetResult.success();
    }

    protected <T> RetResult<T> enterSportGame(UserInfo user, int roomlevel, String clientAddr, InetSocketAddress sncpAddress, Map<String, String> bean) {
        return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
    }

    protected abstract P loadGamePlayer(UserInfo user, String clientAddr, InetSocketAddress sncpAddress, int roomlevel, Map<String, String> bean, P oldPlayer);

    protected <T> RetResult<T> indulgeEnterGame(int userid, UserInfo user, Map<String, String> bean) {
        int age = user.getAge();
        if (age == 0) { //游客
            if (confIndulgeGuestTryplaySeconds > 0) {
                GameAccount account = loadAccount(userid);
                account.setOnlinetodaystarttime(System.currentTimeMillis());
                if (account.currOnlineSeconds() > confIndulgeGuestTryplaySeconds) {
                    account.setLeaving(true);
                    return RetCodes.retResult(RetCodes.RET_USER_GUEST_PLAYTIME_LIMIT);
                }
            }
        } else if (age < 18) {
            long chamills = System.currentTimeMillis() - Utility.midnight();
            int minmills = dictService.findDictValue(DictInfo.PLATF_INDULGE_UN18AGE_MINMILLS, 0);
            int maxmills = dictService.findDictValue(DictInfo.PLATF_INDULGE_UN18AGE_MAXMILLS, 0);
            if ((minmills > 0 && chamills <= minmills)) return RetCodes.retResult(RetCodes.RET_USER_UN18AGE_PLAYRANGE_LIMIT, maxmills / 3600_000, minmills / 3600_000);
            if ((maxmills > 0 && chamills >= maxmills)) return RetCodes.retResult(RetCodes.RET_USER_GUEST_PLAYTIME_LIMIT, maxmills / 3600_000, minmills / 3600_000);
            if (confIndulgeUn18AgeDayplaySeconds > 0) {
                GameAccount account = loadAccount(userid);
                account.setOnlinetodaystarttime(System.currentTimeMillis());
                if (account.currTodayOnlineSeconds() > confIndulgeUn18AgeDayplaySeconds) {
                    account.setLeaving(true);
                    return RetCodes.retResult(RetCodes.RET_USER_UN18AGE_PLAYTIME_LIMIT);
                }
            }
        }
        return null;
    }

    protected <T> RetResult<T> authEnterGame(int userid, UserInfo user, Map<String, String> bean) {
        return null;
    }

    protected void leaveGame(int userid) {
        leaveGame(userid, false);
    }

    protected void leaveGame(int userid, boolean ignoreUserLeave) {
        P player = removeLivingPlayer((Integer) userid);
        if (player != null) {
            player.offline();
            if (!ignoreUserLeave) userLeaveGame(userid);
            leaveAccount(userid);
            afterLeaveGame(player);
        }
    }

    @Override
    @RestMapping(auth = true, comment = "离开游戏")
    public <T> RetResult<T> leaveGame(int userid, Map<String, String> bean) {
        if (logger.isLoggable(Level.FINEST) && !UserInfo.isRobot(userid)) logger.log(Level.FINEST, "leaveGame " + gameId() + ": userid=" + userid);
        leaveGame(userid);
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = true, comment = "玩家离线")
    public <T> RetResult<T> offlineGame(int userid, Map<String, String> bean) {
        if (logger.isLoggable(Level.FINEST) && !UserInfo.isRobot(userid)) logger.log(Level.FINEST, "offlineGame " + gameId() + ": userid=" + userid);
        leaveGame(userid);
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = false, comment = "用户更新签到")
    public RetResult<String> notifyDutyRecord(final int userid, final DutyRecord duty) {
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = false, comment = "用户更新设备商品")
    public RetResult<String> notifyGoodsInfo(final int userid, final short goodstype, final int goodscount, final List<GoodsItem> items) {
        return RetResult.success();
    }

    @Override
    @RestMapping(auth = false, comment = "平台用户信息更新通知")
    public RetResult<String> notifyPlatfPlayer(final int userid, final Map<String, String> bean) {
        P player = findLivingPlayer(userid);
        if (player != null) player.copyFromUser(findUserInfo(userid));
        return RetResult.success();
    }

    @RestMapping(auth = false, comment = "房间场次的最小金币要求")
    public RetResult<Range.LongRange[]> loadConfRoomCoinStages() {
        return new RetResult<>(this.confRoomCoinStages);
    }

    @Override
    @RestMapping(auth = true, comment = "检测GameTable是否正常")
    public RetResult<String> checkTable(String tableid) {
        return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
    }

    @Override
    @RestMapping(auth = true, comment = "加入房间")
    public RetResult<Table> joinTable(int userid, @RestAddress String clientAddr, @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS) InetSocketAddress sncpAddress, GTBean bean) {
        return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
    }

    @Override
    @RestMapping(auth = false, comment = "强制关闭房间")
    public RetResult<Table> forceDismissTable(GTBean bean) {
        return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
    }

    //获取玩家正在玩的场次，为-1表示当前没有在玩, 为0表示玩家在此游戏内，但不分场次
    @Override
    @RestMapping(auth = false, comment = "玩家房间场次")
    public int getPlayingRoomLevel(int userid) {
        GamePlayer player = findLivingPlayer(userid);
        if (player == null) return -1;
        return player.getRoomlevel();
    }

    @Override
    @RestMapping(auth = false, comment = "每个场次的在线玩家数")
    public Map<Integer, Long> getPlayingRoomUserMap() {
        Function<P, Integer> roomLevelFunc = p -> Math.max(p.getRoomlevel(), 1);
        return livingPlayers.values().stream().filter(x -> {
            if (x.isRobot() || !x.isOnline()) return false;
            boolean rs = webSocketNode.existsWebSocket(x).join();
            if (!rs) notExistsWebSocket(x);
            return rs;
        }).collect(Collectors.groupingBy(roomLevelFunc, Collectors.counting()));
    }

    @Comment("livingPlayers存在但WebSocket已不存在")
    protected void notExistsWebSocket(P player) {
        RetResult rs = leaveGame(player.getUserid(), (Map) null);
        logger.log(Level.WARNING, gameId() + " notExistsWebSocket player = " + player.getUserid() + ", leaveGame result = " + rs);
    }

    @Comment("模拟器配置")
    public void testConfig(Map<String, Long> map) {
        final GameConfigFunc func = GameConfigFunc.createFromMap(map);
        reloadSuperConfig(func);
        reloadConfig(func);
    }

}
