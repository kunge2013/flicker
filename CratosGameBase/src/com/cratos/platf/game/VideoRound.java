/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import java.util.*;
import javax.persistence.Column;

/**
 *
 * @author zhangjx
 */
public class VideoRound extends GameRound {

    @Column(comment = "亲友圈ID")
    protected int clubid;

    @Column(comment = "当前回合index")
    protected int currRoundIndex;

    @Column(comment = "金币底注，为0表示非金币场")
    protected int baseBetCoin;

    @Column(nullable = false, comment = "扩展选项")
    protected Map<String, String> extmap;

    @Column(length = 2048, comment = "Round的扩展信息，是map结构的json")
    protected String custjson = "";

    //------------- his 字段------------------------
    @Column(comment = "分享码")
    protected String shareno = "";

    @Column(comment = "分享时间")
    protected long sharetime;

    @Column(comment = "玩家1")
    protected int userid1;

    @Column(comment = "玩家1胡的牌型，0表示没胡")
    protected long huType1;

    @Column(comment = "玩家1分数")
    protected int roundScore1;

    @Column(comment = "玩家2")
    protected int userid2;

    @Column(comment = "玩家2胡的牌型，0表示没胡")
    protected long huType2;

    @Column(comment = "玩家2分数")
    protected int roundScore2;

    @Column(comment = "玩家3")
    protected int userid3;

    @Column(comment = "玩家3胡的牌型，0表示没胡")
    protected long huType3;

    @Column(comment = "玩家3分数")
    protected int roundScore3;

    @Column(comment = "玩家4")
    protected int userid4;

    @Column(comment = "玩家4胡的牌型，0表示没胡")
    protected long huType4;

    @Column(comment = "玩家4分数")
    protected int roundScore4;

    @Column(comment = "玩家列表")
    protected List<VideoPlayer> videoPlayers;

    @Column(length = 65536, comment = "回放明细")
    protected String eventJson;

    public VideoPlayer addVideoPlayer(GamePlayer player, long huType, int roundScore) {
        return this.addVideoPlayer(new VideoPlayer(player, huType, roundScore));
    }

    public VideoPlayer addVideoPlayer(VideoPlayer player) {
        if (this.videoPlayers == null) this.videoPlayers = new ArrayList<>();
        this.videoPlayers.add(player);
        return player;
    }

    public int getClubid() {
        return clubid;
    }

    public void setClubid(int clubid) {
        this.clubid = clubid;
    }

    public int getBaseBetCoin() {
        return baseBetCoin;
    }

    public void setBaseBetCoin(int baseBetCoin) {
        this.baseBetCoin = baseBetCoin;
    }

    public int getCurrRoundIndex() {
        return currRoundIndex;
    }

    public void setCurrRoundIndex(int currRoundIndex) {
        this.currRoundIndex = currRoundIndex;
    }

    public Map<String, String> getExtmap() {
        return extmap;
    }

    public void setExtmap(Map<String, String> extmap) {
        this.extmap = extmap;
    }

    public String getCustjson() {
        return custjson;
    }

    public void setCustjson(String custjson) {
        this.custjson = custjson;
    }

    public String getShareno() {
        return shareno;
    }

    public void setShareno(String shareno) {
        this.shareno = shareno;
    }

    public long getSharetime() {
        return sharetime;
    }

    public void setSharetime(long sharetime) {
        this.sharetime = sharetime;
    }

    public int getUserid1() {
        return userid1;
    }

    public void setUserid1(int userid1) {
        this.userid1 = userid1;
    }

    public long getHuType1() {
        return huType1;
    }

    public void setHuType1(long huType1) {
        this.huType1 = huType1;
    }

    public int getRoundScore1() {
        return roundScore1;
    }

    public void setRoundScore1(int roundScore1) {
        this.roundScore1 = roundScore1;
    }

    public int getUserid2() {
        return userid2;
    }

    public void setUserid2(int userid2) {
        this.userid2 = userid2;
    }

    public long getHuType2() {
        return huType2;
    }

    public void setHuType2(long huType2) {
        this.huType2 = huType2;
    }

    public int getRoundScore2() {
        return roundScore2;
    }

    public void setRoundScore2(int roundScore2) {
        this.roundScore2 = roundScore2;
    }

    public int getUserid3() {
        return userid3;
    }

    public void setUserid3(int userid3) {
        this.userid3 = userid3;
    }

    public long getHuType3() {
        return huType3;
    }

    public void setHuType3(long huType3) {
        this.huType3 = huType3;
    }

    public int getRoundScore3() {
        return roundScore3;
    }

    public void setRoundScore3(int roundScore3) {
        this.roundScore3 = roundScore3;
    }

    public int getUserid4() {
        return userid4;
    }

    public void setUserid4(int userid4) {
        this.userid4 = userid4;
    }

    public long getHuType4() {
        return huType4;
    }

    public void setHuType4(long huType4) {
        this.huType4 = huType4;
    }

    public int getRoundScore4() {
        return roundScore4;
    }

    public void setRoundScore4(int roundScore4) {
        this.roundScore4 = roundScore4;
    }

    public List<VideoPlayer> getVideoPlayers() {
        return videoPlayers;
    }

    public void setVideoPlayers(List<VideoPlayer> videoPlayers) {
        this.videoPlayers = videoPlayers;
    }

    public String getEventJson() {
        return eventJson;
    }

    public void setEventJson(String eventJson) {
        this.eventJson = eventJson;
    }

}
