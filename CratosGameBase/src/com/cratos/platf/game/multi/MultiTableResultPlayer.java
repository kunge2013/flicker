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
 * @param <P> MultiGamePlayer
 */
public class MultiTableResultPlayer<P extends MultiGamePlayer> extends BaseBean {

    private static final Reproduce<MultiTableResultPlayer, MultiGamePlayer> copyer = Reproduce.create(MultiTableResultPlayer.class, MultiGamePlayer.class);

    @Comment("玩家ID")
    protected int userid;

    @Comment("玩家坐位,0表示无坐位概念, 1-4")
    protected int sitepos;

    @Comment("玩家账号最新金币数")
    protected long coins;

    @Comment("整局总战绩")
    protected int tableScore;

    @Comment("整局总战绩")
    protected long tableCoin;

    @Comment("扩展信息")
    protected Map<String, Object> attrs;

    public MultiTableResultPlayer() {
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public MultiTableResultPlayer(MultiGamePlayer player, BiConsumer<MultiGamePlayer, MultiTableResultPlayer> consumer) {
        copyer.apply(this, player);
        if (consumer != null) consumer.accept(player, this);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public MultiTableResultPlayer(MultiGamePlayer player, Object... attributes) {
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

    public MultiTableResultPlayer attr(String key, Object value) {
        if (this.attrs == null) this.attrs = new LinkedHashMap<>();
        this.attrs.put(key, value);
        return this;
    }

    public MultiTableResultPlayer attrs(Object... attributes) {
        this.attrs = Utility.ofMap(attributes);
        return this;
    }

    public MultiTableResultPlayer attrs(Map<String, Object> attrs) {
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

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public int getTableScore() {
        return tableScore;
    }

    public void setTableScore(int tableScore) {
        this.tableScore = tableScore;
    }

    public long getTableCoin() {
        return tableCoin;
    }

    public void setTableCoin(long tableCoin) {
        this.tableCoin = tableCoin;
    }

}
