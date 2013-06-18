package org.jdownloader.api.notification;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.jdownloader.extensions.myjdownloader.MyJDownloaderHttpConnection;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage.TYPE;

public class NotificationAPIImpl implements NotificationAPI {

    @Override
    public boolean enable(RemoteAPIRequest request, TYPE type) {
        MyJDownloaderHttpConnection connection = MyJDownloaderHttpConnection.getMyJDownloaderHttpConnection(request);
        if (connection != null) {

        }
        return false;
    }

    @Override
    public boolean disable(RemoteAPIRequest request, TYPE type) {
        return false;
    }

}
