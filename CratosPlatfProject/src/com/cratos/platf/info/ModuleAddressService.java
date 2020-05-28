/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.info;

import com.cratos.platf.base.*;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.convert.json.JsonConvert;
import org.redkale.service.*;
import org.redkale.source.CacheSource;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Local
public class ModuleAddressService extends BaseService {

    private static final Type RETRESULT_STRING = new TypeToken<RetResult<String>>() {
    }.getType();

    protected ScheduledThreadPoolExecutor scheduler;

    /**
     * 存放的信息类型: (IP端口为HTTP服务对应的地址)
     * 1、模块对应的nodeid和ip端口信息。 格式： key = module:xxx, value = 10@192.168.1.1:6161; 20@192.168.1.2:6262;
     *
     */
    @Resource(name = "wsgame")
    protected CacheSource<InetSocketAddress> gatewayNodes;

    //key为模块名， value的map的key为nodeid
    protected Map<String, Map<String, InetSocketAddress>> moduleAddress = new ConcurrentHashMap<>();

    @Override
    public void init(AnyValue conf) {
        scheduler = new ScheduledThreadPoolExecutor(1, (Runnable r) -> {
            final Thread t = new Thread(r, "ModuleAddress-Task-Thread");
            t.setDaemon(true);
            return t;
        });

        loadModuleAddress();

        scheduler.scheduleAtFixedRate(() -> {
            loadModuleAddress();
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void destroy(AnyValue conf) {
        if (scheduler != null) scheduler.shutdownNow();
    }
    
    public Map<String, Map<String, InetSocketAddress>> loadModuleMap() {
        return moduleAddress;
    }

    public RetResult<String> remoteGameModule(int userid, String gameid, String actionurl, Map<String, Object> bean) {
        Map<String, Map<String, InetSocketAddress>> moduleMap = loadModuleMap();
        Map<String, InetSocketAddress> addrMap = moduleMap.get(gameid);
        RetResult<String> rs = RetResult.success();
        if (addrMap != null) {
            final String module = gameid;
            final int sub = module.indexOf('_');
            String url = null;
            String uri = "/pipes/" + (sub > 0 ? module.substring(sub + 1) : module) + "/" + actionurl + "?userid=" + userid;
            if (bean != null && !bean.isEmpty()) {
                for (Map.Entry<String, Object> en : bean.entrySet()) {
                    if (en.getValue() instanceof CharSequence) {
                        uri += "&" + en.getKey() + "=" + URLEncoder.encode(en.getValue().toString(), StandardCharsets.UTF_8);
                    } else {
                        uri += "&" + en.getKey() + "=" + URLEncoder.encode(JsonConvert.root().convertTo(en.getValue()), StandardCharsets.UTF_8);
                    }
                }
            }
            for (InetSocketAddress addr : addrMap.values()) {
                try {
                    url = "http://" + addr.getHostString() + ":" + addr.getPort() + uri;
                    String content = Utility.getHttpContent(url, 3000);
                    rs = JsonConvert.root().convertFrom(RETRESULT_STRING, content);
                    if (logger.isLoggable(Level.FINEST)) logger.finest("platf." + actionurl + ".url = " + url + ", result:" + content);
                    if (!rs.isSuccess()) return rs;
                } catch (Exception e) {
                    rs = RetCodes.retResult(RetCodes.RET_PARAMS_ILLEGAL);
                    logger.log(Level.SEVERE, url + " remote error", e);
                }
            }
        }
        return rs;
    }

    private void loadModuleAddress() {
        Map<String, Map<String, InetSocketAddress>> moduleMap = new ConcurrentHashMap<>(moduleAddress);
        List<String> keys = gatewayNodes.queryKeysStartsWith("module:");
        if (keys != null) {
            for (String key : keys) {
                if (!key.startsWith("module:")) continue;
                Collection<String> addrs = gatewayNodes.getStringCollection(key);
                String module = key.substring("module:".length());
                Map<String, InetSocketAddress> map = new HashMap<>();
                for (String addr : addrs) {
                    int pos = addr.indexOf('@');
                    int pos2 = addr.indexOf(':');
                    String snodeid = addr.substring(0, pos);
                    String hostname = addr.substring(pos + 1, pos2);
                    int port = Integer.parseInt(addr.substring(pos2 + 1));
                    map.put(snodeid, new InetSocketAddress(hostname, port));
                }
                moduleMap.put(module, map);
            }
        }
        this.moduleAddress = moduleMap;
    }
}
