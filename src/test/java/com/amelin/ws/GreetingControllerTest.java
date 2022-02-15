package com.amelin.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GreetingControllerTest {

    @LocalServerPort
    private Integer port;

    private WebSocketStompClient webSocketStompClient_1;
    private WebSocketStompClient webSocketStompClient_2;

    @BeforeEach
    public void setup() {
        this.webSocketStompClient_1 = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        this.webSocketStompClient_2 = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
    }

    @Test
    public void verifyMessageExchange() throws Exception {
        BlockingQueue<String> blockingQueue_1 = new ArrayBlockingQueue(1);
        BlockingQueue<String> blockingQueue_2 = new ArrayBlockingQueue(1);

        webSocketStompClient_1.setMessageConverter(new MappingJackson2MessageConverter());
        webSocketStompClient_2.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session_1 = webSocketStompClient_1
                .connect(getWsPath(), new StompSessionHandlerAdapter() {})
                .get(1, SECONDS);
        StompSession session_2 = webSocketStompClient_2
                .connect(getWsPath(), new StompSessionHandlerAdapter() {})
                .get(1, SECONDS);

        session_1.subscribe("/user/queue/message", new StompFrameHandler() {

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Message.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue_1.add(((Message) payload).getMessage());
            }
        });

        session_2.subscribe("/user/queue/message", new StompFrameHandler() {

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Message.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue_2.add(((Message) payload).getMessage());
            }
        });

        Message message = new Message();
        message.setFrom(String.valueOf(1));
        message.setTo(String.valueOf(2));
        message.setMessage("Hello, second");
        session_1.send("/app/message", message);

        assertEquals("Hello, second", blockingQueue_2.poll(1, SECONDS));
    }

    @Test
    public void verifyGreetingIsReceived() throws Exception {

        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue(1);

        webSocketStompClient_1.setMessageConverter(new StringMessageConverter());

        StompSession session = webSocketStompClient_1
                .connect(getWsPath(), new StompSessionHandlerAdapter() {})
                .get(1, SECONDS);

        session.subscribe("/topic/greetings", new StompFrameHandler() {

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Received message: " + payload);
                blockingQueue.add((String) payload);
            }
        });

        session.send("/app/welcome", "Mike");

        assertEquals("Hello, Mike!", blockingQueue.poll(1, SECONDS));
    }

    @Test
    public void verifyWelcomeMessageIsSent() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        webSocketStompClient_1.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = webSocketStompClient_1
                .connect(getWsPath(), new StompSessionHandlerAdapter() {
                })
                .get(1, SECONDS);

        session.subscribe("/app/chat", new StompFrameHandler() {

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Message.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                latch.countDown();
            }
        });

        if (!latch.await(1, TimeUnit.SECONDS)) {
            fail("Message not received");
        }
    }

    private String getWsPath() {
        return String.format("ws://localhost:%d/ws-endpoint", port);
    }
}
