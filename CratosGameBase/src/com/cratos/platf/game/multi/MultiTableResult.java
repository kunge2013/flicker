/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.base.BaseBean;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import javax.persistence.Column;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 * @param <RRP> MultiTableResultPlayer
 */
public class MultiTableResult<RRP extends MultiTableResultPlayer> extends BaseBean {

    @Column(comment = "结束类型")
    protected short finishtype;

    @Column(comment = "结束时间")
    protected long finishtime;

    @Comment("金币底注")
    protected int baseBetCoin;

    @Comment("玩家")
    protected RRP[] players;

    @Comment("扩展信息")
    protected Map<String, Object> attrs;

    public MultiTableResult() {
    }

    public MultiTableResult(MultiGameTable table, List<? extends MultiGamePlayer> playingPlayers, long now) {
        this(table, playingPlayers, null, now);
    }

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public MultiTableResult(MultiGameTable table, List<? extends MultiGamePlayer> playingPlayers, BiConsumer<MultiGamePlayer, RRP> consumer, long now) {
        this.baseBetCoin = table.getBaseBetCoin();
        this.finishtime = now;
        for (MultiGamePlayer player : playingPlayers) {
            this.add((RRP) new MultiTableResultPlayer(player, consumer));
        }
    }

    public void add(RRP player) {
        if (this.players == null) { //不处理第1个会变成MultiTableResultPlayer[] 而不是RRP[]
            this.players = (RRP[]) Array.newInstance(player.getClass(), 1);
            this.players[0] = player;
            return;
        }
        this.players = Utility.append(this.players, player);
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

    public MultiTableResult attr(String key, Object value) {
        if (this.attrs == null) this.attrs = new LinkedHashMap<>();
        this.attrs.put(key, value);
        return this;
    }

    public MultiTableResult attrs(Object... attributes) {
        this.attrs = Utility.ofMap(attributes);
        return this;
    }

    public MultiTableResult attrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }

    public long getFinishtime() {
        return finishtime;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public int getBaseBetCoin() {
        return baseBetCoin;
    }

    public void setBaseBetCoin(int baseBetCoin) {
        this.baseBetCoin = baseBetCoin;
    }

    public RRP[] getPlayers() {
        return players;
    }

    public void setPlayers(RRP[] players) {
        this.players = players;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    public short getFinishtype() {
        return finishtype;
    }

    public void setFinishtype(short finishtype) {
        this.finishtype = finishtype;
    }

}
