/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.hundred;

import com.cratos.platf.base.*;
import com.cratos.platf.game.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.service.RetResult;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(name = "hundredround")
@DistributeTable(strategy = HundredGameRound.TableStrategy.class)
public class HundredGameRound extends GameRound {

    @Column(comment = "用户ID")
    protected int userid;

    @Column(length = 32, comment = "局ID")
    protected String tableid = "";

    @Column(comment = "押注位置")
    protected int betpos;

    @Column(comment = "下注金币")
    protected long betcoin = 0;

    @Column(comment = "盈利倍数")
    protected int factor = 0;

    @Column(comment = "玩家输赢金币,负数表示输给系统(一般为负数的押注金币数)")
    protected long wincoin;

    @Column(comment = "手续费")
    protected long taxcoin;

    //-----------------非数据库字段-------------------
    @Transient
    @Column(comment = "真实玩家下注的总金币数;")
    protected AtomicLong humenAllBetCoins = new AtomicLong();

    @Transient
    @Column(comment = "状态;50:押注中;70:已结算;")
    protected short roudStatus;

    //key: userid
    @Transient
    protected Map<Integer, HundredBetEntry>[] betOptionEntrys;

    @Transient
    protected AtomicLong[] betOptionSumCoins;

    @Transient
    @Column(comment = "庄家的结算结果")
    protected GameWinner resultBankerWinner;

    @Transient
    @Column(comment = "真实玩家的所输赢的金币数")
    protected final Map<Integer, AtomicLong> resultHumenWinners = new HashMap<>();

    @Transient
    @Column(comment = "本回合玩家输赢前几名，包含电脑")
    protected List<GameWinner> resultTopWinners;

    public void init(int betEntryCount) {
        this.betOptionEntrys = new ConcurrentHashMap[betEntryCount];
        this.betOptionSumCoins = new AtomicLong[betEntryCount];
        this.roudStatus = ROUND_STATUS_BETTING;
        this.roomlevel = 1;
        for (int i = 0; i < betOptionSumCoins.length; i++) {
            betOptionSumCoins[i] = new AtomicLong();
        }
        for (int i = 0; i < betOptionEntrys.length; i++) {
            betOptionEntrys[i] = new ConcurrentHashMap<>();
        }
    }

    public boolean isBetting(int userid) { //是否已经押注了
        for (Map<Integer, HundredBetEntry> betEntryMap : this.betOptionEntrys) {
            if (betEntryMap.containsKey(userid)) return true;
        }
        return false;
    }

    //获取所有的押注金币数
    public long[] allBetCoins() {
        long[] rs = new long[betOptionEntrys.length];
        for (int i = 0; i < rs.length; i++) {
            rs[i] = getBetCoinsSum(betOptionEntrys[i]);
        }
        return rs;
    }

    public long getBetCoinsSum(Map<Integer, HundredBetEntry> betEntryMap) {
        Stream<Map.Entry<Integer, HundredBetEntry>> stream = betEntryMap.entrySet().stream();
        return stream.mapToLong(x -> x.getValue().getBetcoin()).sum();
    }

    public long getHumanBetCoinsSum(Map<Integer, HundredBetEntry> betEntryMap) {
        Stream<Map.Entry<Integer, HundredBetEntry>> stream = betEntryMap.entrySet().stream();
        stream = stream.filter(x -> !UserInfo.isRobot(x.getKey()));
        return stream.mapToLong(x -> x.getValue().getBetcoin()).sum();
    }

    public long getRobotBetCoinsSum(Map<Integer, HundredBetEntry> betEntryMap) {
        Stream<Map.Entry<Integer, HundredBetEntry>> stream = betEntryMap.entrySet().stream();
        stream = stream.filter(x -> UserInfo.isRobot(x.getKey()));
        return stream.mapToLong(x -> x.getValue().getBetcoin()).sum();
    }

    //获取指定用户的押注金币总数
    public long getUserBetCoinSum(int userid) {
        long rs = 0;
        for (Map<Integer, HundredBetEntry> rmap : betOptionEntrys) {
            HundredBetEntry entry = rmap.get(userid);
            if (entry != null) rs += entry.getBetcoin();
        }
        return rs;
    }

