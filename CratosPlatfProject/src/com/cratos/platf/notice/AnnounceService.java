/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.notice;

import com.cratos.platf.base.*;
import static com.cratos.platf.base.RetCodes.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("公告服务")
public class AnnounceService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    @Comment("登陆公告")
    protected Announcement loginAnnouncement;

    private ScheduledThreadPoolExecutor scheduler;

    @Override
    public void init(AnyValue config) {
        loadLoginAnnouncement();
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "-LoginAnnouncementTask-Thread");
            t.setDaemon(true);
            return t;
        });
        final long seconds = 1 * 60 * 1000L;
        final long delay = seconds - System.currentTimeMillis() % seconds; //每分钟执行
        scheduler.scheduleAtFixedRate(() -> {
            try {
                //logger.finest(this.getClass().getSimpleName() + " run LoginAnnouncementTask");
                loadLoginAnnouncement();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "scheduleAtFixedRate error", e);
            }
        }, delay, seconds, TimeUnit.MILLISECONDS);
        logger.finest(this.getClass().getSimpleName() + " start LoginAnnouncementTask task scheduler executor");
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void loadLoginAnnouncement() {
        AnnounceBean bean = new AnnounceBean();
        bean.setStatus(BaseEntity.STATUS_NORMAL);
        bean.setType(Announcement.ANNOUNCE_TYPE_LOGIN);
        bean.setStarttime(System.currentTimeMillis());
        bean.setEndtime(System.currentTimeMillis());
        List<Announcement> list = source.queryList(Announcement.class, new Flipper(1, "endtime ASC"), bean);
        this.loginAnnouncement = list.isEmpty() ? null : list.get(0);
    }

    @Comment("新增公告")
    public RetResult<Integer> createAnnouncement(Announcement entity) {
        if (entity == null) return retResult(RET_PARAMS_ILLEGAL);
        if (entity.getContent() == null || entity.getContent().isEmpty()) return retResult(RET_PARAMS_ILLEGAL);
        if (entity.getStatus() < 1) entity.setStatus(BaseEntity.STATUS_NORMAL);
        entity.setCreatetime(System.currentTimeMillis());
        int today = Utility.today() % 1000000;
        FilterNode node = FilterNode.create("announceid", FilterExpress.GREATERTHAN, today * 1000);
        int maxid = source.getNumberResult(Announcement.class, FilterFunc.MAX, today * 1000, "announceid", node).intValue();
        boolean ok = false;
        for (int i = 0; i < 20; i++) {
            try {
                entity.setAnnounceid(maxid + 1);
                source.insert(entity);
                ok = true;
                break;
            } catch (Exception e) { //并发时可能会重复创建， 忽略异常
                logger.log(Level.INFO, "create Announcement error: " + entity, e);
                maxid = source.getNumberResult(Announcement.class, FilterFunc.MAX, today * 1000, "announceid", node).intValue();
            }
        }
        if (entity.getType() == Announcement.ANNOUNCE_TYPE_LOGIN
            && entity.getStarttime() < System.currentTimeMillis()
            && entity.getEndtime() > System.currentTimeMillis()) {
            this.loginAnnouncement = entity;
        }
        if (!ok) return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        return new RetResult().result(entity.getAnnounceid());
    }

    @Comment("修改公告")
    public RetResult<Integer> updateAnnouncement(Announcement entity) {
        if (entity == null || entity.getAnnounceid() <= 0) {
            return retResult(RET_PARAMS_ILLEGAL);
        }
        if (entity.getContent() == null || entity.getContent().isEmpty()) return retResult(RET_PARAMS_ILLEGAL);
        if (entity.getStatus() < 1) entity.setStatus(BaseEntity.STATUS_NORMAL);
        Announcement originalAnnouncement = source.find(Announcement.class, entity.getAnnounceid());
        // 已过期的公告不允许修改
        if (originalAnnouncement == null || originalAnnouncement.getStatus() == Announcement.ANNOUNCE_TYPE_HROLL) {
            return retResult(RET_PARAMS_ILLEGAL);
        }
        try {
            source.update(entity);
        } catch (Exception e) {
            logger.log(Level.INFO, "update Announcement error: " + entity, e);
            return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        }
        if (entity.getType() == Announcement.ANNOUNCE_TYPE_LOGIN
            && entity.getStarttime() < System.currentTimeMillis()
            && entity.getEndtime() > System.currentTimeMillis()) {
            this.loginAnnouncement = entity;
        }
        return new RetResult().result(entity.getAnnounceid());
    }

    @Comment("删除公告")
    public RetResult<Integer> deleteAnnouncement(int announceid) {
        source.updateColumn(Announcement.class, announceid, "status", BaseEntity.STATUS_DELETED);
        return RetResult.success();
    }

    @Comment("查询公告列表")
    public Sheet<Announcement> queryAnnouncement(AnnounceBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(Announcement.class, flipper, bean);
    }

    @Comment("获取登陆公告")
    public Announcement getLoginAnnouncement() {
        return loginAnnouncement;
    }

}
