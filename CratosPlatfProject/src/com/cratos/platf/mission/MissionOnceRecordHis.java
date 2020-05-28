/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.mission;

import javax.persistence.Column;
import org.redkale.convert.*;

/**
 *
 * @author zhangjx
 */
public class MissionOnceRecordHis extends MissionOnceRecord {

    @Column(updatable = false, comment = "完成时间")
    protected long finishtime;

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getFinishtime() {
        return finishtime;
    }
}
