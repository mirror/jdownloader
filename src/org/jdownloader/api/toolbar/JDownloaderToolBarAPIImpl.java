package org.jdownloader.api.toolbar;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.BrokenCrawlerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerFilter;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.linkcrawler.UnknownCrawledLinkHandler;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.api.toolbar.LinkCheckResult.STATUS;
import org.jdownloader.api.toolbar.specialurls.YouTubeSpecialUrlHandling;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksProgress;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;
import org.jdownloader.updatev2.UpdateController;

//Toolbar NameSpace
//http://localhost:3128/toolbar/
//---------------------------------------------------------------------------------------------------------------
//toggleStopAfterCurrentDownload:
//->toggles the internal StopAfterCurrentDownload and returns its new State
//->boolean as return value
//---------------------------------------------------------------------------------------------------------------
//togglePremium:
//->toggles the internal *use Premium Accounts* and returns its new State
//->boolean as return value
//---------------------------------------------------------------------------------------------------------------
//togglePauseDownloads:
//->toggles the internal *Pause* and returns its new State
//->boolean as return value
//---------------------------------------------------------------------------------------------------------------
//toggleDownloadSpeedLimit:
//->toggles the internal *enable/disable Speedlimiter* and returns its new State
//->boolean as return value
//---------------------------------------------------------------------------------------------------------------
//toggleClipboardMonitoring:
//->toggles the internal *enable/disable clipboard monitoring* and returns its new State
//->boolean as return value
//---------------------------------------------------------------------------------------------------------------
//toggleAutomaticReconnect:
//->toggles the internal *enable/disable auto-reconnect* and returns its new State
//->boolean as return value
//---------------------------------------------------------------------------------------------------------------
//stopDownloads:
//->tell JDownloader to stop all running downloads
//->will always return true
//---------------------------------------------------------------------------------------------------------------
//startDownloads:
//->tell JDownloader to start downloads
//->will always return true
//---------------------------------------------------------------------------------------------------------------
//isAvailable:
//->will always return true, to check if API is available
//---------------------------------------------------------------------------------------------------------------
//getStatus:
//->returns current states of internal stuff and other usefull information
//Example:
//{
//  "data" : {
//    "pause" : false, /*pause state, use togglePauseDownloads to change*/
//    "limit" : false, /*limit state, use toggleDownloadSpeedLimit to change*/
//    "speed" : 0, /*average download speed in byte/s, for speedgraph*/
//    "reconnect" : false, /*auto-reconnect state, use toggleAutomaticReconnect to change*/
//    "download_complete" : 0, /*size of all enabled/todo downloads in bytes, for progress bar*/
//    "premium" : true, /*premium state, use togglePremium to change*/
//    "stopafter" : false, /*stopAfter state, use toggleStopAfterCurrentDownload to change*/
//    "clipboard" : false, /*clipboard-state, use toggleClipboardMonitoring to change*/
//    "running" : false, /*running-state, use stopDownloads/startDownloads to change*/
//    "download_current" : 0, /*already downloaded bytes, for progress bar*/
//    "limitspeed" : 0 /*speedlimit in byte/s if limit is true*/
//  }
//}
//---------------------------------------------------------------------------------------------------------------
//addLinksFromDOM:
//->send current DOM/Source to JDownloader in multiple Chunks as get-Requests are limited in length
//->The following parameters are available:
//1.) index: number of current Chunk (integer, 0,1,2,3....)
//2.) data: data of current Chunk (part of the DOM,String)
//3.) sessionID: a unique ID, so we know what chunks belong to the same DOM (unique String, must be same for all Chunks of current DOM/Page)
//4.) url: current URL (String)
//5.) lastchunk: boolean (must be true if this is the last Chunk of this sessionID
//
//returns:(json)
//-status, boolean value
//-msg, string in case something went wrong (will contain an exception message)
//---------------------------------------------------------------------------------------------------------------
//checkLinksFromDOM:
//->send current DOM/Source to JDownloader in multiple Chunks as get-Requests are limited in length
//->see addLinksFromDOM how to send DOM
//
//returns: (json)
//-same as addLinksFromDOM
//-when lastchunk was submitted, then checkLinksFromDOM will return additional checkID (String, unique)
//---------------------------------------------------------------------------------------------------------------
//pollCheckedLinksFromDOM:
//->check result for given checkID (provided by checkLinksFromDOM)
//->example: /pollCheckedLinksFromDOM?checkID
//
//returns: (json)
//-status(String): NA (not available, no such checkID known, results no longer available),PENDING(linkcheck still in progress),FINISHED(linkcheck finished, this will be the last response for given checkID)
//-links(Array<LinkStatus>): (optional, whenever there are new links available from linkchecker
//LinkStatus:
//Name(String): name of the file
//Host(String): name of the host
//Size(long): size in bytes, can be -1 for unknown
//URL(String): url of the file
//LinkState(String): status of the file (can be ONLINE,OFFLINE,UKNOWN,TEMP_UNKNOWN)
//---------------------------------------------------------------------------------------------------------------
//specialURLHandling:
//->check if we have special LinkCheck features for given URL
//->example: /specialURLHandling?http://google.de
//
//returns:(json)
//-nothing in case we do not have special LinkCheck features available for given URL (default for now)
//-js code we want to inject, we do LinkCheck ourselves

