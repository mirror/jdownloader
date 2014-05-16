package jd.controlling.linkchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcrawler.CheckableLink;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class LinkChecker<E extends CheckableLink> {

    private static class InternCheckableLink {
        protected final CheckableLink                        link;
        protected final int                                  linkCheckerGeneration;
        protected final LinkChecker<? extends CheckableLink> checker;

        public InternCheckableLink(CheckableLink link, LinkChecker<? extends CheckableLink> checker) {
            this.link = link;
            this.linkCheckerGeneration = checker.checkerGeneration.get();
            this.checker = checker;
        }

        public CheckableLink getCheckableLink() {
            return this.link;
        }

        public boolean linkCheckAllowed() {
            return this.linkCheckerGeneration == checker.checkerGeneration.get();
        }

        public LinkChecker<? extends CheckableLink> getLinkChecker() {
            return checker;
        }

    }

    /* static variables */
    private static AtomicInteger                              CHECKER                = new AtomicInteger(0);
    private static AtomicInteger                              LINKCHECKER_THREAD_NUM = new AtomicInteger(0);
    private final static int                                  MAX_THREADS;
    private final static int                                  KEEP_ALIVE;
    private static HashMap<String, Thread>                    CHECK_THREADS          = new HashMap<String, Thread>();
    private static HashMap<String, List<InternCheckableLink>> LINKCHECKER            = new HashMap<String, List<InternCheckableLink>>();
    private static final Object                               LOCK                   = new Object();

    /* local variables for this LinkChecker */
    private AtomicLong                                        linksRequested         = new AtomicLong(0);
    private AtomicLong                                        linksDone              = new AtomicLong(0);
    private final boolean                                     forceRecheck;
    private LinkCheckerHandler<E>                             handler                = null;
    private static int                                        SPLITSIZE              = 80;
    private static LinkCheckerEventSender                     EVENTSENDER            = new LinkCheckerEventSender();
    protected AtomicInteger                                   checkerGeneration      = new AtomicInteger(0);

    public static LinkCheckerEventSender getEventSender() {
        return EVENTSENDER;
    }

    static {
        MAX_THREADS = Math.max(JsonConfig.create(LinkCheckerConfig.class).getMaxThreads(), 1);
        KEEP_ALIVE = Math.max(JsonConfig.create(LinkCheckerConfig.class).getThreadKeepAlive(), 100);
    }

    public LinkChecker() {
        this(false);
    }

    public LinkChecker(boolean forceRecheck) {
        this.forceRecheck = forceRecheck;
    }

    public boolean isForceRecheck() {
        return forceRecheck;
    }

    public void setLinkCheckHandler(LinkCheckerHandler<E> handler) {
        this.handler = handler;
    }

    public LinkCheckerHandler<E> getLinkCheckHandler() {
        return handler;
    }

    public void stopChecking() {
        checkerGeneration.incrementAndGet();
        if (linksDone.get() == linksRequested.get()) {
            CHECKER.decrementAndGet();
            EVENTSENDER.fireEvent(new LinkCheckerEvent(this, LinkCheckerEvent.Type.STOPPED));
        }
    }

    @SuppressWarnings("unchecked")
    protected void linkChecked(InternCheckableLink link) {
        if (link == null) {
            return;
        }
        boolean stopped = linksDone.incrementAndGet() == linksRequested.get();
        if (stopped) {
            CHECKER.decrementAndGet();
            EVENTSENDER.fireEvent(new LinkCheckerEvent(this, LinkCheckerEvent.Type.STOPPED));
        }
        LinkCheckerHandler<E> h = handler;
        if (h != null && link.linkCheckAllowed()) {
            h.linkCheckDone((E) link.getCheckableLink());
        }
    }

    public void check(List<E> links) {
        if (links == null) {
            throw new IllegalArgumentException("links is null?");
        }
        for (E link : links) {
            check(link);
        }
    }

    public void check(E link) {
        if (link == null || link.getDownloadLink() == null) {
            throw new IllegalArgumentException("links is null?");
        }
        DownloadLink dlLink = link.getDownloadLink();
        /* get Host of the link */
        String host = dlLink.getHost();
        if (Plugin.FTP_HOST.equalsIgnoreCase(host) || Plugin.DIRECT_HTTP_HOST.equalsIgnoreCase(host) || Plugin.HTTP_LINKS_HOST.equalsIgnoreCase(host)) {
            /* direct and ftp links are divided by their hostname */
            String specialHost = Browser.getHost(dlLink.getDownloadURL());
            if (specialHost != null) {
                host = host + "_" + specialHost;
            }
        }
        InternCheckableLink checkableLink = new InternCheckableLink(link, this);
        if (linksRequested.getAndIncrement() == linksDone.get()) {
            CHECKER.incrementAndGet();
            EVENTSENDER.fireEvent(new LinkCheckerEvent(this, LinkCheckerEvent.Type.STARTED));
        }
        synchronized (LOCK) {
            List<InternCheckableLink> hosterTodos = LINKCHECKER.get(host);
            if (hosterTodos == null) {
                hosterTodos = new ArrayList<InternCheckableLink>();
                LINKCHECKER.put(host, hosterTodos);
            }
            hosterTodos.add(checkableLink);
            /* notify linkcheckThread or try to start new one */
            Thread thread = CHECK_THREADS.get(host);
            if (thread == null) {
                startNewThreads();
            } else if (thread.isAlive() == false) {
                CHECK_THREADS.remove(host);
                startNewThreads();
            }
        }
    }

    /**
     * is the LinkChecker running
     * 
     * @return
     */
    public boolean isRunning() {
        return linksRequested.get() != linksDone.get();
    }

    public long checksRequested() {
        return linksRequested.get();
    }

    public long checksDone() {
        return linksDone.get();
    }

    public static boolean isChecking() {
        return CHECKER.get() > 0;
    }

    /* wait till all requested links are done */
    public boolean waitForChecked() {
        while (isRunning()) {
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
        return isRunning() == false;
    }

    /* start a new linkCheckThread for the given host */
    private static void startNewThread(final String threadHost) {
        synchronized (LOCK) {
            if (CHECK_THREADS.size() >= MAX_THREADS) {
                return;
            }
            final LinkCheckerThread newThread = new LinkCheckerThread() {

                public void run() {
                    int stopDelay = 1;
                    PluginForHost plg = null;
                    LogSource logger = null;
                    try {
                        while (true) {
                            /*
                             * arraylist to hold the current checkable links
                             */
                            java.util.List<InternCheckableLink> roundComplete = new ArrayList<InternCheckableLink>();
                            try {
                                synchronized (LOCK) {
                                    List<InternCheckableLink> hosterTodos = LINKCHECKER.get(threadHost);
                                    if (hosterTodos != null) {
                                        roundComplete.addAll(hosterTodos);
                                        hosterTodos.clear();
                                    }
                                }
                                /* add LinkStatus from roundComplete */
                                for (InternCheckableLink link : roundComplete) {
                                    if (link.linkCheckAllowed()) {
                                        if (link.getLinkChecker().isForceRecheck()) {
                                            /*
                                             * linkChecker instance is set to forceRecheck
                                             */
                                            link.getCheckableLink().getDownloadLink().setAvailableStatus(AvailableStatus.UNCHECKED);
                                        }
                                        link.getCheckableLink().getDownloadLink().setLinkStatus(new LinkStatus(link.getCheckableLink().getDownloadLink()));
                                    }
                                }
                                int n = roundComplete.size();
                                for (int i = 0; i < n; i += SPLITSIZE) {
                                    List<InternCheckableLink> roundSplit = roundComplete.subList(i, Math.min(n, i + SPLITSIZE));
                                    System.out.println(roundSplit.size() + " of " + roundComplete.size());
                                    if (roundSplit.size() > 0) {
                                        stopDelay = 1;
                                        HashSet<DownloadLink> massLinkCheck = new HashSet<DownloadLink>();
                                        for (InternCheckableLink link : roundSplit) {
                                            if (link.linkCheckAllowed()) {
                                                massLinkCheck.add(link.getCheckableLink().getDownloadLink());
                                            }
                                        }
                                        /* now we check the links */
                                        if (plg == null && massLinkCheck.size() > 0) {
                                            /* create plugin if not done yet */
                                            PluginClassLoaderChild cl;
                                            Thread.currentThread().setContextClassLoader(cl = PluginClassLoader.getInstance().getChild());
                                            DownloadLink first = massLinkCheck.iterator().next();
                                            LazyHostPlugin lazyp = HostPluginController.getInstance().get(first.getDefaultPlugin().getHost());
                                            plg = lazyp.newInstance(cl);
                                            plg.setLogger(logger = LogController.getFastPluginLogger(plg.getHost()));
                                            logger.info("LinkChecker: " + threadHost);
                                            ((BrowserSettingsThread) Thread.currentThread()).setLogger(logger);
                                            plg.setBrowser(new Browser());
                                            plg.init();
                                        }
                                        this.plugin = plg;
                                        try {
                                            boolean massCheck = false;
                                            try {
                                                /* try mass link check */
                                                logger.clear();
                                                plg.setBrowser(new Browser());
                                                logger.info("Check Multiple FileInformation");
                                                massCheck = plg.checkLinks(massLinkCheck.toArray(new DownloadLink[massLinkCheck.size()]));
                                            } catch (final Throwable e) {
                                                logger.log(e);
                                                logger.flush();
                                            } finally {
                                                logger.clear();
                                                massLinkCheck = null;
                                                try {
                                                    plg.getBrowser().getHttpConnection().disconnect();
                                                } catch (Throwable e) {
                                                }
                                            }
                                            logger.info("Multiple FileInformation Available: " + massCheck);
                                            if (massCheck == false) {
                                                HashSet<DownloadLink> dupCheck = new HashSet<DownloadLink>();
                                                for (InternCheckableLink link : roundSplit) {
                                                    if (link.linkCheckAllowed() && plg != null) {
                                                        /*
                                                         * this will check the link, if not already checked
                                                         */
                                                        if (dupCheck.add(link.getCheckableLink().getDownloadLink())) {
                                                            LinkChecker.updateAvailableStatus(plg, link.getCheckableLink().getDownloadLink(), logger);
                                                        }
                                                    }
                                                    link.getLinkChecker().linkChecked(link);
                                                }
                                            } else {
                                                for (InternCheckableLink link : roundSplit) {
                                                    link.getLinkChecker().linkChecked(link);
                                                }
                                            }
                                        } finally {
                                            this.plugin = null;
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                LogController.CL().log(e);
                            } finally {
                                /* remove LinkStatus from roundComplete */
                                for (InternCheckableLink link : roundComplete) {
                                    link.getCheckableLink().getDownloadLink().setLinkStatus(null);
                                }
                                try {
                                    logger.close();
                                } catch (final Throwable e) {
                                }
                            }
                            try {
                                Thread.sleep(KEEP_ALIVE);
                            } catch (InterruptedException e) {
                                LogController.CL().log(e);
                                synchronized (LOCK) {
                                    CHECK_THREADS.remove(threadHost);
                                    return;
                                }
                            }
                            synchronized (LOCK) {
                                List<InternCheckableLink> todos = LINKCHECKER.get(threadHost);
                                if (todos == null || todos.size() == 0) {
                                    stopDelay--;
                                    if (stopDelay < 0) {
                                        LINKCHECKER.remove(threadHost);
                                        CHECK_THREADS.remove(threadHost);
                                        startNewThreads();
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (final Throwable ignore) {
                        ignore.printStackTrace();
                    } finally {
                        try {
                            plg.clean();
                        } catch (final Throwable e) {
                        }
                    }
                }

                @Override
                public boolean isDebug() {
                    return true;
                }

                @Override
                public boolean isVerbose() {
                    return true;
                }

            };
            newThread.setName("LinkChecker: " + LINKCHECKER_THREAD_NUM.incrementAndGet() + ":" + threadHost);
            newThread.setDaemon(true);
            newThread.setPriority(Thread.MIN_PRIORITY);
            CHECK_THREADS.put(threadHost, newThread);
            newThread.start();
        }
    }

    /* start new linkCheckThreads until max is reached or no left to start */
    private static void startNewThreads() {
        synchronized (LOCK) {
            Set<String> removeHosts = new HashSet<String>();
            Set<Entry<String, List<InternCheckableLink>>> allTodos = LINKCHECKER.entrySet();
            for (Entry<String, List<InternCheckableLink>> set : allTodos) {
                String host = set.getKey();
                Thread thread = CHECK_THREADS.get(host);
                if (thread == null || !thread.isAlive()) {
                    CHECK_THREADS.remove(host);
                    List<InternCheckableLink> hosterTodos = set.getValue();
                    if (hosterTodos != null) {
                        ArrayList<InternCheckableLink> removeList = new ArrayList<InternCheckableLink>();
                        for (InternCheckableLink hosterTodo : hosterTodos) {
                            if (hosterTodo.linkCheckAllowed() == false) {
                                removeList.add(hosterTodo);
                            }
                        }
                        hosterTodos.removeAll(removeList);
                    }
                    if (hosterTodos == null || hosterTodos.size() == 0) {
                        removeHosts.add(host);
                    } else if (CHECK_THREADS.size() < MAX_THREADS) {
                        startNewThread(host);
                    } else {
                        break;
                    }
                }
            }
            for (String host : removeHosts) {
                LINKCHECKER.remove(host);
            }
        }
    }

    private static void updateAvailableStatus(PluginForHost plgToUse, DownloadLink link, LogSource logger) {
        if (link.getAvailableStatus() != AvailableStatus.UNCHECKED) {
            logger.info("Link " + link.getDownloadURL() + " is(already) " + link.getAvailableStatus());
            logger.clear();
            return;
        }
        AvailableStatus availableStatus = null;
        try {
            plgToUse.setBrowser(new Browser());
            logger.clear();
            plgToUse.reset();
            logger.info("Check FileInformation: " + link.getDownloadURL());
            availableStatus = plgToUse.requestFileInformation(link);
        } catch (PluginException e) {
            logger.log(e);
            switch (e.getLinkStatus()) {
            case LinkStatus.ERROR_FILE_NOT_FOUND:
                availableStatus = AvailableStatus.FALSE;
                break;
            case LinkStatus.ERROR_PREMIUM:
                if (e.getValue() == PluginException.VALUE_ID_PREMIUM_ONLY) {
                    availableStatus = AvailableStatus.UNCHECKABLE;
                    break;
                }
            default:
                availableStatus = AvailableStatus.UNCHECKABLE;
                plgToUse.errLog(e, plgToUse.getBrowser(), link);
                break;
            }
        } catch (Throwable e) {
            plgToUse.errLog(e, plgToUse.getBrowser(), link);
            logger.log(e);
            logger.flush();
            availableStatus = AvailableStatus.UNCHECKABLE;
        } finally {
            if (availableStatus == null) {
                logger.severe("Link " + link.getDownloadURL() + " is broken, status was null");
                availableStatus = AvailableStatus.UNCHECKABLE;
            }
            logger.info("Link " + link.getDownloadURL() + " is " + availableStatus);
            switch (availableStatus) {
            case UNCHECKABLE:
                logger.flush();
                break;
            case TRUE:
                if (FinalLinkState.OFFLINE.equals(link.getFinalLinkState())) {
                    link.setFinalLinkState(null);
                }
            case FALSE:
            default:
                logger.clear();
                break;
            }
            try {
                plgToUse.getBrowser().getHttpConnection().disconnect();
            } catch (Throwable e) {
            }
            link.setAvailableStatus(availableStatus);
        }
    }
}
