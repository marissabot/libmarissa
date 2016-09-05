package org.marissabot.libmarissa;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import org.marissabot.libmarissa.model.Context;

public interface RoutingEventListener {
    @Suspendable
    void routingEvent(Context context, String trigger, Response response) throws InterruptedException, SuspendExecution;
}
