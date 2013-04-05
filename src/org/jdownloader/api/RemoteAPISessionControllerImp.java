package org.jdownloader.api;

import java.util.HashMap;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.utils.net.httpserver.session.HttpSessionController;
import org.jdownloader.extensions.myjdownloader.MyJDownloaderHttpConnection;

public class RemoteAPISessionControllerImp extends HttpSessionController<RemoteAPISession> {

    private final HashMap<String, RemoteAPISession> sessions = new HashMap<String, RemoteAPISession>();

    @Override
    public RemoteAPISession getSession(final org.appwork.utils.net.httpserver.requests.HttpRequest request, final String id) {
        if (request.getConnection() instanceof MyJDownloaderHttpConnection) { return new RemoteAPISession(this) {

            @Override
            public String getSessionID() {
                return ((MyJDownloaderHttpConnection) (request.getConnection())).getRequestConnectToken();
            }

            @Override
            public boolean isAlive() {
                return true;
            }
        }; }
        synchronized (this.sessions) {
            return this.sessions.get(id);
        }
    }

    @Override
    protected RemoteAPISession newSession(RemoteAPIRequest request, final String username, final String password) {
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
