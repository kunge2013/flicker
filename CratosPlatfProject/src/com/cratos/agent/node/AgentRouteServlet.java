/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.agent.node;

import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.util.AnyValue;

/**
 *
 * @author zhangjx
 */
@WebServlet(value = {"/abcdefglhijklmn/*"}, repair = false, comment = "无效Servlet服务")
public class AgentRouteServlet extends HttpServlet {

    @Resource
    protected AgentRouteService service;

    @Override
    public void init(HttpContext context, AnyValue config) {
        service.initAgent(context.getServerAddress());
    }
}
