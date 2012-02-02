package org.jdownloader.api.toolbar;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.BrokenCrawlerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.UnknownCrawledLinkHandler;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.gui.UIConstants;
import jd.gui.UserIF;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksProgress;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class JDownloaderToolBarAPIImpl implements JDownloaderToolBarAPI, StateEventListener {

    private class ChunkedDom {
        protected HashMap<Integer, String> domChunks   = new HashMap<Integer, String>();
        protected String                   URL         = null;
        protected String                   completeDOM = null;
    }

    private class CheckedDom extends ChunkedDom {

        protected CheckedDom(ChunkedDom dom) {
            this.completeDOM = dom.completeDOM;
            this.URL = dom.URL;
        }

        protected int status = 0;
    }

    private HashMap<String, MinTimeWeakReference<ChunkedDom>> domSessions   = new HashMap<String, MinTimeWeakReference<ChunkedDom>>();
    private HashMap<String, CheckedDom>                       checkSessions = new HashMap<String, CheckedDom>();

    public JDownloaderToolBarAPIImpl() {
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
    }

    public synchronized Object getStatus() {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        int running = DownloadWatchDog.getInstance().getActiveDownloads();
        ret.put("running", running > 0);
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
        if (running == 0) {
            ret.put("speed", 0);
        } else {
            ret.put("speed", DownloadWatchDog.getInstance().getConnectionManager().getIncommingSpeedMeter().getSpeedMeter());
        }
        ret.put("pause", DownloadWatchDog.getInstance().isPaused());

        List<DownloadLink> calc_progress = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            public int returnMaxResults() {
                return 0;
            }

            public boolean isChildrenNodeFiltered(DownloadLink node) {
                if (!node.isEnabled()) return false;
                if (node.getLinkStatus().isFailed()) return false;
                if (AvailableStatus.FALSE == node.getAvailableStatusInfo()) return false;
                return true;
            }
        });

        long todo = 0;
        long done = 0;
        for (DownloadLink link : calc_progress) {
            done += Math.max(0, link.getDownloadCurrent());
            todo += Math.max(0, link.getDownloadSize());
        }
        ret.put("download_current", done);
        ret.put("download_complete", todo);
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
            ChunkedDom chunkedDom = getCompleteDOM(request);
            if (chunkedDom != null) {
                final String url = chunkedDom.URL;
                final String dom = chunkedDom.completeDOM;
                /*
                 * we first check if the url itself can be handled by a plugin
                 */
                CrawledLink link = new CrawledLink(chunkedDom.URL);
                link.setUnknownHandler(new UnknownCrawledLinkHandler() {

                    public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc) {
                        /*
                         * if the url cannot be handled by a plugin, we check
                         * the dom
                         */
                        addCompleteDom(url, dom, link);
                    }

                });

                link.setBrokenCrawlerHandler(new BrokenCrawlerHandler() {

                    public void brokenCrawler(CrawledLink link, LinkCrawler lc) {
                        /*
                         * if the url cannot be handled because a plugin is
                         * broken, we check the dom
                         */
                        addCompleteDom(url, dom, link);

                    }
                });
                ArrayList<CrawledLink> links = new ArrayList<CrawledLink>();
                links.add(link);
                LinkCollector.getInstance().addCrawlerJob(links);
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        try {
                            JDGui.getInstance().requestPanel(UserIF.Panels.LINKGRABBER, null);
                            if (JDGui.getInstance().getMainFrame().getState() != JFrame.ICONIFIED && JDGui.getInstance().getMainFrame().isVisible()) {
                                JDGui.getInstance().setFrameStatus(UIConstants.WINDOW_STATUS_FOREGROUND);
                            }
                        } catch (Throwable e) {

                        }
                    }
                };
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

    private ChunkedDom getCompleteDOM(RemoteAPIRequest request) throws IOException {
        String index = HttpRequest.getParameterbyKey(request, "index");
        String data = HttpRequest.getParameterbyKey(request, "data");
        String sessionID = HttpRequest.getParameterbyKey(request, "sessionid");
        final String url = HttpRequest.getParameterbyKey(request, "url");
        boolean lastChunk = "true".equalsIgnoreCase(HttpRequest.getParameterbyKey(request, "lastchunk"));
        boolean debug = "true".equalsIgnoreCase(HttpRequest.getParameterbyKey(request, "debug"));
        if (url == null) { throw new WTFException("No url?!"); }
        if (sessionID == null) { throw new WTFException("No sessionID?!"); }
        if (index == null) { throw new WTFException("No index?!"); }
        if (data == null) { throw new WTFException("No data?!"); }
        if (debug) {
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
                chunkedDom.domChunks.clear();
                chunkedDom.completeDOM = sb.toString();
                return chunkedDom;
            }
        }
        return null;
    }

    public void addCompleteDom(final String url, final String dom, CrawledLink link) {

        final LinkCollectingJob job = new LinkCollectingJob(dom);
        job.setCustomSourceUrl(url);
        AddLinksProgress d = new AddLinksProgress(job) {
            protected String getSearchInText() {

                return url;
            }

            protected Point getForcedLocation() {
                Point loc = MouseInfo.getPointerInfo().getLocation();
                loc.x -= getPreferredSize().width / 2;
                loc.y += 30;
                return loc;

            }

        };

        if (d.isHiddenByDontShowAgain()) {
            Thread thread = new Thread("AddLinksDialog") {
                public void run() {
                    LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(job);

                    lc.waitForCrawling();
                    System.out.println("JOB DONE: " + lc.crawledLinksFound());

                }
            };

            thread.start();
        } else {
            try {
                Dialog.getInstance().showDialog(d);
            } catch (DialogClosedException e) {
                e.printStackTrace();
            } catch (DialogCanceledException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean togglePauseDownloads() {
        boolean b = DownloadWatchDog.getInstance().isPaused();
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(!b);
        return !b;
    }

    public Object checkLinksFromDOM(RemoteAPIRequest request) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put("checkid", null);
        try {
            ChunkedDom chunkedDom = getCompleteDOM(request);
            if (chunkedDom != null) {
                CheckedDom checkSession = new CheckedDom(chunkedDom);
                String checkID = "check" + System.nanoTime();
                synchronized (checkSessions) {
                    checkSessions.put(checkID, checkSession);
                }
                ret.put("checkid", checkID);
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

    private void writeString(RemoteAPIResponse response, RemoteAPIRequest request, String string, boolean wrapCallback) {
        OutputStream out = null;
        try {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "text/html", false));
            out = RemoteAPI.getOutputStream(response, request, false, true);
            if (wrapCallback && request.getJqueryCallback() != null) {
                if (string == null) string = "";
                string = "{\"content\": \"" + string.trim() + "\"}";
            }
            out.write(string.getBytes("UTF-8"));
        } catch (Throwable e) {
            throw new RemoteAPIException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    public void pollCheckedLinksFromDOM(RemoteAPIResponse response, RemoteAPIRequest request, String checkID) {
        CheckedDom session = null;
        synchronized (checkSessions) {
            session = checkSessions.get(checkID);
            if (session != null && session.status == 2) {
                checkSessions.remove(checkID);
            }
        }
        String ret = "-1";
        if (session != null) {
            synchronized (session) {
                switch (session.status) {
                case 0:
                    session.status = 1;
                    ret = "0";
                    break;
                case 1:
                    session.status = 2;
                    ret = "var jDownloaderObj = {statusCheck: function(){alert(\"jDObj: script injected to page successfully\");}};jDownloaderObj.statusCheck();";
                    break;
                default:
                    ret = "-1";
                    break;
                }
            }
        }
        writeString(response, request, ret, false);
    }

    public void onStateChange(StateEvent event) {
    }

    public void onStateUpdate(StateEvent event) {
    }
}
