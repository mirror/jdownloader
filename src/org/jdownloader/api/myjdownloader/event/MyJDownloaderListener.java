package org.jdownloader.api.myjdownloader.event;

import java.util.EventListener;

public interface MyJDownloaderListener extends EventListener {

    void onMyJDownloaderConnectionStatusChanged(boolean connected);

}