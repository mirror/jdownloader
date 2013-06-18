package org.jdownloader.api.notification;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.jdownloader.extensions.myjdownloader.MyJDownloaderHttpConnection;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage.REQUESTTYPE;

public class NotificationAPIImpl implements NotificationAPI {

    @Override
    public boolean enablenotification(RemoteAPIRequest request, REQUESTTYPE requesttype) {
        MyJDownloaderHttpConnection connection = MyJDownloaderHttpConnection.getMyJDownloaderHttpConnection(request);
        if (connection != null) {

        }
        return false;
    }

    @Override
    public boolean disablenotification(RemoteAPIRequest request, REQUESTTYPE requesttype) {
        return false;
    }

}
