package org.jdownloader.api.toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.UnknownCrawledLinkHandler;
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
        protected HashMap<Integer, String> domChunks = new HashMap<Integer, String>();
        protected String                   URL       = null;
    }

    private HashMap<String, MinTimeWeakReference<ChunkedDom>> domSessions = new HashMap<String, MinTimeWeakReference<ChunkedDom>>();

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
            if (true) {
                System.out.println("Session: " + sessionID + "|Chunk: " + index + "|URL: " + url + "|Data: " + data.length() + "|LastChunk: " + lastChunk);
            }
            ChunkedDom chunkedDom = null;
            synchronized (domSessions) {
                /* cleanup Sessions */
                Iterator<String> it = domSessions.keySet().iterator();
                while (it.hasNext()) {
                    String sID = it.next();
                    MinTimeWeakReference<ChunkedDom> tmp = domSessions.get(sID);
                    if (tmp != null && tmp.superget() == null) it.remove();
                }
                MinTimeWeakReference<ChunkedDom> tmp = domSessions.get(sessionID);
                if (tmp != null) chunkedDom = tmp.get();
                if (chunkedDom == null) {
                    /* create new domSession */
                    chunkedDom = new ChunkedDom();
                    chunkedDom.URL = url;
                    domSessions.put(sessionID, new MinTimeWeakReference<JDownloaderToolBarAPIImpl.ChunkedDom>(chunkedDom, 60 * 1000l, sessionID));
                }
                /* process existing domSession */
                if (chunkedDom.domChunks.put(Integer.parseInt(index), data) != null) {
                    /* we tried to replace existing chunk! */
                    throw new WTFException("Replace existing Chunk?!");
                }
                if (lastChunk) {
                    /* this is last chunk, remove it from domSessions */
                    domSessions.remove(sessionID);
                    StringBuilder sb = new StringBuilder();
                    for (int chunkIndex = 0; chunkIndex < chunkedDom.domChunks.size(); chunkIndex++) {
                        String chunk = chunkedDom.domChunks.get(chunkIndex);
                        if (chunk != null) {
                            sb.append(chunk);
                        } else {
                            throw new WTFException("Chunk " + chunkIndex + " missing!");
                        }
                    }
                    final String dom = sb.toString();
                    sb = null;
                    /*
                     * we first check if the url itself can be handled by a
                     * plugin
                     */
                    CrawledLink link = new CrawledLink(chunkedDom.URL);
                    link.setUnknownHandler(new UnknownCrawledLinkHandler() {

                        public void unhandledCrawledLink(CrawledLink link) {
                            /*
                             * if the url cannot be handled by a plugin, we
                             * check the dom
                             */
                            LinkCollectingJob job = new LinkCollectingJob(dom);
                            job.setCustomSourceUrl(link.getURL());
                            LinkCollector.getInstance().addCrawlerJob(job);
                        }
                    });
                    ArrayList<CrawledLink> links = new ArrayList<CrawledLink>();
                    links.add(link);
                    LinkCollector.getInstance().addCrawlerJob(links);
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
