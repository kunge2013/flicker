/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.notice;

import com.cratos.platf.base.BaseBean;
import org.redkale.source.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
@Comment("公告过滤类")
public class AnnounceBean extends BaseBean implements FilterBean {

    @FilterColumn(comment = "[状态]: 10:正常;70:过期;80:删除;")
    private short status;

    @FilterColumn(comment = "公告类型; 10:登陆公告; 20:滚动公告; 30:紧急公告; 40:常规公告;")
    private short type;

    @FilterGroup("[OR]start")
    @FilterColumn(express = FilterExpress.LESSTHANOREQUALTO, comment = "公告起始时间")
    private long starttime;

    @FilterGroup("[OR]start")
    @FilterColumn(name = "starttime", comment = "公告起始时间", least = -1)
    private long starttime2 = 0;

    @FilterGroup("[OR]end")
    @FilterColumn(express = FilterExpress.GREATERTHANOREQUALTO, comment = "公告结束时间")
    private long endtime;

    @FilterGroup("[OR]end")
    @FilterColumn(name = "endtime", comment = "公告结束时间", least = -1)
    private long endtime2 = 0;

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getEndtime() {
        return endtime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

    public long getStarttime2() {
        return starttime2;
    }

    public void setStarttime2(long starttime2) {
        this.starttime2 = starttime2;
    }

    public long getEndtime2() {
        return endtime2;
    }

    public void setEndtime2(long endtime2) {
        this.endtime2 = endtime2;
    }

}
