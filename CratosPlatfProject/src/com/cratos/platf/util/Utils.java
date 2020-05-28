/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.util;

import java.io.*;
import java.util.logging.LogManager;
import java.util.stream.Stream;
import org.redkale.boot.LogFileHandler;

/**
 *
 * @author zhangjx
 */
public abstract class Utils {

    public static final String HEADNAME_WS_SNCP_ADDRESS = "WS-SncpAddress";

    private Utils() {
    }

    public static void initLogConfig() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            final PrintStream ps = new PrintStream(out);
            ps.println("handlers = java.util.logging.ConsoleHandler");
            ps.println(".level = FINEST");
            ps.println("java.util.logging.ConsoleHandler.level = FINEST");
            ps.println("java.util.logging.ConsoleHandler.formatter = " + LogFileHandler.LoggingFormater.class.getName());
            LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(out.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当天yyyyMMddHHmmss格式的long值
     *
     * @return yyyyMMddHHmmss格式的long值
     */
    public static long datetime14() {
        java.time.LocalDateTime day = java.time.LocalDateTime.now();
        return day.getYear() * 10000_000000L + day.getMonthValue() * 100_000000 + day.getDayOfMonth() * 1000000
            + day.getHour() * 10000 + day.getMinute() * 100 + day.getSecond();
    }

    /**
     * 获取当天yyMMddHHmmss格式的long值
     *
     * @return yyMMddHHmmss格式的long值
     */
    public static long datetime12() {
        java.time.LocalDateTime day = java.time.LocalDateTime.now();
        return day.getYear() % 100 * 10000_000000L + day.getMonthValue() * 100_000000 + day.getDayOfMonth() * 1000000
            + day.getHour() * 10000 + day.getMinute() * 100 + day.getSecond();
    }

    public static <T extends Weightable> int[] calcIndexWeights(Stream<T> stream) {
        return calcIndexWeights(stream.mapToInt(t -> t.getWeight()).toArray());
    }

    public static int[] calcIndexWeights(int[] weights) {
        int size = 0;
        for (int w : weights) {
            size += w;
        }
        int[] newWeights = new int[size];
        int index = -1;
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i]; j++) {
                newWeights[++index] = i;
            }
        }
        return newWeights;
    }
}
