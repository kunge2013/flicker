/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.game.skywar;

import com.cratos.platf.game.battle.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author zhangjx
 */
public class SkywarEnemyArmy extends BattleEnemyArmy {

    protected final AtomicInteger groupseq = new AtomicInteger(10000);

    public SkywarEnemyArmy(BattleCoinGameService service, BattleGameTable table) {
        super(service, table);
    }

    @Override
    protected void afterCreateEnemyRecord(List<BattleEnemyRecord> enemys) {
        if (enemys.size() != 1) return;
        BattleEnemyRecord enemy = enemys.get(0);
        final BattleEnemyKind kind = enemy.getKind();
        if (kind.getKindtype() == 2) { //巡逻机成批
            final int groupid = groupseq.incrementAndGet();
            if (groupseq.get() >= 10_0000) groupseq.set(10000);
            enemy.setEnemygroupid(groupid);
            int delayms = Math.max(400, (kind.getLifems() * (kind.getWidth() + 5)) / enemy.getLine().getPoints().length);
            int count = 4 + random.nextInt(1);
            for (int i = 0; i < count; i++) {
                this.scheduler.schedule(() -> {
                    BattleEnemyRecord e = table.createEnemyRecord(service, kind, enemy.getLine(), 0, seq);
                    e.setEnemygroupid(groupid);
                    queue.add(List.of(e));
                }, delayms * (i + 1), TimeUnit.MILLISECONDS);
            }
        }
    }
}
