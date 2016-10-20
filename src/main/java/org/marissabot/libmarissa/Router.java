package org.marissabot.libmarissa;

import co.paralleluniverse.fibers.SuspendExecution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.marissabot.libmarissa.model.Context;

import org.slf4j.LoggerFactory;

public class Router {

    private final Map<Pattern, RoutingEventListener> routingTable = new LinkedHashMap<>();
    private final Pattern baseName;
    private final boolean matchAll;

    public Router(String baseName, boolean matchAll)
    {
        if (baseName==null) {
            throw new IllegalArgumentException("Base name can't be null");
        }

        this.baseName=Pattern.compile(baseName+"\\s+");
        this.matchAll=matchAll;
    }

    public void on(String pattern, RoutingEventListener routingEventListener)
    {
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

        for (Map.Entry<Pattern, RoutingEventListener> entry : routingTable.entrySet()) {
            Pattern key = entry.getKey();

            if (key.matcher(trimmed).matches()) {
                fireEventAsync(context, key, trimmed, useResponse);

                if (!this.matchAll) {
                    break;
                }
            }
        }
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

    public Map<Pattern, RoutingEventListener> getRoutingTable()
    {
        return this.routingTable;
    }

    public Pattern getBaseName()
    {
        return this.baseName;
    }

    public boolean getMatchAll()
    {
        return this.matchAll;
    }
}
