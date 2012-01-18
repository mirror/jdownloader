package org.jdownloader.api.toolbar;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class JDownloaderToolBarAPIImpl implements JDownloaderToolBarAPI {

    private class ChunkedDom {
        protected String                  sessionID = null;
        protected HashMap<String, String> domChunks = new HashMap<String, String>();
        protected String                  URL       = null;
        protected boolean                 finished  = false;
    }

    private LinkedList<MinTimeWeakReference<ChunkedDom>> domSessions = new LinkedList<MinTimeWeakReference<ChunkedDom>>();

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
        ret.put("pause", DownloadWatchDog.getInstance().isPaused());
        ret.put("download_current", new Random().nextInt(100));
        ret.put("download_complete", 100);
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
            if (url == null) { throw new WTFException("No url?!"); }
            if (sessionID == null) { throw new WTFException("No sessionID?!"); }
            if (index == null) { throw new WTFException("No index?!"); }
            if (data == null) { throw new WTFException("No data?!"); }

            ChunkedDom chunkedDom = null;
            synchronized (domSessions) {
                for (int indexSessions = domSessions.size() - 1; indexSessions >= 0; indexSessions--) {
                    /*
                     * we use superget so we dont refresh the
                     * mintimeweakreference
                     */
                    chunkedDom = domSessions.get(indexSessions).superget();
                    if (chunkedDom == null || chunkedDom.finished) {
                        /* session expired, remove it */
                        domSessions.remove(indexSessions);
                        chunkedDom = null;
                    } else if (sessionID.equalsIgnoreCase(chunkedDom.sessionID)) {
                        /* session is correct */
                        /* now we refresh the mintimeweakreference */
                        chunkedDom = domSessions.get(indexSessions).get();
                        break;
                    } else {
                        /* nothing found */
                        chunkedDom = null;
                    }
                }

                if (chunkedDom == null) {
                    /* create new domSession */
                    chunkedDom = new ChunkedDom();
                    chunkedDom.sessionID = sessionID;
                    chunkedDom.URL = url;
                    domSessions.add(new MinTimeWeakReference<JDownloaderToolBarAPIImpl.ChunkedDom>(chunkedDom, 60 * 1000l, sessionID));
                }
                /* process existing domSession */
                if (chunkedDom.domChunks.put(index, data) != null) {
                    /* we tried to replace existing chunk! */
                    throw new WTFException("Replace existing Chunk?!");
                }
                if (lastChunk) {
                    /* this is last chunk, remove it from domSessions */
                    chunkedDom.finished = true;
                    StringBuilder sb = new StringBuilder();
                    for (int chunkIndex = 0; chunkIndex < chunkedDom.domChunks.size(); chunkIndex++) {
                        String chunk = chunkedDom.domChunks.get(chunkedDom + "");
                        if (chunk != null) {
                            sb.append(chunk);
                        } else {
                            throw new WTFException("Chunk " + chunkIndex + " missing!");
                        }
                    }
                    chunkedDom.domChunks.clear();
                    LinkCollectingJob job = new LinkCollectingJob(sb.toString());
                    sb = null;
                    job.setCustomSourceUrl(chunkedDom.URL);
                    LinkCollector.getInstance().addCrawlerJob(job);
                }
            }
            ret.put("status", true);
            ret.put("msg", (Object) null);
        } catch (final Throwable e) {
            e.printStackTrace();
            ret.put("status", false);
            ret.put("msg", e.getMessage());
        }
        return ret;
    }

    public boolean togglePauseDownloads() {
        boolean b = DownloadWatchDog.getInstance().isPaused();
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(!b);
        return !b;
    }
}
