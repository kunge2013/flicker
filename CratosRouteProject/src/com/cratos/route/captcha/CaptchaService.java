/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.route.captcha;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.source.CacheSource;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
@Local
@RestService(name = "captcha", repair = false, comment = "验证码服务")
public class CaptchaService extends AbstractService {

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    public static final String CAPTCHA_RET_JSON = JsonConvert.root().convertTo(new RetResult(30010001, "参数无效"));

    private static final SecureRandom random = new SecureRandom();

    private static final char[] char36sources = "023456789abcdefghjkmnpqrstuvwxyz".toCharArray();

    @Resource(name = "captcha")
    protected CacheSource<String> cache;

    //检测验证码
    public CompletableFuture<Boolean> check(final String captchakey, final String captchacode) {
        if (captchakey == null || captchakey.isEmpty()) return CompletableFuture.completedFuture(false);
        if (captchacode == null || captchacode.isEmpty()) return CompletableFuture.completedFuture(false);
        return cache.getAsync(captchakey).thenCompose(code -> {
            if (logger.isLoggable(Level.FINEST)) logger.finest("CAPTCHA_KEY = " + captchakey + " find CODE =" + code);
            if (code == null) return CompletableFuture.completedFuture(false);
            cache.removeAsync(captchakey);
            //if (logger.isLoggable(Level.FINEST)) logger.finest("remove CAPTCHA_KEY = " + captchakey);
            return CompletableFuture.completedFuture(code.equalsIgnoreCase(captchacode));
        });
    }

    //生成验证码 [0]:key [1]:code
    @RestMapping(name = "create64", auth = false)
    public HttpResult createCaptcha64() throws IOException {
        HttpResult rs = createCaptcha();
        byte[] bs = (byte[]) rs.getResult();
        rs.contentType("text/plain; charset=utf-8").result(Utility.ofMap("image", Base64.getEncoder().encodeToString(bs)));
        return rs;
    }

    //生成验证码 [0]:key [1]:code
    @RestMapping(name = "create", auth = false)
    public HttpResult createCaptcha() throws IOException {
        int w = 150;
        int h = 50;
        final String code = random4Code();
        int verifySize = code.length();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random rand = new Random();
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[] colors = new Color[5];
        Color[] colorSpaces = new Color[]{Color.WHITE, Color.CYAN,
            Color.GRAY, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
            Color.PINK, Color.YELLOW};
        float[] fractions = new float[colors.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = colorSpaces[rand.nextInt(colorSpaces.length)];
            fractions[i] = rand.nextFloat();
        }
        Arrays.sort(fractions);

        g2.setColor(Color.GRAY);// 设置边框色  
        g2.fillRect(0, 0, w, h);

        Color c = getRandColor(200, 250);
        g2.setColor(c);// 设置背景色  
        g2.fillRect(0, 2, w, h - 4);

        //绘制干扰线  
        Random localrand = new Random();
        g2.setColor(getRandColor(160, 200));// 设置线条的颜色  
        for (int i = 0; i < 20; i++) {
            int x = localrand.nextInt(w - 1);
            int y = localrand.nextInt(h - 1);
            int xl = localrand.nextInt(6) + 1;
            int yl = localrand.nextInt(12) + 1;
            g2.drawLine(x, y, x + xl + 40, y + yl + 20);
        }

        // 添加噪点  
        float yawpRate = 0.05f;// 噪声率  
        int area = (int) (yawpRate * w * h);
        for (int i = 0; i < area; i++) {
            int x = localrand.nextInt(w);
            int y = localrand.nextInt(h);
            int rgb = getRandomIntColor();
            image.setRGB(x, y, rgb);
        }

        shear(g2, w, h, c);// 使图片扭曲  

        g2.setColor(getRandColor(100, 160));
        int fontSize = h - 4;
        Font font = new Font("Algerian", Font.ITALIC, fontSize);
        g2.setFont(font);
        char[] chars = code.toCharArray();
        for (int i = 0; i < verifySize; i++) {
            AffineTransform affine = new AffineTransform();
            affine.setToRotation(Math.PI / 4 * rand.nextDouble() * (rand.nextBoolean() ? 1 : -1), (w / verifySize) * i + fontSize / 2, h / 2);
            g2.setTransform(affine);
            g2.drawChars(chars, i, 1, ((w - 10) / verifySize) * i + 5, h / 2 + fontSize / 2 - 10);
        }
        g2.dispose();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", os);
        String key = Utility.uuid();
        cache.set(180, key, code); //3分钟过期
        if (logger.isLoggable(Level.FINEST)) logger.finest("CAPTCHA_KEY = " + key + ", CAPTCHA_CODE = " + code);
        return new HttpResult().contentType("image/jpeg").header("CAPTCHA_KEY", key).cookie("CAPTCHA_KEY", key).result(os.toByteArray());
    }

    private static String random4Code() {
        char[] chars = new char[4];
        int codesLen = char36sources.length;
        for (int i = 0; i < chars.length; i++) {
            chars[i] = char36sources[random.nextInt(codesLen - 1)];
        }
        return new String(chars);
    }

    private static Color getRandColor(int fc, int bc) {
        if (fc > 255) fc = 255;
        if (bc > 255) bc = 255;
        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);
        return new Color(r, g, b);
    }

    private static int getRandomIntColor() {
        int[] rgb = getRandomRgb();
        int color = 0;
        for (int c : rgb) {
            color = color << 8;
            color = color | c;
        }
        return color;
    }

    private static int[] getRandomRgb() {
        int[] rgb = new int[3];
        for (int i = 0; i < 3; i++) {
            rgb[i] = random.nextInt(255);
        }
        return rgb;
    }

    private static void shear(Graphics g, int w1, int h1, Color color) {
        shearX(g, w1, h1, color);
        shearY(g, w1, h1, color);
    }

    private static void shearX(Graphics g, int w1, int h1, Color color) {

        int period = random.nextInt(2);

        boolean borderGap = true;
        int frames = 1;
        int phase = random.nextInt(2);

        for (int i = 0; i < h1; i++) {
            double d = (double) (period >> 1)
                * Math.sin((double) i / (double) period
                    + (6.2831853071795862D * (double) phase)
                    / (double) frames);
            g.copyArea(0, i, w1, 1, (int) d, 0);
            if (borderGap) {
                g.setColor(color);
                g.drawLine((int) d, i, 0, i);
                g.drawLine((int) d + w1, i, w1, i);
            }
        }

    }

    private static void shearY(Graphics g, int w1, int h1, Color color) {

        int period = random.nextInt(40) + 10; // 50;  

        boolean borderGap = true;
        int frames = 20;
        int phase = 7;
        for (int i = 0; i < w1; i++) {
            double d = (double) (period >> 1)
                * Math.sin((double) i / (double) period
                    + (6.2831853071795862D * (double) phase)
                    / (double) frames);
            g.copyArea(i, 0, 1, h1, 0, (int) d);
            if (borderGap) {
                g.setColor(color);
                g.drawLine(i, (int) d, i, 0);
                g.drawLine(i, (int) d + h1, i, h1);
            }

        }

    }
}
