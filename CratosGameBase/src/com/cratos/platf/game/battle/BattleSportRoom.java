/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseEntity;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import javax.persistence.Column;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 * @param <GT> BattleGameTable
 */
public class BattleSportRoom<GT extends BattleGameTable> extends BaseEntity {

    @Column(comment = "比赛场名称")
    protected String roomname = "";
 
    @Column(comment = "比赛场场次")
    protected int roomlevel;

    @Comment("当前正在进行的房间")
    protected final Queue<GT> currTables = new ConcurrentLinkedQueue();

    public void addTable(GT table) {
        this.currTables.add(table);
        table.sportRoom = this;
    }

    public void removeTable(GT table) {
        this.currTables.remove(table);
    }

    public Stream<GT> streamTables() {
        return this.currTables.stream();
    }

    public String getRoomname() {
        return roomname;
    }

    public void setRoomname(String roomname) {
        this.roomname = roomname;
    }

    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

}
