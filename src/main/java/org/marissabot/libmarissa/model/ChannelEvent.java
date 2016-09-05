package org.marissabot.libmarissa.model;

public class ChannelEvent<T> {

    public enum EventType {
        XMPP,
        CONTROL
    }

    private EventType eventType;
    private T payload;

    public ChannelEvent(EventType eventType, T payload) {
        this.eventType = eventType;
        this.payload = payload;
    }

    public EventType getEventType() {
        return eventType;
    }

    public T getPayload() {
        return payload;
    }
}
