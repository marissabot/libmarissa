package org.marissabot.libmarissa;

import org.marissabot.libmarissa.model.Address;
import org.marissabot.libmarissa.model.ChannelEvent;
import rocks.xmpp.core.stanza.MessageEvent;
import rocks.xmpp.core.stanza.model.Message;

public class XMPPChannelEventFactory {

    private XMPPChannelEventFactory() {}

    public static ChannelEvent<Message> makeChannelEvent(Message m) {
        return new ChannelEvent<>(ChannelEvent.EventType.XMPP, m);
    }



}
