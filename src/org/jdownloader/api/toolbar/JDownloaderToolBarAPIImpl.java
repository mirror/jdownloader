package org.jdownloader.api.toolbar;

import java.util.HashMap;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class JDownloaderToolBarAPIImpl implements JDownloaderToolBarAPI {

    public Object getStatus() {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put("running", DownloadWatchDog.getInstance().getActiveDownloads() > 0);
        ret.put("limit", GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled());
        if (GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled()) {
            ret.put("limitspeed", GeneralSettings.DOWNLOAD_SPEED_LIMIT.getValue());
        } else {
            ret.put("limitspeed", 0);
        }
        ret.put("reconnect", GeneralSettings.AUTO_RECONNECT_ENABLED.isEnabled());
        ret.put("clipboard", GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.isEnabled());
        ret.put("stopafter", DownloadWatchDog.getInstance().isStopMarkSet());
        ret.put("premium", GeneralSettings.USE_AVAILABLE_ACCOUNTS.isEnabled());
        ret.put("speed", DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage());
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

    public boolean togglePremium() {
        GeneralSettings.USE_AVAILABLE_ACCOUNTS.toggle();
        return GeneralSettings.USE_AVAILABLE_ACCOUNTS.isEnabled();
    }

    public Object addLinksFromDOM(RemoteAPIRequest request) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        try {
            String index = HttpRequest.getParameterbyKey(request, "index");
            String data = HttpRequest.getParameterbyKey(request, "data");
            String sessionID = HttpRequest.getParameterbyKey(request, "sessionid");
            String url = HttpRequest.getParameterbyKey(request, "url");
            boolean lastChunk = "true".equalsIgnoreCase(HttpRequest.getParameterbyKey(request, "lastchunk"));
            StringBuilder sb = new StringBuilder();
            if (url != null) {
                sb.append("URL:" + url + "|");
            } else {
                sb.append("URL:unknown|");
            }
            if (sessionID != null) {
                sb.append("SESSION:" + sessionID + "|");
            } else {
                sb.append("SESSION:unknown|");
            }
            if (index != null) {
                sb.append("CHUNK:" + index + "|");
            } else {
                sb.append("CHUNK:unknown|");
            }
            if (data != null) {
                sb.append("DATA:" + data.length() + "|");
            } else {
                sb.append("DATA:none|");
            }
            if (lastChunk) {
                sb.append("LASTCHUNK");
            }
            System.out.println(sb.toString());
            ret.put("status", true);
            ret.put("msg", (Object) null);
        } catch (final Throwable e) {
            ret.put("status", false);
            ret.put("msg", e.getMessage());
        }
        return ret;
    }
}
