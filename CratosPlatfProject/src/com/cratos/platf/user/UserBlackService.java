/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.user;

import com.cratos.platf.base.BaseService;
import java.util.*;
import javax.annotation.Resource;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class UserBlackService extends BaseService {

    @Resource(name = "platf")
    protected DataSource source;

    public void removeUserBlackItem(String blackitemid, int memberid) {
        UserBlackItem item = source.find(UserBlackItem.class, blackitemid);
        if (item == null) return;
        source.insert(item.createUserBlackItemHis(memberid));
        source.delete(UserBlackItem.class, item.getBlackitemid());
    }

    public void removeUserBlackItem(short blacktype, String blackvalue, int memberid) {
        UserBlackItem item = source.find(UserBlackItem.class, FilterNode.create("blackvalue", blackvalue).and("blacktype", blacktype));
        if (item == null) return;
        source.insert(item.createUserBlackItemHis(memberid));
        source.delete(UserBlackItem.class, item.getBlackitemid());
    }

    public void insertUserBlackItem(UserBlackItem code) {
        if (code.getBlackvalue() == null || code.getBlackvalue().isEmpty()) return;
        if (existsUserBlackItem(code.getBlackvalue())) return;
        code.setCreatetime(System.currentTimeMillis());
        code.setBlackitemid(Utility.format36time(code.getCreatetime()) + Utility.uuid());
        source.insert(code);
    }

    public void insertUserBlackRecord(short blacktype, String blackvalue, String account) {
        UserBlackRecord record = new UserBlackRecord();
        record.setBlacktype(blacktype);
        record.setBlackvalue(blackvalue);
        record.setAccount(account);
        record.setCreatetime(System.currentTimeMillis());
        record.setBlackrecordid(Utility.format36time(record.getCreatetime()) + Utility.uuid());
        source.insert(record);
    }

    public boolean existsUserBlackItem(String blackvalue) {
        return source.exists(UserBlackItem.class, FilterNode.create("blackvalue", blackvalue));
    }

    public List<String> queryUserBlackIp() {
        UserBlackBean bean = new UserBlackBean();
        bean.setBlacktype(UserBlackItem.BLACKTYPE_IP);
        List<UserBlackItem> list = source.queryList(UserBlackItem.class, bean);
        List<String> rs = new ArrayList<>();
        for (UserBlackItem item : list) {
            rs.add(item.getBlackvalue());
        }
        return rs;
    }

    public List<String> queryUserBlackApptoken() {
        UserBlackBean bean = new UserBlackBean();
        bean.setBlacktype(UserBlackItem.BLACKTYPE_APPTOKEN);
        List<UserBlackItem> list = source.queryList(UserBlackItem.class, bean);
        List<String> rs = new ArrayList<>();
        for (UserBlackItem item : list) {
            rs.add(item.getBlackvalue());
        }
        return rs;
    }

    public Sheet<UserBlackRecord> queryUserBlackRecord(UserBlackBean bean, Flipper flipper) {
        return source.querySheet(UserBlackRecord.class, flipper, bean);
    }

    public Sheet<UserBlackItem> queryUserBlackItem(UserBlackBean bean, Flipper flipper) {
        return source.querySheet(UserBlackItem.class, flipper, bean);
    }

    public Sheet<UserBlackItemHis> queryUserBlackItemHis(UserBlackBean bean, Flipper flipper) {
        return source.querySheet(UserBlackItemHis.class, flipper, bean);
    }
}
