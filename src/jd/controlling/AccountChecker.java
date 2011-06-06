package jd.controlling;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

public class AccountChecker {

    class AccountCheckJob {

        private Account     account;
        private boolean     isChecked  = false;
        private boolean     isChecking = false;
        private boolean     force      = false;
        private AccountInfo ai         = null;

        public AccountCheckJob(Account account, boolean force) {
            this.account = account;
            this.force = force;
        }

        public AccountCheckJob(Account account) {
            this(account, false);
        }

        protected void check() {
            isChecking = true;
            try {
                ai = AccountController.getInstance().updateAccountInfo((String) null, account, force);
            } finally {
                isChecking = false;
                synchronized (AccountCheckJob.this) {
                    isChecked = true;
                    AccountCheckJob.this.notify();
                }
                jobsDone.incrementAndGet();
            }
        }

        public boolean isChecked() {
            return isChecked;
        }

        public boolean isChecking() {
            return isChecking;
        }

        public AccountInfo getAccountInfo() {
            return ai;
        }

        public AccountInfo waitChecked() {
            while (isChecked == false) {
                synchronized (AccountCheckJob.this) {
                    if (isChecked == true) break;
                    try {
                        AccountCheckJob.this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return ai;
        }

    }

    private static AccountChecker                        INSTANCE      = new AccountChecker();

    private HashMap<String, Thread>                      checkThreads  = new HashMap<String, Thread>();
    private HashMap<String, LinkedList<AccountCheckJob>> jobs          = new HashMap<String, LinkedList<AccountCheckJob>>();
    private AtomicLong                                   jobsRequested = new AtomicLong(0);
    private AtomicLong                                   jobsDone      = new AtomicLong(0);

    private AccountCheckerEventSender                    eventSender   = new AccountCheckerEventSender();                    ;

    /**
     * @return the eventSender
     */
    public AccountCheckerEventSender getEventSender() {
        return eventSender;
    }

    public static AccountChecker getInstance() {
        return INSTANCE;
    }

    private AccountChecker() {
    }

    public AccountCheckJob check(Account account, boolean force) {
        AccountCheckJob ret = null;
        boolean started = false;
        synchronized (AccountChecker.this) {
            final String host = AccountController.getInstance().getHosterName(account);
            if (host == null) return null;
            /* get joblist for same host */
            LinkedList<AccountCheckJob> list = jobs.get(host);
            if (list == null) {
                list = new LinkedList<AccountCheckJob>();
                jobs.put(host, list);
            }
            /* add job to joblist */
            ret = new AccountCheckJob(account, force);
            jobsRequested.incrementAndGet();
            list.add(ret);
            /* get thread to check this hoster */
            Thread thread = checkThreads.get(host);
            if (thread == null || !thread.isAlive()) {
                started = checkThreads.isEmpty();
                thread = new Thread(new Runnable() {

                    public void run() {
                        AccountCheckJob job = null;
                        boolean stopped = false;
                        while (true) {
                            synchronized (AccountChecker.this) {
                                LinkedList<AccountCheckJob> joblist = jobs.get(host);
                                if (joblist != null && joblist.size() > 0) {
                                    job = joblist.removeFirst();
                                } else {
                                    jobs.remove(host);
                                    checkThreads.remove(host);
                                    stopped = checkThreads.isEmpty();
                                    break;
                                }
                            }
                            try {
                                job.check();
                            } catch (final Throwable e) {
                                e.printStackTrace();
                            }
                        }
                        if (stopped) {
                            eventSender.fireEvent(new AccountCheckerEvent(AccountChecker.this, AccountCheckerEvent.Types.CHECK_STOPPED, null));
                        }
                    }

                });
                thread.setName("AccountChecker: " + host);
                thread.setDaemon(true);
                checkThreads.put(host, thread);
                thread.start();
            }
        }
        if (started) this.eventSender.fireEvent(new AccountCheckerEvent(this, AccountCheckerEvent.Types.CHECK_STARTED, null));
        return ret;
    }

    public boolean isRunning() {
        return this.jobsRequested.get() != this.jobsDone.get();
    }
}
