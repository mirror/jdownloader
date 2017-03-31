package jd.controlling.linkchecker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class LinkChecker<E extends CheckableLink> {

    protected static class InternCheckableLink {
        protected final CheckableLink                        link;
        protected final long                                 linkCheckerGeneration;
        protected final LinkChecker<? extends CheckableLink> checker;
        protected final AtomicBoolean                        checkFlag = new AtomicBoolean(false);

        public InternCheckableLink(CheckableLink link, LinkChecker<? extends CheckableLink> checker) {
            this.link = link;
            this.linkCheckerGeneration = checker.checkerGeneration.get();
            this.checker = checker;
        }

        public final CheckableLink getCheckableLink() {
            return this.link;
        }

        public final boolean isChecked() {
            return checkFlag.get();
        }

        public final boolean check() {
            return checkFlag.compareAndSet(false, true);
        }

        public final boolean linkCheckAllowed() {
            if (this.linkCheckerGeneration == getLinkChecker().checkerGeneration.get()) {
                if (link instanceof CrawledLink) {
                    final CrawledLink cl = (CrawledLink) link;
                    final UniqueAlltimeID pn = cl.getPreviousParentNodeID();
                    if (pn != null) {
                        // we need at least previousParentNode, that will be set after changing the parentNode
                        final CrawledPackage cn = cl.getParentNode();
                        if (cn == null || cn.getControlledBy() == null) {
                            return false;
                        }
                    }
                } else {
                    final DownloadLink dlLink = link.getDownloadLink();
                    final FilePackage fp = dlLink.getFilePackage();
                    if (FilePackage.isDefaultFilePackage(fp) || fp.getControlledBy() == null) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        public final LinkChecker<? extends CheckableLink> getLinkChecker() {
            return checker;
        }

    }

    /* static variables */
    private final static AtomicInteger                                                                 CHECKER                = new AtomicInteger(0);
    private final static AtomicLong                                                                    LINKCHECKER_THREAD_NUM = new AtomicLong(0);
    private final static int                                                                           MAX_THREADS;
    private final static int                                                                           KEEP_ALIVE;
    private final static HashMap<String, Thread>                                                       CHECK_THREADS          = new HashMap<String, Thread>();
    private final static HashMap<String, WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>>> LINKCHECKER            = new HashMap<String, WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>>>();
    private final static Object                                                                        LOCK                   = new Object();

    /* local variables for this LinkChecker */
    private final AtomicLong                                                                           linksRequested         = new AtomicLong(0);
    private final boolean                                                                              forceRecheck;
    private LinkCheckerHandler<E>                                                                      handler                = null;
    private final static int                                                                           ROUNDSIZE              = 80;
    private final static LinkCheckerEventSender                                                        EVENTSENDER            = new LinkCheckerEventSender();
    protected final AtomicLong                                                                         checkerGeneration      = new AtomicLong(0);
    protected final AtomicBoolean                                                                      runningState           = new AtomicBoolean(false);

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
        if (linksRequested.get() == 0) {
            final boolean stopEvent;
            synchronized (CHECKER) {
                if (linksRequested.get() == 0 && runningState.compareAndSet(true, false)) {
                    stopEvent = true;
                    if (CHECKER.get() > 0) {
                        CHECKER.decrementAndGet();
                    }
                } else {
                    stopEvent = false;
                }
            }
            if (stopEvent) {
                EVENTSENDER.fireEvent(new LinkCheckerEvent(this, LinkCheckerEvent.Type.STOPPED));
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void linkChecked(InternCheckableLink link) {
        if (link != null && !link.isChecked()) {
            final boolean stopEvent;
            if (link.check() && linksRequested.decrementAndGet() == 0) {
                synchronized (CHECKER) {
                    if (linksRequested.get() == 0 && runningState.compareAndSet(true, false)) {
                        stopEvent = true;
                        if (CHECKER.get() > 0) {
                            CHECKER.decrementAndGet();
                        }
                    } else {
                        stopEvent = false;
                    }
                }
                if (stopEvent) {
                    EVENTSENDER.fireEvent(new LinkCheckerEvent(this, LinkCheckerEvent.Type.STOPPED));
                }
            }
            final LinkCheckerHandler<E> h = handler;
            if (h != null && link.linkCheckAllowed()) {
                h.linkCheckDone((E) link.getCheckableLink());
            }
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
            final String specialHost = Browser.getHost(dlLink.getPluginPatternMatcher());
            if (specialHost != null) {
                host = host + "_" + specialHost;
            }
        }
        final InternCheckableLink checkableLink = new InternCheckableLink(link, this);
        if (linksRequested.getAndIncrement() == 0) {
            final boolean event;
            synchronized (CHECKER) {
                event = runningState.compareAndSet(false, true);
                if (event) {
                    CHECKER.incrementAndGet();
                }
            }
            if (event) {
                EVENTSENDER.fireEvent(new LinkCheckerEvent(this, LinkCheckerEvent.Type.STARTED));
            }
        }
        synchronized (LOCK) {
            WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>> map = LINKCHECKER.get(host);
            if (map == null) {
                map = new WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>>();
                LINKCHECKER.put(host, map);
            }
            ArrayDeque<InternCheckableLink> list = map.get(this);
            if (list == null) {
                list = new ArrayDeque<InternCheckableLink>(64);
                map.put(this, list);
            }
            list.add(checkableLink);
            /* notify linkcheckThread or try to start new one */
            final Thread thread = CHECK_THREADS.get(host);
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
        return runningState.get();
    }

    public long checksRequested() {
        return linksRequested.get();
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
                    try {
                        while (true) {
                            /*
                             * arraylist to hold the current checkable links
                             */
                            this.checkableLinks = new ArrayList<InternCheckableLink>();
                            try {
                                synchronized (LOCK) {
                                    final WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>> map = LINKCHECKER.get(threadHost);
                                    final ArrayList<Iterator<InternCheckableLink>> its = new ArrayList<Iterator<InternCheckableLink>>();
                                    for (final ArrayDeque<InternCheckableLink> list : map.values()) {
                                        if (list.size() > 0) {
                                            its.add(list.iterator());
                                        }
                                    }
                                    if (its.size() > 0) {
                                        if (its.size() == 1) {
                                            final Iterator<InternCheckableLink> it = its.get(0);
                                            while (it.hasNext()) {
                                                checkableLinks.add(it.next());
                                                it.remove();
                                                if (checkableLinks.size() > ROUNDSIZE) {
                                                    break;
                                                }
                                            }
                                        } else {
                                            int index = 0;
                                            final int size = its.size();
                                            int again = size;
                                            while (true) {
                                                final Iterator<InternCheckableLink> it = its.get(index++);
                                                index = index % size;
                                                if (it.hasNext()) {
                                                    again = size;
                                                    checkableLinks.add(it.next());
                                                    if (checkableLinks.size() > ROUNDSIZE) {
                                                        break;
                                                    }
                                                    it.remove();
                                                } else {
                                                    if (--again == 0) {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (checkableLinks.size() > 0) {
                                    /* add LinkStatus from roundComplete */
                                    for (InternCheckableLink link : checkableLinks) {
                                        if (link.linkCheckAllowed()) {
                                            if (link.getLinkChecker().isForceRecheck()) {
                                                /*
                                                 * linkChecker instance is set to forceRecheck
                                                 */
                                                link.getCheckableLink().getDownloadLink().setAvailableStatus(AvailableStatus.UNCHECKED);
                                            }
                                        }
                                    }
                                    stopDelay = 1;
                                    /* now we check the links */
                                    LogSource logger = null;
                                    if (this.plugin == null) {
                                        /* create plugin if not done yet */
                                        final DownloadLink first = checkableLinks.get(0).getCheckableLink().getDownloadLink();
                                        try {
                                            final PluginClassLoaderChild cl = PluginClassLoader.getSharedChild(first.getDefaultPlugin());
                                            PluginClassLoader.setThreadPluginClassLoaderChild(cl, null);
                                            this.plugin = first.getDefaultPlugin().getLazyP().newInstance(cl);
                                            this.plugin.setLogger(logger = LogController.getFastPluginLogger(plugin.getHost() + "_" + plugin.getLazyP().getClassName()));
                                            ((BrowserSettingsThread) Thread.currentThread()).setLogger(logger);
                                            this.plugin.setBrowser(new Browser());
                                            this.plugin.init();
                                        } catch (final Throwable e) {
                                            LogController.CL().log(e);
                                        } finally {
                                            if (logger != null) {
                                                logger.close();
                                            }
                                        }
                                    }
                                    try {
                                        if (this.plugin == null) {
                                            for (final InternCheckableLink link : checkableLinks) {
                                                link.getLinkChecker().linkChecked(link);
                                            }
                                        } else {
                                            this.plugin.setLogger(logger = LogController.getFastPluginLogger(plugin.getHost() + "_" + plugin.getLazyP().getClassName()));
                                            ((BrowserSettingsThread) Thread.currentThread()).setLogger(logger);
                                            if (PluginForHost.implementsCheckLinks(this.plugin)) {
                                                logger.info("Check Multiple FileInformation");
                                                try {
                                                    final HashSet<DownloadLink> downloadLinks = new HashSet<DownloadLink>();
                                                    for (InternCheckableLink link : checkableLinks) {
                                                        if (link.linkCheckAllowed()) {
                                                            final DownloadLink dlLink = link.getCheckableLink().getDownloadLink();
                                                            if (dlLink.getAvailableStatus() != AvailableStatus.UNCHECKED) {
                                                                logger.info("Link " + dlLink.getPluginPatternMatcher() + " is(already) " + dlLink.getAvailableStatus());
                                                            } else {
                                                                downloadLinks.add(dlLink);
                                                            }
                                                        }
                                                    }
                                                    /* try mass link check */
                                                    logger.clear();
                                                    if (downloadLinks.size() > 0) {
                                                        this.plugin.setBrowser(new Browser());
                                                        this.plugin.reset();
                                                    }
                                                    if (downloadLinks.size() == 0 || this.plugin.checkLinks(downloadLinks.toArray(new DownloadLink[downloadLinks.size()]))) {
                                                        for (final InternCheckableLink link : checkableLinks) {
                                                            link.getLinkChecker().linkChecked(link);
                                                        }
                                                        continue;
                                                    }
                                                } catch (final Throwable e) {
                                                    logger.log(e);
                                                    logger.flush();
                                                } finally {
                                                    logger.clear();
                                                    try {
                                                        this.plugin.getBrowser().getHttpConnection().disconnect();
                                                    } catch (Throwable e) {
                                                    }
                                                    resetLinkStatus();
                                                }
                                            }
                                            final HashSet<DownloadLink> dupCheck = new HashSet<DownloadLink>();
                                            for (final InternCheckableLink link : checkableLinks) {
                                                if (link.linkCheckAllowed()) {
                                                    /*
                                                     * this will check the link, if not already checked
                                                     */
                                                    if (dupCheck.add(link.getCheckableLink().getDownloadLink())) {
                                                        LinkChecker.updateAvailableStatus(this.plugin, link.getCheckableLink().getDownloadLink(), logger);
                                                    }
                                                }
                                                link.getLinkChecker().linkChecked(link);
                                            }
                                        }
                                    } finally {
                                        try {
                                            if (logger != null) {
                                                logger.close();
                                            }
                                        } catch (final Throwable e) {
                                        }
                                        resetLinkStatus();
                                    }
                                }
                            } catch (Throwable e) {
                                LogController.CL().log(e);
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
                                final WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>> map = LINKCHECKER.get(threadHost);
                                if (map == null || !isNotEmpty(map.values())) {
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
                        LogController.CL().log(ignore);
                    } finally {
                        PluginClassLoader.setThreadPluginClassLoaderChild(null, null);
                        try {
                            if (this.plugin != null) {
                                this.plugin.clean();
                            }
                        } catch (final Throwable e) {
                        }
                    }
                }

                private final boolean isNotEmpty(Collection<ArrayDeque<InternCheckableLink>> values) {
                    if (values != null) {
                        for (final ArrayDeque<InternCheckableLink> list : values) {
                            if (list.size() > 0) {
                                return true;
                            }
                        }
                    }
                    return false;
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
            final Set<String> removeHosts = new HashSet<String>();
            final Set<Entry<String, WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>>>> allTodos = LINKCHECKER.entrySet();
            for (final Entry<String, WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>>> set : allTodos) {
                final String host = set.getKey();
                final Thread thread = CHECK_THREADS.get(host);
                if (thread == null || !thread.isAlive()) {
                    CHECK_THREADS.remove(host);
                    final WeakHashMap<LinkChecker<?>, ArrayDeque<InternCheckableLink>> map = set.getValue();
                    if (map != null) {
                        final Iterator<ArrayDeque<InternCheckableLink>> linkCheckerIt = map.values().iterator();
                        while (linkCheckerIt.hasNext()) {
                            final ArrayDeque<InternCheckableLink> next = linkCheckerIt.next();
                            final Iterator<InternCheckableLink> it = next.iterator();
                            while (it.hasNext()) {
                                final InternCheckableLink link = it.next();
                                if (!link.linkCheckAllowed()) {
                                    it.remove();
                                }
                            }
                            if (next.size() == 0) {
                                linkCheckerIt.remove();
                            }
                        }
                    }
                    if (map == null || map.size() == 0) {
                        removeHosts.add(host);
                    } else if (CHECK_THREADS.size() < MAX_THREADS) {
                        startNewThread(host);
                    } else {
                        break;
                    }
                }
            }
            for (final String host : removeHosts) {
                LINKCHECKER.remove(host);
            }
        }
    }

    private static void updateAvailableStatus(PluginForHost plgToUse, DownloadLink link, LogSource logger) {
        if (link.getAvailableStatus() != AvailableStatus.UNCHECKED) {
            logger.info("Link " + link.getPluginPatternMatcher() + " is(already) " + link.getAvailableStatus());
            logger.clear();
            return;
        }
        AvailableStatus availableStatus = null;
        try {
            logger.clear();
            plgToUse.setBrowser(new Browser());
            plgToUse.reset();
            logger.info("Check FileInformation: " + link.getPluginPatternMatcher());
            plgToUse.setDownloadLink(link);
            availableStatus = plgToUse.checkLink(link);
        } catch (PluginException e) {
            logger.log(e);
            switch (e.getLinkStatus()) {
            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                availableStatus = AvailableStatus.UNCHECKABLE;
                break;
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
                plgToUse.errLog(e, plgToUse.getBrowser(), logger, link, null);
                break;
            }
        } catch (Throwable e) {
            logger.log(e);
            plgToUse.errLog(e, plgToUse.getBrowser(), logger, link, null);
            logger.flush();
            availableStatus = AvailableStatus.UNCHECKABLE;
        } finally {
            plgToUse.setDownloadLink(null);
            if (availableStatus == null) {
                logger.severe("Link " + link.getPluginPatternMatcher() + " is broken, status was null");
                availableStatus = AvailableStatus.UNCHECKABLE;
            }
            logger.info("Link " + link.getPluginPatternMatcher() + " is " + availableStatus);
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

    public static boolean isForcedLinkCheck(CheckableLink downloadLink) {
        if (Thread.currentThread() instanceof LinkCheckerThread) {
            LinkChecker<?> linkchecker = ((LinkCheckerThread) Thread.currentThread()).getLinkCheckerByLink(downloadLink);
            if (linkchecker != null) {
                return linkchecker.isForceRecheck();
            }
        }
        // always force if we are not in LinkCheckerThread
        return true;
    }
}
