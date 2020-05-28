/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.*;
import static com.cratos.platf.game.GamePlayer.*;
import static com.cratos.platf.game.GameRetCodes.*;
import static com.cratos.platf.game.GameService.ROBOT_WSSEND;
import java.util.*;
import javax.persistence.*;
import org.redkale.convert.ConvertDisabled;
import org.redkale.service.RetResult;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 * @param <P> 玩家泛型
 */
public abstract class GameTable<P extends GamePlayer> extends BaseEntity implements Comparable<GameTable<P>> {

    //--------------- 房间状态 --------------------------------------
    public static final short TABLE_STATUS_READYING = 10; //准备中

    public static final short TABLE_STATUS_GAMEING = 30; //游戏中

    public static final short TABLE_STATUS_FINISHED = 50; //已结束

    //--------------- 房间结束类型 --------------------------------------
    public static final short TABLE_FNISHTYPE_NORMAL = 10;  //房间关闭类型; 正常结束;

    public static final short TABLE_FNISHTYPE_CLUBISMISS = 20;  //房间关闭类型; 亲友圈解散;

    public static final short TABLE_FNISHTYPE_APPLYDISMISS = 30;  //房间关闭类型; 玩家主动解散;

    public static final short TABLE_FNISHTYPE_HUMANDISMISS = 40;  //房间关闭类型; 人工强制解散;

    public static final short TABLE_FNISHTYPE_PLATFDISMISS = 50;  //房间关闭类型; 系统强制解散;

    //
    public static final short TABLE_CHARGETYPE_AA = 10;  //付费方式; AA付费;

    public static final short TABLE_CHARGETYPE_FZHU = 20;  //付费方式; 房主付费;

    public static final short TABLE_CHARGETYPE_CLUB = 30;  //付费方式; 亲友圈付费;

    //
    public static final short TABLE_JOINTYPE_AUTOJOIN = 10; //加入方式：自动加入

    public static final short TABLE_JOINTYPE_NOTJOIN = 20;//加入方式：不加入, 通常用于代理提前创建好房间

    @Id
    @Comment("牌桌唯一ID")
    protected String tableid = "";

    @Column(comment = "游戏ID")
    protected String gameid;

    @Column(comment = "场次，0表示无场次概念")
    protected int roomlevel;

    @Column(comment = "创建时间")
    protected long createtime;

    @Column(comment = "结束时间")
    protected long finishtime;

    @Transient
    @Comment("玩家们")
    protected P[] players;

    //是否随机坐
    protected boolean randomSite() {
        return false;
    }

