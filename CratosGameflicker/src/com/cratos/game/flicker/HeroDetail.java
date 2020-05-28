package com.cratos.game.flicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.redkale.util.Comment;
import org.redkale.util.Utility;

import com.cratos.game.flicker.bean.HeroFormula;
import com.cratos.platf.base.BaseEntity;

/**
 *
 * @author zc
 */
@Table(comment = "")
public class HeroDetail extends BaseEntity implements HeroFormula {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1965913128411213799L;

	@Id
    @Column(comment = "主键id 英雄id")
    private int heroid;

    @Column(comment = "基础血量（没有装备buff）")
    private int basicbload;

    @Column(comment = "基础防御（没有装备buff）")
    private int basicdefence;

    @Column(comment = "基础攻击（没有装备buff）")
    private int basicattack;

    @Column(comment = "基础速度（没有装备buff）")
    private int basicspeed;

    @Column(comment = "英雄类型 1 水 2 活 3 风 4 日 5 月")
    private short herotype = 1;

    @Column(comment = "英雄的第一个技能id")
    private int heroskill1;

    @Column(comment = "英雄的第二个技能id")
    private int heroskill2;

    @Column(comment = "英雄的第三个技能id")
    private int heroskill3;

    @Column(comment = "英雄的第四个技能id")
    private int heroskill4;

    @Comment("有效血量")
    @Transient
    private int effectivebload;
    
    @Comment("有效防御")
    @Transient
    private int effectivefence;
    
    @Comment("有效攻击")
    @Transient
    private int effectiveAttack;
    
    @Comment("面板攻击")
    @Transient
    private int panelAttack = 0; // 面板攻击
    
    @Comment("面板血量")
    @Transient
    private int panelBload = 0;  // 面板血量
    
    @Comment("面板防御")
    @Transient
    private int panelDefense = 0; //  面板防御
	
    @Comment("阵营光环攻击百分比")
    @Transient
    private int mellenCampPct;
    
    @Comment("神器固定攻击")
    @Transient
    private int artfixedAttack;
    
    @Comment("技能buff攻击百分比")
    @Transient
    private int skillBuffAtkPct;
    
    
    public int getPanelBload() {
		return panelBload;
	}

	public void setPanelBload(int panelBload) {
		this.panelBload = panelBload;
	}

	public int getPanelDefense() {
		return panelDefense;
	}

	public void setPanelDefense(int panelDefense) {
		this.panelDefense = panelDefense;
	}


	@Comment("添加血量")
	public int plusBload(int bloadVal) {
		effectivebload += bloadVal;
		if (effectivebload < 0) effectivebload = 0;
		return effectivebload;
	}
	
	public boolean checkAlive(int bloadVal) {
		return bloadVal + effectivebload >= 0;
	}
	public int getEffectivebload() {
		return effectivebload;
	}

	public void setEffectivebload(int effectivebload) {
		this.effectivebload = effectivebload;
	}

	public int getEffectivefence() {
		return effectivefence;
	}

	public void setEffectivefence(int effectivefence) {
		this.effectivefence = effectivefence;
	}

	public int getEffectiveAttack() {
		return effectiveAttack;
	}

	public void setEffectiveAttack(int effectiveAttack) {
		this.effectiveAttack = effectiveAttack;
	}

	public int getPanelAttack() {
		return panelAttack;
	}

	public void setPanelAttack(int panelAttack) {
		this.panelAttack = panelAttack;
	}

	public int getMellenCampPct() {
		return mellenCampPct;
	}

	public void setMellenCampPct(int mellenCampPct) {
		this.mellenCampPct = mellenCampPct;
	}

	public int getArtfixedAttack() {
		return artfixedAttack;
	}

	public void setArtfixedAttack(int artfixedAttack) {
		this.artfixedAttack = artfixedAttack;
	}

	public int getSkillBuffAtkPct() {
		return skillBuffAtkPct;
	}

	public void setSkillBuffAtkPct(int skillBuffAtkPct) {
		this.skillBuffAtkPct = skillBuffAtkPct;
	}

	public void setHeroid(int heroid) {
        this.heroid = heroid;
    }

    public int getHeroid() {
        return this.heroid;
    }

    public void setBasicbload(int basicbload) {
        this.basicbload = basicbload;
    }

    public int getBasicbload() {
        return this.basicbload;
    }

    public void setBasicdefence(int basicdefence) {
        this.basicdefence = basicdefence;
    }

    public int getBasicdefence() {
        return this.basicdefence;
    }

    public void setBasicattack(int basicattack) {
        this.basicattack = basicattack;
    }

    public int getBasicattack() {
        return this.basicattack;
    }

    public void setBasicspeed(int basicspeed) {
        this.basicspeed = basicspeed;
    }

    public int getBasicspeed() {
        return this.basicspeed;
    }

    public void setHerotype(short herotype) {
        this.herotype = herotype;
    }

    public short getHerotype() {
        return this.herotype;
    }

    public void setHeroskill1(int heroskill1) {
        this.heroskill1 = heroskill1;
    }

    public int getHeroskill1() {
        return this.heroskill1;
    }

    public void setHeroskill2(int heroskill2) {
        this.heroskill2 = heroskill2;
    }

    public int getHeroskill2() {
        return this.heroskill2;
    }

    public void setHeroskill3(int heroskill3) {
        this.heroskill3 = heroskill3;
    }

    public int getHeroskill3() {
        return this.heroskill3;
    }

    public void setHeroskill4(int heroskill4) {
        this.heroskill4 = heroskill4;
    }

    public int getHeroskill4() {
        return this.heroskill4;
    }
    


	public List<Integer> fetchSkillIds() {
    	List<Integer> skills = new ArrayList<Integer>();
    	if (heroskill1 > 0) skills.add(heroskill1);
    	if (heroskill2 > 0) skills.add(heroskill2);
    	if (heroskill3 > 0) skills.add(heroskill3);
    	if (heroskill4 > 0) skills.add(heroskill4);
//    	if (skills.isEmpty()) throw new
    	return skills;
    }

    
	
	@Override
	public int hashCode() {
		return Objects.hash(heroid, basicattack,
								basicbload, basicdefence,
									basicspeed, heroskill1,
										heroskill2, heroskill3,
											heroskill4, panelBload, herotype);
	}
	
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other instanceof HeroDetail) {
			HeroDetail o = (HeroDetail) other;
			if(this.heroid == o.heroid
						&& this.basicbload == o.heroid 
							&& this.basicdefence == o.basicdefence
								&& this.mellenCampPct == o.mellenCampPct) {
				return true;
			}
		}
		return false;
	}
}
