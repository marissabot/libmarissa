package org.marissabot.libmarissa;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.SelectAction;
import org.marissabot.libmarissa.model.Address;
import org.marissabot.libmarissa.model.ChannelEvent;
import org.marissabot.libmarissa.model.Context;
import org.marissabot.libmarissa.model.ControlEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.*;
import rocks.xmpp.core.stanza.MessageEvent;
import rocks.xmpp.core.stanza.model.Message;
import rocks.xmpp.core.stanza.model.Presence;
import rocks.xmpp.extensions.muc.ChatRoom;
import rocks.xmpp.extensions.muc.ChatService;
import rocks.xmpp.extensions.muc.MultiUserChatManager;
import rocks.xmpp.extensions.muc.Occupant;
import rocks.xmpp.extensions.muc.model.DiscussionHistory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static co.paralleluniverse.strands.channels.Selector.receive;
import static co.paralleluniverse.strands.channels.Selector.select;

public class Marissa {

    private final String username;
    private final String password;
    private final String nickname;

    private final List<String> rooms;
    private final Map<String, ChatRoom> joinedRooms;

    private XmppClient xmppClient;
    private Channel<ChannelEvent> rxChannel  = Channels.newChannel(0);
    private Channel<ChannelEvent> txChannel  = Channels.newChannel(0);
    private Channel<ChannelEvent> ctlChannel = Channels.newChannel(0);

    private final Logger log = LoggerFactory.getLogger(Marissa.class);
    private final Consumer<MessageEvent> listener;

    public Marissa(String username, String password, String nickname, final List<String> joinRooms) {

        this.username = username;
        this.password = password;
        this.joinedRooms = new HashMap<>();
        this.nickname = nickname;
        this.rooms = joinRooms;

        this.listener = mi -> {
            try {

                if(mi == null) {
                    return;
                } else if (mi.getMessage().getType() == Message.Type.GROUPCHAT) {
                    String userInChannel = mi.getMessage().getFrom().getResource();

                    if (!userInChannel.equals(nickname)) {
                        rxChannel.send(XMPPChannelEventFactory.makeChannelEvent(mi.getMessage()));
                    }
                }

            } catch (SuspendExecution | InterruptedException x) {
                die("error - suspended or interrupted");
                log.error("died because of interruption", x);
                throw new IllegalStateException("can't suspend or be interrupted here", x);
            } catch(Throwable t) {
                die("error - unexpected item in the bagging area");
                log.error("Time, to die.", t);
                throw new IllegalStateException("can't suspend or be interrupted here", t);
            }
        };

    }

    public void disconnect() {
        try {
            ctlChannel.send(
                    new ChannelEvent(ChannelEvent.EventType.CONTROL,
                            new ControlEvent(ControlEvent.Type.QUIT, "Program aborted"))
            );
        } catch (SuspendExecution | InterruptedException x) {
            // I think according to the docs this is just a marker and cannot happen but.. maybe
            log.error("failed to pipe quit message", x);
        }
    }

    private void die(String reason) {

        log.info("died:" + reason == null ? "" : reason);

        try {

            if (xmppClient.isConnected()) {
                xmppClient.send(new Presence(Presence.Type.UNAVAILABLE));
            }

            xmppClient.close();

        } catch (XmppException e) {
            log.error("failed to die cleanly", e);
            throw new IllegalStateException("failed to die cleanly (with presence)");
        }
    }

    private void joinRooms(final List<String> joinRooms) throws XmppException {

        MultiUserChatManager m = xmppClient.getManager(MultiUserChatManager.class);
        ChatService chatService = m.createChatService(Jid.of("conf.hipchat.com"));

        // leave any rooms we're already in

        this.joinedRooms.values().stream().forEach(room -> {
            try {
                room.removeInboundMessageListener(this.listener);
                room.exit();
            } catch(Exception e) {
                log.warn("Failed to leave room");
            }
        });

        // ok now join the other rooms

        for(String room : joinRooms) {

            ChatRoom cr = chatService.createRoom(room);

            cr.addInboundMessageListener(listener);

            try {
                Future<Presence> res = cr.enter(nickname, DiscussionHistory.forMaxMessages(0));
                Presence p = res.get();
            } catch (Exception e) {
                log.error("couldn't connect to room '" + room + "'", e);
                throw new IllegalArgumentException("couldn't join room '"+room+"'", e);
            }

            joinedRooms.put(room, cr);

        }
    }

