/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import java.util.Map;
import javax.persistence.Column;

/**
 *
 * @author zhangjx
 */
public class GameTableBean extends GamePlayerBean {

    @Column(comment = "玩家ID")
    protected int userid;

    @Column(comment = "房间ID")
    protected String tableid;

    @Column(comment = "牌桌ID")
    protected int tableno;

    @Column(comment = "付费方式; 10:AA付费; 20:房主付费; 30:亲友圈付费;")
    protected short chargeType;

    @Column(comment = "亲友圈ID")
    protected int clubid;

    @Column(comment = "游戏ID")
    protected String gameid;

    @Column(comment = "游戏名称")
    protected String gamename;

    @Column(length = 255, comment = "玩法描述")
    protected String ruleDesc;

    @Column(comment = "机器人数量")
    protected int robot = -1;

    @Column(comment = "最大玩家数")
    protected int maxPlayerCount = -1;

    @Column(comment = "扩展选项")
    protected Map<String, String> extmap;

    @Override
    public Map<String, String> createMap() {
        Map<String, String> map = super.createMap();
        if (extmap != null) map.putAll(extmap);
        map.put("userid", "" + userid);
        if (tableid != null) map.put("tableid", tableid);
        map.put("tableno", "" + tableno);
        map.put("clubid", "" + clubid);
        if (gameid != null) map.put("gameid", gameid);
        if (gamename != null) map.put("gamename", gamename);
        if (ruleDesc != null) map.put("ruleDesc", ruleDesc);
        map.put("robot", "" + robot);
        map.put("maxPlayerCount", "" + maxPlayerCount);
        map.put("chargeType", "" + chargeType);
        return map;
    }

    public String findExtConf(String name, String defvalue) {
        return extmap == null ? defvalue : extmap.getOrDefault(name, defvalue);
    }

    public int findExtConf(String name, int defvalue) {
        return extmap == null ? defvalue : Integer.parseInt(extmap.getOrDefault(name, "" + defvalue));
    }

    public short findExtConf(String name, short defvalue) {
        return extmap == null ? defvalue : Short.parseShort(extmap.getOrDefault(name, "" + defvalue));
    }

    public boolean findExtConf(String name, boolean defvalue) {
        return extmap == null ? defvalue : "true".equalsIgnoreCase(extmap.getOrDefault(name, "" + defvalue));
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public String getTableid() {
        return tableid;
    }

    public void setTableid(String tableid) {
        this.tableid = tableid;
    }

    public int getTableno() {
        return tableno;
    }

    public void setTableno(int tableno) {
        this.tableno = tableno;
    }

    public int getClubid() {
        return clubid;
    }

    public void setClubid(int clubid) {
        this.clubid = clubid;
    }

    public String getGameid() {
        return gameid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    public String getGamename() {
        return gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public String getRuleDesc() {
        return ruleDesc;
    }

    public void setRuleDesc(String ruleDesc) {
        this.ruleDesc = ruleDesc;
    }

    public Map<String, String> getExtmap() {
        return extmap;
    }

    public void setExtmap(Map<String, String> extmap) {
        this.extmap = extmap;
    }

    public int getRobot() {
        return robot;
    }

    public void setRobot(int robot) {
        this.robot = robot;
    }

    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public short getChargeType() {
        return chargeType;
    }

    public void setChargeType(short chargeType) {
        this.chargeType = chargeType;
    }

}
