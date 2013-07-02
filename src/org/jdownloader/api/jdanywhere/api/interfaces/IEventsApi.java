package org.jdownloader.api.jdanywhere.api.interfaces;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("jdanywhere/events")
public interface IEventsApi extends RemoteAPIInterface {

    public boolean RegisterCaptchaPush(String host, String path, String query);
}
