package org.marissabot.libmarissa.model;

public class Context {

    private Address room;
    private Address user;

    public Context(Address room, Address user) {
        this.room = room;
        this.user = user;
    }

    public Address getRoom() {
        return room;
    }

    public Address getUser() {
        return user;
    }

}
