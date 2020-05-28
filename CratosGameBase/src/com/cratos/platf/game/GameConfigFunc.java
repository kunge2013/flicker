/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import java.util.*;

/**
 *
 * @author zhangjx
 */
public interface GameConfigFunc {

    public static final boolean throwed = true;

    public boolean contains(String name);

    public Collection<String> getNames();

    public int getInt(String name, int defValue);

    public long getLong(String name, long defValue);

    public static GameConfigFunc createEmpty() {
        return new GameConfigFunc() {
            @Override
            public boolean contains(String name) {
                return true;
            }

            @Override
            public Collection<String> getNames() {
                return new ArrayList<>();
            }

            @Override
            public int getInt(String name, int defValue) {
                return defValue;
            }

            @Override
            public long getLong(String name, long defValue) {
                return defValue;
            }
        };
    }

    public static GameConfigFunc createFromMap(final Map<String, ? extends Number> map) {
        return new GameConfigFunc() {
            @Override
            public boolean contains(String name) {
                return map.containsKey(name);
            }

            @Override
            public Collection<String> getNames() {
                return map.keySet();
            }

            @Override
            public int getInt(String name, int defValue) {
                Number rs = map.get(name);
                if (throwed && rs == null) throw new RuntimeException("缺少Config值: " + name);
                return rs == null ? defValue : rs.intValue();
            }

            @Override
            public long getLong(String name, long defValue) {
                Number rs = map.get(name);
                if (throwed && rs == null) throw new RuntimeException("缺少Config值: " + name);
                return rs == null ? defValue : rs.longValue();
            }
        };
    }

    public static GameConfigFunc createFromProperties(final Properties map) {
        return new GameConfigFunc() {
            @Override
            public boolean contains(String name) {
                return map.containsKey(name);
            }

            @Override
            public Collection<String> getNames() {
                return map.stringPropertyNames();
            }

            @Override
            public int getInt(String name, int defValue) {
                if (throwed && !map.containsKey(name)) throw new RuntimeException("缺少Config值: " + name);
                return Integer.parseInt(map.getProperty(name, "" + defValue));
            }

            @Override
            public long getLong(String name, long defValue) {
                if (throwed && !map.containsKey(name)) throw new RuntimeException("缺少Config值: " + name);
                return Long.parseLong(map.getProperty(name, "" + defValue));
            }
        };
    }
}
