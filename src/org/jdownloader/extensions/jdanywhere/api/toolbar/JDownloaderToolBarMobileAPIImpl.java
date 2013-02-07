package org.jdownloader.extensions.jdanywhere.api.toolbar;

import java.util.HashMap;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.remoteapi.EventsAPIEvent;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;

public class JDownloaderToolBarMobileAPIImpl implements JDownloaderToolBarMobileAPI, StateEventListener {

    JDownloaderToolBarAPIImpl tbAPI = new JDownloaderToolBarAPIImpl();

    public JDownloaderToolBarMobileAPIImpl() {
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
    }

    public synchronized Object getStatus() {
        return tbAPI.getStatus();
    }

    public boolean startDownloads() {
        return tbAPI.startDownloads();
    }

    public boolean stopDownloads() {
        return tbAPI.stopDownloads();
    }

    // pauses a download
    public boolean pauseDownloads() {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(!DownloadWatchDog.getInstance().isPaused());
        return true;
    }

    public void onStateChange(StateEvent event) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("message", "Running State Changed");
        data.put("data", event.getNewState().getLabel());
        RemoteAPIController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("runningstate", data), null);
    }

    public void onStateUpdate(StateEvent event) {
    }
}
