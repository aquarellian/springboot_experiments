package com.example.process;

import lombok.RequiredArgsConstructor;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

@RequiredArgsConstructor
public class ZMQPublisher {
    private final ZMQ.Socket client;

    public void publish(String message) {
        try {
            client.send(message);
            System.out.println("Sent: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ZMQPublisher createInstance() {
        ZMQ.Socket client = SocketFactory.createSocket("127.0.0.1", 6003, true, SocketType.PUB);
        return new ZMQPublisher(client);
    }

    public static void main(String[] args) {
        ZMQPublisher publisher = createInstance();
        for (int i = 0; i < 10; i++) {
            publisher.publish(String.valueOf(i));
        }
    }

}
