/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.redkale.service.*;

/**
 * 6000xxxx 为通用游戏的错误码定义：（与部署端口类似）
 * 6001xxxx：斗地主
 * 6002xxxx：牛牛
 *
 *
 * 6100xxxx：通用麻将
 *
 * @author zhangjx
 */
@SuppressWarnings("unchecked")
public abstract class GameRetCodes {

    //2000_0001 - 2999_9999 预留给 Redkale的扩展包redkalex使用
    //3000_0001 - 7999_9999 为平台系统使用
    //8000_0001 - 9999_9999 为OSS系统使用
    //------------------------------------- 通用游戏模块 -----------------------------------------
    @RetLabel("游戏房间不存在")
    public static final int RET_GAME_TABLE_NOTEXISTS = 6000_0010;

    @RetLabel("房间状态不正确")
    public static final int RET_GAME_TABLE_STATUS_ILLEGAL = 6000_0011;

    @RetLabel("游戏已经开始了")
    public static final int RET_GAME_TABLE_STATUS_PLAYING = 6000_0012;

    @RetLabel("当前房间不是可押注状态")
    public static final int RET_GAME_TABLE_STATUS_NOBETTING = 6000_0013;

    @RetLabel("房间位置不正确")
    public static final int RET_GAME_TABLE_SITPOS_ILLEGAL = 6000_0014;

    @RetLabel("房间位置已有玩家")
    public static final int RET_GAME_TABLE_SITPOS_HADPLAYER = 6000_0015;

    @RetLabel("其他玩家已当庄家")
    public static final int RET_GAME_TABLE_BANKER_EXISTS = 6000_0016;

    @RetLabel("房间玩家人数已满")
    public static final int RET_GAME_TABLE_PLAYERS_LIMIT = 6000_0017;

    @RetLabel("玩家不在房间内")
    public static final int RET_GAME_TABLE_PLAYER_NOTIN = 6000_0018;

    @RetLabel("用户金币数达不到当庄要求{0}")
    public static final int RET_GAME_TABLE_BANKER_COINSLESS = 6000_0020;

    @RetLabel("押注位置错误")
    public static final int RET_GAME_TABLE_BETPOS_ILLEGAL = 6000_0021;

    @RetLabel("押注金币总数达到庄家上限")
    public static final int RET_GAME_TABLE_COINGREAT_BANKER = 6000_0022;

    @RetLabel("玩家金币不满足房间要求")
    public static final int RET_GAME_TABLE_COINRANGE_ILLEGAL = 6000_0023;

    @RetLabel("线条值不正确")
    public static final int RET_GAME_TABLE_LINENUM_ILLEGAL = 6000_0024;

    @RetLabel("房间自动解散")
    public static final int RET_GAME_TABLE_AUTODISMISS = 6000_0025;

    @RetLabel("游戏服务关闭")
    public static final int RET_GAME_SHUTDOWN = 6000_0026;

    @RetLabel("比赛场尚未开始")
    public static final int RET_GAME_SPORT_UNSTART = 6000_0027;

    //----------------------------
    @RetLabel("玩家状态不正确")
    public static final int RET_GAME_PLAYER_STATUS_ILLEGAL = 6000_0041;

    @RetLabel("玩家长时间未操作")
    public static final int RET_GAME_PLAYER_DONOTHING = 6000_0042;

    @RetLabel("游戏正在进行中，不能离开")
    public static final int RET_GAME_PLAYER_CANNOTLEAVE_GAMING = 6000_0043;

    @RetLabel("玩家重复加入")
    public static final int RET_GAME_PLAYER_JOIN_REPEAT = 6000_0044;

    @RetLabel("玩家钻石不正确")
    public static final int RET_GAME_PLAYER_DOMAINDS_ILLEGAL = 6000_0045;

    @RetLabel("无空闲座位")
    public static final int RET_GAME_PLAYER_SITDOWN_NOPOS = 6000_0046;

    @RetLabel("玩家重复准备")
    public static final int RET_GAME_PLAYER_READY_REPEAT = 6000_0047;

    @RetLabel("玩家准备人数不足")
    public static final int RET_GAME_PLAYER_READY_LESS = 6000_0048;

    //
    @RetLabel("玩家押注达到上限")
    public static final int RET_GAME_PLAYER_BETTING_LIMIT = 6000_0051;

    @RetLabel("玩家押注选项不正确")
    public static final int RET_GAME_PLAYER_BETITEM_ILLEGAL = 6000_0052;

    @RetLabel("押注金币数不正确")
    public static final int RET_GAME_PLAYER_BETCOINS_ILLEGAL = 6000_0053;

    @RetLabel("玩家不满足抽奖条件")
    public static final int RET_GAME_PLAYER_CANNOTCJ = 6000_0054;

    @RetLabel("玩家进入的场次不对")
    public static final int RET_GAME_PLAYER_ROOMLEVEL_ILLEGAL = 6000_0055;

    @RetLabel("玩家三叉戟能量不足")
    public static final int RET_GAME_PLAYER_FISH_NOENERGY = 6000_0056;

    @RetLabel("玩家金币数不足")
    public static final int RET_GAME_PLAYER_COINS_NOTENOUGH = 6000_0057;

