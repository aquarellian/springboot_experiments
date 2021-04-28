package com.example.servingwebcontent;

import com.example.process.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.zeromq.channel.ZeroMqChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;


@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ZeroMqService {
    public static final String TOPIC_HEADER = "topic";
    public static final String REQUEST_ID_HEADER = "requestId";
    public static final String RESPONSE_ID_HEADER = "responseId";
    public static final String ERROR_HEADER = "error";

    private final ZeroMqChannel reqChannel;
    private final ObjectMapper objectMapper;

    public Future<String> getText(String param) {
        return imitateReqRep(param, m -> (String) m.getPayload());
    }

    private <V> Future<V> imitateReqRep(String request, Function<Message<?>, V> responseFunction) {
        String requestId = UUID.randomUUID().toString();
        GenericMessage<Event> message = new GenericMessage<>(new Event(request), Collections.unmodifiableMap(new HashMap<String, Object>() {{
            put(TOPIC_HEADER, "topic");
            put(REQUEST_ID_HEADER, requestId);
        }}));
        reqChannel.send(message);
        CompletableFuture<V> future = new CompletableFuture<>();
        ConfigMessageHandler<V> handler = new ConfigMessageHandler<>(future, requestId, responseFunction);
        reqChannel.subscribe(handler);
        future.whenComplete((res, ex) -> reqChannel.unsubscribe(handler));
        return future;
    }

    @SneakyThrows
    private String getString(String request)  {
        return objectMapper.writeValueAsString(request);
    }

    @RequiredArgsConstructor
    private class ConfigMessageHandler<V> implements MessageHandler {
        private final CompletableFuture<V> future;
        private final String requestId;
        private final Function<Message<?>, V> responseAction;

        @Override
        public void handleMessage(Message<?> m) throws MessagingException {
            if (m.getHeaders().get(RESPONSE_ID_HEADER) == null) {
                return;
            }
            try {
                String responseId = Objects.requireNonNull(m.getHeaders().get(RESPONSE_ID_HEADER)).toString();
                Object error = m.getHeaders().get(ERROR_HEADER);

                if (requestId.equals(responseId)) {
                    if (error != null) {
                        String message = objectMapper.convertValue(m.getPayload(), String.class);
                        future.completeExceptionally(new RuntimeException(message));
                    } else {
                        future.complete(responseAction.apply(m));
                    }
                    reqChannel.unsubscribe(ConfigMessageHandler.this);
                }
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
                reqChannel.unsubscribe(ConfigMessageHandler.this);
            }
        }
    }
}
