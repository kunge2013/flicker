package com.cratos.game.flicker.bean;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.redkale.convert.json.JsonConvert;
import org.redkale.util.Comment;

import com.cratos.game.flicker.HeroDetailBean;

public class ConfrontationCamp extends BaseBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8634256627716217042L;

	@Comment("阵融信息1 ")
	private LineUpInfo lineUpInfoOne;

	
	@Comment("阵融信息2 ")
	private LineUpInfo lineUpInfoTwo;


	public AtomicInteger roundtime = new AtomicInteger(10);
	
	
	public AtomicInteger getRoundtime() {
		return roundtime;
	}


	public void setRoundtime(AtomicInteger roundtime) {
		this.roundtime = roundtime;
	}

	
	@Comment("获取自己的阵容")
	public LineUpInfo fetchOwnerLineUp(HeroDetailBean heroDetail) {
		if (lineUpInfoOne.isOwnerLineUp(heroDetail)) return lineUpInfoOne;
		if (lineUpInfoTwo.isOwnerLineUp(heroDetail)) return lineUpInfoTwo;
		return null;
	}
	
	/**
	 * 计算每个回合出手先后顺序
	 * 
	 * @param roundRecord
	 */
	public void calcAttackSeq(RoundRecord roundRecord) {
		// 1.排序出场顺序
		List<HeroDetailBean> oneHeroDetails = lineUpInfoOne.getHeroDetails();
		List<HeroDetailBean> twoHeroDetails = lineUpInfoTwo.getHeroDetails();

		List<HeroDetailBean> attackHeros = new LinkedList<HeroDetailBean>();
		attackHeros.addAll(oneHeroDetails);
		attackHeros.addAll(twoHeroDetails);
		// 按照大到小的顺序排列
		Collections.sort(attackHeros, (bean, other) -> {
			if (bean.getBasicspeed() > other.getBasicspeed()) {
				return -1;
			} else if (bean.getBasicspeed() < other.getBasicspeed()) {
				return 1;
			} else {
				return 0;
			}

		});
		roundRecord.setAttackHeroQuene(attackHeros);
	}

	
	public LineUpInfo getlineUpInfoOne() {
		return lineUpInfoOne;
	}
	
	public void setlineUpInfoOne(LineUpInfo lineUpInfoOne) {
		this.lineUpInfoOne = lineUpInfoOne;
	}

	public LineUpInfo getlineUpInfoTwo() {
		return lineUpInfoTwo;
	}

	public void setlineUpInfoTwo(LineUpInfo lineUpInfoTwo) {
		this.lineUpInfoTwo = lineUpInfoTwo;
	}

	public void initLineUpInfo(LineUpInfo lineUpInfoOne, LineUpInfo lineUpInfoTwo) {
		this.lineUpInfoOne = lineUpInfoOne;
		this.lineUpInfoTwo = lineUpInfoTwo;
		//双方阵容
		this.lineUpInfoOne.setEnemylineup(lineUpInfoTwo);
		this.lineUpInfoTwo.setEnemylineup(lineUpInfoOne);
		// 初始化英雄速度信息，血量，攻击，防御等属性
		this.lineUpInfoOne.flushHerosInfo();
		this.lineUpInfoTwo.flushHerosInfo();
	}

	public void flushHerosInfo() {
		// 初始化英雄速度信息，血量，攻击，防御等属性
		this.lineUpInfoOne.flushHerosInfo();
		this.lineUpInfoTwo.flushHerosInfo();
	}
	
	
	@Comment("检测当前是否已经结束了战斗")
	public boolean checkRoundOver() {
		// 回合结束
		if (roundtime.get() == 0) return true;
		if (!lineUpInfoTwo.checkHerosIsAlive()) return true;
		if (!lineUpInfoOne.checkHerosIsAlive()) return true;
		return false;
	}

	public static ConfrontationCamp build(LineUpInfo lineUpInfoOne, LineUpInfo lineUpInfoTwo, int roundtime) {
		ConfrontationCamp confrontationCamp = new ConfrontationCamp();
		confrontationCamp.setlineUpInfoOne(lineUpInfoOne);
		confrontationCamp.setlineUpInfoTwo(lineUpInfoTwo);
		confrontationCamp.setRoundtime(new AtomicInteger(roundtime));
		lineUpInfoOne.setEnemylineup(lineUpInfoTwo);
		lineUpInfoTwo.setEnemylineup(lineUpInfoOne);
		return confrontationCamp;
	}

	public static void main(String[] args) {

		// 初始化阵容 1
		LineUpInfo lineUpInfoOne = new LineUpInfo();
		{
			HeroDetailBean detail1 = new HeroDetailBean();
			detail1.setBasicspeed(1024);
			detail1.setPanelAttack(100);
			detail1.setPanelBload(100);
			detail1.setPanelDefense(2100);
			
			HeroDetailBean detail2 = new HeroDetailBean();
			detail2.setBasicspeed(3578);
			detail2.setPanelAttack(100);
			detail2.setPanelBload(100);
			detail2.setPanelDefense(2100);
			lineUpInfoOne.setHeroDetails(Arrays.asList(detail1, detail2));
			lineUpInfoOne.setCampAuraPercents(
					Arrays.asList(CampAuraPercent.from(1, 30, 10), CampAuraPercent.from(2, 50, 10)));
		}

		// 初始化阵容2
		LineUpInfo lineUpInfoTwo = new LineUpInfo();
		{
			HeroDetailBean detail1 = new HeroDetailBean();
			detail1.setBasicspeed(1055);
			detail1.setPanelAttack(100);
			detail1.setPanelBload(100);
			detail1.setPanelDefense(2100);
			HeroDetailBean detail2 = new HeroDetailBean();
			detail2.setBasicspeed(8952);
			detail2.setPanelAttack(100);
			detail2.setPanelBload(100);
			detail2.setPanelDefense(2100);
			lineUpInfoTwo.setHeroDetails(Arrays.asList(detail1, detail2));
			lineUpInfoTwo.setCampAuraPercents(
					Arrays.asList(CampAuraPercent.from(1, 15, 10), CampAuraPercent.from(2, 89, 10)));
		}

		ConfrontationCamp camp = build(lineUpInfoOne, lineUpInfoTwo, 10);
		camp.flushHerosInfo();
		RoundRecord roundRecord = new RoundRecord();
		camp.calcAttackSeq(roundRecord);//出手先后顺序
		// 计算出手技能
		roundRecord.getAttackHeroQuene();
		System.out.println(JsonConvert.root().convertTo(camp));
		System.out.println(JsonConvert.root().convertTo(roundRecord));
	}
	
	@Override
	public String toString() {
		return "ConfrontationCamp [lineUpInfoOne=" + lineUpInfoOne + ", lineUpInfoTwo=" + lineUpInfoTwo + ", roundtime="
				+ roundtime + "]";
	}

}
