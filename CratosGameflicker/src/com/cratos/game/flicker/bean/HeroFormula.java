package com.cratos.game.flicker.bean;

import org.redkale.util.Comment;
/**
 * 基础计算公式
 * @author zc
 *
 */
public interface HeroFormula {
	
	/**
	 * 战斗有效攻击（血量）={ [面板攻击*（1+阵营光环攻击百分比）]+神器固定攻击｝*(1+技能buff攻击百分比) 备注1:
	 * 技能buff攻击百分比，辅助的重复加攻不能叠加，包括潘、艾雷、娜迦、酒神等等，辅助的加攻和战士本身的加攻可以叠加，比如波塞东14技能加攻，狼4技能加攻等等。
	 * 备注2：技能buff攻击百分比需要扣除赫拉的1技能降攻、潘多拉的降攻之类。
	 * 备注:3：血量，防制基本也是同样计算，不过阵营光环没有防御，技能buff没有血量。
	 * 
	 * @param panelAttack     面板攻击
	 * @param mellenCampPct   阵营光环攻击百分比
	 * @param artfixedAttack  神器固定攻击
	 * @param skillBuffAtkPct 技能buff攻击百分比
	 */
	@Comment("战斗有效攻击")
	default int combatEffectiveAttack(int panelAttack, int mellenCampPct, int artfixedAttack, int skillBuffAtkPct) {
		// TODO 精度丢失需要控制吗
		int effectiveAttack = (panelAttack * (1 + mellenCampPct) + artfixedAttack ) * (1 + skillBuffAtkPct);
		return effectiveAttack;
	}

	
	/**
	 * 战斗有效血量={ [面板血量*（1+阵营光环攻击百分比）]+神器固定攻击｝*(1+技能buff攻击百分比) 备注1:
	 * 技能buff攻击百分比，辅助的重复加攻不能叠加，包括潘、艾雷、娜迦、酒神等等，辅助的加攻和战士本身的加攻可以叠加，比如波塞东14技能加攻，狼4技能加攻等等。
	 * 备注2：技能buff攻击百分比需要扣除赫拉的1技能降攻、潘多拉的降攻之类。
	 * 备注:3：血量，防制基本也是同样计算，不过阵营光环没有防御，技能buff没有血量。
	 * 
	 * @param panelAttack     面板血量
	 * @param mellenCampPct   阵营光环血量百分比
	 * @param artfixedAttack  神器固定血量
	 * @param skillBuffAtkPct 技能buff血量百分比
	 */
	@Comment("战斗有效血量")
	default int initCombatEffectiveBload(int panelBload, int mellenCampBldPct, int artfixedBload, int skillBuffBldPct) {
		// TODO 精度丢失需要控制吗
		int bloodvolume = (panelBload * (1 + mellenCampBldPct) + artfixedBload ) * (1 + skillBuffBldPct);
		return bloodvolume;
	}

	/**
	 * 战斗有效血量={ [面板血量*（1+阵营光环攻击百分比）]+神器固定攻击｝*(1+技能buff攻击百分比) 备注1:
	 * 技能buff攻击百分比，辅助的重复加攻不能叠加，包括潘、艾雷、娜迦、酒神等等，辅助的加攻和战士本身的加攻可以叠加，比如波塞东14技能加攻，狼4技能加攻等等。
	 * 备注2：技能buff攻击百分比需要扣除赫拉的1技能降攻、潘多拉的降攻之类。
	 * 备注:3：血量，防制基本也是同样计算，不过阵营光环没有防御，技能buff没有血量。
	 * 
	 * @param panelAttack     面板血量
	 * @param mellenCampPct   阵营光环血量百分比
	 * @param artfixedAttack  神器固定血量
	 * @param skillBuffAtkPct 技能buff血量百分比
	 */
	@Comment("战斗有效血量")
	default int combatEffectiveBload(int currentBload, int skillBuffBldPct) {
		// TODO 精度丢失需要控制吗
		int bloodvolume = currentBload * (1 + skillBuffBldPct);
		return bloodvolume;
	}
	
	/**
	 * 战斗有效防御=面板防御（1+技能buff防御百分比-破甲）*（1-穿透-技能无视防御）
	 * 备注1：破甲指的艾雷，魅魔，熊猫，新暗等破甲，加成主要是小鹿，穿透指符文天赋的穿透技能，技能无视防御指波塞东的1技能30%，雷神1技能40%。
	 * 
	 * @param panelDefense        面板防御
	 * @param skillBuffDefencePct 技能buff防御百分比
	 * @param sunderArmor         破甲
	 * @param pierceThrough       穿透
	 * @param skillIgnoreDefense  技能无视防御
	 */
	@Comment("战斗有效防御")
	default int combatEffectiveDefense(int panelDefense, int skillBuffDefencePct, int sunderArmor, int pierceThrough,
			int skillIgnoreDefense) {
		// TODO 精度丢失需要控制吗
		int defensevolume = panelDefense * (100 + skillBuffDefencePct - sunderArmor) * (100 - pierceThrough - skillIgnoreDefense); 
		return defensevolume;
	}
	
	/**
	 * 战斗伤害=战斗有效攻击*｛1-[战斗有效防御/（战斗有效防御+1000）]｝*（1+加伤-减伤）*技能攻击系数*暴伤加成
	 *	备注1：加伤包括魔力，神力，神装物伤法伤，神战，镇压，驱灵，灭魂，精准，火牧1技能，海拉的降物免，维纳斯降法免，潘3层标记，公会技能加伤，符文基础属性加伤等等，包括种族克制伤害加成。
	 *	备注2：免伤指神体，铁甲，大祭祀1技能减伤，公会技能减伤，坦克技能减伤，符文基础属性减伤，神装物免法免等等。
	 *	备注3：比较特殊的有几个，比如瓦妹的3技能，暴击时，增加20%的伤害，并不是20%的加伤，而是战斗伤害*1.2。维纳斯1技能减少30%伤害，反弹伤害，它最终伤害是战斗伤害*0.7，反弹伤害是战斗伤害*反弹系数，并不是30的减伤直接算在战斗伤害中的，而是先算出战斗伤害，然后根据战斗伤害来计算最终伤害和反弹伤害。
	 *
	 * @param effdefensevolume 战斗有效防御
	 * @param additionalInjury 加伤
	 * @param injuryReduction 减伤
	 * @param skillAttackCoeff 技能攻击系数
	 * @param criticalDamageBonus 暴伤加成
	 * @return
	 */
	@Comment("战斗伤害")
	default int battleDamage(int effdefensevolume, int additionalInjury, int injuryReduction,  int skillAttackCoeff, int criticalDamageBonus) {
		// TODO 战斗有效攻击*｛1-[战斗有效防御/（战斗有效防御+1000）]｝*（1+加伤-减伤）*技能攻击系数*暴伤加成
		int battleDamage = effdefensevolume * (100- (additionalInjury/(additionalInjury + 1000)))
					* (1 + additionalInjury - injuryReduction) * skillAttackCoeff * criticalDamageBonus;
		return battleDamage;
	}

	
	
}
