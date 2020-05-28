/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.base.BaseBean;
import javax.persistence.Transient;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class ConfigLogBean extends BaseBean implements FilterBean {

    @FilterColumn(name = "keyname", comment = "名称集合")
    private String[] keynames;

    @FilterColumn(comment = "名称")
    private String keyname = "";


    @FilterColumn(comment = "修改时间")
    private Range.LongRange createtime;

    @Transient
    private String gameid = "";

    public String[] getKeynames() {
        return keynames;
    }

    public void setKeynames(String[] keynames) {
        this.keynames = keynames;
    }

    public String getKeyname() {
        return keyname;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
    }

    public Range.LongRange getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Range.LongRange createtime) {
        this.createtime = createtime;
    }

    public String getGameid() {
        return gameid;
    }

    public void setGameid(String gameid) {
        this.gameid = gameid;
    }

    
}

