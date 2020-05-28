/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.base;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import javax.persistence.Transient;
import org.redkale.service.*;

/**
 *
 * @author zhangjx
 */
public abstract class BaseService extends AbstractService {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected final boolean finer = logger.isLoggable(Level.FINER);

    protected final boolean finest = logger.isLoggable(Level.FINEST);

    @Transient //用户锁
    private static final ConcurrentHashMap<Integer, Object> userLockMap = new ConcurrentHashMap();

    protected Object userLock(int userid) {
        return userLockMap.computeIfAbsent(userid, (id) -> new Object());
    }

    protected Object removeUserLock(int userid) {
        return userLockMap.remove((Integer) userid);
    }

    protected static void await(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
        }
    }
}
