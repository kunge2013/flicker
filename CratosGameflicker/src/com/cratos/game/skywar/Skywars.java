/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.util.*;
import org.redkale.util.Comment;

/**
 * 扑克面值 <br>
 * 2--10 J Q K A<br>
 * 2--10 11 12 13 14<br>
 * 黑桃--6 红桃--5 梅花--4 方块--3      <br>
 * 例如： 红桃3：503 黑桃A：614        <br>
 *
 * @author zhangjx
 */
public abstract class Skywars {

    //道具-火箭头
    public static final int PROPID_ROCKET = 601;

    //道具-导弹
    public static final int PROPID_MISSILE = 602;

    //道具-核弹
    public static final int PROPID_NUCLEAR = 603;

    //道具-百宝箱
    public static final int PROPID_BAOBOX = 608;

    //道具-巡航
    public static final int PROPID_RADAR = 611;

    //道具-僚机
    public static final int PROPID_WING = 612;

    //道具-狂暴
    public static final int PROPID_FRENZY = 613;

    //道具-追踪
    public static final int PROPID_TRACK = 614;

    //道具-传送
    public static final int PROPID_TRANSMIT = 615;

    //特殊敌机
    public static final short KIND_TYPE_SPECIAL = 8;

    //可赠送的道具
    public static final int[] GIVABLE_PROPIDS = {PROPID_NUCLEAR, PROPID_RADAR, PROPID_WING, PROPID_FRENZY, PROPID_TRACK, PROPID_TRANSMIT};

    //底注的选项
    public static final int[] BASECOIN_ITEMS = {1, 100, 1000, 100, 1000};

    public static final long WING_PERIOD_MILLS = 30 * 60 * 1000L;

    public static final int[][] SHOT_LEVELS = new int[][]{
        {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100},
        {100, 150, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800, 900, 1000},
        {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 6000, 7000, 8000, 9000, 10000},
        {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000},
        {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000}
    };

    public static final float[] PROP_UFO_FACTOR_ITEMS = {1f, 1.2f, 1.5f, 1.8f, 2f, 2.2f, 2.5f, 2.8f, 3f};

    public static final int[] PROP_UFO_FACTOR_WEIGHTS = Utils.calcIndexWeights(new int[]{30, 28, 25, 22, 20, 18, 15, 12, 10});

    public static final int[] PROP_UFO_COIN_ITEMS = {200, 100, 50, 1000, 350, 500, 250, 600, 300, 150, 800, 400};

    public static final int[] PROP_UFO_COIN_WEIGHTS = Utils.calcIndexWeights(new int[]{9, 11, 12, 1, 6, 4, 8, 3, 7, 10, 2, 5});

    @Comment("牌型：豹子")
    public static final int CARDTYPE_BAOZI = 6;   //500

    @Comment("牌型：顺金")
    public static final int CARDTYPE_SHUNJIN = 5;  //350

    @Comment("牌型：同花")
    public static final int CARDTYPE_TONGHUA = 4; //250

    @Comment("牌型：顺子")
    public static final int CARDTYPE_SHUNZI = 3;  //180

    @Comment("牌型：对子")
    public static final int CARDTYPE_DUIZI = 2; //110

    @Comment("牌型：单牌")
    public static final int CARDTYPE_DANPAI = 1; //40

    @Comment("牌型倍数")
    public static final int[] CARDTYPE_FACTORS = {1, 40, 110, 180, 250, 350, 500};

