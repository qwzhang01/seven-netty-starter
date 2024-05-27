package com.iwj.netty.pojo;

import java.util.concurrent.ConcurrentHashMap;

public class Online {
    private ConcurrentHashMap<String, Session> account;
    private ConcurrentHashMap<Session, String> session;

    private ConcurrentHashMap<String, Session> getAccount() {
        if (account != null) {
            return account;
        }
        synchronized (this) {
            if (account != null) {
                return account;
            }
            account = new ConcurrentHashMap<>();
            return account;
        }
    }

    private ConcurrentHashMap<Session, String> getSession() {
        if (session != null) {
            return session;
        }
        synchronized (this) {
            if (session != null) {
                return session;
            }
            session = new ConcurrentHashMap<>();
            return session;
        }
    }

    public void add(String account, Session session) {
        getAccount().put(account, session);
        getSession().put(session, account);
    }

    public Session getSession(String account) {
        if (account == null) {
            return null;
        }
        return getAccount().get(account);
    }

    public String getAccount(Session session) {
        if (session == null) {
            return null;
        }
        return getSession().get(session);
    }

    public void remove(String account) {
        Session sessions = getAccount().get(account);
        if (sessions != null) {
            getSession().remove(sessions);
        }
        getAccount().remove(account);
    }

    public void remove(Session sessions) {
        String accounts = getSession().get(sessions);
        if (accounts != null && !"".equals(accounts)) {
            getAccount().remove(accounts);
        }
        getSession().remove(sessions);
    }

    public int count() {
        if (account == null) {
            return 0;
        }
        return account.size();
    }
}