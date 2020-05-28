/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.base.BaseBean;
import java.util.*;
import java.util.function.BiConsumer;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 * @param <P> GamePlayer
 */
public class MultiRoundResultPlayer<P extends MultiGamePlayer> extends BaseBean {

    private static final Reproduce<MultiRoundResultPlayer, MultiGamePlayer> copyer = Reproduce.create(MultiRoundResultPlayer.class, MultiGamePlayer.class);

    @Comment("玩家ID")
    protected int userid;

    @Comment("玩家坐位,0表示无坐位概念, 1-4")
    protected int sitepos;

    @Comment("玩家账号最新金币数")
    protected long coins;

    @Comment("当前回合的战绩")
    protected int roundScore;

    @Comment("当前回合的押注金币")
    protected long roundCostCoin;
    
    @Comment("当前回合的战绩金币")
    protected long roundCoin;

    @Comment("备注")
    protected String roundRemark = "";

    @Comment("扩展信息")
    protected Map<String, Object> attrs;

    public MultiRoundResultPlayer() {
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public MultiRoundResultPlayer(MultiGamePlayer player, BiConsumer<MultiGamePlayer, MultiRoundResultPlayer> consumer) {
        copyer.apply(this, player);
        if (consumer != null) consumer.accept(player, this);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public MultiRoundResultPlayer(MultiGamePlayer player, Object... attributes) {
        copyer.apply(this, player);
        if (attributes != null && attributes.length > 0) {
            this.attrs = Utility.ofMap(attributes);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttr(String name) {
        return attrs == null ? null : (T) attrs.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttr(String name, T defValue) {
        return attrs == null ? defValue : (T) attrs.getOrDefault(name, defValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttr(String name) {
        if (attrs == null) return null;
        return (T) attrs.remove(name);
    }

    public MultiRoundResultPlayer attr(String key, Object value) {
        if (this.attrs == null) this.attrs = new LinkedHashMap<>();
        this.attrs.put(key, value);
        return this;
    }

    public MultiRoundResultPlayer attrs(Object... attributes) {
        this.attrs = Utility.ofMap(attributes);
        return this;
    }

    public MultiRoundResultPlayer attrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getSitepos() {
        return sitepos;
    }

    public void setSitepos(int sitepos) {
        this.sitepos = sitepos;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public void setRoundScore(int roundScore) {
        this.roundScore = roundScore;
    }

    public long getRoundCoin() {
        return roundCoin;
    }

    public void setRoundCoin(long roundCoin) {
        this.roundCoin = roundCoin;
    }

    public long getRoundCostCoin() {
        return roundCostCoin;
    }

    public void setRoundCostCoin(long roundCostCoin) {
        this.roundCostCoin = roundCostCoin;
    }

    public String getRoundRemark() {
        return roundRemark;
    }

    public void setRoundRemark(String roundRemark) {
        this.roundRemark = roundRemark;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

}
