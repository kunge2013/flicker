/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseBean;
import com.cratos.platf.game.GamePoint;
import java.awt.geom.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import javax.xml.stream.*;
import org.redkale.convert.json.JsonConvert;

/**
 *
 * @author zhangjx
 */
public class Beziers {

    public static void main(String[] args) throws Throwable {
        List<BattleEnemyLine> lines = new ArrayList<>();
        int index = 210020;
        File root = new File("C:\\Users\\zhangjx\\Desktop\\PlaneGame\\game\\res\\splines");
        for (File xml : root.listFiles()) {
            if (!xml.getName().endsWith(".xml")) continue;
            if (xml.getName().endsWith("line1.xml")) continue;
            if (xml.getName().endsWith("line2.xml")) continue;
            if (xml.getName().endsWith("line3.xml")) continue;
            if (xml.getName().endsWith("line4.xml")) continue;
            if (xml.getName().endsWith("line6.xml")) continue;
            if (xml.getName().endsWith("line7.xml")) continue;
            if (xml.getName().endsWith("line22.xml")) continue;
            if (xml.getName().endsWith("line24.xml")) continue;
            if (xml.getName().endsWith("line26.xml")) continue;
            if (xml.getName().endsWith("line28.xml")) continue;
            if (xml.getName().endsWith("line29.xml")) continue;
            if (xml.getName().endsWith("line47.xml")) continue;
            if (xml.getName().endsWith("line49.xml")) continue;
            if (xml.getName().endsWith("line62.xml")) continue;
            if (xml.getName().endsWith("line64.xml")) continue;
            if (xml.getName().endsWith("line67.xml")) continue;
            if (xml.getName().endsWith("line70.xml")) continue;
            if (xml.getName().endsWith("line82.xml")) continue;
            if (xml.getName().endsWith("line102.xml")) continue;
            if (xml.getName().startsWith("line5") && xml.getName().length() == "line500.xml".length()) continue; //line5xx
            final List<Point2D.Double> doublePoints = parsePoint(xml);
            LinkedHashSet<GamePoint> points = gen(doublePoints);
            BattleEnemyLine line = new BattleEnemyLine();
            index += 10;
            line.setLineid(index);
            GamePoint[] ps = points.toArray(new GamePoint[points.size()]);
            line.setPoints(ps);
            if (xml.getName().endsWith("line5.xml") || xml.getName().endsWith("line21.xml")
                || xml.getName().endsWith("line23.xml") || xml.getName().endsWith("line25.xml")
                || xml.getName().endsWith("line27.xml") || xml.getName().endsWith("line81.xml")) {
                line.setTurns("1");
                if (xml.getName().endsWith("line23.xml") || xml.getName().endsWith("line27.xml")) {
                    line.setKinds(new short[]{3});
                } else {
                    line.setKinds(new short[]{2, 3});
                }
                line.setRemark(xml.getName().replace(".xml", "") + ", 靠左从上到下半直线或大弧度并右转");
            } else if (xml.getName().endsWith("line8.xml") || xml.getName().endsWith("line30.xml")) {
                line.setTurns("1");
                line.setKinds(new short[]{2, 3});
                line.setRemark(xml.getName().replace(".xml", "") + ", 从左到右向下半弧形");
            } else if (xml.getName().startsWith("line4") || xml.getName().endsWith("line50.xml")
                || xml.getName().endsWith("line61.xml") || xml.getName().endsWith("line63.xml")
                || xml.getName().endsWith("line65.xml") || xml.getName().endsWith("line66.xml")
                || xml.getName().endsWith("line68.xml")  || xml.getName().endsWith("line69.xml") 
                || xml.getName().endsWith("line101.xml")) { //line4x
                line.setTurns("1");
                line.setKinds(new short[]{4, 5, 6, 8});
                line.setRemark(xml.getName().replace(".xml", "") + ", 偏复杂轨迹");
            } else if (ps[0].x == ps[1].x && ps[0].x == ps[2].x && ps[0].x == ps[3].x && ps[0].x == ps[4].x && ps[0].x == ps[5].x && ps[0].x == ps[6].x) {
                if (ps[0].x == ps[ps.length - 1].x && ps[0].x == ps[ps.length - 2].x) {
                    line.setRemark(xml.getName().replace(".xml", "") + (ps[0].y < ps[7].y ? "从左到右" : "从右到左") + " 竖线轨迹");
                } else {
                    line.setRemark(xml.getName().replace(".xml", "") + (ps[0].y < ps[7].y ? "从左到右" : "从右到左") + " 半竖线轨迹");
                }
            } else if (ps[0].y == ps[1].y && ps[0].y == ps[2].y && ps[0].y == ps[3].y && ps[0].y == ps[4].y && ps[0].y == ps[5].y && ps[0].y == ps[6].y) {
                if (ps[0].y == ps[ps.length - 1].y && ps[0].y == ps[ps.length - 2].y) {
                    line.setRemark(xml.getName().replace(".xml", "") + (ps[0].x < ps[7].x ? "从下到上" : "从上到下") + " 直线轨迹");
                } else {
                    line.setRemark(xml.getName().replace(".xml", "") + (ps[0].x < ps[7].x ? "从下到上" : "从上到下") + " 半直线轨迹");
                }
            } else {
                line.setRemark(xml.getName().replace(".xml", "") + " 轨迹");
            }
            lines.add(line);

            File destHtml = new File("C:\\Users\\zhangjx\\Desktop\\linehtml\\" + xml.getName().replace("xml", "html"));
            FileWriter out = new FileWriter(destHtml, Charset.forName("UTF-8"));
            out.write("<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + " <head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"Generator\" content=\"EditPlus®\">\n"
                + "  <meta name=\"Author\" content=\"\">\n"
                + "  <meta name=\"Keywords\" content=\"\">\n"
                + "  <meta name=\"Description\" content=\"\">\n"
                + "  <title>Document</title>\n"
                + " </head>\n"
                + " <body>\n"
                + " <canvas id=\"canvas\" width=\"720\" height=\"1280\"  style=\"border:1px solid #d3d3d3;\"/>\n"
                + "  <script type=\"text/javascript\">\n"
                + "	canvas = document.getElementById(\"canvas\");\n"
                + "	ctx = canvas.getContext(\"2d\")\n"
                + "	ctx.fillStyle = \"#FF0000\";\n"
                + "	ctx.lineWidth = 2;\n"
                + "	ctx.strokeStyle = \"#0090D2\";\n"
                + "	//ctx.beginPath();\n");
            //out.write("	ctx.moveTo(" + doublePoints.get(0).x + ", " + doublePoints.get(0).y + ");\n");
            StringBuilder sbs = new StringBuilder();
            for (int i = 1; i < doublePoints.size(); i++) {
                if (sbs.length() > 0) sbs.append(", ");
                sbs.append(doublePoints.get(i).x + ", " + doublePoints.get(i).y);
            }
            int flag = 0;
            for (GamePoint point : line.getPoints()) {
                if (flag < 100) out.write("	ctx.fillStyle = \"#0000FF\";\n");
                out.write("  ctx.fillRect(" + (point.x + 360) + "-2," + (point.y + 640) + "-2,4,4); \n");
                if (flag >= 100) out.write("	ctx.fillStyle = \"#FF0000\";\n");
                flag++;
            }
            //out.write("	ctx.quadraticCurveTo(" + sbs + ");\n");
            out.write("	//ctx.stroke();\n"
                + "  </script>\n"
                + " </body>\n"
                + "</html>\n");
            out.flush();
            out.close();
        }
        StringBuilder sb = new StringBuilder();
        for (BattleEnemyLine line : lines) {
            sb.append("    \"").append(line.getLineid()).append("\": {\r\n");
            sb.append("        \"turns\": \"").append((line.getTurns() == null || line.getTurns().isEmpty()) ? (line.getRemark().contains("直线") ? "1" : "") : line.getTurns()).append("\",\r\n");
            sb.append("        \"kinds\" : ").append(line.getKinds() == null ? "[]" : JsonConvert.root().convertTo(line.getKinds())).append(",\r\n");
            sb.append("        \"remark\": \"").append(line.getRemark()).append("\",\r\n");
            sb.append("        \"points\": ").append(JsonConvert.root().convertTo(line.getPoints())).append("\r\n");
            sb.append("    },\r\n");
        }
        System.out.println(sb);
    }

