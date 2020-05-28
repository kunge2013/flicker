/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.util;

/**
 * <pre>
 * 经纬度单独编码长度
 * // 长度        宽偏差      高偏差
 * //   1      5009.4km    4992.6km
 * //   2      1252.3km     624.1km
 * //   3       156.5km       156km
 * //   4        39.1km      19.5km
 * //   5         4.9km       4.9km
 * //   6         1.2km      609.4m
 * //   7        152.9m      152.4m
 * //   8         38.2m        9.0m
 * //   9          4.8m        4.8m
 * //  10          1.2m      59.5cm
 * //  11        14.9cm      14.9cm
 * //  12         3.7cm       1.9cm
 * </pre>
 *
 * @author zhangjx
 */
public class GeoHash {

    private static final int numbits = 30;

    private final static char[] digits = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'b', 'c', 'd', 'e', 'f', 'g',
        'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r',
        's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    //对经纬度进行编码
    public static String encode(double lat, double lon) {
        long val = encodeBits(0, 1, lon, -180, 180); //前位
        val = encodeBits(val, 0, lat, -90, 90); //后位
        //Base32编码
        char[] buf = new char[65];
        int pos = 64;
        boolean negative = (val < 0);
        if (!negative) val = -val;
        while (val <= -32) {
            buf[pos--] = digits[(int) (-(val % 32))];
            val /= 32;
        }
        buf[pos] = digits[(int) (-val)];
        if (negative) buf[--pos] = '-';
        return new String(buf, pos, (65 - pos));
    }

    //根据经纬度和范围，获取对应二进制
    private static long encodeBits(long val, int index, double lat, double floor, double ceiling) {
        final int p = (numbits - 1) * 2 + index;
        for (int i = 0; i < numbits; i++) {
            double mid = (floor + ceiling) / 2;
            if (lat >= mid) {
                val |= (1L << (p - i * 2));
                floor = mid;
            } else {
                ceiling = mid;
            }
        }
        return val;
    }

    private static final double EARTH_RADIUS = 6378.137 * 1000 * 100;  //单位：cm

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 计算两经纬度之间的距离， 返回单位：cm
     *
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     *
     * @return
     */
    public static long distance(double lat1, double lng1, double lat2, double lng2) {
        if (lat1 == 0 && lat2 == 0) return 0;
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = EARTH_RADIUS * 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        return Math.round(s);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("yb0bh2n0p058");
        System.out.println(GeoHash.encode(45, 125));
        System.out.println("距离：" + GeoHash.distance(45, 125, 45, 124.9999));
        System.out.println("解码后：45.0 124.9999999254942");
        //double[] geo = geohash.decode(s);
        //System.out.println(geo[0] + " " + geo[1]);
    }

    /**
     * <pre>
     *  //定义编码映射关系
     * final static HashMap<Character, Integer> lookup = new HashMap<>();
     *
     * //初始化编码映射内容
     * static {
     * int i = 0;
     * for (char c : digits)
     * lookup.put(c, i++);
     * }
     *
     * //对编码后的字符串解码
     * public double[] decode(String geohash) {
     * StringBuilder buffer = new StringBuilder();
     * for (char c : geohash.toCharArray()) {
     *
     * int i = lookup.get(c) + 32;
     * buffer.append(Integer.toString(i, 2).substring(1));
     * }
     *
     * BitSet lonset = new BitSet();
     * BitSet latset = new BitSet();
     *
     * //偶数位，经度
     * int j = 0;
     * for (int i = 0; i < numbits * 2; i += 2) {
     * boolean isSet = false;
     * if (i < buffer.length())
     * isSet = buffer.charAt(i) == '1';
     * lonset.set(j++, isSet);
     * }
     *
     * //奇数位，纬度
     * j = 0;
     * for (int i = 1; i < numbits * 2; i += 2) {
     * boolean isSet = false;
     * if (i < buffer.length())
     * isSet = buffer.charAt(i) == '1';
     * latset.set(j++, isSet);
     * }
     *
     * double lon = decode(lonset, -180, 180);
     * double lat = decode(latset, -90, 90);
     *
     * return new double[]{lat, lon};
     * }
     *
     * //根据二进制和范围解码
     * private double decode(BitSet bs, double floor, double ceiling) {
     * double mid = 0;
     * for (int i = 0; i < bs.length(); i++) {
     * mid = (floor + ceiling) / 2;
     * if (bs.get(i))
     * floor = mid;
     * else
     * ceiling = mid;
     * }
     * return mid;
     * }
     * </pre>
     */
}
