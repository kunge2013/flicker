/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.util;

import java.security.SecureRandom;
import java.util.*;

/**
 *
 * @author zhangjx
 */
public class ShuffleRandom {

    private static final SecureRandom srandom = createRandom();

    private ShuffleRandom() {
    }

    public static SecureRandom[] createRandom(int count) {
        SecureRandom[] randoms = new SecureRandom[count];
        for (int i = 0; i < randoms.length; i++) {
            randoms[i] = createRandom();
        }
        return randoms;
    }

    public static SecureRandom createRandom() {
        SecureRandom random = new SecureRandom();
        byte[] bs = new byte[48];
        random.nextBytes(bs);
        random.setSeed(bs);
        return random;
    }

    public static int random(Random random, int[] array) {
        return array[random.nextInt(array.length)];
    }

    public static long random(Random random, long[] array) {
        return array[random.nextInt(array.length)];
    }

    public static <T> T random(Random random, List<T> array) {
        return array.get(random.nextInt(array.size()));
    }

    /**
     * 随机排序
     *
     * @param cards 数组
     *
     * @return byte[]
     */
    public static byte[] shuffle(byte[] cards) {
        if (cards == null || cards.length < 2) return cards;
        int length = cards.length;
        while (0 != length) {
            int rand = srandom.nextInt(length);
            length--;
            byte temp = cards[length];
            cards[length] = cards[rand];
            cards[rand] = temp;
        }
        return cards;
    }

    /**
     * 随机排序
     *
     * @param cards 数组
     *
     * @return int[]
     */
    public static int[] shuffle(int[] cards) {
        if (cards == null || cards.length < 2) return cards;
        int length = cards.length;
        while (0 != length) {
            int rand = srandom.nextInt(length);
            length--;
            int temp = cards[length];
            cards[length] = cards[rand];
            cards[rand] = temp;
        }
        return cards;
    }

    /**
     *
     * 随机排序
     *
     * @param cards 数组
     *
     * @return long[]
     */
    public static long[] shuffle(long[] cards) {
        if (cards == null || cards.length < 2) return cards;
        int length = cards.length;
        while (0 != length) {
            int rand = srandom.nextInt(length);
            length--;
            long temp = cards[length];
            cards[length] = cards[rand];
            cards[rand] = temp;
        }
        return cards;
    }

    /**
     * 随机排序
     *
     * @param cards 数组
     *
     * @return String[]
     */
    public static String[] shuffle(String[] cards) {
        if (cards == null || cards.length < 2) return cards;
        int length = cards.length;
        while (0 != length) {
            int rand = srandom.nextInt(length);
            length--;
            String temp = cards[length];
            cards[length] = cards[rand];
            cards[rand] = temp;
        }
        return cards;
    }

}
