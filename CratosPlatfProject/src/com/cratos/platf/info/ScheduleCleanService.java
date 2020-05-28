/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.BaseService;
import com.cratos.platf.mission.MissionDayRecord;
import com.cratos.platf.profit.ProfitDayRecord;
import com.cratos.platf.user.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("定时删除数据服务")
@Priority(800000)
public class ScheduleCleanService extends BaseService {

    public static final int DELDATA_EXPIRE_DAYS = 5; //31 //删除过期天数的数据

    @Resource(name = "platf")
    protected DataSource source;

    protected ScheduledThreadPoolExecutor scheduler;

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(3, (Runnable r) -> {
            final Thread t = new Thread(r, "ScheduleCleanService-Task-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                dropDistRecordTable(System.currentTimeMillis());
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, Utility.midnight() + 24 * 60 * 60 * 1000L - System.currentTimeMillis(), 24 * 60 * 60 * 1000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void dropDistRecordTable(long firsttime) {
        long time = firsttime - DELDATA_EXPIRE_DAYS * 24 * 60 * 60 * 1000L; //
        FilterNode node = FilterNode.create("createtime", time);
        source.dropTable(UserDiamondRecord.class, node);
        source.dropTable(UserCouponRecord.class, node);
        source.dropTable(UserDayRecord.class, node);
        source.dropTable(UserLoginRecord.class, node);
        source.dropTable(ProfitDayRecord.class, node);
        source.dropTable(MissionDayRecord.class, node);
    }

    public void run() {
    }

}
