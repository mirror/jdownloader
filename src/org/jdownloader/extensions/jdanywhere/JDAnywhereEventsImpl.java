package org.jdownloader.extensions.jdanywhere;

import org.appwork.remoteapi.EventsAPI;
import org.appwork.utils.net.httpserver.session.HttpSession;

public class JDAnywhereEventsImpl extends EventsAPI {

    @Override
    public boolean isSessionAllowed(HttpSession session) {
        if (session == null || !session.isAlive()) return false;
        return true;
    }

}
