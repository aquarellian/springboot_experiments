package com.example.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.integration.mapping.ConvertingBytesMessageMapper;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.springframework.integration.zeromq.channel.ZeroMqChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.zeromq.ZAuth;
import org.zeromq.ZContext;
import zmq.util.Z85;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

@RequiredArgsConstructor
public class ZMQServer implements MessageHandler {

    public static final String RESPONSE_ID = "responseId";
    public static final String REQUEST_ID = "requestId";
    public static final String HEADERS = "headers";
    public static final String ERROR = "error";
    private final ZeroMqChannel channel;
    private final ObjectMapper mapper;

    @Override
    public void handleMessage(Message<?> m) throws MessagingException {
        if (m.getHeaders().get(RESPONSE_ID) != null) {
            return;
        }
        JsonNode originalMessage = null;
        try {
            originalMessage = mapper.readValue((byte[]) m.getPayload(), JsonNode.class);
            String requestId = originalMessage.get(HEADERS).get(REQUEST_ID).asText();
            if (requestId != null) {
                String l = "zmq";
                GenericMessage<String> respMessage = new GenericMessage<>(new ObjectMapper().writeValueAsString(new Event(l)), Collections.singletonMap(RESPONSE_ID, requestId));
                new Thread(() -> channel.send(respMessage)).start();
            }
        } catch (Throwable e) {
            if (originalMessage != null) {
                String requestId = originalMessage.get(HEADERS).get(REQUEST_ID).asText();
                GenericMessage<String> respMessage = new GenericMessage<>(getString(e.getMessage()), new HashMap<String, Object>() {{
                    put(RESPONSE_ID, requestId);
                    put(ERROR, e.getClass().getCanonicalName());
                }});
                new Thread(() -> channel.send(respMessage)).start();
            } else {
                System.out.println("Could not send a response");
            }
        }
    }

    @SneakyThrows
    private String getString(Object o)  {
        return mapper.writeValueAsString(o);
    }


    private static ZeroMqProxy zeroMqProxy(ZContext context, int frontendPort, int backendPort) {
        ZeroMqProxy proxy = new ZeroMqProxy(context, ZeroMqProxy.Type.SUB_PUB);
        proxy.setExposeCaptureSocket(true);
        proxy.setFrontendPort(frontendPort);
        proxy.setBackendPort(backendPort);
        /*proxy.setFrontendSocketConfigurer(socket -> {
            socket.setZAPDomain("global".getBytes());
            socket.setCurveServer(true);
            socket.setCurvePublicKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
            socket.setCurveSecretKey(Z85.decode("3*Kg{9Wy@Pvcy2TCLODMK74b2Df!H<xFx%aeU3^a"));
        });
        proxy.setBackendSocketConfigurer(socket -> {
            socket.setZAPDomain("global".getBytes());
            socket.setCurveServer(true);
            socket.setCurvePublicKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
            socket.setCurveSecretKey(Z85.decode("3*Kg{9Wy@Pvcy2TCLODMK74b2Df!H<xFx%aeU3^a"));
        });*/
        return proxy;
    }


    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        String host = "localhost";
        int frontendPort = 6001;
        int backendPort = 6002;

        String url = "tcp://" + host + ":" + frontendPort + ":" + backendPort;

        ZContext context = new ZContext();
        ZAuth authenticator = new ZAuth(context); //create authenticator for incoming clients
        authenticator.setVerbose(true); //get indication of what the authenticator is deciding
        authenticator.configureCurve(ZAuth.CURVE_ALLOW_ANY);


        ZeroMqChannel channel = new ZeroMqChannel(context, true);
        channel.setConnectUrl(url);
//        channel.setZeroMqProxy(zeroMqProxy(context, frontendPort, backendPort));
//        GenericMessageConverter messageConverter = new GenericMessageConverter();
//        channel.setMessageConverter(messageConverter);
//        channel.setMessageMapper(new ConvertingBytesMessageMapper(messageConverter));
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        channel.setMessageConverter(messageConverter);
        channel.setMessageMapper(new EmbeddedJsonHeadersMessageMapper());
        channel.setSubscribeSocketConfigurer(socket -> {
            socket.setZAPDomain("global".getBytes());
            socket.setCurveServer(true);
            socket.setCurvePublicKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
            socket.setCurveSecretKey(Z85.decode("3*Kg{9Wy@Pvcy2TCLODMK74b2Df!H<xFx%aeU3^a"));
        });
        channel.setSendSocketConfigurer(socket -> {
            socket.setZAPDomain("global".getBytes());
            socket.setCurveServer(true);
            socket.setCurvePublicKey(Z85.decode(".HgpMsGxpb?n!Yub))n#+{YzLL{&)7D$icCIx6#?"));
            socket.setCurveSecretKey(Z85.decode("3*Kg{9Wy@Pvcy2TCLODMK74b2Df!H<xFx%aeU3^a"));
        });

        channel.afterPropertiesSet();
        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
        channel.subscribe(new ZMQServer(channel, objectMapper));
    }

}
