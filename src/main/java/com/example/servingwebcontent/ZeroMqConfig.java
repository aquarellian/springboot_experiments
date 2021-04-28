package com.example.servingwebcontent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.springframework.integration.zeromq.channel.ZeroMqChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.zeromq.ZAuth;
import org.zeromq.ZCert;
import org.zeromq.ZContext;
import zmq.util.Z85;

import java.time.Duration;

@Configuration
public class ZeroMqConfig {

    @Bean
    ZeroMqProxy zeroMqProxy(ZContext context, @Value("${zmq.channel.port.frontend}") int frontendPort,
                            @Value("${zmq.channel.port.backend}") int backendPort) {
        ZeroMqProxy proxy = new ZeroMqProxy(context, ZeroMqProxy.Type.SUB_PUB);
        proxy.setExposeCaptureSocket(true);
        proxy.setFrontendPort(frontendPort);
        proxy.setBackendPort(backendPort);
        /*ZCert cert = new ZCert();
        proxy.setFrontendSocketConfigurer(socket -> {
            socket.setCurvePublicKey(cert.getPublicKey());
            socket.setCurveSecretKey(cert.getSecretKey());
            socket.setCurveServerKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
        });
        proxy.setBackendSocketConfigurer(socket -> {
            socket.setCurvePublicKey(cert.getPublicKey());
            socket.setCurveSecretKey(cert.getSecretKey());
            socket.setCurveServerKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
        });*/
        return proxy;
    }

    @Bean
    ObjectMapper messagingAwareObjectMapper() {
        return new ObjectMapper(); // configs are irrelevant
    }

    @Bean
    public ZContext zContext() {
        ZContext context = new ZContext();
        ZAuth auth = new ZAuth(context);
        auth.configureCurve(ZAuth.CURVE_ALLOW_ANY);
        auth.setVerbose(true);
        return context;
    }

    @Bean(name = "zeroMqPubChannel")
    ZeroMqChannel zeroMqPubChannel(ZContext context, ObjectMapper objectMapper,
                                   @Value("${zmq.channel.host}") String host,
                                   @Value("${zmq.channel.port.frontend}") int frontendPort,
                                   @Value("${zmq.channel.port.backend}") int backendPort) {
        //get indication of what the authenticator is deciding

        ZeroMqChannel channel = new ZeroMqChannel(context, true);
        channel.setConnectUrl("tcp://" + host + ":" + frontendPort + ":" + backendPort);
//        channel.setZeroMqProxy(proxy);
        channel.setConsumeDelay(Duration.ofMillis(100));
        channel.setMessageConverter(new GenericMessageConverter());
        ZCert cert = new ZCert();
        channel.setSendSocketConfigurer(socket -> {
            socket.setCurvePublicKey(cert.getPublicKey());
            socket.setCurveSecretKey(cert.getSecretKey());
            socket.setCurveServerKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
        });
        channel.setSubscribeSocketConfigurer(socket -> {
            socket.setCurvePublicKey(cert.getPublicKey());
            socket.setCurveSecretKey(cert.getSecretKey());
            socket.setCurveServerKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
        });
        EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper(objectMapper);
        channel.setMessageMapper(mapper);
        return channel;
    }

    @Bean
    @ServiceActivator(inputChannel = "zeroMqPubChannel")
    public MessageHandler subscribe() {
        return message -> System.out.println(message);
    }
}

