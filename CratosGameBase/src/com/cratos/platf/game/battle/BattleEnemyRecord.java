/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.battle;

import com.cratos.platf.base.BaseEntity;
import com.cratos.platf.game.GamePoint;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 * @param <K> BattleEnemyKind
 */
public class BattleEnemyRecord<K extends BattleEnemyKind> extends BaseEntity implements Comparable<BattleEnemyRecord> {

    @Column(comment = "对象ID,随机数*1000+kindid")
    protected int enemyid;

    @Column(comment = "对象组ID")
    protected int enemygroupid;

    @Column(comment = "种类")
    protected BattleEnemyKind kind;

    @Column(comment = "轨迹")
    protected BattleEnemyLine line;

    @Column(comment = "自定义的生命周期, 没有值则取line中的值")
    protected int lifemsx;

    @Column(comment = "刷新数量")
    protected int newnum;

    @Column(comment = "刷新概率")
    protected int newrate;

    @Column(comment = "创建时间")
    protected long createtime;

    @Transient
    @Column(comment = "当前生命值")
    protected AtomicInteger[] currhps;

    public BattleEnemyRecord() {
    }

    public BattleEnemyRecord(K kind, BattleEnemyLine line, AtomicInteger seq) {
        this(kind, line, 0, seq);
    }

    public BattleEnemyRecord(K kind, BattleEnemyLine line, int lifetimesx, AtomicInteger seq) {
        this.kind = kind;
        this.line = line;
        this.lifemsx = lifetimesx;
        this.enemyid = (seq == null ? 1 : seq.incrementAndGet() * 1000) + kind.getKindid();
        int hp = kind.getHp();
        this.currhps = new AtomicInteger[]{new AtomicInteger(hp), new AtomicInteger(hp), new AtomicInteger(hp), new AtomicInteger(hp)};
        this.createtime = System.currentTimeMillis();
    }

    public BattleEnemyRecord resetCreatetime() {
        //250ms为输出和请求的网络延迟
        this.createtime = System.currentTimeMillis() + 250 + (this.getLifems() * (this.kind.getWidth() / 2) / this.line.getPoints().length);
        return this;
    }

    @Comment("当前敌人的坐标, 返回null表示敌人已不在屏幕内")
    public GamePoint currGamePoint() {
        GamePoint[] points = this.line.getPoints();
        int index = points.length * ((int) (System.currentTimeMillis() - this.createtime)) / this.getLifems();
        if (index < 0) return points[0]; //还没进入屏幕
        return index >= points.length ? null : points[index];
    }

    @ConvertDisabled
    @Comment("是否在指定范围内")
    public boolean isRange(Logger logger, float x, float y) {
        GamePoint point = currGamePoint();
        if (point == null) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("敌人(id:" + getEnemyid() + ",w:" + kind.getWidth() + ")当前位置为空");
            }
            return false;
        }
        double distance = Math.sqrt(Math.abs((point.x - x) * (point.x - x) + (point.y - y) * (point.y - y)));
        return distance <= Math.max(kind.getWidth(), kind.getHeight());
    }

    @ConvertDisabled
    @Comment("获取两个敌机之间的距离")
    public double distance(BattleEnemyRecord enemy) {
        GamePoint selfpoint = currGamePoint();
        if (selfpoint == null) return Integer.MAX_VALUE;
        GamePoint otherpoint = enemy.currGamePoint();
        if (otherpoint == null) return Integer.MAX_VALUE;
        return Math.sqrt(Math.abs((selfpoint.x - otherpoint.x) * (selfpoint.x - otherpoint.x) + (selfpoint.y - otherpoint.y) * (selfpoint.y - otherpoint.y)));
    }

    @ConvertDisabled
    @Comment("是否已死(包含过期)")
    public boolean isDead() {
        return isExpired() || isEmptyBlood();
    }

    @ConvertDisabled
    @Comment("是否空血")
    public boolean isEmptyBlood() {
        for (AtomicInteger currhp : currhps) {
            if (currhp.get() < 1) return true;
        }
        return false;
    }

    @ConvertDisabled
    @Comment("是否过期")
    public boolean isExpired() {
        return getLivingms() > (getLifems() + 3000);
    }

    @Comment("生命周期，存活的毫秒数")
    public int getLifems() {
        return lifemsx > 0 ? lifemsx : (kind == null ? 0 : kind.getLifems());
    }

    @Comment("存活的毫秒数")
    public int getLivingms() {
        int rs = (int) (System.currentTimeMillis() - this.createtime);
        return rs >= 0 ? rs : 0;
    }

    @Comment("轨迹ID")
    public int getLineid() {
        return line == null ? 0 : line.getLineid();
    }

    @Comment("敌机种类")
    public int getKindid() {
        return kind == null ? 0 : kind.getKindid();
    }

    public int getEnemyid() {
        return enemyid;
    }

    public void setEnemyid(int enemyid) {
        this.enemyid = enemyid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getEnemygroupid() {
        return enemygroupid;
    }

    public void setEnemygroupid(int enemygroupid) {
        this.enemygroupid = enemygroupid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public BattleEnemyKind getKind() {
        return kind;
    }

    public void setKind(BattleEnemyKind kind) {
        this.kind = kind;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public BattleEnemyLine getLine() {
        return line;
    }

    public void setLine(BattleEnemyLine line) {
        this.line = line;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getLifemsx() {
        return lifemsx;
    }

    public void setLifemsx(int lifemsx) {
        this.lifemsx = lifemsx;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public AtomicInteger[] getCurrhps() {
        return currhps;
    }

    public void setCurrhps(AtomicInteger[] currhps) {
        this.currhps = currhps;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getNewnum() {
        return newnum;
    }

    public void setNewnum(int newnum) {
        this.newnum = newnum;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public int getNewrate() {
        return newrate;
    }

    public void setNewrate(int newrate) {
        this.newrate = newrate;
    }

    @Override
    public int compareTo(BattleEnemyRecord o) {
        return this.getKindid() - o.getKindid();
    }

}
