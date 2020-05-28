/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.battle.BattleSportRoom;
import com.cratos.platf.order.GoodsItem;
import java.util.Set;
import java.util.concurrent.*;
import javax.persistence.Column;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class SkywarSportOnceRoom extends BattleSportRoom<SkywarTable> {

    @Column(comment = "比赛日期; 20190909")
    protected int intday;

    @Comment("已报名的玩家")
    protected Set<Integer> applyUserids = new CopyOnWriteArraySet<>();

    @Comment("已玩过的玩家")
    protected Set<Integer> playedUserids = new CopyOnWriteArraySet<>();

    @Comment("前三名的奖励")
    protected GoodsItem[] goodsitems;

    public SkywarSportOnceRoom() {
    }

    public SkywarSportOnceRoom(int roomlevel, String roomname, int intday, GoodsItem[] goodsitems) {
        this.roomlevel = roomlevel;
        this.roomname = roomname;
        this.intday = intday;
        this.goodsitems = goodsitems;
    }

    //是否已报名
    public boolean containsApplyUserid(int userid) {
        return applyUserids.contains(userid);
    }

    //报名
    public void addApplyUserid(int userid) {
        applyUserids.add(userid);
    }

    //是否已玩过
    public boolean containsPlayedUserid(int userid) {
        return playedUserids.contains(userid);
    }

    //玩过
    public void addPlayedUserid(int userid) {
        playedUserids.add(userid);
    }
}
