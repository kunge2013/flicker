/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.base;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.redkale.service.*;

/**
 * 6001xxxx 为 连环夺宝 的错误码定义
 * 6002xxxx 为 大话骰 的错误码定义
 * 6003xxxx 为 斗地主 的错误码定义
 * 6004xxxx 为 招财进宝 的错误码定义
 * 6005xxxx 为 牛牛 的错误码定义
 * 6006xxxx 为 捕鱼 的错误码定义
 *
 * @author zhangjx
 */
@SuppressWarnings("unchecked")
public abstract class RetCodes {

    //2000_0001 - 2999_9999 预留给 Redkale的扩展包redkalex使用
    //3000_0001 - 7999_9999 为平台系统使用
    //8000_0001 - 9999_9999 为OSS系统使用
    //------------------------------------- 通用模块 -----------------------------------------
    @RetLabel("参数无效")
    public static final int RET_PARAMS_ILLEGAL = 30010001;

    @RetLabel("无上传文件")
    public static final int RET_UPLOAD_NOFILE = 30010002;

    @RetLabel("上传文件过大")
    public static final int RET_UPLOAD_FILETOOBIG = 30010003;

    @RetLabel("上传文件不是图片")
    public static final int RET_UPLOAD_NOTIMAGE = 30010004;

    @RetLabel("文件写入失败")
    public static final int RET_FILE_WRITE_ERROR = 30010005;

    @RetLabel("系统内部异常")
    public static final int RET_INNER_ILLEGAL = 30010006;

    @RetLabel("调用远程接口异常")
    public static final int RET_REMOTE_ILLEGAL = 30010007;

    @RetLabel("调用远程接口超时")
    public static final int RET_REMOTE_TIMEOUT = 30010008;

    @RetLabel("重复操作")
    public static final int RET_REPEAT_ILLEGAL = 30010009;

    @RetLabel("不支持的操作")
    public static final int RET_SUPPORT_ILLEGAL = 30010010;

    @RetLabel("配置错误")
    public static final int RET_CONFIG_ILLEGAL = 30010011;

    //------------------------------------- 用户模块 -----------------------------------------
    @RetLabel("未登陆")
    public static final int RET_USER_UNLOGIN = 30020001;

    @RetLabel("用户登录失败")
    public static final int RET_USER_LOGIN_FAIL = 30020002;

    @RetLabel("用户或密码错误")
    public static final int RET_USER_ACCOUNT_PWD_ILLEGAL = 30020003;

    @RetLabel("密码设置无效")
    public static final int RET_USER_PASSWORD_ILLEGAL = 30020004;

    @RetLabel("用户被禁用")
    public static final int RET_USER_FREEZED = 30020005;

    @RetLabel("用户权限不够")
    public static final int RET_USER_AUTH_ILLEGAL = 30020006;

    @RetLabel("用户不存在")
    public static final int RET_USER_NOTEXISTS = 30020007;

    @RetLabel("用户状态异常")
    public static final int RET_USER_STATUS_ILLEGAL = 30020008;

    @RetLabel("用户注册参数无效")
    public static final int RET_USER_SIGNUP_ILLEGAL = 30020009;

    @RetLabel("用户性别参数无效")
    public static final int RET_USER_GENDER_ILLEGAL = 30020010;

    @RetLabel("用户名无效")
    public static final int RET_USER_USERNAME_ILLEGAL = 30020011;

    @RetLabel("用户账号无效")
    public static final int RET_USER_ACCOUNT_ILLEGAL = 30020012;

    @RetLabel("用户账号已存在")
    public static final int RET_USER_ACCOUNT_EXISTS = 30020013;

    @RetLabel("手机号码无效")
    public static final int RET_USER_MOBILE_ILLEGAL = 30020014;

    @RetLabel("手机号码已存在")
    public static final int RET_USER_MOBILE_EXISTS = 30020015;

    @RetLabel("手机验证码发送过于频繁")
    public static final int RET_USER_MOBILE_SMSFREQUENT = 30020016;

    @RetLabel("邮箱地址无效")
    public static final int RET_USER_EMAIL_ILLEGAL = 30020017;

    @RetLabel("邮箱地址已存在")
    public static final int RET_USER_EMAIL_EXISTS = 30020018;

    @RetLabel("微信绑定号无效")
    public static final int RET_USER_WXID_ILLEGAL = 30020019;

    @RetLabel("微信绑定号已存在")
    public static final int RET_USER_WXID_EXISTS = 30020020;

    @RetLabel("绑定微信号失败")
    public static final int RET_USER_WXID_BIND_FAIL = 30020021;

