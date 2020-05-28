/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.BaseService;
import java.io.*;
import java.util.HashMap;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.service.Local;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Local
@AutoLoad(false)
@Comment("脏字过滤服务")
public class SwearWordService extends BaseService {

    private static final char END_CHAR = 0x200b;

    private final HashMap<Character, HashMap> swearWordMap = new HashMap();

    @Resource(name = "APP_HOME")
    private File home;

    private char replacechar = '*';

    public static void main(String[] args) throws Throwable {
        SwearWordService service = new SwearWordService();
        service.init(null);
        //System.out.println(service.swearWordMap);
        System.out.println(service.replaceSwearWord("有一个傻吊"));
        System.out.println(service.replaceSwearWord("有一个傻傻吊吊"));
    }

    @Override
    public void init(AnyValue conf) {
        if (conf == null) conf = new AnyValue.DefaultAnyValue();
        String wordsfile = conf.getValue("wordsfile");
        String replacechars = conf.getValue("replacechar");
        if (replacechars != null && replacechars.trim().length() == 1) {
            this.replacechar = replacechars.trim().charAt(0);
        }

        if (wordsfile != null && home != null) wordsfile = wordsfile.replace("${APP_HOME}", home.getPath());
        try {
            InputStream in = wordsfile == null ? SwearWordService.class.getResourceAsStream("swearwords.txt") : new FileInputStream(wordsfile);
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                char[] chs = line.trim().toCharArray();
                if (chs.length < 1) continue;
                HashMap<Character, HashMap> map = swearWordMap;
                for (int i = 0; i < chs.length; i++) {
                    char ch = chs[i];
                    HashMap submap = map.get(ch);
                    if (submap == null) {
                        submap = new HashMap();
                        map.put(ch, submap);
                    }
                    if (i == chs.length - 1) {
                        submap.put(END_CHAR, new HashMap<>());
                    }
                    map = submap;
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, SwearWordService.class.getSimpleName() + " init error, config = " + conf, e);
        }
    }

    public String replaceSwearWord(String src) {
        if (src == null || src.isEmpty()) return src;
        char[] chs = src.toCharArray();
        for (int i = 0; i < chs.length;) {
            int len = getSwearWordLength(chs, i, swearWordMap, 0);
            if (len > 0) {
                for (int j = 0; j < len; j++) {
                    chs[i++] = this.replacechar;
                }
            } else {
                i++;
            }
        }
        return new String(chs);
    }

    private int getSwearWordLength(char[] chs, int i, HashMap<Character, HashMap> map, int len) {
        if (i > chs.length - 1) return 0;
        HashMap submap = map.get(chs[i]);
        if (submap == null) return 0;
        if (submap.size() == 1 && submap.get(END_CHAR) != null) return len + 1;
        int newlen = getSwearWordLength(chs, i + 1, submap, len + 1);
        if (newlen == 0 && submap.get(END_CHAR) != null) return len + 1;
        return newlen;
    }
}