    @RetLabel("当前用户不能免费旋转")
    public static final int RET_GAME_PLAYER__FREE_ILLEGAL = 6000_0058;

    @RetLabel("当前用户不能玩骰子")
    public static final int RET_GAME_PLAYER__DICE_ILLEGAL = 6000_0059;

    @RetLabel("玩家重复报名")
    public static final int RET_GAME_PLAYER_APPLY_REPEAT = 6000_0060;

    @RetLabel("玩家尚未报名比赛场")
    public static final int RET_GAME_PLAYER_SPORT_UNAPPLY = 6000_0061;

    @RetLabel("玩家已参与过此次比赛")
    public static final int RET_GAME_PLAYER_SPORT_PLAYED = 6000_0062;

    //----------------------------
    @RetLabel("玩家无此操作")
    public static final int RET_GAME_ACTEVENT_ILLEGAL = 6000_0061;

    @RetLabel("玩家尚有其他操作没完成")
    public static final int RET_GAME_ACTEVENT_STEPILLEGAL = 6000_0062;

    @RetLabel("玩家操作超时")
    public static final int RET_GAME_ACTEVENT_TIMEOUT = 6000_0063;

    @RetLabel("操作执行发生异常")
    public static final int RET_GAME_ACTEVENT_ERROR = 6000_0064;

    @RetLabel("玩家重复操作")
    public static final int RET_GAME_ACTREPEAT_ILLEGAL = 6000_0065;

    //----------------------------
    @RetLabel("CD时间内不能操作")
    public static final int RET_GAME_CDTIME_ILLEGAL = 6000_0066;

    //----------------------------
    @RetLabel("记录不存在或已过期")
    public static final int RET_GAME_RECORD_ILLEGAL = 6000_0071;

    //----------------------------
    @RetLabel("押注金币数错误")
    public static final int RET_GAME_BETCOINS_ILLEGAL = 6000_0081;

    //-----------------------------------------------------------------------------------------------------------
    protected static final Map<String, Map<Integer, String>> rets = RetLabel.RetLoader.loadMap(GameRetCodes.class);

    protected static final Map<Integer, String> defret = rets.get("");

    public static <T> RetResult<T> retResult(int retcode) {
        if (retcode == 0) return RetResult.success();
        return new RetResult(retcode, retInfo(retcode));
    }

    public static <T> RetResult<T> retResult(String locale, int retcode) {
        if (retcode == 0) return RetResult.success();
        return new RetResult(retcode, retInfo(locale, retcode));
    }

    public static <T> RetResult<T> retResult(int retcode, Object... args) {
        if (retcode == 0) return RetResult.success();
        if (args == null || args.length < 1) return new RetResult(retcode, retInfo(retcode));
        String info = MessageFormat.format(retInfo(retcode), args);
        return new RetResult(retcode, info);
    }

    public static <T> RetResult<T> retResult(String locale, int retcode, Object... args) {
        if (retcode == 0) return RetResult.success();
        if (args == null || args.length < 1) return new RetResult(retcode, retInfo(locale, retcode));
        String info = MessageFormat.format(retInfo(locale, retcode), args);
        return new RetResult(retcode, info);
    }

    public static <T> CompletableFuture<RetResult<T>> retResultFuture(int retcode) {
        return CompletableFuture.completedFuture(retResult(retcode));
    }

    public static <T> CompletableFuture<RetResult<T>> retResultFuture(String locale, int retcode) {
        return CompletableFuture.completedFuture(retResult(locale, retcode));
    }

    public static <T> CompletableFuture<RetResult<T>> retResultFuture(int retcode, Object... args) {
        return CompletableFuture.completedFuture(retResult(retcode, args));
    }

    public static <T> CompletableFuture<RetResult<T>> retResultFuture(String locale, int retcode, Object... args) {
        return CompletableFuture.completedFuture(retResult(locale, retcode, args));
    }

    public static RetResult set(RetResult result, int retcode, Object... args) {
        if (retcode == 0) return result.retcode(0).retinfo("");
        if (args == null || args.length < 1) return result.retcode(retcode).retinfo(retInfo(retcode));
        String info = MessageFormat.format(retInfo(retcode), args);
        return result.retcode(retcode).retinfo(info);
    }

    public static RetResult set(RetResult result, String locale, int retcode, Object... args) {
        if (retcode == 0) return result.retcode(0).retinfo("");
        if (args == null || args.length < 1) return result.retcode(retcode).retinfo(retInfo(locale, retcode));
        String info = MessageFormat.format(retInfo(locale, retcode), args);
        return result.retcode(retcode).retinfo(info);
    }

    public static String retInfo(int retcode) {
        if (retcode == 0) return "Success";
        return defret.getOrDefault(retcode, "Error");
    }

    public static String retInfo(String locale, int retcode) {
        if (locale == null || locale.isEmpty()) return retInfo(retcode);
        if (retcode == 0) return "Success";
        String key = locale == null ? "" : locale;
        Map<Integer, String> map = rets.get(key);
        if (map == null) return "Error";
        return map.getOrDefault(retcode, "Error");
    }
}
