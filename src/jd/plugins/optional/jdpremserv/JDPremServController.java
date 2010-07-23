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

    private HashMap<String, DownloadLink> requestedLinks = new HashMap<String, DownloadLink>();
    private HashMap<DownloadLink, Integer> requestedDownloads = new HashMap<DownloadLink, Integer>();
    private HashMap<DownloadLink, Long> lastAccessLinks = new HashMap<DownloadLink, Long>();

    private JDPremServController() {
        premServFilePackage = getPremServFilePackage();
        /* WORKAROUND: we don't remove finishd download links at the moment */
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
                requestedDownloads.remove(link);
                lastAccessLinks.remove(link);
            }
        }
        premServFilePackage.remove(remove);
    }

    public static JDPremServController getInstance() {
        return INSTANCE;
    }

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
        found.setPostProcessing(false);
        if (found.getDownloadLinkList() != null) {
            for (DownloadLink link : found.getDownloadLinkList()) {
                /* TODO: nullpointer */
                // lastAccessLinks.put(link, System.currentTimeMillis());
            }
        }
        return found;
    }

    public synchronized DownloadLink getDownloadLink(String url) {
        if (url == null || url.length() == 0) return null;
        if (requestedLinks.containsKey(url)) {
            /* url already in requestedLinks */
            DownloadLink ret = requestedLinks.get(url);
            ret.getPlugin().setAGBChecked(true);
            lastAccessLinks.put(ret, System.currentTimeMillis());
            return ret;
        }
        /* search url for valid hostlinks */
        DistributeData search = new DistributeData(url);
        ArrayList<DownloadLink> found = search.findLinks();
        /* only one hostlink may exist */
        if (found == null || found.size() != 1) return null;
        String hostUrl = found.get(0).getDownloadURL();
        DownloadLink ret = null;
        /* search premservfilepackage for downloadlink with this url */
        for (DownloadLink current : premServFilePackage.getDownloadLinkList()) {
            if (current.getDownloadURL().equalsIgnoreCase(hostUrl)) {
                ret = current;
                break;
            }
        }
        if (ret == null) {
            /* none found, so we add it */
            ret = found.get(0);
            premServFilePackage.add(ret);
        }
        ret.getPlugin().setAGBChecked(true);
        requestedLinks.put(url, ret);
        DownloadController.getInstance().addPackage(premServFilePackage);
        lastAccessLinks.put(ret, System.currentTimeMillis());
        return ret;
    }

    public synchronized int addRequestedDownload(DownloadLink link) {
        if (link == null) return -1;
        Integer ret = requestedDownloads.get(link);
        if (ret == null) {
            ret = new Integer(1);
        } else {
            ret++;
        }
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
        requestedDownloads.put(link, ret);
        return ret;
    }

}
