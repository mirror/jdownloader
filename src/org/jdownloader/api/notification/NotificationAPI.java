package org.jdownloader.api.notification;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage;

@ApiNamespace("notify")
public interface NotificationAPI extends RemoteAPIInterface {

    public boolean enable(RemoteAPIRequest request, NotificationRequestMessage.TYPE type);

    public boolean disable(RemoteAPIRequest request, NotificationRequestMessage.TYPE type);
}
