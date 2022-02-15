package com.amelin.ws;

public class Message {

    private String from;
    private String to;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "com.amelin.ws.Message{" +
                "message='" + message + '\'' +
                '}';
    }
}