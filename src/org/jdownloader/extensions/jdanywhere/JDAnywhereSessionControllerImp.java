package org.jdownloader.extensions.jdanywhere;

import java.util.HashMap;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.utils.net.httpserver.session.HttpSessionController;

public class JDAnywhereSessionControllerImp extends HttpSessionController<JDAnywhereSession> {

    private final HashMap<String, JDAnywhereSession> sessions = new HashMap<String, JDAnywhereSession>();
    private String                                   user;
    private String                                   pass;

    @Override
    public JDAnywhereSession getSession(org.appwork.utils.net.httpserver.requests.HttpRequest request, final String id) {
        synchronized (this.sessions) {
            return this.sessions.get(id);
        }
    }

    public JDAnywhereSessionControllerImp(String user, String pass) {
        super();
        this.user = user;
        this.pass = pass;
    }

    @Override
    protected JDAnywhereSession newSession(RemoteAPIRequest request, final String username, final String password) {
        if (pass.equals(password) && user.equals(username)) {
            JDAnywhereSession session = new JDAnywhereSession(this);
            synchronized (this.sessions) {
                this.sessions.put(session.getSessionID(), session);
            }
            return session;
        } else {
            return null;
        }
    }

    @Override
    protected boolean removeSession(final JDAnywhereSession session) {
        if (session == null) { return false; }
        synchronized (this.sessions) {
            final JDAnywhereSession ret = this.sessions.remove(session.getSessionID());
            if (ret == null) { return false; }
            ret.setAlive(false);
            return true;
        }
    }

}
