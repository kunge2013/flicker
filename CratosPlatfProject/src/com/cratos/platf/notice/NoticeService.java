/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.notice;

import com.cratos.platf.base.BaseService;
import javax.annotation.Resource;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("消息推送服务")
public class NoticeService extends BaseService {

    @Resource(name = "platf")
    protected DataSource noticeSource;

    public Sheet<NoticeRecord> queryNoticeRecord(NoticeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return noticeSource.querySheet(NoticeRecord.class, flipper, bean);
    }
}
