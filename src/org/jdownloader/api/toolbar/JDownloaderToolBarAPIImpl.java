package org.jdownloader.api.toolbar;

import java.util.HashMap;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;

import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class JDownloaderToolBarAPIImpl implements JDownloaderToolBarAPI {

    public Object getStatus() {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put("running", DownloadWatchDog.getInstance().getActiveDownloads() > 0);
        ret.put("limit", GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled());
        ret.put("reconnect", GeneralSettings.AUTO_RECONNECT_ENABLED.isEnabled());
        ret.put("clipboard", GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.isEnabled());
        ret.put("stopafter", DownloadWatchDog.getInstance().isStopMarkSet());
        ret.put("premium", GeneralSettings.USE_AVAILABLE_ACCOUNTS.isEnabled());
        return ret;
    }

    public boolean isAvailable() {
        return true;
    }

    public boolean startDownloads() {
        DownloadWatchDog.getInstance().startDownloads();
        return true;
    }

    public boolean stopDownloads() {
        DownloadWatchDog.getInstance().stopDownloads();
        return true;
    }

    public boolean toggleDownloadSpeedLimit() {
        GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.toggle();
        return GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled();
    }

    public boolean toggleClipboardMonitoring() {
        boolean b = GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.isEnabled();
        GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.setValue(!b);
        return !b;
    }

    public boolean toggleAutomaticReconnect() {
        GeneralSettings.AUTO_RECONNECT_ENABLED.toggle();
        return GeneralSettings.AUTO_RECONNECT_ENABLED.isEnabled();
    }

    public boolean toggleStopAfterCurrentDownload() {
        final ToolBarAction stopMark = ActionController.getToolBarAction("toolbar.control.stopmark");
        if (stopMark != null) {
            stopMark.actionPerformed(null);
        }
        return DownloadWatchDog.getInstance().isStopMarkSet();
    }

    public synchronized long[] getSpeedMeter() {
        if (GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled()) {
            return new long[] { GeneralSettings.DOWNLOAD_SPEED_LIMIT.getValue(), DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() };
        } else {
            return new long[] { 0, DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() };
        }
    }

    public boolean togglePremium() {
        GeneralSettings.USE_AVAILABLE_ACCOUNTS.toggle();
        return GeneralSettings.USE_AVAILABLE_ACCOUNTS.isEnabled();
    }

}
