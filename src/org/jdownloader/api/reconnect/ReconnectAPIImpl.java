package org.jdownloader.api.reconnect;

import jd.controlling.downloadcontroller.DownloadWatchDog;

public class ReconnectAPIImpl implements ReconnectAPI {

    @Override
    public void doReconnect() {
        try {
            DownloadWatchDog.getInstance().requestReconnect(false);
        } catch (InterruptedException e) {
        }
    }

}
