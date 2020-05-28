/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.rpc;

import com.cratos.platf.base.BaseService;
import com.cratos.platf.order.*;
import com.cratos.platf.user.UserService;
import java.util.Map;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;

/**
 * 供OSS系统使用的远程模式服务
 *
 * @author zhangjx
 */
@RestService(name = "remote")
public class RemoteService extends BaseService {

    @Resource
    protected CardService cardService;

    @Resource
    protected UserService userService;

    @Resource
    protected OrderService orderService;

    @RestMapping(auth = false, comment = "更新银行卡或支付宝账号信息")
    public RetResult<String> updateCardInfo(CardInfo bean) {
        return cardService.updateCardInfo(bean);
    }

    @RestMapping(auth = false, comment = "设置玩家角色")
    public RetResult updateType(int userid, short type) {
        return this.userService.updateType(userid, type);
    }

    @RestMapping(auth = false, comment = "获取当前在线用户数, 包含大厅和游戏里的玩家人数")
    public int getCurrUserSize() {
        return userService.getCurrUserSize();
    }

    @RestMapping(auth = false, comment = "获取每种游戏的在线人数。 key=gameid, value=人数")
    public Map<String, Long> getUserPlayingCount() {
        return userService.getUserPlayingCount();
    }

    @RestMapping(auth = false, comment = "刷新当前支付配置")
    public RetResult refreshPayConfig() {
        return this.orderService.refreshPayConfig(false);
    }
}
