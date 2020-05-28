/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.line;

import com.cratos.platf.util.*;
import java.util.Arrays;

/**
 * 扑克面值 <br>
 * A 2--10 J Q K 小王 大王  <br>
 * 1 2--10 20 30 40 860 870   <br>
 * 黑桃--6 红桃--5 梅花--4 方块--3       <br>
 * 例如： 红桃3：503 黑桃A：601         <br>
 *
 * @author zhangjx
 */
public abstract class LineNius {

    public static final int SMALL_KING = 860; //小王

    public static final int BIG_KING = 870;  //大王

    //庄闲牛牛赔率, 牛牛5倍， 牛九4倍 牛八3倍 牛七2倍  其他1倍
    private static final int[] NIU4_FACTORS = {1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 5};

    //庄闲牛牛底注的选项
    public static final int[] NIU4_BASECOIN_ITEMS = {10_00, 50_00, 100_00, 500_00};

    //百人牛牛可押注的选项  1:1
    public static final long[] NIU0_BETCOINS_ITEMS = {500, 1000, 5000, 1_0000, 2_0000, 5_0000};

    //百人牛牛机器人押注权重
    public static final int[] NIU0_BETCOINS_WEIGHTS = Utils.calcIndexWeights(new int[]{36, 35, 34, 13, 12, 11});

    //申请当庄最小金币数
    public static final long NIU0_BANKER_APPLY_MIN_COINS = 10000_00L;

    //系统当庄默认金币数
    public static final long NIU0_BANKER_OS_COINS = 100_0000_00L;

    //百人牛牛可押注的选项  1:10000
    //public static final long[] NIU0_BETCOINS_ITEMS = {100, 500, 1_000, 5_000, 1_0000, 2_0000, 5_0000, 10_0000, 20_0000, 50_0000};    
    //public static final int[] NIU0_BETCOINS_WEIGHTS = Utils.calcIndexWeights(new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1}); //百人牛牛机器人押注权重
    private LineNius() {
    }

    //获取赔率
    public static int getNiu4Factor(int niuniu) {
        return NIU4_FACTORS[niuniu];
    }

    public static void main(String[] args) throws Throwable {
        int[] card5s = Arrays.copyOf(random54Pokers(), 5);
        boolean flag = false; //为true表示必须有大小王
        do {
            for (int i : card5s) {
                if (i >= SMALL_KING) { //有王
                    flag = false;
                }
            }
            if (flag) card5s = Arrays.copyOf(random54Pokers(), 5);
        } while (flag);
        System.out.println("牌是: " + Arrays.toString(card5s));
        int[] nius = getNiuResult(card5s);
        System.out.println("牛几: " + Arrays.toString(nius));
    }

    //获取玩家的赔率， 如果玩家赢返回正数， 如果玩家输返回负数
    //百人牛牛赔率: 牛牛10倍，牛1-牛9是1-9倍，无牛是1
    public static int getNiu0PlayerFactor(int[] bankerCards, int bankerNiu, int[] playerCards, int playerNiu) {
        if (playerNiu > bankerNiu) return Math.max(1, playerNiu);
        if (playerNiu < bankerNiu) return -Math.max(1, bankerNiu);
        //相同的比花色
        int bankerMax = LineNius.maxCard(bankerCards);
        return LineNius.maxCard(bankerMax, LineNius.maxCard(playerCards)) != bankerMax ? Math.max(1, playerNiu) : -Math.max(1, bankerNiu);
    }

    //判断牛几， 0表示无牛，10表示牛牛，1-9表示牛1-牛9 
    //返回长度6的数组: [0]表示牛几; [1][2][3]表示组合10的三张牌的数组下标， [4][5]表示牛几;
    public static int[] getNiuResult(final int[] card5s) {
        int niu = 0;
        int c0 = 0, c1 = 0, c2 = 0, c3 = 0, c4 = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = x + 1; y < 4; y++) {
                for (int z = y + 1; z < 5; z++) {
                    if ((card5s[x] < SMALL_KING && card5s[y] < SMALL_KING && card5s[z] < SMALL_KING) //没有大小王
                        && (card5s[x] + card5s[y] + card5s[z]) % 10 != 0) continue;
                    int temp = 0;
                    int a = 0, b = 0;
                    for (int j = 0; j < card5s.length; j++) {
                        if (j != x && j != y && j != z) {
                            temp += card5s[j];
                            if (a == 0) {
                                a = j;
                            } else {
                                b = j;
                            }
                        }
                    }
                    temp %= 10;
                    if (temp == 0) temp = 10;
                    if (temp > niu) {
                        c0 = x;
                        c1 = y;
                        c2 = z;
                        c3 = a;
                        c4 = b;
                        niu = temp;
                    }
                }
            }
        }
        return new int[]{niu, c0, c1, c2, c3, c4};
    }

    //获取最大的一张牌
    public static int maxCard(int... cards) {
        int max = cards[0];
        for (int i = 1; i < cards.length; i++) {
            int c = cards[i] % 100;
            if (c > max % 100) {
                max = cards[i];
            } else if (c == max % 100) {
                if (cards[i] / 100 > max / 100) {
                    max = cards[i];
                }
            }
        }
        return max;
    }

    //不带大小王
    public static int[] random52Pokers() {
        int[] cards = new int[52];
        int index = 0;
        for (int i = 3; i <= 6; i++) {
            for (int j = 1; j <= 10; j++) {
                cards[index++] = i * 100 + j;
            }
            cards[index++] = i * 100 + 20;
            cards[index++] = i * 100 + 30;
            cards[index++] = i * 100 + 40;
        }
        return ShuffleRandom.shuffle(cards);
    }

    //带大小王
    public static int[] random54Pokers() {
        int[] cards = new int[54];
        int index = 0;
        for (int i = 3; i <= 6; i++) {
            for (int j = 1; j <= 10; j++) {
                cards[index++] = i * 100 + j;
            }
            cards[index++] = i * 100 + 20;
            cards[index++] = i * 100 + 30;
            cards[index++] = i * 100 + 40;
        }
        cards[index++] = SMALL_KING;
        cards[index++] = BIG_KING;
        return ShuffleRandom.shuffle(cards);
    }

}
