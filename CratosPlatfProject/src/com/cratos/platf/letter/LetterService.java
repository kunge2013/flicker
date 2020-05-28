/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.letter;

import com.cratos.platf.base.*;
import com.cratos.platf.notice.RandomCode;
import com.cratos.platf.order.*;
import com.cratos.platf.user.UserService;
import java.text.MessageFormat;
import java.util.*;
import javax.annotation.Resource;
import org.redkale.net.http.WebSocketNode;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Comment("邮件服务")
public class LetterService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    @Resource
    protected UserService userService;

    @Resource
    protected GoodsService goodsService;

    @Resource(name = "wsgame")
    protected WebSocketNode webSocketNode;

    protected ResourceBundle bundle;

    @Override
    public void init(AnyValue conf) {
        this.initBundle();
    }

    private void initBundle() {
        this.bundle = ResourceBundle.getBundle(this.getClass().getPackage().getName() + ".letterbundle", Locale.forLanguageTag("zh"));
    }

    public String bundleResourceValue(String name, Object... params) {
        if (bundle == null) initBundle();
        String rs = bundle.getString(name);
        if (params == null || params.length == 0) return rs;
        return MessageFormat.format(rs, params);
    }

    @Comment("创建邮件")
    public RetResult<LetterRecord> createLetterRecord(LetterRecord record) {
        record.setCreatetime(System.currentTimeMillis());
        record.setStatus(LetterRecord.LETTER_STATUS_UNREAD);
        record.setLetterid(record.getLettertype() + "-" + record.getUser36id() + "-" + record.getFromuser36id() + "-" + RandomCode.random5Code() + "-" + Utility.format36time(record.getCreatetime()));
        source.insert(record);
        if (webSocketNode != null && !UserInfo.isRobot(record.getUserid())) {
            webSocketNode.sendMessage(Utility.ofMap("onUserNoticeMessage", Utility.ofMap("letterCount", 1)), record.getUserid());
        }
        return new RetResult<>(record);
    }

    @Comment("批量创建邮件")
    public RetResult<Integer> createLetterRecordBatch(long time, List<LetterRecord> recordList) {
        recordList.forEach((LetterRecord record) -> {
            record.setCreatetime(time);
            record.setStatus(LetterRecord.LETTER_STATUS_UNREAD);
            record.setLetterid(record.getLettertype() + "-" + record.getUser36id() + "-" + record.getFromuser36id() + "-" + RandomCode.random5Code() + "-" + Utility.format36time(record.getCreatetime()));
        });
        source.insert(recordList.toArray());
        return RetResult.success();
    }

    @Comment("读取邮件")
    public RetResult readLetterRecord(LetterBean bean) {
        List<LetterRecord> list;
        if (bean.getLetterid() != null && !bean.getLetterid().isEmpty()) {
            list = new ArrayList<>();
            LetterRecord one = source.find(LetterRecord.class, bean.getLetterid());
            if (one != null && one.getStatus() != LetterRecord.LETTER_STATUS_UNREAD) {
                return RetCodes.retResult(RetCodes.RET_LETTER_STATUS_ILLEGAL);
            }
            if (one != null && (bean.getUserid() < 1 || one.getUserid() == bean.getUserid())) {
                list.add(one);
            }
        } else {
            list = source.queryList(LetterRecord.class, bean);
        }
        long now = System.currentTimeMillis();
        boolean ok = false;
        for (LetterRecord record : list) {
            if (record.getStatus() != LetterRecord.LETTER_STATUS_UNREAD) continue;
            String module = record.getModule();
            if (module == null || module.isEmpty()) module = "letter";
            String remark = record.getRemark();
            if (remark == null || remark.isEmpty()) remark = "邮件领取; letterid=" + record.getLetterid();
            if (record.getCoins() > 0 || record.getDiamonds() > 0 || record.getCoupons() > 0) {
                userService.updatePlatfUserCoinDiamondCoupons(record.getUserid(), record.getCoins(), record.getDiamonds(), record.getCoupons(), now, module, remark);
            }
            if (record.getGoodsitems() != null) {
                RetResult rs = goodsService.receiveGoodsItems(record.getUserid(), GoodsInfo.GOODS_TYPE_PACKETS, 0, now, module, remark, record.getGoodsitems());
                if (!rs.isSuccess()) return rs;
            }
            source.insert(record.createLetterRecordHis(LetterRecord.LETTER_STATUS_READED, now));
            source.delete(record);
            ok = true;
        }
        if (!ok) return RetCodes.retResult(RetCodes.RET_LETTER_STATUS_ILLEGAL);
        return RetResult.success();
    }

    @Comment("删除邮件")
    public RetResult deleteLetterRecord(LetterBean bean) {
        List<LetterRecordHis> list;
        if (bean.getLetterid() != null && !bean.getLetterid().isEmpty()) {
            list = new ArrayList<>();
            LetterRecordHis one = source.find(LetterRecordHis.class, bean.getLetterid());
            if (one != null && (bean.getUserid() < 1 || one.getUserid() == bean.getUserid())) {
                list.add(one);
            }
        } else {
            list = source.queryList(LetterRecordHis.class, bean);
        }
        for (LetterRecordHis record : list) {
            record.setStatus(LetterRecord.LETTER_STATUS_EXPIRE);
            source.updateColumn(record, "status");
        }
        return RetResult.success();
    }

    @Comment("查询邮件")
    public Sheet<LetterRecord> queryLetterRecord(LetterBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        if (bean != null && Utility.contains(bean.getStatus(), LetterRecord.LETTER_STATUS_READED)) {
            int unreadCount = source.getNumberResult(LetterRecord.class, FilterFunc.COUNT, 0, null, bean).intValue();
            int readedCount = source.getNumberResult(LetterRecordHis.class, FilterFunc.COUNT, 0, null, bean).intValue();
            if (unreadCount + readedCount < 1) return Sheet.empty();
            ArrayList<LetterRecord> list = new ArrayList<>();
            if (unreadCount > 0) {
                list.addAll(source.queryList(LetterRecord.class, flipper, bean));
            }
            if (readedCount > 0 && (flipper == null || list.size() < flipper.getLimit())) {
                Flipper flipper2 = flipper == null ? null : flipper.clone();
                if (flipper2 != null) {
                    flipper2.setLimit(flipper.getLimit() - list.size());
                    flipper2.setOffset(0);
                }
                list.addAll(source.queryList(LetterRecordHis.class, flipper2, bean));
            }
            list.forEach(record -> record.setFromuser(userService.findUserInfo(record.getFromuserid())));
            return new Sheet<>(unreadCount + readedCount, list);
        } else {
            Sheet<LetterRecord> sheet = source.querySheet(LetterRecord.class, flipper, bean);
            sheet.forEach(record -> record.setFromuser(userService.findUserInfo(record.getFromuserid())));
            return sheet;
        }
    }

    @Comment("获取未读邮件条数")
    public RetResult<Integer> getUnreadLetterCount(int userid) {
        FilterNode node = FilterNode.create("userid", userid).and("status", LetterRecord.LETTER_STATUS_UNREAD);
        return RetResult.success(source.getNumberResult(LetterRecord.class, FilterFunc.COUNT, 0, null, node).intValue());
    }

    @Comment("查询历史邮件")
    public Sheet<LetterRecordHis> queryLetterRecordHis(LetterBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(LetterRecordHis.class, flipper, bean);
    }

}
