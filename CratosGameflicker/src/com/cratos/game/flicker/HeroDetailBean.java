package com.cratos.game.flicker;

import java.util.List;

import org.redkale.util.Comment;
import org.redkale.util.Reproduce;
import org.redkale.util.Utility;

import com.cratos.platf.base.UserInfo;

public class HeroDetailBean extends HeroDetail {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5947170048378118802L;
	@Comment("出手技能")
	private FlickerSkill releaseSkill;

	public List<HeroDetailBean> targetHeros;
	
	private static final Reproduce<HeroDetailBean, HeroDetailBean> copyer = Reproduce.create(HeroDetailBean.class, HeroDetailBean.class);
	 

    
    public String uuid = Utility.uuid();
    
	public FlickerSkill getReleaseSkill() {
		return releaseSkill;
	}

	public void setReleaseSkill(FlickerSkill releaseSkill) {
		this.releaseSkill = releaseSkill;
	}

	public List<HeroDetailBean> getTargetHeros() {
		return targetHeros;
	}

	public void setTargetHeros(List<HeroDetailBean> targetHeros) {
		this.targetHeros = targetHeros;
	}
	
	public boolean isAlive() {
		return this.getEffectivebload() > 0;
	}
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		// TODO Auto-generated method stub
		if (other == null) return false;
		if (other instanceof HeroDetail) {
			HeroDetailBean o = (HeroDetailBean) other;
			if (this.uuid.equalsIgnoreCase(o.uuid) && super.equals(other)) {
				return true;
			}
		}
		return false;
	}

	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public HeroDetailBean copyEffectiveValue() {
		HeroDetailBean hero = new HeroDetailBean();
		hero.setEffectivebload(getEffectivebload());
		hero.setBasicbload(getBasicbload());
		hero.setArtfixedAttack(getArtfixedAttack());
		hero.setBasicspeed(getBasicspeed());
		hero.setHeroid(getHeroid());
		return hero;
	}
}
