/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.hundred;

import com.cratos.platf.base.BaseEntity;
import java.util.*;
import org.redkale.convert.*;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
public class HundredGameResult extends BaseEntity {

    protected int resultid;

    protected String cards = "";

    protected Map<String, Object> attributes;

    public HundredGameResult() {
    }

    public HundredGameResult(int resultid) {
        this.resultid = resultid;
    }

    public HundredGameResult(int resultid, String cards) {
        this.resultid = resultid;
        this.cards = cards;
    }

    public HundredGameResult(int resultid, Object... attributes) {
        this.resultid = resultid;
        this.attributes = Utility.ofMap(attributes);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return attributes == null ? null : (T) attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, T defValue) {
        return attributes == null ? defValue : (T) attributes.getOrDefault(name, defValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(String name) {
        if (attributes == null) return null;
        return (T) attributes.remove(name);
    }

    public HundredGameResult addAttribute(String key, Object value) {
        if (this.attributes == null) this.attributes = new LinkedHashMap<>();
        this.attributes.put(key, value);
        return this;
    }

    public HundredGameResult attributes(Object... attributes) {
        this.attributes = Utility.ofMap(attributes);
        return this;
    }

    public HundredGameResult attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public int getResultid() {
        return resultid;
    }

    public void setResultid(int resultid) {
        this.resultid = resultid;
    }

    public String getCards() {
        return cards;
    }

    public void setCards(String cards) {
        this.cards = cards;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}
