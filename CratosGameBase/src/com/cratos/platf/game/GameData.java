/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseEntity;
import javax.persistence.*;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.LogLevel;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
public class GameData extends BaseEntity {

    public static final String GAMEDATA_GAMEID_COINPOOL_1 = "GAMEDATA_{GAMEID}_COINPOOL_1";

    public static final String GAMEDATA_GAMEID_COINPOOL_2 = "GAMEDATA_{GAMEID}_COINPOOL_2";

    public static final String GAMEDATA_GAMEID_COINPOOL_3 = "GAMEDATA_{GAMEID}_COINPOOL_3";

    public static final String GAMEDATA_GAMEID_COINPOOL_4 = "GAMEDATA_{GAMEID}_COINPOOL_4";

    public static final String GAMEDATA_GAMEID_COINPOOL2_1 = "GAMEDATA_{GAMEID}_COINPOOL2_1";

    public static final String GAMEDATA_GAMEID_COINPOOL2_2 = "GAMEDATA_{GAMEID}_COINPOOL2_2";

    public static final String GAMEDATA_GAMEID_COINPOOL2_3 = "GAMEDATA_{GAMEID}_COINPOOL2_3";

    public static final String GAMEDATA_GAMEID_COINPOOL2_4 = "GAMEDATA_{GAMEID}_COINPOOL2_4";

    public static final String GAMEDATA_GAMEID_COINPOOL3_1 = "GAMEDATA_{GAMEID}_COINPOOL3_1";

    public static final String GAMEDATA_GAMEID_COINPOOL3_2 = "GAMEDATA_{GAMEID}_COINPOOL3_2";

    public static final String GAMEDATA_GAMEID_COINPOOL3_3 = "GAMEDATA_{GAMEID}_COINPOOL3_3";

    public static final String GAMEDATA_GAMEID_COINPOOL3_4 = "GAMEDATA_{GAMEID}_COINPOOL3_4";

    @Id
    @Column(length = 32, comment = "KEY, 大写 ")
    protected String keyname = "";

    @Column(comment = "KEY的数值")
    protected long numvalue;

    @Column(length = 2048, comment = "KEY的字符串值")
    protected String strvalue = "";

    @Column(updatable = false, length = 255, comment = "KEY描述")
    protected String keydesc = "";

    @Column(comment = "更新时间")
    protected long updatetime;

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    public String getKeyname() {
        return this.keyname;
    }

    public void setNumvalue(long numvalue) {
        this.numvalue = numvalue;
    }

    public long getNumvalue() {
        return this.numvalue;
    }

    public String getStrvalue() {
        return strvalue;
    }

    public void setStrvalue(String strvalue) {
        this.strvalue = strvalue;
    }

    public void setKeydesc(String keydesc) {
        this.keydesc = keydesc;
    }

    public String getKeydesc() {
        return this.keydesc;
    }

    public void setUpdatetime(long updatetime) {
        this.updatetime = updatetime;
    }

    public long getUpdatetime() {
        return this.updatetime;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }

}
