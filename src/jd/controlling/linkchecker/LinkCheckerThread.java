package jd.controlling.linkchecker;

import jd.http.BrowserSettingsThread;
import jd.plugins.UseSetLinkStatusThread;

public class LinkCheckerThread extends BrowserSettingsThread implements UseSetLinkStatusThread {

    public LinkCheckerThread(Runnable r) {
        super(r);
    }

}