    private static List< Point2D.Double> parsePoint(File file) throws Throwable {
        final List< Point2D.Double> inits = new ArrayList<>();
        final InputStream in = new FileInputStream(file);
        XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in, "GBK");
        boolean flag = false;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("control".equalsIgnoreCase(reader.getLocalName())) {
                    String x = reader.getAttributeValue(null, "x");
                    String y = reader.getAttributeValue(null, "y");
                    if (x == null) continue;
                    inits.add(new Point2D.Double(Double.parseDouble(x), Double.parseDouble(y)));
                }
            }
        }
        in.close();
        return inits;
    }

    private static LinkedHashSet<GamePoint> gen(List<Point2D.Double> inits) throws Throwable {

        int n = inits.size() - 1; //
        int i, r;
        float u;
        List<Point2D.Double> list = new ArrayList<>();
        // u的步长决定了曲线点的精度
        for (u = 0; u <= 1; u += 0.0005) {
            Point2D.Double p[] = new Point2D.Double[n + 1];
            for (i = 0; i <= n; i++) {
                p[i] = new Point2D.Double(inits.get(i).x, inits.get(i).y);
            }
            for (r = 1; r <= n; r++) {
                for (i = 0; i <= n - r; i++) {
                    p[i].x = (1 - u) * p[i].x + u * p[i + 1].x;
                    p[i].y = (1 - u) * p[i].y + u * p[i + 1].y;
                }
            }
            list.add(p[0]);
        }
        LinkedHashSet<SubPoint> floatset = new LinkedHashSet<>();
        SubPoint last = null;
        for (Point2D.Double point : list) {
            SubPoint p = new SubPoint(point.x, point.y);
            floatset.add(p);
            last = p;
        }
        LinkedHashSet<SubPoint> intset = new LinkedHashSet<>();
        int offset = 150;
        for (SubPoint point : floatset) {
            if (point.ix < -offset || point.ix >= (720 + offset)) continue;
            if (point.iy < -offset || point.iy >= (1280 + offset)) continue;
            intset.add(new SubPoint(point.ix, point.iy, point.fx - 360, 640 - point.fy));
        }
        LinkedHashSet<GamePoint> result = new LinkedHashSet<>();
        for (SubPoint point : intset) {
            result.add(new GamePoint(point.fx, point.fy));
        }
        return result;
    }

    private static class SubPoint extends BaseBean {

        public int ix;

        public int iy;

        public float fx;

        public float fy;

        public SubPoint(int ix, int iy, float fx, float fy) {
            this.ix = ix;
            this.iy = iy;
            this.fx = fx;
            this.fy = fy;
        }

        public SubPoint(double x, double y) {
            this.ix = Math.round((float) x);
            this.iy = Math.round((float) y);
            this.fx = ((int) (x * 10000)) / 10000.0f;
            this.fy = ((int) (y * 10000)) / 10000.0f;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Math.round(ix);
            hash = 41 * hash + Math.round(iy);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final SubPoint other = (SubPoint) obj;
            if (this.ix != other.ix)
                return false;
            if (this.iy != other.iy)
                return false;
            return true;
        }

    }
}
