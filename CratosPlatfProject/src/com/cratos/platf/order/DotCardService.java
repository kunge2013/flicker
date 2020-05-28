/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.order;

import com.cratos.platf.base.*;
import com.cratos.platf.info.DictService;
import com.cratos.platf.notice.RandomCode;
import static com.cratos.platf.order.DotCard.CARD_TYPE_YAOQING;
import com.cratos.platf.user.*;
import java.util.*;
import javax.annotation.Resource;
import org.redkale.boot.Application;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("点卡服务")
public class DotCardService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    private DictService dictService;

    @Resource
    protected UserService userService;

    @Override
    public void init(AnyValue config) {
        //生成测试数据
        int count = source.getNumberResult(DotCardGroup.class, FilterFunc.COUNT, 0, null, FilterNode.create("remains", FilterExpress.GREATERTHAN, 1)).intValue();
        if (count < 10) {
            DotCardGroup group = new DotCardGroup();
            group.setCardtype(DotCard.CARD_TYPE_DIANKA);
            group.setAmount(100);
            group.setCoins(10000);
            group.setReason("测试数据"); 
            createGoodsCardGroup(group);
        }
    }

    @Comment("新增点卡批次")
    public RetResult<Integer> createGoodsCardGroup(@Comment("点卡批次对象") DotCardGroup group) {
        if (group.getReason().isEmpty()) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        if (group.getAmount() < 1 || group.getAmount() > 10000) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        Number maxid = source.getNumberResult(DotCardGroup.class, FilterFunc.MAX, 0, "cardgroupid", (FilterNode) null);
        if (maxid.longValue() < 10_0000) maxid = 10_0000;
        group.setCardgroupid(maxid.intValue() + 1);
        group.setCreatetime(System.currentTimeMillis());
        group.setRemains(group.getAmount());
        if (group.getCardtype() == 0) group.setCardtype(CARD_TYPE_YAOQING);
        DotCard[] cards = new DotCard[group.getAmount()];
        source.insert(group);
        super.runAsync(() -> {
            List<DotCard> list = new ArrayList<>();
            for (int i = 0; i < cards.length; i++) {
                DotCard card = new DotCard();
                cards[i] = card;
                card.setCardid(RandomCode.random16Code());
                card.setCardgroupid(group.getCardgroupid());
                card.setCardstatus(DotCard.CARD_STATUS_UNUSE);
                card.setCardtype(group.getCardtype());
                card.setCoins(group.getCoins());
                card.setCreatetime(group.getCreatetime());
                list.add(card);
                if (list.size() % 500 == 0) {
                    source.insert(list.toArray(new DotCard[list.size()]));
                    list.clear();
                }
            }
            if (!list.isEmpty()) {
                source.insert(list.toArray(new DotCard[list.size()]));
                list.clear();
            }
        });
        return new RetResult().result(group.getCardgroupid());
    }

    @Comment("充值点卡")
    public synchronized RetResult useDotCard(final int userid, final String cardid, int agencyid) {
        if (userid < 1 || cardid == null) return RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
        DotCard card = source.find(DotCard.class, cardid);
        if (card == null) return RetCodes.retResult(RetCodes.RET_ORDER_GOODSCARD_ILLEGAL);
        UserInfo user = userService.findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        if (agencyid != 0) {
            UserInfo agency = userService.findUserInfo(agencyid);
            if (agency == null) return RetCodes.retResult(RetCodes.RET_USER_NOTEXISTS);
        }
        if (card.getCardtype() == DotCard.CARD_TYPE_YAOQING && source.exists(DotCardHis.class, FilterNode.create("userid", userid))) {
            return RetCodes.retResult(RetCodes.RET_USER_GOODS_USELIMIT);
        }
        long now = System.currentTimeMillis();
        RetResult rr = userService.updatePlatfUserCoinDiamondCoupons(userid, card.getCoins(), card.getDiamonds(), card.getCoupons(), now, "dotcard", "点卡充值");
        if (!rr.isSuccess()) return rr;
        source.insert(card.createGoodsCardHis(userid, DotCard.CARD_STATUS_USED, agencyid, now));
        source.updateColumn(DotCardGroup.class, card.getCardgroupid(), ColumnValue.inc("remains", -1));
        source.delete(DotCard.class, cardid);
        if (agencyid != 0) userService.updateAgencyid(userid, agencyid, 0);
        return new RetResult(Utility.ofMap("goodsitems", card.getGoodsItems()));
    }

    public static void main(String[] args) throws Throwable {
        DotCardService service = Application.singleton(DotCardService.class);
        DotCardGroup group = new DotCardGroup();
        group.setAmount(800);
        group.setCoins(10_0000);
        group.setReason("过节活动");
        long s = System.currentTimeMillis();
        service.createGoodsCardGroup(group);
        long e = System.currentTimeMillis() - s;
        System.out.println(e);
        Thread.sleep(3000);
    }
}
