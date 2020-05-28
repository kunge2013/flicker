/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.util;

import com.cratos.platf.base.BaseEntity;
import org.redkale.convert.ConvertDisabled;
import org.redkale.source.DataSource;

/**
 *
 * @author zhangjx
 */
public class SourceAndEntity extends BaseEntity {

    protected DataSource source;

    protected BaseEntity entity;

    public SourceAndEntity() {
    }

    public SourceAndEntity(DataSource source, BaseEntity entity) {
        this.source = source;
        this.entity = entity;
    }

    @ConvertDisabled
    public DataSource getSource() {
        return source;
    }

    public void setSource(DataSource source) {
        this.source = source;
    }

    public BaseEntity getEntity() {
        return entity;
    }

    public void setEntity(BaseEntity entity) {
        this.entity = entity;
    }

}
