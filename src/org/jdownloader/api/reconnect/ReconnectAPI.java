package org.jdownloader.api.reconnect;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.ReconnectInterface.NAMESPACE)
public interface ReconnectAPI extends RemoteAPIInterface {

    public void doReconnect();
}
