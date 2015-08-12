package org.jdownloader.api.useragent;

import java.util.EventListener;

public interface UserAgentListener extends EventListener {

    void onNewAPIUserAgent(ConnectedDevice ua);

    void onRemovedAPIUserAgent(ConnectedDevice fua);

    void onAPIUserAgentUpdate(ConnectedDevice fua);

}