    @RetLabel("QQ绑定号无效")
    public static final int RET_USER_QQID_ILLEGAL = 30020022;

    @RetLabel("QQ绑定号已存在")
    public static final int RET_USER_QQID_EXISTS = 30020023;

    @RetLabel("绑定QQ号失败")
    public static final int RET_USER_QQID_BIND_FAIL = 30020024;

    @RetLabel("获取绑定QQ信息失败")
    public static final int RET_USER_QQID_INFO_FAIL = 30020025;

    @RetLabel("验证码无效")
    public static final int RET_USER_RANDCODE_ILLEGAL = 30020026; //邮件或者短信验证码

    @RetLabel("验证码已过期")
    public static final int RET_USER_RANDCODE_EXPIRED = 30020027; //邮件或者短信验证码

    @RetLabel("验证码错误或失效")
    public static final int RET_USER_CAPTCHA_ILLEGAL = 30020028; //图片验证码

    @RetLabel("用户类型无效")
    public static final int RET_USER_TYPE_ILLEGAL = 30020029;

    @RetLabel("用户设备ID无效")
    public static final int RET_USER_APPTOKEN_ILLEGAL = 30020030;

    @RetLabel("同一天不可重复签到")
    public static final int RET_USER_CHECKIN_EXISTS = 30020031;

    @RetLabel("父账号不存在")
    public static final int RET_USER_PARENT_ILLEGAL = 30020032;

    @RetLabel("手机号码所在运营商不存在")
    public static final int RET_USER_MOBILE_NONET = 30020033;

    @RetLabel("用户钻石不足")
    public static final int RET_USER_DOMAINDS_NOTENOUGH = 30020034;

    @RetLabel("用户金币不足")
    public static final int RET_USER_COINS_NOTENOUGH = 30020035;

    @RetLabel("用户等级不够")
    public static final int RET_USER_LEVEL_ILLEGAL = 30020036;

    @RetLabel("银行密码错误")
    public static final int RET_USER_BANKPWD_ILLEGAL = 30020037;

    @RetLabel("金币数错误")
    public static final int RET_COINS_ILLEGAL = 30020038;

    @RetLabel("用户已绑定在其他设备上了")
    public static final int RET_USER_APPTOKEN_BINDED = 30020039;

    @RetLabel("用户点卡使用达到上限")
    public static final int RET_USER_GOODS_USELIMIT = 30020040;

    @RetLabel("短信发送失败")
    public static final int RET_SMS_SEND_ERROR = 30020041;

    @RetLabel("同一设备注册次数达到上限")
    public static final int RET_USER_REG_APPSAME_LIMIT = 30020042;

    @RetLabel("已被限制登录或注册")
    public static final int RET_USER_LOGINORREG_LIMIT = 30020043;

    @RetLabel("城信APP绑定号已存在")
    public static final int RET_USER_CXID_EXISTS = 30020044;

    @RetLabel("禁止循环代理")
    public static final int RET_USER_AGENCY_REPEAT = 30020045;

    @RetLabel("没有可提取的奖励")
    public static final int RET_USER_PROFIT_MONEY_ILLEGAL = 30020046;

    @RetLabel("收款资料不全")
    public static final int RET_USER_PROFIT_CARD_ILLEGAL = 30020047;

    @RetLabel("还有尚未处理完的申请")
    public static final int RET_USER_TRADE_TRADING = 30020048;

    @RetLabel("记录已不存在")
    public static final int RET_USER_TRADE_NOT_EXISTS = 30020049;

    @RetLabel("记录已处理")
    public static final int RET_USER_TRADE_FINISHED = 30020050;

    @RetLabel("没有绑定推荐人")
    public static final int RET_USER_AGENCY_ILLEGAL = 30020051;

    @RetLabel("金额太小")
    public static final int RET_USER_MONEY_TOOSMALL = 30020052;

    @RetLabel("陪练角色不能取消")
    public static final int RET_USER_UNMATE_ILLEGAL = 30020053;

    @RetLabel("陪练角色无此操作权限")
    public static final int RET_USER_MATE_AUTHILLEGAL = 30020054;

    @RetLabel("重复签到")
    public static final int RET_DUTY_REPEAT = 30020055;

    @RetLabel("签到错误")
    public static final int RET_DUTY_ILLEGAL = 30020056;

    @RetLabel("用户奖券不足")
    public static final int RET_USER_COUPONS_NOTENOUGH = 30020057;

    @RetLabel("救济金已经领取过了")
    public static final int RET_BENEFIT_ALMS_GOTILLEGAL = 30020058;

    @RetLabel("用户领取救济金的条件不足")
    public static final int RET_BENEFIT_ALMS_OPTIONILLEGAL = 30020059;

