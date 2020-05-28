package com.cratos.game.flicker.bean;

import org.redkale.util.Comment;

@Comment("阵营光环百分比")
public class CampAuraPercent extends BaseBean {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8530758893161459041L;

	@Comment("1 苍翠之力;2 辉光之力; 3深蓝之力;4 永燃之力; 5 深谙之力; 6融合之力; 7 灵域之力 ;0 所有统计总和")
	private int type = 0;
	
	@Comment("血量")
	private int bloadPercent;

	@Comment("攻击")
	private int attackPercent;

	
	public void addBloadPercent(int bloadPercent) {
		this.bloadPercent += bloadPercent;
	}
	
	public void addAttackPercent(int attackPercent) {
		this.attackPercent += attackPercent;
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getBloadPercent() {
		return bloadPercent;
	}

	public void setBloadPercent(int bloadPercent) {
		this.bloadPercent = bloadPercent;
	}

	public int getAttackPercent() {
		return attackPercent;
	}

	public void setAttackPercent(int attackPercent) {
		this.attackPercent = attackPercent;
	}
	
	
	
	public CampAuraPercent(int type, int bloadPercent, int attackPercent) {
		super();
		this.type = type;
		this.bloadPercent = bloadPercent;
		this.attackPercent = attackPercent;
	}
	
	public CampAuraPercent() {
		// TODO Auto-generated constructor stub
	}

	public static CampAuraPercent from(int type, int bloadPercent, int attackPercent) {
		return  new CampAuraPercent(type, bloadPercent, attackPercent);
	}
	
}
