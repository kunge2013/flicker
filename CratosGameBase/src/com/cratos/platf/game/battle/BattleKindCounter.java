/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseBean;
import java.util.concurrent.atomic.AtomicLong;
import org.redkale.convert.ConvertDisabled;

/**
 *
 * @author zhangjx
 */
public class BattleKindCounter extends BaseBean {

    //生成的个数
    public AtomicLong count = new AtomicLong();

    //打到的个数
    public AtomicLong shoted = new AtomicLong();

    //闪避次数
    public AtomicLong missed = new AtomicLong();

    //距离外个数
    public AtomicLong toofar = new AtomicLong();

    //被击杀个数
    public AtomicLong killed = new AtomicLong();

    //被打到次数, 包含击杀
    public AtomicLong hitted = new AtomicLong();

    @ConvertDisabled
    public boolean isEmpty() {
        return this.count.get() == 0 && this.shoted.get() == 0
            && this.missed.get() == 0 && this.toofar.get() == 0
            && this.killed.get() == 0 && this.hitted.get() == 0;
    }

    public void increMissed() {
        missed.incrementAndGet();
    }

    public void increToofar() {
        toofar.incrementAndGet();
    }

    public void increCount() {
        count.incrementAndGet();
    }

    public void increShoted() {
        shoted.incrementAndGet();
    }

    public void increKilled() {
        killed.incrementAndGet();
    }

    public void increHitted() {
        hitted.incrementAndGet();
    }

}
