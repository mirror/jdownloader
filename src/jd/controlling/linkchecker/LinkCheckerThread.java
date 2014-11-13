package jd.controlling.linkchecker;

import java.util.List;

import jd.controlling.linkchecker.LinkChecker.InternCheckableLink;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.BrowserSettingsThread;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.UseSetLinkStatusThread;

public class LinkCheckerThread extends BrowserSettingsThread implements UseSetLinkStatusThread {

    public LinkCheckerThread() {
        super();
    }

    protected List<InternCheckableLink> checkableLinks;

    public LinkChecker<?> getLinkCheckerByLink(CheckableLink link) {
        if (checkableLinks != null) {
            for (InternCheckableLink l : checkableLinks) {

                if (match(l.getCheckableLink(), link)) {
                    return l.getLinkChecker();
                }
            }
        }
        return null;

    }

    private boolean match(CheckableLink a, CheckableLink b) {
        if (a instanceof CrawledLink) {
            if (b instanceof DownloadLink) {
                return a.getDownloadLink() == b;
            }
        }
        if (b instanceof CrawledLink) {
            if (a instanceof DownloadLink) {
                return b.getDownloadLink() == a;
            }
        }
        return a == b;
    }

    protected PluginForHost plugin;

    public PluginForHost getPlugin() {
        return plugin;
    }
}
