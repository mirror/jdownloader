package org.jdownloader.api.downloads;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.storage.Storable;
import org.jdownloader.settings.GeneralSettings;

public class DownloadStatusAPIStorable implements Storable {

    public final long    speed;
    public final int     connections;

    // Prototype. Will be moved to /settings/get.
    @Deprecated
    public final boolean reconnectEnabled;
    @Deprecated
    public final boolean speedlimitEnabled;
    @Deprecated
    public final int     speedlimit;

    public DownloadStatusAPIStorable() {
        speed = DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
        connections = DownloadWatchDog.getInstance().getConnectionManager().getIncommingConnections();
        // TODO: remaining and downloaded file size + ETA.

        reconnectEnabled = GeneralSettings.AUTO_RECONNECT_ENABLED.getValue();
        speedlimitEnabled = GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.getValue();
        speedlimit = GeneralSettings.DOWNLOAD_SPEED_LIMIT.getValue();

    }
}
