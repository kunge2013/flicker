package com.cratos.game.flicker;

import javax.persistence.*;

import org.redkale.util.Reproduce;

import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zc
 */
@Table(comment = "")
public class FlickerSkillCast extends BaseEntity {

    @Id
    @Column(comment = "技能释放效果id")
    private int castid;

    @Column(comment = "技能释放组id")
    private int castgroup;

    @Column(length = 255, comment = "释放效果名称")
    private String castname = "";

    @Column(length = 2048, comment = "技能释放描述")
    private String castdesc = "";

    @Column(comment = "技能释放最佳目标")
    private int castbesttarget;

    @Column(length = 2048, comment = "技能释放目标类型 类型数值为 none = 0, single = 1,//type2,//前方群体    type3,    type4,    type5,    type6}")
    private String casttarget = "";

    @Column(length = 2048, comment = "释放目标随机命中概率")
    private String casttargetrand = "";

    @Column(comment = "释放对象的目标数量")
    private int castcount;

    @Column(length = 2048, comment = "释放效果打出的buff 格式为：[[id, 概率],.......]")
    private String castbuffs = "";

    @Column(length = 2048, comment = "技能buff概率")
    private String castbuffrand = "";

    @Column(comment = "伤害系数")
    private int hurtratio;

    private static final Reproduce<FlickerSkillCast, FlickerSkillCast> copyer = Reproduce.create(FlickerSkillCast.class, FlickerSkillCast.class);
    
    public void setCastid(int castid) {
        this.castid = castid;
    }

    public int getCastid() {
        return this.castid;
    }

    public void setCastgroup(int castgroup) {
        this.castgroup = castgroup;
    }

    public int getCastgroup() {
        return this.castgroup;
    }

    public void setCastname(String castname) {
        this.castname = castname;
    }

    public String getCastname() {
        return this.castname;
    }

    public void setCastdesc(String castdesc) {
        this.castdesc = castdesc;
    }

    public String getCastdesc() {
        return this.castdesc;
    }

    public void setCastbesttarget(int castbesttarget) {
        this.castbesttarget = castbesttarget;
    }

    public int getCastbesttarget() {
        return this.castbesttarget;
    }

    public void setCasttarget(String casttarget) {
        this.casttarget = casttarget;
    }

    public String getCasttarget() {
        return this.casttarget;
    }

    public void setCasttargetrand(String casttargetrand) {
        this.casttargetrand = casttargetrand;
    }

    public String getCasttargetrand() {
        return this.casttargetrand;
    }

    public void setCastcount(int castcount) {
        this.castcount = castcount;
    }

    public int getCastcount() {
        return this.castcount;
    }

    public void setCastbuffs(String castbuffs) {
        this.castbuffs = castbuffs;
    }

    public String getCastbuffs() {
        return this.castbuffs;
    }

    public void setCastbuffrand(String castbuffrand) {
        this.castbuffrand = castbuffrand;
    }

    public String getCastbuffrand() {
        return this.castbuffrand;
    }

    public void setHurtratio(int hurtratio) {
        this.hurtratio = hurtratio;
    }

    public int getHurtratio() {
        return this.hurtratio;
    }
    
    
    public FlickerSkillCast copy() {
    	return copyer.apply(new FlickerSkillCast(), this);
    }
}
