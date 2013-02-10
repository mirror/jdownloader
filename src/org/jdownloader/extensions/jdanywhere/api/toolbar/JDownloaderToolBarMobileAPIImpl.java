package org.jdownloader.extensions.jdanywhere.api.toolbar;

import java.util.HashMap;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.remoteapi.EventsAPIEvent;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;
import org.jdownloader.extensions.jdanywhere.CheckUser;
import org.jdownloader.extensions.jdanywhere.JDAnywhereController;

public class JDownloaderToolBarMobileAPIImpl implements JDownloaderToolBarMobileAPI, StateEventListener {

    JDownloaderToolBarAPIImpl tbAPI = new JDownloaderToolBarAPIImpl();
    private String            user;
    private String            pass;
    private CheckUser         checkUser;

    public JDownloaderToolBarMobileAPIImpl(String user, String pass) {
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        this.user = user;
        this.pass = pass;
        checkUser = new CheckUser(user, pass);
    }

    public synchronized Object getStatus(final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        return tbAPI.getStatus();
    }

    public boolean startDownloads(final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        return tbAPI.startDownloads();
    }

    public boolean stopDownloads(final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        return tbAPI.stopDownloads();
    }

    // pauses a download
    public boolean pauseDownloads(final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(!DownloadWatchDog.getInstance().isPaused());
        return true;
    }

    public void onStateChange(StateEvent event) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("message", "Running State Changed");
        data.put("data", event.getNewState().getLabel());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("runningstate", data), null);
    }

    public void onStateUpdate(StateEvent event) {
    }

    public String getUsername() {
        return user;
    }

    public String getPassword() {
        return pass;
    }

}
