/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.flicker;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class AutoClassCreator {

    private static final String currentpkg = AutoClassCreator.class.getPackage().getName();

    private static final String jdbc_url = "jdbc:mysql://localhost:3306/skywar_game?serverTimezone=GMT&userSSL=false";//数据库url

    private static final String jdbc_user = "root"; //数据库用户名

    private static final String jdbc_pwd = "root"; //数据库密码

    public static void main(String[] args) throws Exception {

        String pkg = "com.cratos.game.flicker";  //与base同级的包名

        final String entityClass = "HeroDetail";//类名

        final String superEntityClass = "";//父类名

        loadEntity(pkg, entityClass, superEntityClass); //Entity内容

    }

    private static void loadEntity(String pkg, String classname, String superclassname) throws Exception {
        String entityBody = createEntityContent(pkg, classname, superclassname); //源码内容
        final File entityFile = new File("src/" + pkg.replace('.', '/') + "/" + classname + ".java");
        if (entityFile.isFile()) throw new RuntimeException(classname + ".java 已经存在");
        FileOutputStream out = new FileOutputStream(entityFile);
        out.write(entityBody.getBytes("UTF-8"));
        out.close();
    }

    private static String createEntityContent(String pkg, String classname, String superclassname) throws Exception {
        Properties prop = new Properties();
        prop.setProperty(DataSources.JDBC_URL, jdbc_url);
        prop.setProperty(DataSources.JDBC_USER, jdbc_user);
        prop.setProperty(DataSources.JDBC_PWD, jdbc_pwd);
        DataSqlSource source = (DataSqlSource) DataSources.createDataSource("", prop);

        final StringBuilder sb = new StringBuilder();
        final StringBuilder tostring = new StringBuilder();
        final StringBuilder tableComment = new StringBuilder();
        final Map<String, String> uniques = new HashMap<>();
        final Map<String, String> indexs = new HashMap<>();
        final List<String> columns = new ArrayList<>();
        final Set<String> superColumns = new HashSet<>();
        source.directQuery("SHOW CREATE TABLE " + classname.toLowerCase(), new Function<ResultSet, String>() {
            @Override
            public String apply(ResultSet tcs) {
                try {
                    tcs.next();
                    final String createsql = tcs.getString(2);
                    for (String str : createsql.split("\n")) {
                        str = str.trim();
                        if (str.startsWith("`")) {
                            str = str.substring(str.indexOf('`') + 1);
                            columns.add(str.substring(0, str.indexOf('`')));
                        } else if (str.startsWith("UNIQUE KEY ")) {
                            str = str.substring(str.indexOf('`') + 1);
                            uniques.put(str.substring(0, str.indexOf('`')), str.substring(str.indexOf('(') + 1, str.indexOf(')')));
                        } else if (str.startsWith("KEY ")) {
                            str = str.substring(str.indexOf('`') + 1);
                            indexs.put(str.substring(0, str.indexOf('`')), str.substring(str.indexOf('(') + 1, str.indexOf(')')));
                        }
                    }
                    int pos = createsql.indexOf("COMMENT='");
                    if (pos > 0) {
                        tableComment.append(createsql.substring(pos + "COMMENT='".length(), createsql.lastIndexOf('\'')));
                    } else {
                        tableComment.append("");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            }
        });

        if (superclassname != null && !superclassname.isEmpty()) {
            source.directQuery("SELECT * FROM information_schema.columns WHERE  table_name = '" + superclassname.toLowerCase() + "'", new Function<ResultSet, String>() {
                @Override
                public String apply(ResultSet rs) {
                    try {
                        while (rs.next()) {
                            superColumns.add(rs.getString("COLUMN_NAME"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "";
                }
            });
        }
        source.directQuery("SELECT * FROM information_schema.columns WHERE  table_name = '" + classname.toLowerCase() + "'", new Function<ResultSet, String>() {
            @Override
            public String apply(ResultSet rs) {
                try {
                    sb.append("package " + pkg + ";" + "\r\n\r\n");
                    sb.append("import javax.persistence.*;\r\n");
                    //sb.append("import org.redkale.util.*;\r\n");
                    if (superclassname == null || superclassname.isEmpty()) {
                        try {
                            Class.forName("com.cratos.platf.base.BaseEntity");
                            sb.append("import com.cratos.platf.base.BaseEntity;\r\n");
                        } catch (Throwable t) {
                            sb.append("import org.redkale.convert.json.*;\r\n");
                            tostring.append("\r\n    @Override\r\n    public String toString() {\r\n");
                            tostring.append("        return JsonConvert.root().convertTo(this);\r\n");
                            tostring.append("    }\r\n");
                        }
                    }
                    sb.append("\r\n/**\r\n"
                        + " *\r\n"
                        + " * @author " + System.getProperty("user.name") + "\r\n"
                        + " */\r\n");
                    //if (classname.contains("Info")) sb.append("@Cacheable\r\n");        
                    sb.append("@Table(comment = \"" + tableComment + "\"");
                    if (!uniques.isEmpty()) {
                        sb.append("\r\n        , uniqueConstraints = {");
                        boolean first = true;
                        for (Map.Entry<String, String> en : uniques.entrySet()) {
                            if (!first) sb.append(", ");
                            sb.append("@UniqueConstraint(name = \"" + en.getKey() + "\", columnNames = {" + en.getValue().replace('`', '"') + "})");
                            first = false;
                        }
                        sb.append("}");
                    }
                    if (!indexs.isEmpty()) {
                        sb.append("\r\n        , indexes = {");
                        boolean first = true;
                        for (Map.Entry<String, String> en : indexs.entrySet()) {
                            if (!first) sb.append(", ");
                            sb.append("@Index(name = \"" + en.getKey() + "\", columnList = \"" + en.getValue().replace("`", "") + "\")");
                            first = false;
                        }
                        sb.append("}");
                    }
                    sb.append(")\r\n");
                    sb.append("public class " + classname
                        + (superclassname != null && !superclassname.isEmpty() ? (" extends " + superclassname) : (tostring.length() == 0 ? " extends BaseEntity" : " implements java.io.Serializable")) + " {\r\n\r\n");
                    Map<String, StringBuilder> columnMap = new HashMap<>();
                    Map<String, StringBuilder> getsetMap = new HashMap<>();
                    while (rs.next()) {
                        String column = rs.getString("COLUMN_NAME");
                        String type = rs.getString("DATA_TYPE").toUpperCase();
                        String remark = rs.getString("COLUMN_COMMENT");
                        String def = rs.getString("COLUMN_DEFAULT");
                        String key = rs.getString("COLUMN_KEY");
                        StringBuilder fieldsb = new StringBuilder();
                        if (key != null && key.contains("PRI")) {
                            fieldsb.append("    @Id");
                        } else if (superColumns.contains(column)) {  //跳过被继承的重复字段
                            continue;
                        }
                        fieldsb.append("\r\n");

                        int length = 0;
                        int precision = 0;
                        int scale = 0;
                        String ctype = "NULL";
                        String precisionstr = rs.getString("NUMERIC_PRECISION");
                        String scalestr = rs.getString("NUMERIC_SCALE");
                        if ("INT".equalsIgnoreCase(type)) {
                            ctype = "int";
                        } else if ("BIGINT".equalsIgnoreCase(type)) {
                            ctype = "long";
                        } else if ("SMALLINT".equalsIgnoreCase(type)) {
                            ctype = "short";
                        } else if ("FLOAT".equalsIgnoreCase(type)) {
                            ctype = "float";
                        } else if ("DECIMAL".equalsIgnoreCase(type)) {
                            ctype = "float";
                            precision = precisionstr == null ? 0 : Integer.parseInt(precisionstr);
                            scale = scalestr == null ? 0 : Integer.parseInt(scalestr);
                        } else if ("DOUBLE".equalsIgnoreCase(type)) {
                            ctype = "double";
                            precision = precisionstr == null ? 0 : Integer.parseInt(precisionstr);
                            scale = scalestr == null ? 0 : Integer.parseInt(scalestr);
                        } else if ("VARCHAR".equalsIgnoreCase(type)) {
                            ctype = "String";
                            String maxsize = rs.getString("CHARACTER_MAXIMUM_LENGTH");
                            length = maxsize == null ? 0 : Integer.parseInt(maxsize);
                        } else if (type.contains("TEXT")) {
                            ctype = "String";
                        } else if (type.contains("BLOB")) {
                            ctype = "byte[]";
                        }
                        fieldsb.append("    @Column(");
                        if ("createtime".equals(column)) fieldsb.append("updatable = false, ");
                        if (length > 0) fieldsb.append("length = ").append(length).append(", ");
                        if (precision > 0) fieldsb.append("precision = ").append(precision).append(", ");
                        if (scale > 0) fieldsb.append("scale = ").append(scale).append(", ");
                        fieldsb.append("comment = \"" + remark.replace('"', '\'') + "\")\r\n");

                        fieldsb.append("    private " + ctype + " " + column);
                        if (def != null && !"0".equals(def)) {
                            String d = def.replace('\'', '\"');
                            fieldsb.append(" = ").append(d.isEmpty() ? "\"\"" : d.toString());
                        } else if ("String".equals(ctype)) {
                            fieldsb.append(" = \"\"");
                        }
                        fieldsb.append(";\r\n");

                        char[] chs2 = column.toCharArray();
                        chs2[0] = Character.toUpperCase(chs2[0]);
                        String sgname = new String(chs2);

                        StringBuilder setgetsb = new StringBuilder();
                        setgetsb.append("\r\n    public void set" + sgname + "(" + ctype + " " + column + ") {\r\n");
                        setgetsb.append("        this." + column + " = " + column + ";\r\n");
                        setgetsb.append("    }\r\n");

                        setgetsb.append("\r\n    public " + ctype + " get" + sgname + "() {\r\n");
                        setgetsb.append("        return this." + column + ";\r\n");
                        setgetsb.append("    }\r\n");
                        columnMap.put(column, fieldsb);
                        getsetMap.put(column, setgetsb);
                    }
                    List<StringBuilder> list = new ArrayList<>();
                    for (String column : columns) {
                        if (superColumns.contains(column)) continue;
                        list.add(columnMap.get(column));
                    }
                    for (String column : columns) {
                        if (superColumns.contains(column)) continue;
                        list.add(getsetMap.get(column));
                    }
                    for (StringBuilder item : list) {
                        sb.append(item);
                    }
                    sb.append(tostring);
                    sb.append("}\r\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            }
        });

        return sb.toString();
    }
}
