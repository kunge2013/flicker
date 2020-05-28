/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game;

import com.cratos.platf.user.Location;
import static com.cratos.platf.util.Utils.HEADNAME_WS_SNCP_ADDRESS;
import java.net.InetSocketAddress;
import java.util.*;
import javax.persistence.Column;
import org.redkale.net.http.*;

/**
 * enterGame 参数
 *
 * @author zhangjx
 */
public class GamePlayerBean extends Location {

    @Column(comment = "场次")
    protected int roomlevel;

    @Column(comment = "玩家坐位, 1-N")
    protected int sitepos;

    @RestAddress
    protected String clientAddr = "";

    @RestHeader(name = HEADNAME_WS_SNCP_ADDRESS)
    protected InetSocketAddress sncpAddress;

    public Map<String, String> createMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("roomlevel", "" + roomlevel);
        map.put("sitepos", "" + sitepos);
        if (clientAddr != null) map.put("clientAddr", clientAddr);
        return map;
    }

    public int getRoomlevel() {
        return roomlevel;
    }

    public void setRoomlevel(int roomlevel) {
        this.roomlevel = roomlevel;
    }

    public int getSitepos() {
        return sitepos;
    }

    public void setSitepos(int sitepos) {
        this.sitepos = sitepos;
    }

    public String getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    public InetSocketAddress getSncpAddress() {
        return sncpAddress;
    }

    public void setSncpAddress(InetSocketAddress sncpAddress) {
        this.sncpAddress = sncpAddress;
    }

}
