package org.marissabot.libmarissa;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import org.junit.Before;
import org.junit.Test;
import org.marissabot.libmarissa.model.Address;
import org.marissabot.libmarissa.model.ChannelEvent;
import org.marissabot.libmarissa.model.Context;
import rocks.xmpp.addr.Jid;


import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RouterTest {

    private Router r;
    private Timeout defaultTimeout;
    private Channel<ChannelEvent> dummy;
    private Context sampleContext = new Context(new Address("bla", "bla"), new Address("bla", "bla"));

    @Before
    public void setUp() throws Exception {
        r = new Router(Pattern.quote("@Mars"));
        defaultTimeout = new Timeout(5, TimeUnit.SECONDS);
        dummy = Channels.newChannel(0);
    }

    @Test
    @Suspendable
    public void testOn() throws Exception {

        Channel<String> channel = Channels.newChannel(0);

        r.on("image\\s+me\\s+ninjas", (c, request, o) -> channel.send("testOn"));
        r.on("some other stuff", (con, request, c) -> fail("incorrect handler triggered"));

        r.triggerHandlersForMessageText(sampleContext, "@Mars image me ninjas", new Response(Jid.of("abc@abc.com"), dummy));

        String result;
        int recv = 0;

        while((result=channel.receive(defaultTimeout)) != null)
        {
            if (!result.equals("testOn")) {
                fail("incorrect message received from event");
            } else {
                recv++;
            }
        }

        assertTrue("correct message not received before timeout (or received more than once)", recv==1);

    }

    @Test
    @Suspendable
    public void testWhenContains() throws Exception {

        Channel<String> channel = Channels.newChannel(0);

        r.whenContains(".*turtles.*", (c, request, o) -> channel.send("done"));
        r.on("some other stuff", (con, request, c) -> fail("incorrect handler triggered"));

        r.triggerHandlersForMessageText(sampleContext, "the world loves some turtles now and again", new Response(Jid.of("abc@abc.com"), dummy));

        String result;
        int recv = 0;

        while((result=channel.receive(defaultTimeout)) != null)
        {
            if (!result.equals("done")) {
                fail("incorrect message received from event");
            } else {
                recv++;
            }
        }

        assertTrue("correct message not received before timeout (or received more than once)", recv==1);

    }

}