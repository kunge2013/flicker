package com.cratos.game.flicker;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.redkale.util.Reproduce;

import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zc
 */
@Table(comment = "")
public class FlickerSkill extends BaseEntity {

    @Id
    @Column(comment = "技能id")
    private int skillid;

    @Column(length = 255, comment = "技能名称")
    private String skillname = "";

    @Column(length = 255, comment = "技能拥有者")
    private String skillowner = "";

    @Column(comment = "技能释放回合")
    private short skillfirecd;

    @Column(comment = "技能冷却回合")
    private short skillcd;

    @Column(comment = "技能类型 1为主动 2为被动")
    private short skilltype;

    @Column(length = 2048, comment = "技能描述")
    private String skilldesc = "";

//    @Column(length = 2048, comment = "技能释放效果ID 格式为:[[castid,概率],...]")
//    private String skillcastid = "";

    @Column(length = 2048, comment = "技能释放效果ID 格式为:[[castid,概率],...]")
    private LinkedHashMap<Integer, Integer> skillcastweight = new LinkedHashMap<Integer, Integer>();
    
    @Column(comment = "技能释放的生效类型 1.全部按概率触发2.随机抽取一个")
    private short skillcasttype;

    @Column(length = 255, comment = "技能等级id数组 格式为：[一级ID,二级ID,......]")
    private String skilllevelids = "";

    @Transient
//    @Column(comment = "英雄id")
    private int heroid;

    @Transient
    private List<FlickerSkillCast> flickerSkillCasts = new LinkedList<FlickerSkillCast>();
    
    @Transient
    private FlickerSkillCast releaseSkillCast;
    
    @Transient
    private HeroDetailBean targetHero;
    
    
    private static final Reproduce<FlickerSkill, FlickerSkill> copyer = Reproduce.create(FlickerSkill.class, FlickerSkill.class);
    
    public List<FlickerSkillCast> getFlickerSkillCasts() {
		return flickerSkillCasts;
	}

	public void setFlickerSkillCasts(List<FlickerSkillCast> flickerSkillCasts) {
		this.flickerSkillCasts = flickerSkillCasts;
	}

	
	public FlickerSkillCast getReleaseSkillCast() {
		return releaseSkillCast;
	}

	public void setReleaseSkillCast(FlickerSkillCast releaseSkillCast) {
		this.releaseSkillCast = releaseSkillCast;
	}

	public void setSkillid(int skillid) {
        this.skillid = skillid;
    }

    public int getSkillid() {
        return this.skillid;
    }

    public void setSkillname(String skillname) {
        this.skillname = skillname;
    }

    public String getSkillname() {
        return this.skillname;
    }

    public void setSkillowner(String skillowner) {
        this.skillowner = skillowner;
    }

    public String getSkillowner() {
        return this.skillowner;
    }

    public void setSkillfirecd(short skillfirecd) {
        this.skillfirecd = skillfirecd;
    }

    public short getSkillfirecd() {
        return this.skillfirecd;
    }

    public void setSkillcd(short skillcd) {
        this.skillcd = skillcd;
    }

    public short getSkillcd() {
        return this.skillcd;
    }

    public void setSkilltype(short skilltype) {
        this.skilltype = skilltype;
    }

    public short getSkilltype() {
        return this.skilltype;
    }

    public void setSkilldesc(String skilldesc) {
        this.skilldesc = skilldesc;
    }

    public String getSkilldesc() {
        return this.skilldesc;
    }

    
    
	public HeroDetailBean getTargetHero() {
		return targetHero;
	}

	public void setTargetHero(HeroDetailBean targetHero) {
		this.targetHero = targetHero;
	}

	public LinkedHashMap<Integer, Integer> getSkillcastweight() {
		return skillcastweight;
	}

	public void setSkillcastweight(LinkedHashMap<Integer, Integer> skillcastweight) {
		this.skillcastweight = skillcastweight;
	}

	public void setSkillcasttype(short skillcasttype) {
        this.skillcasttype = skillcasttype;
    }

    public short getSkillcasttype() {
        return this.skillcasttype;
    }

    public void setSkilllevelids(String skilllevelids) {
        this.skilllevelids = skilllevelids;
    }

    public String getSkilllevelids() {
        return this.skilllevelids;
    }

    public void setHeroid(int heroid) {
        this.heroid = heroid;
    }

    public int getHeroid() {
        return this.heroid;
    }

    public FlickerSkill copy() {
    	return copyer.apply(new FlickerSkill(), this);
    }
    
}
