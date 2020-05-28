/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.battle.BattleSportRoom;
import com.cratos.platf.order.GoodsItem;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.Column;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
public class SkywarSportManyRoom extends BattleSportRoom<SkywarTable> {

    @Column(comment = "比赛日期; 20190909")
    protected int intday;

    @Comment("已报名的玩家")
    protected ConcurrentHashMap<Integer, AtomicInteger> applyUserids = new ConcurrentHashMap<>();

    @Comment("前三名的奖励")
    protected GoodsItem[] goodsitems;

    public SkywarSportManyRoom() {
    }

    public SkywarSportManyRoom(int roomlevel, String roomname, int intday, GoodsItem[] goodsitems) {
        this.roomlevel = roomlevel;
        this.roomname = roomname;
        this.intday = intday;
        this.goodsitems = goodsitems;
    }

}
