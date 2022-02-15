package com.amelin.ws;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GreetingController {
    private final SimpMessagingTemplate messagingTemplate;

    public GreetingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/welcome")
    @SendTo("/topic/greetings")
    public String greeting(String payload) {
        System.out.println("Generating new greeting message for " + payload);
        return "Hello, " + payload + "!";
    }

    @SubscribeMapping("/chat")
    public Message sendWelcomeMessageOnSubscription() {
        Message welcomeMessage = new Message();
        welcomeMessage.setMessage("Hello World!");
        return welcomeMessage;
    }

    @MessageMapping("/message")
    @SendToUser("/queue/message")
    public Message processMessage(Message message){
        messagingTemplate.convertAndSendToUser(message.getTo(), "/queue/message", message);

        return message;
    }

}