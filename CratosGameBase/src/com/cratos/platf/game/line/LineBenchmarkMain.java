/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cratos.platf.game.line;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author zhangjx
 */
public class LineBenchmarkMain {

    public static void main(String[] args) throws Throwable {
        final int wscount = 200;
        final CountDownLatch cdl = new CountDownLatch(wscount);
        final java.net.http.WebSocket.Builder wsBuilder = java.net.http.HttpClient.newHttpClient().newWebSocketBuilder();
        for (int i = 0; i < wscount; i++) {
            final int index = i;
            final long mob = 77777770001L + i;
            URI uri = new URI("ws://116.204.186.140/ws/wsgame?appagent=hjqp%2F0.0.44%3B%20web%2F1.0%3B%20chrome%2F79.0.3945.88%3B%201280*720*1%3B&bean=%7B%22autologin%22%3Atrue%2C%22agencyid%22%3A0%2C%22cookieinfo%22%3A%22%22%2C%22account%22%3A%22" + mob + "%22%2C%22password%22%3A%22e10adc3949ba59abbe56e057f20f883e%22%2C%22netmode%22%3A%22web%22%2C%22appos%22%3A%22web%22%2C%22apptoken%22%3A%22bfa3718d-af76-4a8f-82b0-ae1c0960f82c%22%7D");
            final java.net.http.WebSocket.Listener listener = new WebSocket.Listener() {

                String seqno;

                boolean round;

                AtomicInteger counter = new AtomicInteger(100);

                StringBuilder sb = new StringBuilder("");

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    webSocket.request(1);
                    if (!last) {
                        sb.append(data);
                        return null;
                    }
                    sb.append(data);
                    data = sb.toString();
                    sb = new StringBuilder();

                    if (data.toString().contains("onHrollAnnounceMessage")) return null;

                    if (data.toString().contains("onUserLoginMessage")) {
                        return webSocket.sendText("{\"message\":{\"module\":\"zcjb\",\"action\":\"enterGame\",\"params\":\"bean=%7B%22roomlevel%22%3A1%7D\",\"http_seqno\":\"" + createSeqno() + "\"}}", true);
                    }
                    if (!round && data.toString().contains("{\"onHttpResponseMessage\":{\"http_seqno\":\"" + seqno + "\"")) {
                        round = true;
                        return webSocket.sendText("{\"message\":{\"module\":\"zcjb\",\"action\":\"runRound\",\"params\":\"bean=%7B%22linenum%22%3A1%7D\",\"http_seqno\":\"" + createSeqno() + "\"}}", true);
                    }
                    if (round && data.toString().contains("{\"onHttpResponseMessage\":{\"http_seqno\":\"" + seqno + "\"")) {
                        if (counter.decrementAndGet() < 1) {
                            webSocket.abort();
                            cdl.countDown();
                            System.out.println("关闭连接: " + cdl.getCount());
                            return null;
                        }
                        return webSocket.sendText("{\"message\":{\"module\":\"zcjb\",\"action\":\"runRound\",\"params\":\"bean=%7B%22linenum%22%3A1%7D\",\"http_seqno\":\"" + createSeqno() + "\"}}", true);
                    }
                    System.out.println("接收内容(last=" + last + "): " + data);
                    return null;
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    cdl.countDown();
                    System.out.println(index + "连接关闭了");
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    cdl.countDown();
                    System.out.println(index + "连接异常了");
                }

                private synchronized String createSeqno() {
                    seqno = "hsn" + System.nanoTime();
                    return seqno;
                }
            };
            wsBuilder.buildAsync(uri, listener).whenComplete((ws, e) -> {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }
                System.out.println("连接成功: " + index);
            });
        }
        cdl.await();
    }
}
