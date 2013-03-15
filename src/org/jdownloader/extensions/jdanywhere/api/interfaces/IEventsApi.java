package org.jdownloader.extensions.jdanywhere.api.interfaces;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("jdanywhere/events")
public interface IEventsApi extends RemoteAPIInterface {

    public boolean RegisterCaptchaPush(String host, String path, String query);
}
