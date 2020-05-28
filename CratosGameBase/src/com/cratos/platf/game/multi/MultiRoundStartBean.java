/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.multi;

import com.cratos.platf.base.BaseBean;
import com.cratos.platf.game.GameActionEvent;
import java.util.*;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
public class MultiRoundStartBean extends BaseBean {

    protected List<GameActionEvent> events;

    protected Map<String, Object> attributes;

    public MultiRoundStartBean() {
    }

    public MultiRoundStartBean(List<GameActionEvent> events) {
        this.events = events;
    }

    public MultiRoundStartBean(List<GameActionEvent> events, Object... attributes) {
        this.events = events;
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

    public MultiRoundStartBean addAttribute(String key, Object value) {
        if (this.attributes == null) this.attributes = new LinkedHashMap<>();
        this.attributes.put(key, value);
        return this;
    }

    public MultiRoundStartBean attributes(Object... attributes) {
        this.attributes = Utility.ofMap(attributes);
        return this;
    }

    public MultiRoundStartBean attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public List<GameActionEvent> getEvents() {
        return events;
    }

    public void setEvents(List<GameActionEvent> events) {
        this.events = events;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}
