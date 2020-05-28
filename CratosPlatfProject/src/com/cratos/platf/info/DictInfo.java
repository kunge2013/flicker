package com.cratos.platf.info;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.json.*;
import org.redkale.util.*;

/**
 *
 * GAME_XXX_ANNOUNCE_MINCOINS 表示游戏玩家赢得超过指定金币数就推送滚动公告，XXX为游戏标识
 *
 * GAME_XXX_ANNOUNCE_MINFACTOR 表示游戏玩家赢得超过指定赔率就推送滚动公告，XXX为游戏标识
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Cacheable(interval = 60)
@Table(comment = "全局配置表")
public class DictInfo extends BaseEntity {

    @Comment("防沉迷.是否开启限制; 10:开启;20:关闭;")
    public static final String PLATF_INDULGE_ACTIVATE = "PLATF_INDULGE_ACTIVATE";

    @Comment("防沉迷.游客试玩限时长秒数，0表示无限制")
    public static final String PLATF_INDULGE_GUEST_TRYPLAY_SECONDS = "PLATF_INDULGE_GUEST_TRYPLAY_SECONDS";

    @Comment("防沉迷.未成年人每日游戏限时长秒数，0表示无限制")
    public static final String PLATF_INDULGE_UN18AGE_DAYPLAY_SECONDS = "PLATF_INDULGE_UN18AGE_DAYPLAY_SECONDS";

    @Comment("防沉迷.可充值的最小年纪，0表示无限制")
    public static final String PLATF_INDULGE_PAY_MINAGE = "PLATF_INDULGE_PAY_MINAGE";

    @Comment("防沉迷.小于16岁单次充值的上限金额，0表示无限制")
    public static final String PLATF_INDULGE_PAY_16ONCE_MAXMONEY = "PLATF_INDULGE_PAY_16ONCE_MAXMONEY";

    @Comment("防沉迷.小于16岁每月充值的上限金额，0表示无限制")
    public static final String PLATF_INDULGE_PAY_16MONTH_MAXMONEY = "PLATF_INDULGE_PAY_16MONTH_MAXMONEY";

    @Comment("防沉迷.小于18岁单次充值的上限金额，0表示无限制")
    public static final String PLATF_INDULGE_PAY_18ONCE_MAXMONEY = "PLATF_INDULGE_PAY_18ONCE_MAXMONEY";

    @Comment("防沉迷.小于18岁每月充值的上限金额，0表示无限制")
    public static final String PLATF_INDULGE_PAY_18MONTH_MAXMONEY = "PLATF_INDULGE_PAY_18MONTH_MAXMONEY";

    @Comment("防沉迷.未成年人小于这个时间点不得玩游戏，0表示无限制")
    public static final String PLATF_INDULGE_UN18AGE_MINMILLS = "PLATF_INDULGE_UN18AGE_MINMILLS";

    @Comment("防沉迷.未成年人大于这个时间点不得玩游戏，0表示无限制")
    public static final String PLATF_INDULGE_UN18AGE_MAXMILLS = "PLATF_INDULGE_UN18AGE_MAXMILLS";

    @Comment("syncRemoteGameModule方法中必须同步的gameid，多个用;隔开")
    public static final String PLATF_USER_MUSTSYNC_GAMEIDS = "PLATF_USER_MUSTSYNC_GAMEIDS";

    @Comment("每人每日报名的次数")
    public static final String PLATF_CROWD_APPLYLIMIT = "PLATF_CROWD_APPLYLIMIT";

    @Comment("报名的起始金币数")
    public static final String PLATF_CROWD_APPLYSTARTCOIN = "PLATF_CROWD_APPLYSTARTCOIN";

    @Comment("报名增幅金币数")
    public static final String PLATF_CROWD_APPLYINCRECOIN = "PLATF_CROWD_APPLYINCRECOIN";

    @Comment("打卡起始时间，从凌晨到时间点的毫秒数")
    public static final String PLATF_CROWD_DAKASTART_MILLS = "PLATF_CROWD_DAKASTART_MILLS";

    @Comment("打卡结束时间，从凌晨到时间点的毫秒数")
    public static final String PLATF_CROWD_DAKAEND_MILLS = "PLATF_CROWD_DAKAEND_MILLS";

    @Comment("打卡活动起始时间戳")
    public static final String PLATF_CROWD_STARTTIME = "PLATF_CROWD_STARTTIME";

    @Comment("打卡活动结束时间戳")
    public static final String PLATF_CROWD_ENDTIME = "PLATF_CROWD_ENDTIME";

    //----------------------------------------------------------
    @Comment("普通用户收款人手续费的千分率, 20表示2%")
    public static final String PLATF_BANK_TAXPERMILLAGE = "PLATF_BANK_TOUSER_TAXPERMILLAGE";

    @Comment("游戏名称集合")
    public static final String PLATF_APP_GAMENAMES = "PLATF_APP_GAMENAMES";

    @Comment("白银盘最低积分要求")
    public static final String PLATF_AWARD_SCORE_LEVEL_1 = "PLATF_AWARD_SCORE_LEVEL_1";

    @Comment("黄金盘最低积分要求")
    public static final String PLATF_AWARD_SCORE_LEVEL_2 = "PLATF_AWARD_SCORE_LEVEL_2";

    @Comment("钻石盘最低积分要求")
    public static final String PLATF_AWARD_SCORE_LEVEL_3 = "PLATF_AWARD_SCORE_LEVEL_3";

    @Comment("普通用户单笔最小金币数")
    public static final String PLATF_BANK_TOUSER_MINCOINS = "PLATF_BANK_TOUSER_MINCOINS";

    @Comment("用户每日领取救济金的次数上限")
    public static final String PLATF_BENEFIT_ALMS_DAY_COUNT = "PLATF_BENEFIT_ALMS_DAY_COUNT";

    @Comment("用户每日领取救济金的金币数")
    public static final String PLATF_BENEFIT_ALMS_DAY_GETCOIN = "PLATF_BENEFIT_ALMS_DAY_GETCOIN";

    @Comment("用户每日少于多少金币数才可领取救济金")
    public static final String PLATF_BENEFIT_ALMS_DAY_LESSCOIN = "PLATF_BENEFIT_ALMS_DAY_LESSCOIN";

    @Comment("用户创建亲友圈所需的钻石数")
    public static final String PLATF_CLUB_CREATE_MIN_DIAMOND = "PLATF_CLUB_CREATE_MIN_DIAMOND";

    @Comment("用户创建亲友圈最大数量")
    public static final String PLATF_CLUB_CREATE_MAX_COUNT = "PLATF_CLUB_CREATE_MAX_COUNT";

    @Comment("用户注册赠送金币数")
    public static final String PLATF_USER_REG_GIFT_COIN = "PLATF_USER_REG_GIFT_COIN";

    @Comment("用户注册赠送钻石数")
    public static final String PLATF_USER_REG_GIFT_DIAMOND = "PLATF_USER_REG_GIFT_DIAMOND";

    @Comment("APP的苹果审批版本，为此版本的APP不显示商城和充值")
    public static final String PLATF_APP_IOS_AUDIT_VERSION = "PLATF_APP_IOS_AUDIT_VERSION";

    @Comment("游戏非动态请求的域名, 不能是/结尾， 必须带上http://和端口")
    public static final String PLATF_APP_HOST = "PLATF_APP_HOST";

    @Comment("游戏动态请求的域名, 不能是/结尾， 必须带上http://和端口")
    public static final String PLATF_API_HOST = "PLATF_API_HOST";

    @Comment("联系在线客服")
    public static final String PLATF_KEFU_URL = "PLATF_KEFU_URL";

    @Comment("联系客服QQ")
    public static final String PLATF_KEFU_QQ = "PLATF_KEFU_QQ";

    @Comment("联系客服微信")
    public static final String PLATF_KEFU_WX = "PLATF_KEFU_WX";

    @Comment("联系客服电话")
    public static final String PLATF_KEFU_TEL = "PLATF_KEFU_TEL";

    @Comment("在线客服网址")
    public static final String PLATF_IMPAY_URL = "PLATF_IMPAY_URL";

    @Comment("签到连续性类型; 10:连续签到(前一天没签到则从新开始);20:累计签到;")
    public static final String PLATF_DUTY_TYPE = "PLATF_DUTY_TYPE";

    @Comment("系统收款人的银行卡号")
    public static final String PLATF_ORDERTRADE_BANKACCOUNT = "PLATF_ORDERTRADE_BANKACCOUNT";

    @Comment("系统收款人的姓名")
    public static final String PLATF_ORDERTRADE_BANKUSER = "PLATF_ORDERTRADE_BANKUSER";

    @Comment("系统收款人的银行名称")
    public static final String PLATF_ORDERTRADE_BANKNAME = "PLATF_ORDERTRADE_BANKNAME";

    @Comment("系统支持的支付类型;定额微信: dewxpay;  定额支付宝: dealipay;  微信充值: iewxpay;  支付宝充值: iealipay;  云闪付: ieysfpay;  银行卡充值:iebankpay;  专享闪付:ievippay;")
    public static final String PLATF_PAY_SUBTYPES = "PLATF_PAY_SUBTYPES";

    @Comment("【后台不可编辑】,支付渠道全量选项，仅供PLATF_PAY_XXXPAY_CHANNELS设置使用")
    public static final String PLATF_PAY_ALLCHANNELS = "PLATF_PAY_ALLCHANNELS";

    @Comment("【后台不可编辑】,支付金额全量选项，仅供PLATF_PAY_XXXPAY_ITEMS设置使用")
    public static final String PLATF_PAY_ALLITEMS = "PLATF_PAY_ALLITEMS";

    @Comment("【后台不可编辑】,支付类型全量选项，仅供PLATF_PAY_SUBTYPES设置使用")
    public static final String PLATF_PAY_ALLSUBTYPES = "PLATF_PAY_ALLSUBTYPES";

    @Comment("专享闪付可支持的支付方式列表;微信支付:wxpay;支付宝:alipay;银行卡:bankpay;")
    public static final String PLATF_PAY_IEVIPPAY_TYPES = "PLATF_PAY_IEVIPPAY_TYPES";

    @Comment("定额支付宝支付的渠道设置， 值为json，key=支付方式ID， value=权重")
    public static final String PLATF_PAY_DEALIPAY_CHANNELS = "PLATF_PAY_DEALIPAY_CHANNELS";

    @Comment("定额支付宝支付金额选项，多个值用;分隔， 最多8个")
    public static final String PLATF_PAY_DEALIPAY_ITEMS = "PLATF_PAY_DEALIPAY_ITEMS";

    @Comment("定额微信支付的渠道设置， 值为json，key=支付方式ID， value=权重")
    public static final String PLATF_PAY_DEWXPAY_CHANNELS = "PLATF_PAY_DEWXPAY_CHANNELS";

    @Comment("定额微信支付金额选项，多个值用;分隔， 最多8个")
    public static final String PLATF_PAY_DEWXPAY_ITEMS = "PLATF_PAY_DEWXPAY_ITEMS";

    @Comment("支付宝支付的渠道设置， 值为json，key=支付方式ID， value=权重")
    public static final String PLATF_PAY_IEALIPAY_CHANNELS = "PLATF_PAY_IEALIPAY_CHANNELS";

    @Comment("支付宝支付金额选项，多个值用;分隔， 最多8个")
    public static final String PLATF_PAY_IEALIPAY_ITEMS = "PLATF_PAY_IEALIPAY_ITEMS";

    @Comment("银行卡支付的渠道设置， 值为json，key=支付方式ID， value=权重")
    public static final String PLATF_PAY_IEBANKPAY_CHANNELS = "PLATF_PAY_IEBANKPAY_CHANNELS";

    @Comment("银行卡支付金额选项，多个值用;分隔， 最多8个")
    public static final String PLATF_PAY_IEBANKPAY_ITEMS = "PLATF_PAY_IEBANKPAY_ITEMS";

    @Comment("微信充值的渠道设置， 值为json，key=支付方式ID， value=权重")
    public static final String PLATF_PAY_IEWXPAY_CHANNELS = "PLATF_PAY_IEWXPAY_CHANNELS";

    @Comment("微信充值金额选项，多个值用;分隔， 最多8个")
    public static final String PLATF_PAY_IEWXPAY_ITEMS = "PLATF_PAY_IEWXPAY_ITEMS";

    @Comment("云闪付支付的渠道设置， 值为json，key=支付方式ID， value=权重")
    public static final String PLATF_PAY_IEYSFPAY_CHANNELS = "PLATF_PAY_IEYSFPAY_CHANNELS";

    @Comment("云闪付支付金额选项，多个值用;分隔， 最多8个")
    public static final String PLATF_PAY_IEYSFPAY_ITEMS = "PLATF_PAY_IEYSFPAY_ITEMS";

    @Id
    @Column(length = 32, comment = "KEY, 大写 ")
    private String keyname = "";

    @Column(comment = "KEY的数值")
    private long numvalue;

    @Column(length = 2048, comment = "KEY的字符串值")
    private String strvalue = "";

    @Column(updatable = false, length = 255, comment = "KEY描述")
    private String keydesc = "";

    @Column(comment = "更新时间")
    private long updatetime;

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    public String getKeyname() {
        return this.keyname;
    }

    public void setNumvalue(long numvalue) {
        this.numvalue = numvalue;
    }

    public long getNumvalue() {
        return this.numvalue;
    }

    public String getStrvalue() {
        return strvalue;
    }

    public void setStrvalue(String strvalue) {
        this.strvalue = strvalue;
    }

    public void setKeydesc(String keydesc) {
        this.keydesc = keydesc;
    }

    public String getKeydesc() {
        return this.keydesc;
    }

    public void setUpdatetime(long updatetime) {
        this.updatetime = updatetime;
    }

    public long getUpdatetime() {
        return this.updatetime;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }
}