    @RetLabel("用户无摇奖条件")
    public static final int RET_AWARD_NOEVENT = 30020060;

    @RetLabel("任务不存在")
    public static final int RET_MISSION_NOT_EXISTS = 30020061;

    @RetLabel("任务状态异常")
    public static final int RET_MISSION_STATUS_ILLEGAL = 30020062;

    @RetLabel("活跃度奖励不存在")
    public static final int RET_LIVENESSREWARD_NOT_EXISTS = 30020063;

    @RetLabel("活跃度奖励状态异常")
    public static final int RET_LIVENESSREWARD_ILLEGAL = 30020064;

    @RetLabel("邮件领取状态异常")
    public static final int RET_LETTER_STATUS_ILLEGAL = 30020065;

    @RetLabel("打卡报名次数已达到上限")
    public static final int RET_CROWD_APPLY_LIMIT_ILLEGAL = 30020066;

    @RetLabel("不在报名时间段")
    public static final int RET_CROWD_APPLY_TIME_ILLEGAL = 30020067;

    @RetLabel("不在打卡时间段")
    public static final int RET_CROWD_DAKA_TIME_ILLEGAL = 30020068;

    @RetLabel("数量不足")
    public static final int RET_COUNT_ILLEGAL = 30020069;

    @RetLabel("游客试玩时间已经到期")
    public static final int RET_USER_GUEST_PLAYTIME_LIMIT = 30020070;

    @RetLabel("您未满18周岁，今日试玩时间已满，请明日再玩！")
    public static final int RET_USER_UN18AGE_PLAYTIME_LIMIT = 30020071;

    @RetLabel("每日{0}时至次日{1}时，不为未成年人提供游戏服务")
    public static final int RET_USER_UN18AGE_PLAYRANGE_LIMIT = 30020072;

    //-------------------------------------订单模块-------------------------------------------------------
    @RetLabel("订单不存在")
    public static final int RET_ORDER_NOT_EXISTS = 40010001;

    @RetLabel("订单状态异常")
    public static final int RET_ORDER_STATUS_ILLEGAL = 40010002;

    @RetLabel("订单未支付")
    public static final int RET_ORDER_UNPAY = 40010003;

    @RetLabel("订单不属于当前用户")
    public static final int RET_ORDER_USERNOMATCH = 40010004;

    @RetLabel("商品价格已过期")
    public static final int RET_ORDER_GOODS_PRICEEXPIRED = 40010011;

    @RetLabel("商品已经下架")
    public static final int RET_ORDER_GOODS_EXPIRED = 40010012;

    @RetLabel("没有选择商品")
    public static final int RET_ORDER_GOODS_EMPTY = 40010013;

    @RetLabel("商品不存在")
    public static final int RET_ORDER_GOODS_NOTEXISTS = 40010014;

    @RetLabel("商品类型不正确")
    public static final int RET_ORDER_GOODS_TYPE_ILLEGAL = 40010015;

    @RetLabel("商品支付方式不正确")
    public static final int RET_ORDER_GOODS_PAYTYPEILLEGAL = 40010016;

    @RetLabel("商品分类下存在不可删除的子类")
    public static final int RET_GOODS_GROUP_CANNOTDELETE = 40010017;

    @RetLabel("点卡卡号无效或已被使用")
    public static final int RET_ORDER_GOODSCARD_ILLEGAL = 40010018;

    @RetLabel("支付渠道不正确")
    public static final int RET_ORDER_PAYCHANNELILLEGAL = 40010019;

    @RetLabel("订单清单不存在")
    public static final int RET_ORDER_THING_NOTEXISTS = 40010031;

    @RetLabel("订单与商品信息不符")
    public static final int RET_ORDER_THINGNOMATCH = 40010032;

    @RetLabel("订单支付状态异常")
    public static final int RET_ORDER_PAYSTATUS_ILLEGAL = 40010033;

    @RetLabel("订单支付信息不存在")
    public static final int RET_ORDER_PAYRECORD_NOTEXISTS = 40010034;

    @RetLabel("金币数不正确")
    public static final int RET_ORDER_COINS_ILLEGAL = 40010035;

    @RetLabel("收款人超出每日收款上限")
    public static final int RET_ORDER_COINS_BANKDAYLIMIT = 40010036;

    @RetLabel("无法进行转账")
    public static final int RET_ORDER_TRANSFER_ILLEGAL = 40010037;

    @RetLabel("转账金币数过低")
    public static final int RET_ORDER_TRANSFER_LESS = 40010038;

    @RetLabel("转账过于频繁")
    public static final int RET_ORDER_TRANSFER_FREQUENTLY = 40010039;