public class JDownloaderToolBarAPIImpl implements JDownloaderToolBarAPI, StateEventListener {

    private class ChunkedDom {
        protected HashMap<Integer, String> domChunks   = new HashMap<Integer, String>();
        protected String                   URL         = null;
        protected String                   completeDOM = null;
    }

    private class CheckedDom extends ChunkedDom {

        protected final String ID;

        protected CheckedDom(ChunkedDom dom) {
            this.completeDOM = dom.completeDOM;
            this.URL = dom.URL;
            this.ID = "check" + System.nanoTime();
        }

        protected LinkChecker<CrawledLink> linkChecker;
        protected LinkCrawler              linkCrawler;
        boolean                            finished = false;
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
        ret.put("limit", org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled());
        if (org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled()) {
            ret.put("limitspeed", org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getValue());
        } else {
            ret.put("limitspeed", 0);
        }
        ret.put("reconnect", CFG_RECONNECT.AUTO_RECONNECT_ENABLED.isEnabled());
        ret.put("clipboard", org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.isEnabled());
        ret.put("stopafter", DownloadWatchDog.getInstance().getSession().isStopMarkSet());
        ret.put("premium", org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled());
        if (running == 0) {
            ret.put("speed", 0);
        } else {
            ret.put("speed", DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeedMeter().getSpeedMeter());
        }
        ret.put("pause", DownloadWatchDog.getInstance().isPaused());

        List<DownloadLink> calc_progress = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            public int returnMaxResults() {
                return 0;
            }

            public boolean acceptNode(DownloadLink node) {
                if (!node.isEnabled()) return false;
                if (FinalLinkState.CheckFailed(node.getFinalLinkState())) return false;
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
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.toggle();
        return org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled();
    }

    public boolean toggleClipboardMonitoring() {
        boolean b = org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.isEnabled();
        org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.setValue(!b);
        return !b;
    }

    public boolean toggleAutomaticReconnect() {
        CFG_RECONNECT.AUTO_RECONNECT_ENABLED.toggle();
        return CFG_RECONNECT.AUTO_RECONNECT_ENABLED.isEnabled();
    }

    public boolean toggleStopAfterCurrentDownload() {
        // final ToolBarAction stopMark =
        // ActionController.getToolBarAction("toolbar.control.stopmark");
        // if (stopMark != null) {
        // stopMark.actionPerformed(null);
        // }
        // return DownloadWatchDog.getInstance().isStopMarkSet();
        return false;
    }

