package org.marissabot.libmarissa;

import org.junit.Test;
import org.marissabot.libmarissa.model.ChannelEvent;
import rocks.xmpp.core.stanza.model.Message;


import static org.junit.Assert.*;

public class XMPPChannelEventFactoryTest {

    @Test
    public void testMake() throws Exception {
        Message msg = new Message();
        ChannelEvent<Message> result = XMPPChannelEventFactory.makeChannelEvent(msg);
        assertEquals(ChannelEvent.EventType.XMPP, result.getEventType());
        assertSame(msg, result.getPayload());

        result = XMPPChannelEventFactory.makeChannelEvent(null);
        assertNotNull(result.getEventType());
        assertNull(result.getPayload());
    }
}