/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.route.gateway;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.annotation.Resource;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.source.CacheSource;
import org.redkale.util.AnyValue;

/**
 * 只能是本地模式
 *
 * @author zhangjx
 */
@Local
public class GateWayService extends AbstractService {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    /**
     * 存放的信息类型: (IP端口为HTTP服务对应的地址)
     * 1、模块对应的nodeid和ip端口信息。 格式： key = module:xxx, value = 10@192.168.1.1:6161; 20@192.168.1.2:6262;
     *
     */
    @Resource(name = "wsgame")
    protected CacheSource<InetSocketAddress> gatewayNodes;

    @Resource(name = "wsgame")
    protected WebSocketNode webSocketNode;

    private ScheduledThreadPoolExecutor scheduler;

    //key为模块名， value的map的key为nodeid
    protected Map<String, Map<String, ModuleNodeAddress>> moduleAddress = new ConcurrentHashMap<>();

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(2, (Runnable r) -> {
            final Thread t = new Thread(r, this.getClass().getSimpleName() + "-LoadModuleAddressTask-Thread");
            t.setDaemon(true);
            return t;
        });
        loadModuleAddress();
        logger.info("获得全局模块keys: " + moduleAddress);
        scheduler.scheduleAtFixedRate(() -> {
            loadModuleAddress();
        }, 60, 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> {
            if (webSocketNode == null) return;
            try {
                WebSocketEngine engine = webSocketNode.getLocalWebSocketEngine();
                if (engine == null) return;
                Collection<WebSocket> list = engine.getLocalWebSockets();
                if (list.isEmpty()) return;
                for (WebSocket ws : list) {
                    ((GateWayWebSocket) ws).checkUser();
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "checkUser error", t);
            }
        }, 0, 2, TimeUnit.MINUTES);
        logger.finest(this.getClass().getSimpleName() + " start LoadModuleAddressTask task scheduler executor");

    }

    private void loadModuleAddress() {
        try {
            List<String> keys = gatewayNodes.queryKeysStartsWith("module:");
            if (keys == null) throw new RuntimeException("加载模块IP信息错误");
            Map<String, Map<String, ModuleNodeAddress>> newMap = new ConcurrentHashMap<>(this.moduleAddress);
            for (String key : keys) {
                if (!key.startsWith("module:")) continue;
                Collection<String> addrs = gatewayNodes.getStringCollection(key);
                String module = key.substring("module:".length());
                Map<String, ModuleNodeAddress> map = new ConcurrentHashMap<>();
                for (String addr : addrs) {
                    int pos = addr.indexOf('@');
                    int pos2 = addr.indexOf(':');
                    String nodeid = addr.substring(0, pos);
                    String hostname = addr.substring(pos + 1, pos2);
                    int port = Integer.parseInt(addr.substring(pos2 + 1));
                    map.put(nodeid, new ModuleNodeAddress(new InetSocketAddress(hostname, port)));
                }
                newMap.put(module, map);
            }
            this.moduleAddress = newMap;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "loadModuleAddress error", t);
        }
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    public Map<String, Map<String, ModuleNodeAddress>> getModuleAddress() {
        return this.moduleAddress;
    }

    public CompletableFuture<Integer> getUserSize() {
        return webSocketNode.getUserSize();
    }

    public ModuleNodeAddress loadHttpAddress(String module, String nodeid, Map<String, ModuleNodeAddress> myModuleNodes) {
        final int sub = module.indexOf('_');
        String node = nodeid != null && nodeid.length() > 2 ? nodeid.substring(nodeid.length() - 2) : "0";
        if (node.length() < 2 && myModuleNodes != null) {
            ModuleNodeAddress old = myModuleNodes.get(module);
            if (old != null) return old;
        }
        Map<String, Map<String, ModuleNodeAddress>> localModuleAddress = this.getModuleAddress();
        Map<String, ModuleNodeAddress> map = localModuleAddress.get(module);
        if (map == null && sub > 0) map = localModuleAddress.get(module.substring(sub + 1));
        if (map == null && sub > 0) map = localModuleAddress.get(module.substring(0, sub));
        if (map == null) map = localModuleAddress.get("platf");
        if (map == null || map.isEmpty()) return null;
        ModuleNodeAddress addr;
        if (node.length() < 2) { //第一次进入某个模块
            List<ModuleNodeAddress> addrs = new ArrayList<>(map.values());
            addr = addrs.get((int) (System.currentTimeMillis() % addrs.size()));
        } else {
            addr = map.get(node);
        }
        if (addr != null && myModuleNodes != null) myModuleNodes.put(module, addr);
        return addr;
    }

}
