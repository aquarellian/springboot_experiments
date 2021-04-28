package com.example.process;

import org.zeromq.*;
import zmq.util.Z85;

public class SocketFactory {


    public static ZMQ.Socket createSocket(String host, int port, boolean isServer, SocketType socketType) {

        String url = "tcp://" + host + ":" + port;
        System.out.println((isServer? "Binding": "Connecting") + " as " + socketType.name() + " to url " + url);
        ZContext ctx = new ZContext();

        ZAuth auth = new ZAuth(ctx);
        auth.setVerbose(true);
        auth.configureCurve(ZAuth.CURVE_ALLOW_ANY);

        ZMQ.Socket client = ctx.createSocket(socketType);
        if (isServer) {
//            client.setZAPDomain("global".getBytes());
//            client.setCurveServer(true);
//            client.setCurvePublicKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
//            client.setCurveSecretKey(Z85.decode("3*Kg{9Wy@Pvcy2TCLODMK74b2Df!H<xFx%aeU3^a"));
            client.bind(url);
        } else {
//            ZCert clientCert = new ZCert();
//            client.setCurvePublicKey(clientCert.getPublicKey());
//            client.setCurveSecretKey(clientCert.getSecretKey());
//            client.setCurveServerKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
            client.connect(url);
        }

        return client;
    }
}

