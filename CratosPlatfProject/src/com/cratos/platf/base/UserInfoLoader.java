/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.base;

import java.util.*;
import java.util.function.BiFunction;
import com.cratos.platf.user.UserDetail;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class UserInfoLoader implements BiFunction<DataSource, Class, List> {

    @Override
    public List apply(DataSource source, Class type) {
        List<UserDetail> details = source.queryList(UserDetail.class, (FilterNode) null);
        List<UserInfo> list = new ArrayList<>(details.size());
        for (UserDetail detail : details) {
            list.add(detail.createUserInfo());
        }
        return list;
    }

}
