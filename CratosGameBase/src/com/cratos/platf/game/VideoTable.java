/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import java.util.*;
import javax.persistence.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
@Comment("回放录像房间信息")
public class VideoTable extends GameTable {

    @Column(comment = "房间ID， 固定5-7位数字，根据TABLIENO_LENGTH确定")
    protected int tableno;

    @Column(comment = "游戏名称")
    protected String gamename;

    @Column(comment = "房主ID")
    protected int tableUserid;

    @Column(length = 255, comment = "玩法描述")
    protected String ruleDesc;

    @Column(comment = "亲友圈ID")
    protected int clubid;

    @Column(comment = "亲友圈预扣的钻石")
    protected int clubdiamond;

    @Column(comment = "付费方式; 10:AA付费; 20:房主付费;")
    protected short chargeType;

    @Comment("金币底注，为0表示非金币场")
    protected int baseBetCoin;

    @Column(comment = "初始点数") //比如比赛，先给一定点数， 输完为止
    protected int initTableScore;

    @Column(comment = "封顶分数")
    protected int maxRoundScore;

    @Column(comment = "牌桌状态")
    protected short status;

    @Column(comment = "最大局数")
    protected int maxRoundCount;

    @Column(comment = "最大玩家数")
    protected int maxPlayerCount = 4;

    @Column(comment = "每人4回合耗钻数")
    protected int chargePerOneDiamond = 1;

    @Column(comment = "本房间的总耗钻数")
    protected int costDiamonds;

    @Column(nullable = false, comment = "扩展选项")
    protected Map<String, String> extmap;

    @Column(comment = "当前第几局,从1开始")
    protected int currRoundIndex;

    //------------- his 字段------------------------
    @Column(comment = "最大赢家")
    protected int winMostUserid;

    @Column(length = 2048, comment = "Table的扩展信息，是map结构的json")
    protected String custjson = "";

    @Column(comment = "玩家1")
    protected int userid1;

    @Column(comment = "玩家1分数")
    protected int tableScore1;

    @Column(comment = "玩家2")
    protected int userid2;

    @Column(comment = "玩家2分数")
    protected int tableScore2;

    @Column(comment = "玩家3")
    protected int userid3;

    @Column(comment = "玩家3分数")
    protected int tableScore3;

    @Column(comment = "玩家4")
    protected int userid4;

    @Column(comment = "玩家4分数")
    protected int tableScore4;

    @Column(comment = "玩家列表")
    protected List<VideoPlayer> videoPlayers;

    @Transient
    @Column(comment = "当前回合")
    protected VideoRound currRound;

    public void addVideoPlayer(GamePlayer player, int tableScore) {
        this.addVideoPlayer(new VideoPlayer(player, tableScore));
    }

    public void addVideoPlayer(VideoPlayer player) {
        if (this.videoPlayers == null) this.videoPlayers = new ArrayList<>();
        this.videoPlayers.add(player);
    }

    public int getTableno() {
        return tableno;
    }

    public void setTableno(int tableno) {
        this.tableno = tableno;
    }

    public String getGamename() {
        return gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public int getTableUserid() {
        return tableUserid;
    }

    public void setTableUserid(int tableUserid) {
        this.tableUserid = tableUserid;
    }

    public String getRuleDesc() {
        return ruleDesc;
    }

    public void setRuleDesc(String ruleDesc) {
        this.ruleDesc = ruleDesc;
    }

    public int getClubid() {
        return clubid;
    }

    public void setClubid(int clubid) {
        this.clubid = clubid;
    }

    public int getClubdiamond() {
        return clubdiamond;
    }

    public void setClubdiamond(int clubdiamond) {
        this.clubdiamond = clubdiamond;
    }

    public short getChargeType() {
        return chargeType;
    }

    public void setChargeType(short chargeType) {
        this.chargeType = chargeType;
    }

    public int getBaseBetCoin() {
        return baseBetCoin;
    }

    public void setBaseBetCoin(int baseBetCoin) {
        this.baseBetCoin = baseBetCoin;
    }

    public int getInitTableScore() {
        return initTableScore;
    }

    public void setInitTableScore(int initTableScore) {
        this.initTableScore = initTableScore;
    }

    public int getMaxRoundScore() {
        return maxRoundScore;
    }

    public void setMaxRoundScore(int maxRoundScore) {
        this.maxRoundScore = maxRoundScore;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public int getMaxRoundCount() {
        return maxRoundCount;
    }

    public void setMaxRoundCount(int maxRoundCount) {
        this.maxRoundCount = maxRoundCount;
    }

    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public int getChargePerOneDiamond() {
        return chargePerOneDiamond;
    }

    public void setChargePerOneDiamond(int chargePerOneDiamond) {
        this.chargePerOneDiamond = chargePerOneDiamond;
    }

    public int getCostDiamonds() {
        return costDiamonds;
    }

    public void setCostDiamonds(int costDiamonds) {
        this.costDiamonds = costDiamonds;
    }

    public Map<String, String> getExtmap() {
        return extmap;
    }

    public void setExtmap(Map<String, String> extmap) {
        this.extmap = extmap;
    }

    public int getCurrRoundIndex() {
        return currRoundIndex;
    }

    public void setCurrRoundIndex(int currRoundIndex) {
        this.currRoundIndex = currRoundIndex;
    }

    public int getWinMostUserid() {
        return winMostUserid;
    }

    public void setWinMostUserid(int winMostUserid) {
        this.winMostUserid = winMostUserid;
    }

    public int getUserid1() {
        return userid1;
    }

    public void setUserid1(int userid1) {
        this.userid1 = userid1;
    }

    public int getTableScore1() {
        return tableScore1;
    }

    public void setTableScore1(int tableScore1) {
        this.tableScore1 = tableScore1;
    }

    public int getUserid2() {
        return userid2;
    }

    public void setUserid2(int userid2) {
        this.userid2 = userid2;
    }

    public int getTableScore2() {
        return tableScore2;
    }

    public void setTableScore2(int tableScore2) {
        this.tableScore2 = tableScore2;
    }

    public int getUserid3() {
        return userid3;
    }

    public void setUserid3(int userid3) {
        this.userid3 = userid3;
    }

    public int getTableScore3() {
        return tableScore3;
    }

    public void setTableScore3(int tableScore3) {
        this.tableScore3 = tableScore3;
    }

    public int getUserid4() {
        return userid4;
    }

    public void setUserid4(int userid4) {
        this.userid4 = userid4;
    }

    public int getTableScore4() {
        return tableScore4;
    }

    public void setTableScore4(int tableScore4) {
        this.tableScore4 = tableScore4;
    }

    public List<VideoPlayer> getVideoPlayers() {
        return videoPlayers;
    }

    public void setVideoPlayers(List<VideoPlayer> players) {
        this.videoPlayers = players;
    }

    public String getCustjson() {
        return custjson;
    }

    public void setCustjson(String custjson) {
        this.custjson = custjson;
    }

    public VideoRound getCurrRound() {
        return currRound;
    }

    public void setCurrRound(VideoRound currRound) {
        this.currRound = currRound;
    }

}