    @RetLabel("商品每天只能购买一次")
    public static final int RET_ORDER_BUY_DAYONCE = 40010040;

    @RetLabel("商品只能购买一次")
    public static final int RET_ORDER_BUY_ONCE = 40010041;

    @RetLabel("玩家未满8周岁，不能进行充值！")
    public static final int RET_ORDER_AGE_LESS08_ILLEGAL = 40010042;

    @RetLabel("玩家未满16周岁，单次充值金额不超过{0}元！")
    public static final int RET_ORDER_AGE_LESS16_ONE_ILLEGAL = 40010043;

    @RetLabel("玩家未满16周岁，每月充值金额累计不超过不超过{0}元！")
    public static final int RET_ORDER_AGE_LESS16_MONTH_ILLEGAL = 40010044;

    @RetLabel("玩家未满18周岁，单次充值金额不超过{0}元！")
    public static final int RET_ORDER_AGE_LESS18_ONE_ILLEGAL = 40010045;

    @RetLabel("玩家未满18周岁，每月充值金额累计不超过不超过{0}元！")
    public static final int RET_ORDER_AGE_LESS18_MONTH_ILLEGAL = 40010046;

    @RetLabel("皮肤已存在")
    public static final int RET_ORDER_SKIN_REPEAT = 40010061;

    //-------------------------------------充值模块-------------------------------------------------------
    @RetLabel("充值套餐不存在")
    public static final int RET_CHARGEPKG_NOTEXISTS = 40020001;

    @RetLabel("充值套餐细则不存在")
    public static final int RET_CHARGEPKG_ITEMS_NOTEXISTS = 40020002;

    //-------------------------------------公会模块-------------------------------------------------------
    @RetLabel("亲友圈不存在")
    public static final int RET_CLUB_NOTEXISTS = 40030001;

    @RetLabel("亲友圈状态异常")
    public static final int RET_CLUB_STATUS_ILLEGAL = 40030002;

    @RetLabel("亲友圈操作权限不够")
    public static final int RET_CLUB_AUTH_ILLEGAL = 40030003;

    @RetLabel("用户创建亲友圈数量达到上限")
    public static final int RET_CLUB_CREATE_LIMIT = 40030004;

    @RetLabel("亲友圈的名称重复或含非法字")
    public static final int RET_CLUB_NAME_ILLEGAL = 40030005;

    @RetLabel("玩家已在此亲友圈中")
    public static final int RET_CLUB_PLAYER_EXISTS = 40030006;

    @RetLabel("玩家不在此亲友圈中")
    public static final int RET_CLUB_PLAYER_NOTEXISTS = 40030007;

    @RetLabel("正在申请中")
    public static final int RET_CLUB_ACTION_APPLYING = 40030008;

    @RetLabel("亲友圈人数已满")
    public static final int RET_CLUB_PLAYER_FULL = 40030009;

    @RetLabel("亲友圈ID与房间号不匹配")
    public static final int RET_CLUB_TABLENO_ILLEGAL = 40030010;

    @RetLabel("你不是亲友圈【{0}】的成员")
    public static final int RET_CLUB_PLAYER_NOTMEMBER = 40030011;

    @RetLabel("亲友圈存在未解散的房间")
    public static final int RET_CLUB_HAD_TABLES = 40030012;

    //-------------------------------------公会模块-------------------------------------------------------
    @RetLabel("公会不存在")
    public static final int RET_CLAN_NOTEXISTS = 40040001;

    @RetLabel("公会名称不正确")
    public static final int RET_CLAN_NAME_ILLEGAL = 40040002;

    @RetLabel("用户已加入了其他公会")
    public static final int RET_CLAN_USER_HADCLAN = 40040003;

    @RetLabel("用户已申请加入了其他公会")
    public static final int RET_CLAN_USER_HADAPPLY = 40040004;

    @RetLabel("公会成员已满")
    public static final int RET_CLAN_USER_FULL = 40040005;

    @RetLabel("公会禁止加入")
    public static final int RET_CLAN_USER_APPLYFORBID = 40040006;

    @RetLabel("用户没有加入任何公会")
    public static final int RET_CLAN_USER_NOCLAN = 40040007;

    @RetLabel("用户没有申请加入公会")
    public static final int RET_CLAN_USER_NOAPPLY = 40040008;

    @RetLabel("用户申请加入状态异常")
    public static final int RET_CLAN_USER_APPLYILLEGAL = 40040009;

    //-----------------------------------------------------------------------------------------------------------
    protected static final Map<String, Map<Integer, String>> rets = RetLabel.RetLoader.loadMap(RetCodes.class);

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
