package org.jdownloader.api.myjdownloader.event;

import java.util.EventListener;

import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;

public interface MyJDownloaderListener extends EventListener {

    void onMyJDownloaderConnectionStatusChanged(MyJDownloaderConnectionStatus status, int connections);

}