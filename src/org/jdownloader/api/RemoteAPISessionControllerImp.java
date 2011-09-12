package org.jdownloader.api;

import java.util.HashMap;

import org.appwork.utils.net.httpserver.session.HttpSessionController;

public class RemoteAPISessionControllerImp extends HttpSessionController<RemoteAPISession> {

    private final HashMap<String, RemoteAPISession> sessions = new HashMap<String, RemoteAPISession>();

    @Override
    public RemoteAPISession getSession(final String id) {
        synchronized (this.sessions) {
            return this.sessions.get(id);
        }
    }

    @Override
    protected RemoteAPISession newSession(final String username, final String password) {
        if (!"wron".equals(password)) {
            RemoteAPISession session = new RemoteAPISession(this);
            synchronized (this.sessions) {
                this.sessions.put(session.getSessionID(), session);
            }
            return session;
        } else {
            return null;
        }
    }

    @Override
    protected boolean removeSession(final RemoteAPISession session) {
        if (session == null) { return false; }
        synchronized (this.sessions) {
            final RemoteAPISession ret = this.sessions.remove(session.getSessionID());
            if (ret == null) { return false; }
            ret.setAlive(false);
            return true;
        }
    }

}
