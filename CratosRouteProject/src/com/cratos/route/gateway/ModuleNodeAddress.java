/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.route.gateway;

import java.net.InetSocketAddress;
import java.util.concurrent.Semaphore;

/**
 *
 * @author zhangjx
 */
public class ModuleNodeAddress {

    public final InetSocketAddress address;

    public final Semaphore semaphore = new Semaphore(3000);

    public ModuleNodeAddress(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "{addr=" + address + ", pool=" + semaphore.availablePermits() + "}";
    }
}
