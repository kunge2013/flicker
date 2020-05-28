package com.cratos.game.flicker.bean;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Transient;

import org.redkale.util.Comment;

import com.cratos.game.flicker.HeroDetailBean;
@Comment("阵融信息")
public class LineUpInfo extends BaseBean  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6293048131548876121L;

	/**
	 * 阵营光环百分比汇总
	 */
	private CampAuraPercent totalAuraPercent;
	
	/**
	 * 阵营光环百分比详情
	 */
	private List<CampAuraPercent> campAuraPercents;
	
	private List<HeroDetailBean> heroDetails = new LinkedList<HeroDetailBean>();
	
	private boolean isinit = true;
	
	@Comment("对方阵容")
	@Transient
	private LineUpInfo enemylineup;
	
	public CampAuraPercent getTotalAuraPercent() {
		return totalAuraPercent;
	}

	public void setTotalAuraPercent(CampAuraPercent totalAuraPercent) {
		this.totalAuraPercent = totalAuraPercent;
	}

	public List<CampAuraPercent> getCampAuraPercents() {
		return campAuraPercents;
	}

	// 汇总统计
	public void setCampAuraPercents(List<CampAuraPercent> campAuraPercents) {
		totalAuraPercent = new CampAuraPercent();
		if (campAuraPercents != null) {
			campAuraPercents.forEach(obj -> {
				totalAuraPercent.addAttackPercent(obj.getAttackPercent());
				totalAuraPercent.addBloadPercent(obj.getBloadPercent());
			});
		}
		this.campAuraPercents = campAuraPercents;
	}

	public List<HeroDetailBean> getHeroDetails() {
		return heroDetails;
	}

	public void setHeroDetails(List<HeroDetailBean> heroDetails) {
		this.heroDetails = new LinkedList<HeroDetailBean>(heroDetails);
		sortHero();
	}
	
	
	private void sortHero() {
		Collections.sort(heroDetails, (bean, other) -> {
			if (bean.getBasicspeed() > other.getBasicspeed()) {
				return -1;
			} else if (bean.getBasicspeed() < other.getBasicspeed()) {
				return 1;
			} else {
				return 0;
			}

		});
	}
	
	public void flushHerosInfo() {
		check();
		List<HeroDetailBean> heros = this.getHeroDetails();
		if (!heros.isEmpty()) {
			heros.forEach(hero -> {
				int panelAttack = hero.getPanelAttack(); // 面板攻击
				int panelBload = hero.getPanelBload();  // 面板血量
				int panelDefense = hero.getPanelDefense(); //  面板防御
				int mellenAttackCampPct = totalAuraPercent.getAttackPercent(); 
				int mellenCampBldPct = totalAuraPercent.getBloadPercent(); 
				
				int artfixedAttack = 0; // 神器固定攻击
				int artfixedBload = 0; // 神器固定血量
				int skillBuffAtkPct = 0; // 技能buff攻击百分比
				int skillBuffBldPct = 0; // 技能buff血量百分比
				int skillBuffDefencePct = 0; // 技能buff防御百分比
				int sunderArmor = 0;// 破甲
				int pierceThrough = 0;//穿透
				int skillIgnoreDefense = 0; //技能无视防御
				hero.setEffectiveAttack(hero.combatEffectiveAttack(panelAttack, mellenAttackCampPct, artfixedAttack, skillBuffAtkPct));
				// 初始化通过面板血量计算
				hero.setEffectivebload(isinit ? hero.initCombatEffectiveBload(panelBload, mellenCampBldPct, artfixedBload, skillBuffBldPct) : hero.combatEffectiveBload(hero.getEffectivebload(), skillBuffBldPct));
				hero.setEffectivefence(hero.combatEffectiveDefense(panelDefense, skillBuffDefencePct, sunderArmor, pierceThrough, skillIgnoreDefense));
			});
			isinit = false;
		}
		sortHero();
	}
	
	/**
	 * 检查当前阵容是否有英雄还活着
	 * @return 
	 */
	public boolean checkHerosIsAlive() {
		List<HeroDetailBean> heroDetailBeans = this.getHeroDetails();
		for (HeroDetailBean heroDetailBean : heroDetailBeans) {
			if (heroDetailBean.isAlive()) return true;
		}
		return false;
	}
	
	/**
	 * 校验数据是否存在问题
	 */
	private void check() {
		if (this.heroDetails == null || this.heroDetails.isEmpty()) throw new UnsupportedOperationException("heroDetails is null ");
		if (this.campAuraPercents == null || this.campAuraPercents.isEmpty()) throw new UnsupportedOperationException("campAuraPercents is null ");
		if (this.totalAuraPercent == null) throw new UnsupportedOperationException("totalAuraPercent is null ");
	}
	/**
	 * 一个回合后重新计算相关属性
	 */
	public void flushHerosInfo(List<HeroDetailBean> heroDetails) {
		this.heroDetails = heroDetails;
		flushHerosInfo();
	}
	
	
	public static LineUpInfo from(List<CampAuraPercent> campAuraPercents, List<HeroDetailBean> heroDetails) {
		LineUpInfo auraInfo = new LineUpInfo();
		auraInfo.setHeroDetails(heroDetails);
		auraInfo.setCampAuraPercents(campAuraPercents);
		auraInfo.flushHerosInfo();//刷新排序顺序
		return auraInfo;
	}

	@Override
	public String toString() {
		return "LineUpInfo [totalAuraPercent=" + totalAuraPercent + ", campAuraPercents=" + campAuraPercents
				+ ", heroDetails=" + heroDetails + "]";
	}

	public LineUpInfo getEnemylineup() {
		return enemylineup;
	}

	public void setEnemylineup(LineUpInfo enemylineup) {
		this.enemylineup = enemylineup;
	}
	
	@Comment("判断当前阵容是否属于自己")
	public boolean isOwnerLineUp(HeroDetailBean heroDetailBean) {
		return heroDetails.contains(heroDetailBean);
	}
	
	
}
