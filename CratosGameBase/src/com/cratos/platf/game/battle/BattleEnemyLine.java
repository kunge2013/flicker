/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.game.GamePoint;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class BattleEnemyLine extends BaseEntity {

    private static final Type MAP_ENEMYLINE = new TypeToken<LinkedHashMap<Integer, BattleEnemyLine>>() {
    }.getType();

    @Id
    @Column(comment = "轨迹ID")
    private int lineid;

    @Column(comment = "轨迹的所属的kindtype")
    private short[] kinds;

    @Column(comment = "轨迹的像素点坐标")
    private GamePoint[] points;

    @Column(comment = "轨迹可转换项，多个用;分割; 全翻转值为：1;2;3;4;5;6;7")
    private String turns = "";

    @Column(comment = "轨迹描述")
    private String remark = "";

    public static LinkedHashMap<Integer, BattleEnemyLine> load(Class resClass, String resurl) {
        try {
            LinkedHashMap<Integer, BattleEnemyLine> map = JsonConvert.root().convertFrom(MAP_ENEMYLINE, Utility.readThenClose(resClass.getResourceAsStream(resurl)));
            for (Map.Entry<Integer, BattleEnemyLine> en : new LinkedHashMap<>(map).entrySet()) {
                final BattleEnemyLine line = en.getValue();
                line.setLineid(en.getKey());
                if (line.points == null || line.points.length < 2) {
                    map.remove(line.lineid);
                    continue;
                }
                if (en.getKey() % 10 != 0) continue;
                GamePoint[] newpoints = GamePoint.reverse(line.points);
                if (line.turns != null && line.turns.indexOf('1') >= 0) {
                    //X轴倒转
                    map.put(line.getLineid() + 1, new BattleEnemyLine(line.lineid + 1, line.remark, line.kinds, GamePoint.turnX(line.points)));
                }
                if (line.turns != null && line.turns.indexOf('2') >= 0) {
                    //Y轴倒转
                    map.put(line.getLineid() + 2, new BattleEnemyLine(line.lineid + 2, line.remark, line.kinds, GamePoint.turnY(line.points)));
                }
                if (line.turns != null && line.turns.indexOf('3') >= 0) {
                    //XY轴倒转
                    map.put(line.getLineid() + 3, new BattleEnemyLine(line.lineid + 3, line.remark, line.kinds, GamePoint.turnXY(line.points)));
                }
                if (line.turns != null && line.turns.indexOf('4') >= 0) {
                    //首尾倒转
                    map.put(line.getLineid() + 4, new BattleEnemyLine(line.lineid + 4, line.remark, line.kinds, newpoints));
                }
                if (line.turns != null && line.turns.indexOf('5') >= 0) {
                    //X轴倒转
                    map.put(line.getLineid() + 5, new BattleEnemyLine(line.lineid + 5, line.remark, line.kinds, GamePoint.turnX(newpoints)));
                }
                if (line.turns != null && line.turns.indexOf('6') >= 0) {
                    //Y轴倒转
                    map.put(line.getLineid() + 6, new BattleEnemyLine(line.lineid + 6, line.remark, line.kinds, GamePoint.turnY(newpoints)));
                }
                if (line.turns != null && line.turns.indexOf('7') >= 0) {
                    //XY轴倒转
                    map.put(line.getLineid() + 7, new BattleEnemyLine(line.lineid + 7, line.remark, line.kinds, GamePoint.turnXY(newpoints)));
                }
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BattleEnemyLine() {
    }

    public BattleEnemyLine(int lineid, String linedesc, short[] kinds, GamePoint[] points) {
        this.lineid = lineid;
        this.remark = linedesc;
        this.kinds = kinds;
        this.points = points;
    }

    public int getLineid() {
        return lineid;
    }

    public void setLineid(int lineid) {
        this.lineid = lineid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short[] getKinds() {
        return kinds;
    }

    public void setKinds(short[] kinds) {
        this.kinds = kinds;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public GamePoint[] getPoints() {
        return points;
    }

    public void setPoints(GamePoint[] points) {
        this.points = points;
    }

    public String getTurns() {
        return turns;
    }

    public void setTurns(String turns) {
        this.turns = turns;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public static void main2(String[] args) throws Throwable {
        GamePoint centre = new GamePoint(-640, 0);
        landscapeOvalLineTopLeft(centre, 180, 200, -90);
    }

    public static void main(String[] args) throws Throwable {
//        GamePoint centre = new GamePoint(640, 0);
//        ovalLineTopRight(centre, 240, 260, -90);
//        System.out.println("---------------");
//        ovalLineTopRight(centre, 180, 200, -90);
//        System.out.println("---------------");
//        ovalLineTopRight(centre, 120, 140, -90);

        //心形坐标: [{"x":-635,"y":105}, {"x":-635,"y":-105}, {"x":-640,"y":-60}, {"x":-615,"y":150}, {"x":-640,"y":60}, {"x":-615,"y":-150}, {"x":-610,"y":-25}, {"x":-610,"y":25}, {"x":-565,"y":0}, {"x":-565,"y":-180}, {"x":-565,"y":180}, {"x":-500,"y":-185}, {"x":-500,"y":185}, {"x":-440,"y":-175}, {"x":-440,"y":175}, {"x":-385,"y":-145}, {"x":-385,"y":145}, {"x":-335,"y":-105}, {"x":-335,"y":105}, {"x":-290,"y":-70}, {"x":-290,"y":70}, {"x":-250,"y":-30}, {"x":-250,"y":30}, {"x":-220,"y":0}]
        int[] ys = {0};
        portraitHorizontalLine(0);
    }

    //竖屏生成直线
    protected static void portraitLine(int[] ys) {
        Map<Integer, BattleEnemyLine> map = new LinkedHashMap<>();
        final int startid = 210010;
        int lineid = startid;
        BattleEnemyLine line = new BattleEnemyLine();
        List<GamePoint> gps = new ArrayList();
        for (int i = 0; i < ys.length; i++) {
            lineid = startid + i * 10;
            line = new BattleEnemyLine();
            line.setLineid(lineid);
            line.setRemark("第" + (lineid - 210000) / 10 + "条x=" + (ys[i]) + "从左到右的直线轨迹");
            gps = new ArrayList();
            for (int j = 0; j < 720; j++) {
                gps.add(new GamePoint(j - 360, ys[i]));
            }
            line.setPoints(gps.toArray(new GamePoint[gps.size()]));
            map.put(lineid, line);
        }

        System.out.println("{");
        for (Map.Entry<Integer, BattleEnemyLine> en : map.entrySet()) {
            System.out.println("    \"" + en.getKey() + "\": {");
            line = en.getValue();
            System.out.println("        \"linedesc\": \"" + line.getRemark() + "\",");
            System.out.println("        \"points\": " + JsonConvert.root().convertTo(line.getPoints()));
            System.out.println("    }, ");
        }
        System.out.println("}");
    }

    //竖屏从上到下竖直线
    protected static void portraitVerticalLine(int x) {
        java.util.List<GamePoint> list = new ArrayList<>();
        for (int i = 0; i < 1280; i++) {
            list.add(new GamePoint(x, 640 - i));
        }
        System.out.println(list);
    }

    //竖屏从左到右横直线
    protected static void portraitHorizontalLine(int y) {
        java.util.List<GamePoint> list = new ArrayList<>();
        for (int i = 0; i < 720; i++) {
            list.add(new GamePoint(360 - i, y));
        }
        System.out.println(list);
    }

    //横屏生成从右到左的直线
    protected static void landscapeLine(int[] ys) {
        //main22(args); 
        Map<Integer, BattleEnemyLine> map = new LinkedHashMap<>();
        final int startid = 21010;
        int lineid = startid;
        BattleEnemyLine line = new BattleEnemyLine();
        line.setLineid(lineid);
        line.setRemark("第22条y=0从右到左的直线轨迹");
        List<GamePoint> gps = new ArrayList();
        for (int j = 0; j < 1280; j++) {
            gps.add(new GamePoint(640 - j, 0));
        }
        line.setPoints(gps.toArray(new GamePoint[gps.size()]));
        map.put(lineid, line);

        for (int i = 1; i < ys.length; i++) {
            lineid = startid - 1 + i * 2;
            line = new BattleEnemyLine();
            line.setLineid(lineid);
            line.setRemark("第" + (lineid - 22000) + "条y=" + (-ys[i]) + "从右到左的直线轨迹");
            gps = new ArrayList();
            for (int j = 0; j < 1280; j++) {
                gps.add(new GamePoint(640 - j, -ys[i]));
            }
            line.setPoints(gps.toArray(new GamePoint[gps.size()]));
            map.put(lineid, line);

            lineid = startid - 1 + i * 2 + 1;
            line = new BattleEnemyLine();
            line.setLineid(lineid);
            line.setRemark("第" + (lineid - 22000) + "条y=" + (ys[i]) + "从右到左的直线轨迹");
            gps = new ArrayList();
            for (int j = 0; j < 1280; j++) {
                gps.add(new GamePoint(640 - j, ys[i]));
            }
            line.setPoints(gps.toArray(new GamePoint[gps.size()]));
            map.put(lineid, line);
        }
        System.out.println("{");
        for (Map.Entry<Integer, BattleEnemyLine> en : map.entrySet()) {
            System.out.println("    \"" + en.getKey() + "\": {");
            line = en.getValue();
            System.out.println("        \"linedesc\": \"" + line.getRemark() + "\",");
            System.out.println("        \"points\": " + JsonConvert.root().convertTo(line.getPoints()));
            System.out.println("    }, ");
        }
        System.out.println("}");
    }

    //横屏椭圆，又上开始向左凸出
    protected static void landscapeOvalLineTopRight(final GamePoint centre, int x, int y, final double angle) { //angle: -90为左上方
        java.util.List<GamePoint> list = new ArrayList<>();
        double degree = angle;
        final double end = angle + 180;
        GamePoint last = new GamePoint(0, 0);
        while (degree <= end) {
            GamePoint gp = new GamePoint((centre.x + x * Math.cos((180 + degree) * Math.PI / 180)), (centre.y + y * Math.sin(degree * Math.PI / 180)));
            if (last.x != gp.x || last.y != gp.y) list.add(gp);
            last = gp;
            degree += 0.001;
        }
        System.out.println(list.size());
        System.out.println(list);
    }

    //横屏椭圆，左上开始向右凸出
    protected static void landscapeOvalLineTopLeft(final GamePoint centre, int x, int y, final double angle) { //angle: -90为左上方
        java.util.List<GamePoint> list = new ArrayList<>();
        double degree = angle;
        final double end = angle + 180;
        GamePoint last = new GamePoint(0, 0);
        while (degree <= end) {
            GamePoint gp = new GamePoint((centre.x + x * Math.cos(degree * Math.PI / 180)), (centre.y + y * Math.sin(degree * Math.PI / 180)));
            if (last.x != gp.x || last.y != gp.y) list.add(gp);
            last = gp;
            degree += 0.2;
        }
        System.out.println(list.size());
        System.out.println(list);
    }

    //横屏竖直线
    protected static void landscapeVerticalLine(int x) {
        java.util.List<GamePoint> list = new ArrayList<>();
        for (int i = 0; i < 720; i++) {
            list.add(new GamePoint(x, i - 360));
        }
        System.out.println(list);
    }

    //横屏横直线
    protected static void landscapeHorizontalLine(int y) {
        java.util.List<GamePoint> list = new ArrayList<>();
        for (int i = 0; i < 1280; i++) {
            list.add(new GamePoint(640 - i, y));
        }
        System.out.println(list);
    }
}
