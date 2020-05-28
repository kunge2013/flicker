/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.mission;

import static com.cratos.platf.mission.MissionRecord.MISSION_STATUS_DOING;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
public class MissionOnceRecord extends MissionRecord {

    private static final Reproduce<MissionOnceRecord, MissionInfo> infoReproduce = Reproduce.create(MissionOnceRecord.class, MissionInfo.class);

    private static final Reproduce<MissionOnceRecordHis, MissionOnceRecord> hisReproduce = Reproduce.create(MissionOnceRecordHis.class, MissionOnceRecord.class);

    public MissionOnceRecord() {
    }

    public MissionOnceRecord(MissionInfo info, int userid, long createtime) {
        infoReproduce.apply(this, info);
        this.userid = userid;
        this.createtime = createtime;
        this.missionstatus = MISSION_STATUS_DOING;
        this.missionrecordid = this.missionkind + "-" + this.missionid + "-" + this.userid;
    }

    public MissionOnceRecordHis createRecordHis(long time) {
        MissionOnceRecordHis his = hisReproduce.apply(new MissionOnceRecordHis(), this);
        his.setMissionstatus(MISSION_STATUS_FINISH);
        his.setFinishtime(time);
        return his;
    }
}