    /**
     *
     * 扑克面值 <br>
     * 2--10 J Q K A<br>
     * 2--10 11 12 13 14<br>
     * 黑桃--6 红桃--5 梅花--4 方块--3      <br>
     * 例如： 红桃3：503 黑桃A：614        <br>
     *
     * @param card1
     * @param card2
     * @param card3
     *
     * @return int
     */
    public static int getCardType(int card1, int card2, int card3) {
        final int max = card1 % 100 > card2 % 100 ? (card3 % 100 > card1 % 100 ? card3 : card1) : ((card3 % 100 > card2 % 100 ? card3 : card2));
        final int min = card1 % 100 < card2 % 100 ? (card3 % 100 < card1 % 100 ? card3 : card1) : ((card3 % 100 < card2 % 100 ? card3 : card2));
        final int mid = (card1 == max || card1 == min) ? ((card2 == max || card2 == min) ? card3 : card2) : card1;
        if (max % 100 == mid % 100 && max % 100 == min % 100) return CARDTYPE_BAOZI;
        if (max / 100 == mid / 100 && max / 100 == min / 100) {
            if (max % 100 == mid % 100 + 1 && mid % 100 == min % 100 + 1) return CARDTYPE_SHUNJIN;
            if (max % 100 == 14 && mid % 100 == 3 && min % 100 == 2) return CARDTYPE_SHUNJIN;  //32A
            return CARDTYPE_TONGHUA;
        }
        if (max % 100 == mid % 100 + 1 && mid % 100 == min % 100 + 1) return CARDTYPE_SHUNZI;
        if (max % 100 == 14 && mid % 100 == 3 && min % 100 == 2) return CARDTYPE_SHUNZI;  //32A

        if (max % 100 == mid % 100 || mid % 100 == min % 100 || min % 100 == max % 100) return CARDTYPE_DUIZI;
        return CARDTYPE_DANPAI;
    }

    //不带大小王
    public static int[] random52Pokers() {
        int[] cards = new int[52];
        int index = 0;
        for (int i = 3; i <= 6; i++) {
            for (int j = 2; j <= 14; j++) {
                cards[index++] = i * 100 + j;
            }
        }
        return ShuffleRandom.shuffle(cards);
    }

    /**
     * <blockquote><pre>
     *
     * 等级       晶石  奖励
     * 10          10    1000
     * 15          11    1100     话费
     * 20          12    1200
     * 25          13    1300
     * 30          14    1400
     * 35          15    1500     新手初级宝箱
     * 40          16    1600
     * 45          17    1700
     * 50          18    1800     新手中级宝箱
     * 60          19    1900
     * 70          20    2000     新手高级宝箱
     * 80          21    2100
     * 90          22    2200
     *
     * 100         30    3000
     * 150         35    3200
     * 200         40    3400
     * 250         45    3600
     * 300         50    3800
     * 350         55    4000
     * 400         60    4200
     * 450         65    4400
     * 500         70    4600
     * 600         75    4800
     * 700         80    5000
     * 800         85    5200
     * 900         90    5400
     *
     * 1000        100   6000
     * 1500        200   6400
     *
     *
     *
     *
     * </pre></blockquote>
     *
     * @param firelevel
     *
     * @return int
     */
    public static int nextFirelevel(int firelevel) {
        int factor = 1;
        int level = firelevel;
        while (firelevel % Math.max(10, factor) == 0 && level / Math.max(10, factor) >= 10) {
            factor *= 10;
            level = level / 10;
        }
        level += (level < 50) ? 5 : 10;
        return level * factor;
    }

    public static final long fireLevelAwardCoin(int firelevel) {
        int factor = 1;
        int level = firelevel;
        while (level > 99 && firelevel % Math.max(10, factor) == 0 && firelevel / Math.max(10, factor) >= 10) {
            factor *= 10;
            level = level / 10;
        }
        int index = 13;
        for (int i = 0; i < base_levels.length; i++) {
            if (base_levels[i] == level) {
                index = i;
                break;
            }
        }
        if (factor == 1) return 1000 + index * 100;
        if (factor == 10) return 3000 + index * 200;
        if (factor == 100) return 6000 + index * 400; //12*400 + 6000 = 10800
        if (factor == 1000) return 12000 + index * 800; //12*800 + 12000 = 21600
        if (factor == 10000) return 24000 + index * 1600; //12*1600 + 24000 = 432000
        return 48000 + index * 3200; //12*3200 + 48000 = 864000
    }

    public static final long fireLevelNeedDiamond(int firelevel) {
        int factor = 1;
        int level = firelevel;
        while (level > 99 && firelevel % Math.max(10, factor) == 0 && firelevel / Math.max(10, factor) >= 10) {
            factor *= 10;
            level = level / 10;
        }
        int index = 13;
        for (int i = 0; i < base_levels.length; i++) {
            if (base_levels[i] == level) {
                index = i;
                break;
            }
        }
        if (factor == 1) return 10 + index;
        if (factor == 10) return 30 + index * 5;
        if (factor == 100) return 100 + index * 100;
        if (factor == 1000) return 1500 + index * 150;
        if (factor == 10000) return 4000 + index * 200;
        return 10000 + index * 500;
    }

    private static final int[] base_levels = {10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90};
}
