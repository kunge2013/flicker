/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseBean;
import org.redkale.util.ConstructorParameters;

/**
 *
 * @author zhangjx
 */
public class GamePoint extends BaseBean implements Comparable<GamePoint> {

    public static final GamePoint NONE = new GamePoint(-2000, -2000);

    public float x;

    public float y;

    public GamePoint(double x, double y) {
        this.x = ((int) (x * 10000)) / 10000.f;
        this.y = ((int) (y * 10000)) / 10000.f;
    }

    @ConstructorParameters({"x", "y"})
    public GamePoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    //X轴翻转
    public GamePoint turnX() {
        return new GamePoint(-x, y);
    }

    //X轴翻转
    public GamePoint turnY() {
        return new GamePoint(x, -y);
    }

    //XY轴翻转
    public GamePoint turnXY() {
        return new GamePoint(-x, -y);
    }

    //X轴翻转
    public static GamePoint[] turnX(GamePoint[] points) {
        GamePoint[] news = new GamePoint[points.length];
        for (int i = 0; i < points.length; i++) {
            news[i] = points[i].turnX();
        }
        return news;
    }

    //X轴翻转
    public static GamePoint[] turnY(GamePoint[] points) {
        GamePoint[] news = new GamePoint[points.length];
        for (int i = 0; i < points.length; i++) {
            news[i] = points[i].turnY();
        }
        return news;
    }

    //XY轴翻转
    public static GamePoint[] turnXY(GamePoint[] points) {
        GamePoint[] news = new GamePoint[points.length];
        for (int i = 0; i < points.length; i++) {
            news[i] = points[i].turnXY();
        }
        return news;
    }

    //首尾倒转
    public static GamePoint[] reverse(GamePoint[] points) {
        GamePoint[] news = new GamePoint[points.length];
        int index = -1;
        for (int i = points.length - 1; i >= 0; i--) {
            news[++index] = points[i];
        }
        return news;
    }

    @Override
    public int compareTo(GamePoint o) {
        if (o == null) return -1;
        if (o.x != this.x) return (int) (o.x - this.x);
        return (int) (o.y - this.y);
    }
}
