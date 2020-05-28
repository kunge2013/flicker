package com.cratos.platf.mission;

import javax.persistence.*;
import java.io.Serializable;
import org.redkale.convert.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@LogLevel("FINER")
@Table(comment = "日活跃任务记录表")
@DistributeTable(strategy = MissionDayRecord.TableStrategy.class)
public class MissionDayRecord extends MissionRecord {

    private static final Reproduce<MissionDayRecord, MissionInfo> reproduce = Reproduce.create(MissionDayRecord.class, MissionInfo.class);

    @Column(comment = "日期; 20190909")
    protected int intday;

    @Column(updatable = false, comment = "完成时间")
    protected long finishtime;

    public MissionDayRecord() {
    }

    public MissionDayRecord(MissionInfo info, int userid, long createtime) {
        reproduce.apply(this, info);
        this.userid = userid;
        this.intday = Utility.yyyyMMdd(createtime);
        this.createtime = createtime;
        this.missionstatus = MISSION_STATUS_DOING;
        this.missionrecordid = this.missionkind + "-" + this.missionid + "-" + this.userid + "-" + this.intday;
    }

    public void setIntday(int intday) {
        this.intday = intday;
    }

    public int getIntday() {
        return this.intday;
    }

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getFinishtime() {
        return finishtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<MissionDayRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, MissionDayRecord bean) {
            return table + "_" + bean.getIntday();
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + id.substring(id.lastIndexOf('-') + 1);
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Object intday = node.findValue("intday");
            if (intday != null && intday instanceof Integer) {
                int pos = table.indexOf('.');
                return table.substring(pos + 1) + "_" + intday;
            }
            Object time = node.findValue("createtime");
            if (time == null) time = node.findValue("#createtime");
            if (time instanceof Long) return getSingleTable(table, (Long) time);
            Range.LongRange createtime = (Range.LongRange) time;
            return getSingleTable(table, createtime.getMin());
        }

        private String getSingleTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return table.substring(pos + 1) + "_" + String.format(format, createtime);
        }
    }
}