    //玩家加入
    public RetResult<P> addPlayer(P player, GameService service) {
        synchronized (this) {
            if (this.players == null) return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_STATUS_ILLEGAL);
            if (!((player.getRoomlevel() == 1 || player.getRoomlevel() == 0) || (roomlevel == 1 || roomlevel == 0))) {
                if (player.getRoomlevel() != roomlevel) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL);
            }
            for (P one : this.players) {
                if (one != null && one.getUserid() == player.getUserid()) return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_JOIN_REPEAT);
            }
            int sitepos = player.getSitepos();
            if (sitepos > 0 && this.players.length >= sitepos && this.players[sitepos - 1] == null) {
                this.players[player.getSitepos() - 1] = player;
                player.table(this);
                player.setReadystatus(READYSTATUS_UNREADY);
                return new RetResult(player);
            } else if (randomSite()) {
                List<Integer> emptyIndexs = new ArrayList<>();
                for (int i = 0; i < this.players.length; i++) {
                    if (this.players[i] == null) {
                        emptyIndexs.add(i);
                    }
                }
                if (!emptyIndexs.isEmpty()) {
                    int index = emptyIndexs.get((int) (System.currentTimeMillis() % emptyIndexs.size()));
                    this.players[index] = player;
                    player.table(this);
                    player.setSitepos(index + 1);
                    player.setReadystatus(READYSTATUS_UNREADY);
                    return new RetResult(players[index]);
                }
            } else {
                for (int i = 0; i < this.players.length; i++) {
                    if (this.players[i] == null) {
                        this.players[i] = player;
                        player.table(this);
                        player.setSitepos(i + 1);
                        player.setReadystatus(READYSTATUS_UNREADY);
                        return new RetResult(player);
                    }
                }
            }
        }
        return GameRetCodes.retResult(GameRetCodes.RET_GAME_TABLE_PLAYERS_LIMIT);
    }

    //玩家离开
    public RetResult<Integer> removePlayer(int userid) {
        synchronized (this) {
            for (int i = 0; i < players.length; i++) {
                P player = players[i];
                if (player != null && player.getUserid() == userid) {
                    if (player.getReadystatus() == READYSTATUS_PLAYING) {
                        return GameRetCodes.retResult(GameRetCodes.RET_GAME_PLAYER_CANNOTLEAVE_GAMING);
                    }
                    player.table(null);
                    players[i] = null;
                    return RetResult.success();
                }
            }
        }
        return RetResult.success();
    }

    protected RetResult checkTableInReadyPlayer(final P player) {
        return null;
    }

    //玩家准备， 返回未准备的玩家的个数，为0表示都准备好了, 空位也要算进去
    public RetResult<Integer> readyPlayer(int userid) {
        final P player = findPlayer(userid);
        synchronized (this) {
            if (player == null) return GameRetCodes.retResult(RET_GAME_TABLE_PLAYER_NOTIN);
            if (player.getReadystatus() != READYSTATUS_UNREADY) return GameRetCodes.retResult(RET_GAME_PLAYER_READY_REPEAT);
            RetResult rs = checkTableInReadyPlayer(player);
            if (rs != null && !rs.isSuccess()) return rs.result(null);
            player.setReadystatus(READYSTATUS_READYED);
            P[] ps = this.players;
            if (ps == null) return new RetResult().result(-1);
            int count = 0;
            for (P p : ps) {
                if (p != null && p.getReadystatus() == READYSTATUS_READYED) count++;
            }
            int remain = ps.length - count;
//            if (remain == 0) {  
//                for (P p : ps) {
//                    if (p != null) p.setReadystatus(READYSTATUS_PLAYING); //不直接赋值会产生同步问题
//                }
//            }
            return new RetResult().result(remain);
        }
    }

    //获取玩家信息
    public P findPlayer(int userid) {
        for (P player : players) {
            if (player != null && player.getUserid() == userid) return player;
        }
        return null;
    }

    //玩家离线
    public P offlinePlayer(int userid) {
        P player = findPlayer(userid);
        if (player != null) player.offline();
        return player;
    }

    //获取在线非Robot玩家列表
    public Set<P> onlinePlayers(int... excludeids) {
        Set<P> list = new HashSet<>();
        for (P player : players) {
            if (player == null) continue;
            if (!ROBOT_WSSEND && UserInfo.isRobot(player.getUserid())) continue;
            if (!player.isOnline()) continue;
            if (excludeids.length < 1 || !Utility.contains(excludeids, player.getUserid())) {
                list.add(player);
            }
        }
        return list;
    }

    //人少排前面
    @Override
    public int compareTo(GameTable<P> o) {
        return this.getPlayerSize() - o.getPlayerSize();
    }

    //已准备的玩家数, 必须包含一个真实玩家
    @ConvertDisabled
    public int getReadyedPlayerSize() {
        if (this.players == null) return 0;
        int count = 0;
        for (P player : players) {
            if (player == null) continue;
            if (player.getReadystatus() == READYSTATUS_READYED) {
                count++;
            }
        }
        return count;
    }

    //是否全部已准备
    @ConvertDisabled
    public boolean isAllReadyed() {
        if (this.players == null) return false;
        for (P player : players) {
            if (player == null) continue;
            if (player.getReadystatus() != READYSTATUS_READYED) return false;
        }
        return true;
    }

    //是否玩家人数已满
    @ConvertDisabled
    public boolean isFull() {
        P[] array = this.players;
        if (array == null) return false;
        int count = 0;
        for (P player : array) {
            if (player != null) count++;
        }
        return count == array.length;
    }

    //是否玩家都已离开    
    @ConvertDisabled
    public boolean isEmpty() {
        if (this.players == null) return true;
        int count = 0;
        for (P player : players) {
            if (player != null) count++;
        }
        return count == 0;
    }

    //是否玩家都是机器人
    @ConvertDisabled
    public boolean isAllRobot() {
        for (P player : players) {
            if (player != null && !player.isRobot()) return false;
        }
        return true;
    }

    //是否玩家都已离开
    @ConvertDisabled
    public int getPlayerSize() {
        if (this.players == null) return 0;
        int count = 0;
        for (P player : players) {
            if (player != null) count++;
        }
        return count;
    }

    public P[] getPlayers() {
        return players;
    }

    public void setPlayers(P[] players) {
        this.players = players;
    }

    public String getTableid() {
        return tableid;
    }

    public void setTableid(String tableid) {
        this.tableid = tableid;
    }

    public String getGameid() {
        return gameid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getFinishtime() {
        return finishtime;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

}
