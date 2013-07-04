package org.jdownloader.api.myjdownloader;

import org.appwork.utils.net.httpserver.session.HttpSession;
import org.appwork.utils.net.httpserver.session.HttpSessionController;
import org.jdownloader.api.RemoteAPISession;

public class MyJDownloaderAPISession extends RemoteAPISession {

    private MyJDownloaderHttpConnection connection;

    public MyJDownloaderAPISession(HttpSessionController<? extends HttpSession> controller, MyJDownloaderHttpConnection connection) {
        super(controller);
        this.connection = connection;
    }

    @Override
    public String getSessionID() {
        return connection.getRequestConnectToken();
    }

    public MyJDownloaderHttpConnection getConnection() {
        return connection;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

}
