package jd.controlling.accountchecker;

import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.http.BrowserSettingsThread;

public class AccountCheckerThread extends BrowserSettingsThread {
    protected AccountCheckJob job;

    public AccountCheckJob getJob() {
        return job;
    }

    public AccountCheckerThread() {
        super();
    }

    public static boolean isForced() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof AccountCheckerThread) {
            final AccountCheckJob job = ((AccountCheckerThread) thread).getJob();
            return job.isForce();
        }
        return false;
    }

}
