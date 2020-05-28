/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import static com.cratos.platf.game.GameTable.TABLE_CHARGETYPE_AA;
import com.cratos.platf.game.GameTableBean;
import java.util.Map;
import javax.persistence.Column;

/**
 *
 * @author zhangjx
 */
public class MultiGameTableBean extends GameTableBean {

    protected boolean mutlyMode;

    @Column(comment = "初始点数")
    protected int initTableScore;

    @Column(comment = "封顶分")
    protected int maxRoundScore;

    @Column(comment = "最大局数")
    protected int maxRoundCount;

    public MultiGameTableBean() {
        super();
        this.chargeType = TABLE_CHARGETYPE_AA;
    }
    
    @Override
    public Map<String, String> createMap() {
        Map<String, String> map = super.createMap();
        map.put("initTableScore", "" + initTableScore);
        map.put("maxRoundScore", "" + maxRoundScore);
        map.put("maxRoundCount", "" + maxRoundCount);
        return map;
    }

    public boolean isMutlyMode() {
        return mutlyMode;
    }

    public void setMutlyMode(boolean mutlyMode) {
        this.mutlyMode = mutlyMode;
    }

    public int getMaxRoundScore() {
        return maxRoundScore;
    }

    public void setMaxRoundScore(int maxRoundScore) {
        this.maxRoundScore = maxRoundScore;
    }

    public int getMaxRoundCount() {
        return maxRoundCount;
    }

    public void setMaxRoundCount(int maxRoundCount) {
        this.maxRoundCount = maxRoundCount;
    }

    public int getInitTableScore() {
        return initTableScore;
    }

    public void setInitTableScore(int initTableScore) {
        this.initTableScore = initTableScore;
    }

}
