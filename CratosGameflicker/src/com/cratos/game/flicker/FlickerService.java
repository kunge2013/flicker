package com.cratos.game.flicker;

import static org.redkale.util.Utility.ofMap;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.persistence.Transient;

import org.redkale.boot.Application;
import org.redkale.convert.json.JsonConvert;
import org.redkale.net.http.RestMapping;
import org.redkale.net.http.WebSocketNode;
import org.redkale.service.Local;
import org.redkale.service.RetResult;
import org.redkale.source.DataSource;
import org.redkale.util.AnyValue;
import org.redkale.util.Comment;

import com.cratos.game.flicker.bean.CampAuraPercent;
import com.cratos.game.flicker.bean.ConfrontationCamp;
import com.cratos.game.flicker.bean.LineUpInfo;
import com.cratos.game.flicker.bean.RoundRecord;
import com.cratos.game.flicker.bean.req.ReqBean;
import com.cratos.game.flicker.bean.req.ReqBean.ReqParams;
import com.cratos.platf.base.BaseService;
import com.cratos.platf.util.QueueTask;
import com.cratos.platf.util.ShuffleRandom;

@Comment("闪烁之光计算服务")
public class FlickerService extends BaseService {

	public ThreadPoolExecutor executor;

	@Resource(name = "wsgame")
	protected WebSocketNode webSocketNode;

	@Transient // WS消息缓存队列
	protected final QueueTask<Runnable> wsmessageQueue = new QueueTask<>(1);

	protected LinkedHashMap<Integer, FlickerSkill> skillsMap = new LinkedHashMap<>();

	protected LinkedHashMap<Integer, FlickerSkillCast> skillCastsMap = new LinkedHashMap<>(); 
	
	
	@Resource(name = "skywar")
	private DataSource source;

	protected static final SecureRandom random = ShuffleRandom.createRandom();

	@Comment("定时任务")
	protected ScheduledThreadPoolExecutor scheduler;

	@Override
	public void init(AnyValue config) {
		this.scheduler = new ScheduledThreadPoolExecutor(4 + 2, (Runnable r) -> {
			final Thread t = new Thread(r, "FlickerService" + "-task-Thread");
			t.setDaemon(true);
			return t;
		});
		// 技能初始化
		initFlickerSkill();
		// TODO Auto-generated method stub
		initBuffProperties(); // 初始化配置信息
		initExecutor(); // 初始化线程池
		super.init(config);
		wsmessageQueue.init(logger, (queue, task) -> task.run());
	}

	@Override
	public void destroy(AnyValue config) {
		// TODO Auto-generated method stub
		super.destroy(config);
		executor.shutdownNow();
		wsmessageQueue.destroy();
	}

	@Local
	public CompletableFuture<Integer> sendMap(final Stream<? extends Serializable> userids, Object... messages) {
		if (userids == null || userids.count() == 0)
			return CompletableFuture.completedFuture(0);
		CompletableFuture<Integer> result = new CompletableFuture<>();
		wsmessageQueue.add(() -> {
			if (webSocketNode == null) {
				logger.log(Level.FINEST, "local_SendMap: " + JsonConvert.root().convertTo(ofMap(messages)));
				result.complete(0);
				return;
			}
			webSocketNode.sendMessage(ofMap(messages), userids).whenComplete((r, e) -> {
				if (e != null) {
					result.completeExceptionally(e);
				} else {
					result.complete(r);
				}
			});
		});
		return result;
	}