    public void activate(final Router router) throws XmppException, InterruptedException, SuspendExecution {

        // connect to xmpp
        TcpConnectionConfiguration tcpConfiguration = TcpConnectionConfiguration.builder()
                .hostname("chat.hipchat.com")
                .port(5222)
                .secure(true)
                .build();
        xmppClient = XmppClient.create("chat.hipchat.com", tcpConfiguration);

        xmppClient.connect();
        xmppClient.login(username, password);


        xmppClient.send(new Presence(Presence.Show.CHAT));

        // join the rooms

        joinRooms(this.rooms);

        // send a welcome message

        joinedRooms.values().stream()
            .forEach(cr -> {
                String peeps = String.join(", ", cr.getOccupants().stream()
                    .filter(x -> !x.isSelf())
                    .map(Occupant::getNick)
                    .map(nick -> {
                        String[] nom = nick.trim().split("\\s+");
                        if (nom.length==2&&!nom[0].trim().isEmpty())
                        {
                            return nom[0];
                        } else {
                            return nick;
                        }
                    })
                    .collect(Collectors.toList()));

                String[] greetings = { "Hello", "Hey", "Hi" };

                cr.sendMessage(greetings[new Random(System.currentTimeMillis()).nextInt(greetings.length)] + " " + peeps);
            });

        log.info("Joined room(s) " + String.join(", ", joinedRooms.keySet()));

        // reconnect listener

        xmppClient.addSessionStatusListener(e -> {

            if (e.getStatus() == XmppSession.Status.AUTHENTICATED) {

                try {
                    joinRooms(this.rooms);
                } catch (XmppException e1) {
                    e1.printStackTrace();
                }

            } else {

                log.info("Received unhandled session status: " + e.getStatus());

            }

        });

        selectMessageLoop(router);

    }

    private void selectMessageLoop(final Router router) throws SuspendExecution, InterruptedException {

        // message listener

        boolean isLive = true;

        while (isLive) {

            // TODO can probably flick this to a single event stream now rather than multiple channels
            // TODO can we do all this just with the ChatRoom add inbound message listener method?

            SelectAction<ChannelEvent> sa = select(
                 receive(rxChannel),
                 receive(txChannel),
                 receive(ctlChannel)
            );

            ChannelEvent evt = sa.message();

            if (ChannelEvent.EventType.CONTROL.equals(evt.getEventType())) {

                ControlEvent ctlEvt = (ControlEvent)evt.getPayload();

                if (ControlEvent.Type.QUIT.equals(ctlEvt.getType())) {
                    die(ctlEvt.getAdditionalInfo());
                    isLive = false;
                }

            } else if (ChannelEvent.EventType.XMPP.equals(evt.getEventType())) {

                Message message = (Message)evt.getPayload();

                switch (sa.index()) {
                    case 0:
                        ChatRoom room = joinedRooms.get(message.getFrom().getLocal());
                        Occupant userJid = room.getOccupant(message.getFrom().getResource());
                        Address user = new Address(userJid.getJid().toString(), userJid.getNick());
                        Address roomAddress = new Address(message.getFrom().asBareJid().toString(), room.getNick());
                        Context c = new Context(roomAddress, user);
                        router.triggerHandlersForMessageText(c, message.getBody(), new Response(message.getFrom(), txChannel));
                        break;
                    case 1:
                        ChatRoom cr = joinedRooms.get(message.getTo().getLocal());
                        if (cr != null) {
                            cr.sendMessage(message.getBody());
                        } else {
                            log.error("chatroom isn't joined; " + message.getTo().getLocal());
                        }
                        break;
                }
            }
        }
    }

}
