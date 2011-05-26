package org.jdownloader.extensions.jdpremserv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import jd.config.Configuration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public class JDPremServController {

    private static final String               PackageName = "JDPremServ";
    private static final JDPremServController INSTANCE    = new JDPremServController();

    public static JDPremServController getInstance() {
        return JDPremServController.INSTANCE;
    }

    private FilePackage                    premServFilePackage = null;
    private boolean                        running             = false;

    private Thread                         cleanupThread       = null;
    private HashMap<String, DownloadLink>  requestedLinks      = null;
    private HashMap<DownloadLink, Integer> requestedDownloads  = null;

    private HashMap<DownloadLink, Long>    lastAccessLinks     = null;

    private JDPremServController() {
        this.requestedLinks = new HashMap<String, DownloadLink>();
        this.requestedDownloads = new HashMap<DownloadLink, Integer>();
        this.lastAccessLinks = new HashMap<DownloadLink, Long>();
        this.premServFilePackage = this.getPremServFilePackage();
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, 3);
    }

    public synchronized int addRequestedDownload(final DownloadLink link) {
        if (link == null) { return -1; }
        Integer ret = this.requestedDownloads.get(link);
        if (ret == null) {
            ret = new Integer(1);
        } else {
            ret++;
        }
        link.getLinkStatus().setStatusText(ret + " progressing this link");
        link.requestGuiUpdate();
        this.requestedDownloads.put(link, ret);
        return ret;
    }

    public synchronized void cleanUp() {
        final ArrayList<DownloadLink> remove = new ArrayList<DownloadLink>();
        for (final DownloadLink link : this.premServFilePackage.getDownloadLinkList()) {
            Long last = this.lastAccessLinks.get(link);
            if (last == null) {
                last = 0l;
            }
            Integer reqs = this.requestedDownloads.get(link);
            if (reqs == null) {
                reqs = 0;
            }
            if (reqs == 0 && last + 1000 * 60 * 10 < System.currentTimeMillis()) {
                remove.add(link);
                link.deleteFile(true, true);
                this.requestedDownloads.remove(link);
                this.lastAccessLinks.remove(link);
                this.requestedLinks.values().remove(link);
            }
        }
        this.premServFilePackage.remove(remove);
    }

    public synchronized DownloadLink getDownloadLink(final String url) {
        if (url == null || url.length() == 0) { return null; }
        DownloadLink retLink = null;
        retLink = this.requestedLinks.get(url);
        if (retLink == null) {
            /* search url for valid hostlinks */
            final DistributeData search = new DistributeData(url);
            final ArrayList<DownloadLink> found = search.findLinks();
            /* only one hostlink may exist */
            if (found == null || found.size() != 1) { return null; }
            final String hostUrl = found.get(0).getDownloadURL();
            retLink = null;
            /* search premservfilepackage for downloadlink with this url */
            for (final DownloadLink current : this.premServFilePackage.getDownloadLinkList()) {
                if (current.getDownloadURL().equalsIgnoreCase(hostUrl)) {
                    retLink = current;
                    break;
                }
            }
            if (retLink == null) {
                /* none found, so we add it */
                retLink = found.get(0);
                /* first disabled, maybe user has not enough rights */
                retLink.setEnabled(false);
                this.premServFilePackage.add(retLink);
            }
            if (retLink.getLinkStatus().isFinished() && !retLink.getLinkStatus().isPluginActive()) {
                final File file = new File(retLink.getFileOutput());
                if (!file.exists()) {
                    retLink.reset();
                }
            }
            this.requestedLinks.put(url, retLink);
        }

        retLink.getDefaultPlugin().setAGBChecked(true);
        /* add jdpremserv package to downloadlist */
        DownloadController.getInstance().addPackage(this.premServFilePackage);
        /* update last access */
        this.lastAccessLinks.put(retLink, System.currentTimeMillis());
        return retLink;
    }

    /* get the filepackage to use for jdpremserv */
    private FilePackage getPremServFilePackage() {
        FilePackage found = null;
        synchronized (DownloadController.ACCESSLOCK) {
            for (final FilePackage current : DownloadController.getInstance().getPackages()) {
                if (current.getName().equalsIgnoreCase(JDPremServController.PackageName)) {
                    found = current;
                    break;
                }
            }
            if (found == null) {
                found = FilePackage.getInstance();
                found.setName(JDPremServController.PackageName);
            }
        }
        /* we dont want postprocessing for this filepackage */
        found.setPostProcessing(false);
        if (found.getDownloadLinkList() != null) {
            for (final DownloadLink link : found.getDownloadLinkList()) {
                this.lastAccessLinks.put(link, System.currentTimeMillis());
            }
        }
        return found;
    }

    public synchronized int removeRequestedDownload(final DownloadLink link) {
        if (link == null) { return -1; }
        Integer ret = this.requestedDownloads.get(link);
        if (ret == null) {
            return -1;
        } else {
            ret--;
        }
        if (ret > 0) {
            link.getLinkStatus().setStatusText(ret + " progressing this link");
        } else {
            link.getLinkStatus().setStatusText(null);
        }
        link.requestGuiUpdate();
        this.requestedDownloads.put(link, ret);
        return ret;
    }

    public synchronized boolean resetDownloadLink(final String url) {
        final DownloadLink retLink = this.requestedLinks.get(url);
        if (retLink != null) {
            final Integer ret = this.requestedDownloads.get(retLink);
            if (ret == null || ret == 0) {
                if (!retLink.getLinkStatus().isPluginActive()) {
                    retLink.reset();
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void start() {
        if (this.running) { return; }
        this.running = true;
        this.cleanupThread = new Thread(new Runnable() {

            public void run() {
                while (JDPremServController.this.running) {
                    try {
                        Thread.sleep(1000 * 60 * 10);
                    } catch (final InterruptedException e) {

                    }
                    JDPremServController.this.cleanUp();
                }
            }

        });
        this.cleanupThread.start();
    }

    public synchronized void stop() {
        this.running = false;
        if (this.cleanupThread != null) {
            this.cleanupThread.interrupt();
            this.cleanupThread = null;
        }
    }

}