    public boolean togglePremium() {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.toggle();
        return org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled();
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
                         * if the url cannot be handled by a plugin, we check the dom
                         */
                        addCompleteDom(url, dom, link);
                    }

                });

                link.setBrokenCrawlerHandler(new BrokenCrawlerHandler() {

                    public void brokenCrawler(CrawledLink link, LinkCrawler lc) {
                        /*
                         * if the url cannot be handled because a plugin is broken, we check the dom
                         */
                        addCompleteDom(url, dom, link);

                    }
                });
                java.util.List<CrawledLink> links = new ArrayList<CrawledLink>();
                links.add(link);
                LinkCollector.getInstance().addCrawlerJob(links);
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

        final LinkCollectingJob job = new LinkCollectingJob(LinkOrigin.TOOLBAR, dom);
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
                    if (lc != null) {
                        lc.waitForCrawling();
                        System.out.println("JOB DONE: " + lc.getCrawledLinksFoundCounter());
                    }

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
            boolean hosterOnly = "true".equalsIgnoreCase(HttpRequest.getParameterbyKey(request, "hosteronly"));
            boolean map = "1".equals(HttpRequest.getParameterbyKey(request, "map"));
            ChunkedDom chunkedDom = getCompleteDOM(request);
            if (chunkedDom != null) {
                final CheckedDom checkSession = new CheckedDom(chunkedDom);
                synchronized (checkSessions) {
                    checkSessions.put(checkSession.ID, checkSession);
                }
                ret.put("checkid", checkSession.ID);
                checkSession.linkCrawler = new LinkCrawler();
                final LinkCrawlerHandler defaultHandler = checkSession.linkCrawler.getHandler();
                if (hosterOnly) {
                    checkSession.linkCrawler.setFilter(new LinkCrawlerFilter() {
                        /* ignore crawler/ftp/http links */
                        public boolean dropByUrl(CrawledLink link) {
                            PluginForHost plugin = link.gethPlugin();
                            if (plugin == null) return true;
                            if (("ftp".equalsIgnoreCase(plugin.getHost()) || "http links".equalsIgnoreCase(plugin.getHost()))) return true;
                            return false;
                        }

                        public boolean dropByFileProperties(CrawledLink link) {
                            return false;
                        }
                    });
                }
                checkSession.linkChecker = new LinkChecker<CrawledLink>();
                checkSession.linkChecker.setLinkCheckHandler(new LinkCheckerHandler<CrawledLink>() {

                    public void linkCheckDone(CrawledLink link) {
                        defaultHandler.handleFinalLink(link);
                    }
                });
                checkSession.linkCrawler.setHandler(new LinkCrawlerHandler() {

                    public void handleFinalLink(CrawledLink link) {
                        checkSession.linkChecker.check(link);
                    }

                    public void handleFilteredLink(CrawledLink link) {
                        defaultHandler.handleFilteredLink(link);
                    }

                    @Override
                    public void handleBrokenLink(CrawledLink link) {
                    }

                    @Override
                    public void handleUnHandledLink(CrawledLink link) {
                    }
                });
                if (map == false) {
                    /* we parse complete dom here and check for valid links */
                    checkSession.linkCrawler.crawl(checkSession.completeDOM);
                } else {
                    /* we got a linkID-URL map and will check only those links */
                    HashMap<String, String> linkMap = JSonStorage.restoreFromString(checkSession.completeDOM, new HashMap<String, String>().getClass());
                    java.util.List<CrawledLink> links2Check = new ArrayList<CrawledLink>();
                    Iterator<Entry<String, String>> it = linkMap.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<String, String> next = it.next();
                        links2Check.add(new LinkCheckLink(next.getValue(), next.getKey()));
                    }
                    checkSession.linkCrawler.crawl(links2Check);
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

    public LinkCheckResult pollCheckedLinksFromDOM(String checkID) {
        CheckedDom session = null;
        synchronized (checkSessions) {
            session = checkSessions.get(checkID);
            if (session != null && session.finished) {
                checkSessions.remove(checkID);
            }
        }
        LinkCheckResult result = new LinkCheckResult();
        boolean finished = false;
        if (session != null) {
            synchronized (session) {
                boolean stillRunning = session.linkChecker.isRunning() || session.linkCrawler.isRunning();
                java.util.List<CrawledLink> retL = null;
                synchronized (session.linkCrawler.getCrawledLinks()) {
                    retL = new ArrayList<CrawledLink>(session.linkCrawler.getCrawledLinks());
                    session.linkCrawler.getCrawledLinks().clear();
                }
                result.setStatus(STATUS.PENDING);
                if (retL.size() > 0) {
                    /* we have links to output */
                    java.util.List<LinkStatus> linkStats = new ArrayList<LinkStatus>();
                    for (CrawledLink link : retL) {
                        linkStats.add(new LinkStatus(link));
                    }
                    result.setLinks(linkStats);
                }
                if (stillRunning == false && retL.size() == 0) {
                    /* we are finished an no more links to output */
                    session.finished = true;
                    result.setStatus(STATUS.FINISHED);
                }
            }
        }
        if (finished) {
            synchronized (checkSessions) {
                checkSessions.remove(checkID);
            }
        }
        return result;
    }

    public void onStateChange(StateEvent event) {
    }

    public void onStateUpdate(StateEvent event) {
    }

    public String specialURLHandling(String url) {
        if (url.contains("youtube.com")) {
            return "var jDownloaderObj = {statusCheck: function(){" + YouTubeSpecialUrlHandling.handle(url) + "}};jDownloaderObj.statusCheck();";
        } else {
            return "";
        }
    }

    @Override
    public boolean triggerUpdate() {
        /* WebUpdate is running in its own Thread */
        new Thread() {
            public void run() {
                // runUpdateChecker is synchronized and may block
                UpdateController.getInstance().setGuiVisible(false);
                UpdateController.getInstance().runUpdateChecker(true);
            }
        }.start();

        return true;
    }
}
