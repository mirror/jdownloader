package jd.controlling.linkchecker;

import jd.http.BrowserSettingsThread;

public class LinkCheckerThread extends BrowserSettingsThread {

    public LinkCheckerThread(Runnable r) {
        super(r);
    }

}
