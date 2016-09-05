package org.marissabot.libmarissa.model;


public class User {

    private String jid;
    private String nick;

    public User(String jid, String nick) {
        setJid(jid);
        setNick(nick);
    }

    public String getJid() {
        return jid;
    }

    public final void setJid(String jid) {
        if (jid==null||jid.trim().isEmpty())
            throw new IllegalArgumentException("A user cannot have a blank jid");

        this.jid = jid;
    }

    public String getNick() {
        return nick;
    }

    public final void setNick(String nick) {
        if (nick==null||nick.trim().isEmpty())
            throw new IllegalArgumentException("A nickname cannot be blank or empty");

        this.nick = nick;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!jid.equals(user.jid)) return false;
        return nick.equals(user.nick);

    }

    @Override
    public int hashCode() {
        int result = jid.hashCode();
        result = 31 * result + nick.hashCode();
        return result;
    }
}