	@Comment("初始化线程池")
	private void initExecutor() {
		executor = new ThreadPoolExecutor(4, 8, 0, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>(),
				new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						// TODO Auto-generated method stub
						Thread t = new Thread(r);
						t.setDaemon(true);
						t.setName("executor");
						return t;
					}

				}, new RejectedExecutionHandler() {

					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						if (!executor.isShutdown()) {
							// 移除队头元素
							executor.getQueue().poll();
							// 再尝试入队
							executor.execute(r);
						}
					}
				});
	}

	/**
	 * 初始化buff 配置信息
	 */
	private void initBuffProperties() {

	}

	@Comment("初始化技能缓存")
	private void initFlickerSkill() {
		List<FlickerSkill> skills = source.queryList(FlickerSkill.class);
		if (!skills.isEmpty()) {
			skillsMap = new LinkedHashMap<Integer, FlickerSkill>(
					skills.stream().collect(Collectors.toMap(FlickerSkill::getSkillid, value -> value, (k1, k2) -> k1)));
		}
		List<FlickerSkillCast> skillCasts = source.queryList(FlickerSkillCast.class);
		if (!skills.isEmpty()) {
			skillCastsMap = new LinkedHashMap<Integer, FlickerSkillCast>(
				skillCasts.stream().collect(Collectors.toMap(FlickerSkillCast::getCastid, value -> value, (k1, k2) -> k1)));
		}
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RestMapping(auth = true, comment = "回合计算")
	public RetResult roundCalculation(int userid, ReqBean<ReqParams> bean) {

		return RetResult.success();
	}

	public RetResult doRound(ConfrontationCamp confrontationCamp) {
		executor.execute(() -> {
			try {
				int roundid = 1;
				LinkedList<RoundRecord> roundRecords = new LinkedList<RoundRecord>();
				// 循环计算回合结束
				for (;;) {
					RoundRecord roundRecord = new RoundRecord();
					roundRecord.setRoundid(roundid ++);
					confrontationCamp.flushHerosInfo();// 初始化参数
					confrontationCamp.calcAttackSeq(roundRecord);// 出手先后顺序
					computeHarm(confrontationCamp, roundRecord);
					roundRecords.add(roundRecord);
					confrontationCamp.getRoundtime().decrementAndGet();//次数减1
					if (confrontationCamp.checkRoundOver()) break;
				}
				System.out.println(RetResult.success(roundRecords));
			} catch (BizException e) {
				e.printStackTrace();
				logger.finest(String.format("confrontationCamp exec error , confrontationCamp[%s] ,exception [code = %s, message = %s]" + confrontationCamp.toString() , e.getCode(), e.getMessage()));
			} catch (Exception e) {
				e.printStackTrace();
				logger.finest(String.format("confrontationCamp exec error , confrontationCamp[%s] ,ex [ message = %s]" + confrontationCamp.toString() ,  e.getMessage()));
			}
		
		});
		return RetResult.success();
	}

	/**
	 * 计算每个英雄将要出的技能
	 * 
	 * @param roundRecord 当前回合
	 * @throws Exception
	 */
	private void computeHarm(ConfrontationCamp confrontationCamp, RoundRecord roundRecord) {
		LinkedBlockingQueue<HeroDetailBean> attackHeroQuenes = roundRecord.getAttackHeroQuene();
		if (attackHeroQuenes == null || attackHeroQuenes.isEmpty()) throw new UnsupportedOperationException("calcHeroSkill error , heroDetailBeans is null");
		Iterator<HeroDetailBean> iterator = attackHeroQuenes.iterator();
		while (iterator.hasNext()) {
			HeroDetailBean heroDetailBean = iterator.next();
			List<Integer> skillids = heroDetailBean.fetchSkillIds();
			if (skillids.isEmpty()) throw new BizException(40000_1, String.format("hero %s skillids is empty", heroDetailBean.toString()));
			int skillid = skillids.get(random.nextInt(skillids.size()));
			FlickerSkill flickerSkill = skillsMap.get(skillid).copy();// 随机一个技能
			if (flickerSkill == null) throw new BizException(40000_2, String.format("%s hero flickerSkill not exits !", skillid)); 
			heroDetailBean.setReleaseSkill(flickerSkill);// 出哪一个技能
			// 技能效果根据权重计算
			LinkedHashMap<Integer, Integer> weightSkillCast = flickerSkill.getSkillcastweight();
			Collection<Integer> values  = weightSkillCast.values();
			AtomicInteger size = new AtomicInteger(0);
			values.forEach(data -> size.addAndGet(data));
			Integer[] weight = new Integer[size.get()];
			int cindex = 0;
			if (!weightSkillCast.isEmpty()) {
				for (Integer key : weightSkillCast.keySet()) {
					Integer value= weightSkillCast.get(key);
					int[] cweight = new int[value];
					for (int i = cindex; i < (cindex + cweight.length); i++) {
						weight[i] = key;
					}
				}
			}
			int skillCastId = weight[random.nextInt(weight.length)];
			//当前技能不伤害效果
			FlickerSkillCast releaseSkillCast = skillCastsMap.get(skillCastId).copy();
			flickerSkill.setReleaseSkillCast(releaseSkillCast);
			/*********************随机取出攻击的目标*******************/
			LineUpInfo heroLineUpInfo = confrontationCamp.fetchOwnerLineUp(heroDetailBean);
			if (heroLineUpInfo == null) throw new BizException(40000_3,  String.format("%s heroDetailBean LineUpInfo  not exits !", heroDetailBean.toString()));
			LineUpInfo enemylineup = heroLineUpInfo.getEnemylineup();//敌军阵容
			List<HeroDetailBean> enemyHeroDetails = enemylineup.getHeroDetails();//敌军英雄
			int enemyHeroIndex = random.nextInt(enemyHeroDetails.size());
			HeroDetailBean targetHero = enemyHeroDetails.get(enemyHeroIndex);
			/*********************随机取出攻击的目标*******************/
			int effectiveAttack = heroDetailBean.getEffectiveAttack();
			int skillBuffAtkPct = releaseSkillCast.getHurtratio();
			targetHero.combatEffectiveAttack(targetHero.getPanelAttack(), targetHero.getMellenCampPct(), targetHero.getArtfixedAttack(), skillBuffAtkPct);
			System.out.println("before ==== plusBload =====" + targetHero +", targetHero id ===" + targetHero.getUuid());
			targetHero.plusBload(-effectiveAttack);
			flickerSkill.setTargetHero(targetHero.copyEffectiveValue());//被攻击的英雄
			
			if (!targetHero.isAlive()) {
				attackHeroQuenes.remove(targetHero);//如果英雄死了直接移除队列
			}
			System.out.println("after ==== plusBload =====" + targetHero +", targetHero id ===" + targetHero.getUuid());
		}
		
		
	}

	public static void main(String[] args) throws Exception {
		FlickerService service = Application.singleton(FlickerService.class);
//		FlickerSkill skill = new FlickerSkill();
//		Map<Integer, Integer> data = new HashMap<Integer, Integer>();
//		data.put(1, 2);
//		data.put(2, 25);
//		
//		skill.setSkillcastid(data);
//		service.source.insert(skill);
		testLogic(service);
	}
	
	public static void testLogic(FlickerService service) {
		ConfrontationCamp confrontationCamp = null;
		int roundtime = 2;
		{
			LineUpInfo lineUpInfoOne = new LineUpInfo();
			{
				HeroDetailBean detail1 = new HeroDetailBean();
				detail1.setBasicspeed(1024);
				detail1.setPanelAttack(100);
				detail1.setPanelBload(100);
				detail1.setPanelDefense(2100);
				detail1.setHeroskill1(10001);
				detail1.setHeroskill2(10002);
				detail1.setHeroskill3(10003);
				detail1.setHeroskill4(90010);
				
				HeroDetailBean detail2 = new HeroDetailBean();
				detail2.setBasicspeed(3578);
				detail2.setPanelAttack(100);
				detail2.setPanelBload(100);
				detail2.setPanelDefense(2100);
				
				detail2.setHeroskill1(10001);
				detail2.setHeroskill2(10002);
				detail2.setHeroskill3(10003);
				detail2.setHeroskill4(90010);
				
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
				detail1.setHeroskill1(10001);
				detail1.setHeroskill2(10002);
				detail1.setHeroskill3(10003);
				detail1.setHeroskill4(90010);
				HeroDetailBean detail2 = new HeroDetailBean();
				detail2.setBasicspeed(8952);
				detail2.setPanelAttack(100);
				detail2.setPanelBload(100);
				detail2.setPanelDefense(2100);
				detail2.setHeroskill1(10001);
				detail2.setHeroskill2(10002);
				detail2.setHeroskill3(10003);
				detail2.setHeroskill4(90010);
				lineUpInfoTwo.setHeroDetails(Arrays.asList(detail1, detail2));
				lineUpInfoTwo.setCampAuraPercents(
						Arrays.asList(CampAuraPercent.from(1, 15, 10), CampAuraPercent.from(2, 89, 10)));
			}
			confrontationCamp = ConfrontationCamp.build(lineUpInfoOne, lineUpInfoTwo, roundtime);
		}
		Object obj = service.doRound(confrontationCamp);
		System.out.println(obj);
		try {
			Thread.sleep(1000 * 20);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
