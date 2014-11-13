package jd.controlling.linkchecker;

import java.util.List;

import jd.controlling.linkchecker.LinkChecker.InternCheckableLink;
import jd.controlling.linkcrawler.CheckableLink;
import jd.http.BrowserSettingsThread;
import jd.plugins.PluginForHost;
import jd.plugins.UseSetLinkStatusThread;

public class LinkCheckerThread extends BrowserSettingsThread implements UseSetLinkStatusThread {

    public LinkCheckerThread() {
        super();
    }

    protected List<InternCheckableLink> checkableLinks;

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
}
