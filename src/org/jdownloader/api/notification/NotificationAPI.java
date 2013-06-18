package org.jdownloader.api.notification;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage;

@ApiNamespace("notification")
public interface NotificationAPI extends RemoteAPIInterface {

    public boolean enablenotification(RemoteAPIRequest request, NotificationRequestMessage.REQUESTTYPE requesttype);

    public boolean disablenotification(RemoteAPIRequest request, NotificationRequestMessage.REQUESTTYPE requesttype);
}
