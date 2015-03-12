package jd.controlling.linkchecker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.controlling.linkchecker.LinkChecker.InternCheckableLink;
import jd.controlling.linkcrawler.CheckableLink;
import jd.http.BrowserSettingsThread;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.UseSetLinkStatusThread;

public class LinkCheckerThread extends BrowserSettingsThread implements UseSetLinkStatusThread {

    public LinkCheckerThread() {
        super();
    }

    protected List<InternCheckableLink>   checkableLinks;

    private Map<DownloadLink, LinkStatus> linkStatusMap = new HashMap<DownloadLink, LinkStatus>();

    public LinkChecker<?> getLinkCheckerByLink(CheckableLink link) {
        final List<InternCheckableLink> lcheckableLinks = checkableLinks;
        if (lcheckableLinks != null) {
            for (InternCheckableLink l : lcheckableLinks) {
                if (match(l.getCheckableLink(), link)) {
                    return l.getLinkChecker();
                }
            }
        }
        return null;
    }

    private boolean match(CheckableLink a, CheckableLink b) {
        if (a == b) {
            return true;
        }
        if (a != null && b != null) {
            return a.getDownloadLink() == b.getDownloadLink();
        }
        return false;
    }

    protected PluginForHost plugin;

    public PluginForHost getPlugin() {
        return plugin;
    }

    @Override
    public LinkStatus getLinkStatus(DownloadLink downloadLink) {
        LinkStatus ret = linkStatusMap.get(downloadLink);
        if (ret == null) {
            ret = new LinkStatus(downloadLink);
            linkStatusMap.put(downloadLink, ret);
        }
        return ret;
    }

    @Override
    public void resetLinkStatus() {
        linkStatusMap = new HashMap<DownloadLink, LinkStatus>();
    }
}
