/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.hundred;

import com.cratos.platf.base.*;
import com.cratos.platf.game.*;
import java.util.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.service.RetResult;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 * @param <GR> HundredGameRound
 */
public class HundredGameTable<GR extends HundredGameRound> extends GameTable<HundredGamePlayer> {

    private static final Reproduce<HundredGameTableHis, HundredGameTable> copyer = Reproduce.create(HundredGameTableHis.class, HundredGameTable.class);

    @Column(comment = "庄家用户ID，200_0000视为系统;")
    protected int bankerid;

    @Column(comment = "结果值")
    protected int resultid;

    @Column(length = 2048, comment = "牌面集合")
    protected String cards = "";

    @Column(comment = "真实玩家下注的总金币数;结算后才赋值")
    protected long betcoins;

    @Column(comment = "手续费;结算后才赋值")
    protected long taxcoin;

    @Column(length = 255, comment = "所有人的押注")
    protected String allbets = "";

    @Column(comment = "系统所赢的金币数， 负数表示平台亏损的")
    protected long oswincoins;

    //------------------------------------------------------
    @Transient
    @Column(comment = "庄家")
    protected GamePlayer banker;

    @Transient
    @Column(comment = "当前回合")
    protected GR currRound;

    public final HundredGameTableHis createTableHis(long now) {
        HundredGameTableHis his = copyer.apply(new HundredGameTableHis(), this);
        his.setFinishtime(now);
        his.setTableid(Utility.format36time(now) + "-" + his.getTableid().substring(his.getTableid().indexOf('-') + 1)); //以结束时间为准
        return his;
    }

    @Comment("是否已经押注了")
    public boolean isBetting(int userid) {
        HundredGameRound round = this.currRound;
        if (round == null) return false;
        return round.isBetting(userid);
    }

    @Comment("创建一个Round")
    public GR newRound() {
        return (GR) new HundredGameRound();
    }

    @Comment("回合开始")
    public RetResult<String> roundFinish(GameService gameService) {
        return RetCodes.retResult(RetCodes.RET_SUPPORT_ILLEGAL);
    }

    @Comment("回合开始")
    public RetResult<List<GameActionEvent>> roundStart(GameService gameService) {
        HundredCoinGameService service = (HundredCoinGameService) gameService;
        this.createtime = System.currentTimeMillis();
        GR round = newRound();
        round.init(service.betEntryCount());
        round.setCreatetime(this.createtime);
        this.currRound = round;
        this.banker = service.currBanker;
        this.bankerid = this.banker.getUserid();
        this.tableid = Utility.format36time(this.createtime) + "-" + gameService.nodeid();
        round.setTableid(tableid);
        return RetResult.success();
    }

    public long getBankerMinApplyCoin() {
        return 10000_00L;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getBankerid() {
        return bankerid;
    }

    public void setBankerid(int bankerid) {
        this.bankerid = bankerid;
    }

    public int getResultid() {
        return resultid;
    }

    public void setResultid(int resultid) {
        this.resultid = resultid;
    }

    public String getCards() {
        return cards;
    }

    public void setCards(String cards) {
        this.cards = cards;
    }

    public long getBetcoins() {
        return betcoins;
    }

    public void setBetcoins(long betcoins) {
        this.betcoins = betcoins;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getTaxcoin() {
        return taxcoin;
    }

    public void setTaxcoin(long taxcoin) {
        this.taxcoin = taxcoin;
    }

    public String getAllbets() {
        return allbets;
    }

    public void setAllbets(String allbets) {
        this.allbets = allbets;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getOswincoins() {
        return oswincoins;
    }

    public void setOswincoins(long oswincoins) {
        this.oswincoins = oswincoins;
    }

    public GamePlayer getBanker() {
        return banker;
    }

    public void setBanker(GamePlayer banker) {
        this.banker = banker;
    }

    public GR getCurrRound() {
        return currRound;
    }

    public void setCurrRound(GR currRound) {
        this.currRound = currRound;
    }

}
