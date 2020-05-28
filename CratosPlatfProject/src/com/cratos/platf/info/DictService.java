/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.BaseService;
import javax.annotation.Resource;
import org.redkale.service.Local;
import org.redkale.source.*;
import org.redkale.util.Comment;

/**
 * 字典服务，只能本地化调用
 *
 * @author zhangjx
 */
@Local
@Comment("字典服务")
public class DictService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    @Comment("获取int值")
    public int findDictValue(String key, int defaultValue) {
        DictInfo rs = source.find(DictInfo.class, key);
        return rs == null ? defaultValue : (int) rs.getNumvalue();
    }

    @Comment("获取long值")
    public long findDictValue(String key, long defaultValue) {
        DictInfo rs = source.find(DictInfo.class, key);
        return rs == null ? defaultValue : rs.getNumvalue();
    }

    @Comment("获取字符串值")
    public String findDictValue(String key, String defaultValue) {
        DictInfo rs = source.find(DictInfo.class, key);
        return rs == null ? defaultValue : rs.getStrvalue();
    }

}
