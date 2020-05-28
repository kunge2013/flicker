/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.battle.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.redkale.convert.ConvertDisabled;

/**
 *
 * @author zhangjx
 */
public class SkywarEnemyRecord extends BattleEnemyRecord<SkywarEnemyKind> {

    public SkywarEnemyRecord() {
    }

    public SkywarEnemyRecord(SkywarEnemyKind kind, BattleEnemyLine line, AtomicInteger seq) {
        this(kind, line, 0, seq);
    }

    public SkywarEnemyRecord(SkywarEnemyKind kind, BattleEnemyLine line, int lifetimesx, AtomicInteger seq) {
        super(kind, line, lifetimesx, seq);
    }

    @ConvertDisabled
    public int getFactor() {
        return this.kind.getKindcoin();
    }

}
