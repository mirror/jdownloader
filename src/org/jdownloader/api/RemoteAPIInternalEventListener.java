package org.jdownloader.api;

import java.util.EventListener;

import org.appwork.remoteapi.events.EventObject;

public interface RemoteAPIInternalEventListener extends EventListener {

    void onRemoteAPIEvent(EventObject event);

}