    //获取指定用户的押注金币数
    public long[] getUserBetCoins(int userid) {
        long[] rs = new long[betOptionEntrys.length];
        for (int i = 0; i < rs.length; i++) {
            HundredBetEntry entry = betOptionEntrys[i].get(userid);
            if (entry != null) rs[i] = entry.getBetcoin();
        }
        return rs;
    }

    //获取指定用户的输赢金币数
    public long[] getUserWinCoins(int userid) {
        long[] rs = new long[betOptionEntrys.length];
        for (int i = 0; i < rs.length; i++) {
            HundredBetEntry entry = betOptionEntrys[i].get(userid);
            if (entry != null) rs[i] = entry.getWincoin();
        }
        return rs;
    }

    //玩家和机器人押注
    public RetResult<Long> addBetEntry(int userid, HundredBetEntry bean) {
        int index = bean.getBetpos() - 1;
        Map<Integer, HundredBetEntry> map = betOptionEntrys[bean.getBetpos() - 1];
        betOptionSumCoins[index].addAndGet(bean.getBetcoin());
        HundredBetEntry entry = map.get(userid);
        if (entry == null) {
            map.put(userid, bean);
        } else {
            entry.increBetCoin(bean.getBetcoin());
        }
        if (!UserInfo.isRobot(userid)) this.humenAllBetCoins.addAndGet(bean.getBetcoin());
        return RetResult.success(betOptionSumCoins[index].get());
    }

    public short getRoudStatus() {
        return roudStatus;
    }

    public void setRoudStatus(short roudStatus) {
        this.roudStatus = roudStatus;
    }

    public List<GameWinner> getResultTopWinners() {
        return resultTopWinners;
    }

    public void setResultTopWinners(List<GameWinner> resultTopWinners) {
        this.resultTopWinners = resultTopWinners;
    }

    public GameWinner getResultBankerWinner() {
        return resultBankerWinner;
    }

    public void setResultBankerWinner(GameWinner resultBankerWinner) {
        this.resultBankerWinner = resultBankerWinner;
    }

    //---------------------- 不可见字段 --------------------------------------
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public AtomicLong getHumenAllBetCoins() {
        return humenAllBetCoins;
    }

    public void setHumenAllBetCoins(AtomicLong humenAllBetCoins) {
        this.humenAllBetCoins = humenAllBetCoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public Map<Integer, AtomicLong> getResultHumenWinners() {
        return resultHumenWinners;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public Map<Integer, HundredBetEntry>[] getBetOptionEntrys() {
        return betOptionEntrys;
    }

    public void setBetOptionEntrys(Map<Integer, HundredBetEntry>[] betOptionEntrys) {
        this.betOptionEntrys = betOptionEntrys;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public AtomicLong[] getBetOptionSumCoins() {
        return betOptionSumCoins;
    }

    public void setBetOptionSumCoins(AtomicLong[] betOptionSumCoins) {
        this.betOptionSumCoins = betOptionSumCoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getTableid() {
        return tableid;
    }

    public void setTableid(String tableid) {
        this.tableid = tableid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getBetpos() {
        return betpos;
    }

    public void setBetpos(int betpos) {
        this.betpos = betpos;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getBetcoin() {
        return betcoin;
    }

    public void setBetcoin(long betcoin) {
        this.betcoin = betcoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getFactor() {
        return factor;
    }

    public void setFactor(int factor) {
        this.factor = factor;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getWincoin() {
        return wincoin;
    }

    public void setWincoin(long wincoin) {
        this.wincoin = wincoin;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getTaxcoin() {
        return taxcoin;
    }

    public void setTaxcoin(long taxcoin) {
        this.taxcoin = taxcoin;
    }

    public static class TableStrategy implements DistributeTableStrategy<HundredGameRound> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, HundredGameRound bean) {
            return getTable(table, (Serializable) bean.getTableid());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getSingleTable(table, Long.parseLong(id.substring(0, id.indexOf('-')), 36));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Object time = node.findValue("finishtime");
            if (time == null) time = node.findValue("#finishtime");
            if (time == null) time = node.findValue("createtime");
            if (time instanceof Long) return getSingleTable(table, (Long) time);
            Range.LongRange createtime = (Range.LongRange) time;
            return getSingleTable(table, createtime.getMin());
        }

        private String getSingleTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + String.format(format, createtime);
        }
    }
}
