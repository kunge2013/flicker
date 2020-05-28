package com.cratos.game.flicker.bean;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import com.cratos.game.flicker.HeroDetailBean;

public class RoundRecord {
	
	private int roundid;
	
	LinkedBlockingQueue<HeroDetailBean> attackHeroQuene;

	
	public int getRoundid() {
		return roundid;
	}

	public void setRoundid(int roundid) {
		this.roundid = roundid;
	}

	public LinkedBlockingQueue<HeroDetailBean> getAttackHeroQuene() {
		return attackHeroQuene;
	}

	public void setAttackHeroQuene(List<HeroDetailBean> attackHeroQuene) {
		this.attackHeroQuene = new LinkedBlockingQueue<HeroDetailBean>(attackHeroQuene);
	}
	
	public static RoundRecord from(int roundid, List<HeroDetailBean> attackHeroQuene) {
		RoundRecord record = new RoundRecord();
		record.setRoundid(roundid);
		record.setAttackHeroQuene(attackHeroQuene);
		return record;
	}
	
}
