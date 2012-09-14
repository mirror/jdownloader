package jd.controlling.accountchecker;

import jd.http.BrowserSettingsThread;

public class AccountCheckerThread extends BrowserSettingsThread {

    public AccountCheckerThread(Runnable r) {
        super(r);
    }

}
