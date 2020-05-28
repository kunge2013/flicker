/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.agent.node;

import java.net.InetSocketAddress;
import java.util.*;
import javax.annotation.Resource;
import org.redkale.service.AbstractService;
import org.redkale.source.CacheSource;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class AgentRouteService extends AbstractService {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    @Resource(name = "system.property.nodeid")
    protected int nodeid1;

    @Resource(name = "property.nodeid")
    protected int nodeid2;

    @Resource(name = "system.property.modules")
    protected String modules;

    @Resource(name = "wsgame")
    protected CacheSource<String> gatewayNodes;

    protected InetSocketAddress agentAddr;

    protected Map<String, String> moduleNodes = new HashMap<>();

    public void initAgent(InetSocketAddress address) {
        if (address == null || modules == null) return;
        String ip = address.getAddress().getHostAddress().replace("0.0.0.0", Utility.localInetAddress().getHostAddress());
        this.agentAddr = new InetSocketAddress(ip, address.getPort());
        int nodeid = nodeid2 > 0 ? nodeid2 : nodeid1; //兼容
        for (String module0 : modules.split(";")) {
            String module = module0.trim();
            if (module.isEmpty()) return;
            Collection<String> nodes = gatewayNodes.getStringCollection("module:" + module);
            String addr = nodeid + "@" + this.agentAddr.getHostString() + ":" + this.agentAddr.getPort();
            if (nodes != null) {
                for (String node : nodes) {
                    if (node.startsWith(nodeid + "@") && !node.equals(addr)) {
                        if (winos) {
                            gatewayNodes.removeStringSetItem("module:" + module, node);
                        } else {
                            throw new RuntimeException(node + " had exists");
                        }
                    }
                }
            }
            moduleNodes.put("module:" + module, addr);
            gatewayNodes.appendStringSetItem("module:" + module, addr);
        }
    }

    @Override
    public void destroy(AnyValue config) {
        if (agentAddr == null) return;
        moduleNodes.forEach((x, y) -> gatewayNodes.removeStringSetItem(x, y));
    }
}
