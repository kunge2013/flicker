/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.notice;

import com.cratos.platf.base.*;
import java.util.List;
import java.util.concurrent.*;
import javax.annotation.Resource;
import static com.cratos.platf.base.RetCodes.*;
import java.util.logging.Level;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("验证码服务")
public class RandomService extends BaseService {

    @Resource(name = "platf")
    protected DataSource noticeSource;

    @Resource(name = "property.schedule.task")
    private boolean task;

    private ScheduledThreadPoolExecutor scheduler;

    @Override
    public void init(AnyValue conf) {
        if (task) {
            scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
                final Thread t = new Thread(r, this.getClass().getSimpleName() + "-RandomTask-Thread");
                t.setDaemon(true);
                return t;
            });
            final long dayms = 10 * 60 * 1000L; //10分钟执行一次
            final long delay = dayms - System.currentTimeMillis() % dayms;
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    //超过十分钟视为过期
                    FilterNode node = FilterNode.create("createtime", FilterExpress.LESSTHANOREQUALTO, System.currentTimeMillis() - 10 * 60 * 1000);
                    Flipper flipper = new Flipper();
                    do {
                        Sheet<RandomCode> sheet = noticeSource.querySheet(RandomCode.class, flipper, node);
                        sheet.forEach(x -> {
                            noticeSource.insert(x.createRandomCodeHis(RandomCodeHis.RETCODE_EXP));
                            noticeSource.delete(x);
                        });
                        if (sheet.isEmpty() || sheet.getRows().size() < flipper.getLimit()) break;
                    } while (true);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
                }
            }, delay, dayms, TimeUnit.MILLISECONDS);
            logger.finest(this.getClass().getSimpleName() + " start RandomTask task scheduler executor");
        }
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    public void removeRandomCode(RandomCode code) {
        noticeSource.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        noticeSource.delete(RandomCode.class, code.getRandomcode());
    }

    public void expireRandomCode(RandomCode code) {
        noticeSource.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_EXP));
        noticeSource.delete(RandomCode.class, code.getRandomcode());
    }

    public RetResult<RandomCode> checkRandomCode(String targetid, String randomcode, short type) {
        if (randomcode == null || randomcode.isEmpty()) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        if (targetid != null && targetid.length() > 5 && randomcode.length() < 30) randomcode = targetid + "-" + randomcode;
        RandomCode code = noticeSource.find(RandomCode.class, randomcode);
        if (code != null && type > 0 && code.getType() != type) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        return code == null ? RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL) : (code.isExpired() ? RetCodes.retResult(RET_USER_RANDCODE_EXPIRED) : new RetResult(code));
    }

    public List<RandomCode> queryRandomCodeByMobile(String mobile) {
        return noticeSource.queryList(RandomCode.class, FilterNode.create("randomcode", FilterExpress.LIKE, mobile + "-%"));
    }

    public List<RandomCode> queryRandomCodeByMobile(short type, String mobile) {
        return noticeSource.queryList(RandomCode.class, FilterNode.create("type", type).and("randomcode", FilterExpress.LIKE, mobile + "-%"));
    }

    public RetResult createRandomCode(RandomCode entity) {
        entity.setCreatetime(System.currentTimeMillis());
        noticeSource.insert(entity);
        return RetResult.success();
    }

    public void freshRandomCodeTime(String randomcode, short type) {
        noticeSource.updateColumn(RandomCode.class, randomcode, "createtime", System.currentTimeMillis());
        if (type > 0) noticeSource.updateColumn(RandomCode.class, randomcode, "type", type);
    }

    public boolean existsRandomCode(String randomcode) {
        return noticeSource.exists(RandomCode.class, FilterNode.create("randomcode", randomcode));
    }

    public RandomCode findRandomCode(String randomcode) {
        return noticeSource.find(RandomCode.class, FilterNode.create("randomcode", randomcode));
    }
}
