package org.marissabot.libmarissa;

import co.paralleluniverse.fibers.SuspendExecution;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.marissabot.libmarissa.model.Context;
import org.slf4j.LoggerFactory;

public class Router {

    private final Map<Pattern, RoutingEventListener> routingTable = new HashMap<>();
    private Pattern baseName;

    public Router(String baseName)
    {
        if (baseName==null) {
            throw new IllegalArgumentException("Base name can't be null");
        }

        this.baseName=Pattern.compile(baseName+"\\s+");
    }

    public void on(String pattern, RoutingEventListener routingEventListener)
    {
        if (routingEventListener == null) {
            throw new IllegalArgumentException("response handler can't be null");
        }

        whenContains(baseName.pattern().concat(pattern), routingEventListener);
    }

    public void whenContains(String pattern, RoutingEventListener routingEventListener)
    {
        if (routingEventListener == null) {
            throw new IllegalArgumentException("response handler can't be null");
        }

        routingTable.put(Pattern.compile(pattern), routingEventListener);
    }

    protected void triggerHandlersForMessageText(final Context context, final String sentText, final Response useResponse)
    {
        String trimmed = sentText.trim();
        routingTable.keySet().stream()
                .filter(key -> key.matcher(trimmed).matches())
                .forEach(key -> fireEventAsync(context, key, trimmed, useResponse));
    }

    private void fireEventAsync(final Context context, final Pattern key, final String request, Response useResponse)
    {
        new Thread(() -> {
            try {
                Router.this.routingTable.get(key).routingEvent(context, request, useResponse);
            } catch (InterruptedException | SuspendExecution e) {
                LoggerFactory.getLogger(Router.class).error("this shouldn't happen", e);
            }
        }).start();
    }

}
