package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.extensions.jdanywhere.api.storable.RunningObjectStorable;

@ApiNamespace("jdanywhere/dashboard")
@ApiSessionRequired
public interface IDashboardApi extends RemoteAPIInterface {

    public abstract boolean start();

    public abstract boolean stop();

    public abstract boolean pause(Boolean value);

    // paused = 1
    // stopped = 2
    // running = 0
    public abstract String getState();

    // returns the current downloadspeed
    // used in iPhone-App
    public abstract int speed();

    // returns the current downloadlimit
    // used in iPhone-App
    public abstract int limit();

    // returns the current traffic
    // used in iPhone-App
    public abstract long traffic();

    public abstract boolean setLimitspeed(int speed);

    public abstract boolean activateLimitspeed(boolean activate);

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed Text as an PNG
    // used in iPhone-App
    public abstract void speedMeter(RemoteAPIRequest request, RemoteAPIResponse response);

    public abstract List<RunningObjectStorable> runningLinks();

    @AllowStorage(value = { Object.class })
    public Object getCompleteState();

}