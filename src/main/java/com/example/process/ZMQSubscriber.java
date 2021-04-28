package com.example.process;

import lombok.RequiredArgsConstructor;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

@RequiredArgsConstructor
public class ZMQSubscriber {

    private final ZMQ.Socket client;

    public void processMessage() {
        try {
            String s = client.recvStr(0);
            System.out.println("Received:" + s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ZMQSubscriber createInstance() {
        ZMQ.Socket client = SocketFactory.createSocket("127.0.0.1", 6002, false, SocketType.SUB);
        client.subscribe(new byte[0]);
        return new ZMQSubscriber(client);
    }

    public static void main(String[] args) {
        ZMQSubscriber subscriber = createInstance();
        while (true) {
            subscriber.processMessage();
        }
    }
}
