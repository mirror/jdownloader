package jd.plugins.optional.jdpremserver;

import java.util.ArrayList;
import java.util.HashMap;

import jd.config.Configuration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class JDPremServController implements ControlListener {

    private static final String PackageName = "JDPremServ";
    private static final JDPremServController INSTANCE = new JDPremServController();
    private FilePackage premServFilePackage = null;

    private static HashMap<String, DownloadLink> requestedLinks = new HashMap<String, DownloadLink>();

    private JDPremServController() {
        premServFilePackage = getPremServFilePackage();
        /* WORKAROUND: we don't remove finishd download links at the moment */
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, 3);
    }

    public synchronized void start() {
        JDController.getInstance().addControlListener(this);
    }

    public synchronized void stop() {
        JDController.getInstance().removeControlListener(this);
    }

    public static JDPremServController getInstance() {
        return INSTANCE;
    }

    public FilePackage getPremServFilePackage() {
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
        return found;
    }

    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if (!(event.getSource() instanceof PluginForHost)) return;
            DownloadLink lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (lastDownloadFinished.getLinkStatus().isFinished()) {
                /* react on finished */
            }
            break;
        }
    }

    public synchronized DownloadLink getDownloadLink(String url) {
        if (url == null || url.length() == 0) return null;
        if (requestedLinks.containsKey(url)) {
            /* url already in requestedLinks */
            return requestedLinks.get(url);
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
        requestedLinks.put(url, ret);
        DownloadController.getInstance().addPackage(premServFilePackage);
        return ret;
    }

}
