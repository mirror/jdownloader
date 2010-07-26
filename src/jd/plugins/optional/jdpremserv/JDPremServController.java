package jd.plugins.optional.jdpremserv;

import java.util.ArrayList;
import java.util.HashMap;

import jd.config.Configuration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public class JDPremServController {

    private static final String PackageName = "JDPremServ";
    private static final JDPremServController INSTANCE = new JDPremServController();
    private FilePackage premServFilePackage = null;
    private boolean running = false;
    private Thread cleanupThread = null;

    private HashMap<String, DownloadLink> requestedLinks = null;
    private HashMap<DownloadLink, Integer> requestedDownloads = null;
    private HashMap<DownloadLink, Long> lastAccessLinks = null;

    private JDPremServController() {
        requestedLinks = new HashMap<String, DownloadLink>();
        requestedDownloads = new HashMap<DownloadLink, Integer>();
        lastAccessLinks = new HashMap<DownloadLink, Long>();
        premServFilePackage = getPremServFilePackage();
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, 3);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        cleanupThread = new Thread(new Runnable() {

            public void run() {
                while (running) {
                    try {
                        Thread.sleep(1000 * 60 * 10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cleanUp();
                }
            }

        });
        cleanupThread.start();
    }

    public synchronized void stop() {
        running = false;
        if (cleanupThread != null) {
            cleanupThread.interrupt();
            cleanupThread = null;
        }
    }

    public synchronized void cleanUp() {
        ArrayList<DownloadLink> remove = new ArrayList<DownloadLink>();
        for (DownloadLink link : premServFilePackage.getDownloadLinkList()) {
            Long last = lastAccessLinks.get(link);
            if (last == null) last = 0l;
            Integer reqs = requestedDownloads.get(link);
            if (reqs == null) reqs = 0;
            if (reqs == 0 && (last + (1000 * 60 * 10)) < System.currentTimeMillis()) {
                remove.add(link);
                link.deleteFile(true, true);
                requestedDownloads.remove(link);
                lastAccessLinks.remove(link);
                requestedLinks.values().remove(link);
            }
        }
        premServFilePackage.remove(remove);
    }

    public static JDPremServController getInstance() {
        return INSTANCE;
    }

    /* get the filepackage to use for jdpremserv */
    private FilePackage getPremServFilePackage() {
        FilePackage found = null;
        for (FilePackage current : DownloadController.getInstance().getPackages()) {
            if (current.getName().equalsIgnoreCase(PackageName)) {
                found = current;
                break;
            }
        }
        if (found == null) {
            found = FilePackage.getInstance();
            found.setName(PackageName);
        }
        /* we dont want postprocessing for this filepackage */
        found.setPostProcessing(false);
        if (found.getDownloadLinkList() != null) {
            for (DownloadLink link : found.getDownloadLinkList()) {
                lastAccessLinks.put(link, System.currentTimeMillis());
            }
        }
        return found;
    }

    public synchronized boolean resetDownloadLink(String url) {
        DownloadLink retLink = requestedLinks.get(url);
        if (retLink != null) {
            if (retLink.getLinkStatus().isFailed() && !retLink.getLinkStatus().isPluginActive()) {
                retLink.reset();
                return true;
            }
        }
        return false;
    }

    public synchronized DownloadLink getDownloadLink(String url) {
        if (url == null || url.length() == 0) return null;
        DownloadLink retLink = null;
        retLink = requestedLinks.get(url);
        if (retLink == null) {
            /* search url for valid hostlinks */
            DistributeData search = new DistributeData(url);
            ArrayList<DownloadLink> found = search.findLinks();
            /* only one hostlink may exist */
            if (found == null || found.size() != 1) return null;
            String hostUrl = found.get(0).getDownloadURL();
            retLink = null;
            /* search premservfilepackage for downloadlink with this url */
            for (DownloadLink current : premServFilePackage.getDownloadLinkList()) {
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
                premServFilePackage.add(retLink);
            }
            requestedLinks.put(url, retLink);
        }

        retLink.getPlugin().setAGBChecked(true);
        /* add jdpremserv package to downloadlist */
        DownloadController.getInstance().addPackage(premServFilePackage);
        /* update last access */
        lastAccessLinks.put(retLink, System.currentTimeMillis());
        return retLink;
    }

    public synchronized int addRequestedDownload(DownloadLink link) {
        if (link == null) return -1;
        Integer ret = requestedDownloads.get(link);
        if (ret == null) {
            ret = new Integer(1);
        } else {
            ret++;
        }
        link.getLinkStatus().setStatusText(ret + " progressing this link");
        link.requestGuiUpdate();
        requestedDownloads.put(link, ret);
        return ret;
    }

    public synchronized int removeRequestedDownload(DownloadLink link) {
        if (link == null) return -1;
        Integer ret = requestedDownloads.get(link);
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
        requestedDownloads.put(link, ret);
        return ret;
    }

}
