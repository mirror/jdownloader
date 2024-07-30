package jd.controlling.linkcrawler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerConfig.DirectHTTPPermission;
import jd.controlling.linkcrawler.LinkCrawlerRule.RULE;
import jd.http.AuthenticationFactory;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.SimpleFTP;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.HTMLParser.HtmlParserCharSequence;
import jd.parser.html.HTMLParser.HtmlParserResultSet;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.ClearableLogInterface;
import org.appwork.utils.logging2.ClosableLogInterface;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.UrlProtection;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.plugins.components.abstractGenericHTTPDirectoryIndexCrawler;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.container.ContainerPluginController;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GeneralSettings;

public class LinkCrawler {
    private static enum DISTRIBUTE {
        STOP,
        BLACKLISTED,
        NEXT,
        CONTINUE,
        PARTIAL_MATCH
    }

    protected static enum DUPLICATE {
        CONTAINER,
        CRAWLER,
        FINAL,
        DEEP
    }

    private final static String                                           DIRECT_HTTP                 = "directhttp";
    private final static String                                           HTTP_LINKS                  = "http links";
    private final static int                                              MAX_THREADS;
    private java.util.List<CrawledLink>                                   crawledLinks                = new ArrayList<CrawledLink>();
    private AtomicInteger                                                 crawledLinksCounter         = new AtomicInteger(0);
    private java.util.List<CrawledLink>                                   filteredLinks               = new ArrayList<CrawledLink>();
    private AtomicInteger                                                 filteredLinksCounter        = new AtomicInteger(0);
    private java.util.List<CrawledLink>                                   brokenLinks                 = new ArrayList<CrawledLink>();
    private AtomicInteger                                                 brokenLinksCounter          = new AtomicInteger(0);
    private java.util.List<CrawledLink>                                   unhandledLinks              = new ArrayList<CrawledLink>();
    private final AtomicInteger                                           unhandledLinksCounter       = new AtomicInteger(0);
    private final AtomicInteger                                           processedLinksCounter       = new AtomicInteger(0);
    private final List<LinkCrawlerTask>                                   tasks                       = new ArrayList<LinkCrawlerTask>();
    private final static Set<LinkCrawler>                                 CRAWLER                     = new HashSet<LinkCrawler>();
    private final Map<String, Object>                                     duplicateFinderContainer;
    private final Map<LazyCrawlerPlugin, Set<String>>                     duplicateFinderCrawler;
    private final Map<String, CrawledLink>                                duplicateFinderFinal;
    private final Map<String, Object>                                     duplicateFinderDeep;
    private final Map<CrawledLink, Object>                                loopPreventionEmbedded;
    private LinkCrawlerHandler                                            handler                     = null;
    protected static final ThreadPoolExecutor                             threadPool;
    private LinkCrawlerFilter                                             filter                      = null;
    private final LinkCrawler                                             parentCrawler;
    private final long                                                    created;
    public final static String                                            PACKAGE_ALLOW_MERGE         = "ALLOW_MERGE";
    public final static String                                            PACKAGE_ALLOW_INHERITANCE   = "ALLOW_INHERITANCE";
    public final static String                                            PACKAGE_CLEANUP_NAME        = "CLEANUP_NAME";
    public final static String                                            PACKAGE_IGNORE_VARIOUS      = "PACKAGE_IGNORE_VARIOUS";
    public final static String                                            PROPERTY_AUTO_REFERER       = "autoReferer";
    public static final UniqueAlltimeID                                   PERMANENT_OFFLINE_ID        = new UniqueAlltimeID();
    private boolean                                                       doDuplicateFinderFinalCheck = true;
    private List<LazyCrawlerPlugin>                                       unsortedLazyCrawlerPlugins;
    protected final PluginClassLoaderChild                                classLoader;
    private final String                                                  defaultDownloadFolder;
    private final AtomicReference<List<LazyCrawlerPlugin>>                sortedLazyCrawlerPlugins    = new AtomicReference<List<LazyCrawlerPlugin>>();
    private final AtomicReference<List<LazyHostPlugin>>                   sortedLazyHostPlugins       = new AtomicReference<List<LazyHostPlugin>>();
    private final AtomicReference<List<LinkCrawlerRule>>                  linkCrawlerRules            = new AtomicReference<List<LinkCrawlerRule>>();
    private LinkCrawlerDeepInspector                                      deepInspector               = null;
    private DirectHTTPPermission                                          directHTTPPermission        = DirectHTTPPermission.ALWAYS;
    protected final UniqueAlltimeID                                       uniqueAlltimeID             = new UniqueAlltimeID();
    protected final WeakHashMap<LinkCrawler, Object>                      children                    = new WeakHashMap<LinkCrawler, Object>();
    protected final static WeakHashMap<LinkCrawler, Set<LinkCrawlerLock>> LOCKS                       = new WeakHashMap<LinkCrawler, Set<LinkCrawlerLock>>();
    protected final AtomicReference<LinkCrawlerGeneration>                linkCrawlerGeneration       = new AtomicReference<LinkCrawlerGeneration>(null);
    protected final static WeakHashMap<LinkCrawler, Map<String, Object>>  CRAWLER_CACHE               = new WeakHashMap<LinkCrawler, Map<String, Object>>();

    public Object getCrawlerCache(final String key) {
        synchronized (CRAWLER_CACHE) {
            final Map<String, Object> cache = CRAWLER_CACHE.get(this);
            return cache != null ? cache.get(key) : null;
        }
    }

    public Object putCrawlerCache(final String key, Object value) {
        synchronized (CRAWLER_CACHE) {
            Map<String, Object> cache = CRAWLER_CACHE.get(this);
            if (cache == null) {
                cache = new HashMap<String, Object>();
                CRAWLER_CACHE.put(this, cache);
            }
            return cache.put(key, value);
        }
    }

    protected LinkCrawlerGeneration getValidLinkCrawlerGeneration() {
        synchronized (linkCrawlerGeneration) {
            LinkCrawlerGeneration ret = linkCrawlerGeneration.get();
            if (ret == null || !ret.isValid()) {
                ret = new LinkCrawlerGeneration();
                linkCrawlerGeneration.set(ret);
            }
            return ret;
        }
    }

    protected LinkCrawlerGeneration getCurrentLinkCrawlerGeneration() {
        synchronized (linkCrawlerGeneration) {
            final LinkCrawlerGeneration ret = linkCrawlerGeneration.get();
            return ret;
        }
    }

    public class LinkCrawlerTask {
        private final AtomicBoolean         runningFlag = new AtomicBoolean(true);
        private final LinkCrawlerGeneration generation;
        private final LinkCrawler           crawler;
        private final String                taskID;

        public final LinkCrawler getCrawler() {
            return crawler;
        }

        protected LinkCrawlerTask(LinkCrawler linkCrawler, LinkCrawlerGeneration generation, String taskID) {
            this.generation = generation;
            this.crawler = linkCrawler;
            this.taskID = taskID + ":" + UniqueAlltimeID.next();
        }

        public String getTaskID() {
            return taskID;
        }

        public LinkCrawlerGeneration getLinkCrawlerGeneration() {
            return generation;
        }

        public final boolean isRunning() {
            return runningFlag.get();
        }

        protected final boolean invalidate() {
            return runningFlag.compareAndSet(true, false);
        }
    }

    public class LinkCrawlerGeneration {
        private final AtomicBoolean validFlag = new AtomicBoolean(true);

        public final boolean isValid() {
            return validFlag.get() && LinkCrawler.this.linkCrawlerGeneration.get() == this;
        }

        protected final void invalidate() {
            validFlag.set(false);
        }
    }

    protected LinkCrawlerLock getLinkCrawlerLock(final LazyCrawlerPlugin plugin, final CrawledLink crawledLink) {
        synchronized (LOCKS) {
            LinkCrawlerLock ret = null;
            // find best matching LinkCrawlerLock
            for (final Set<LinkCrawlerLock> locks : LOCKS.values()) {
                for (final LinkCrawlerLock lock : locks) {
                    if (ret != lock && (ret == null || lock.getMaxConcurrency() < ret.getMaxConcurrency() && lock.matches(plugin, crawledLink))) {
                        ret = lock;
                    }
                }
            }
            if (ret == null && LinkCrawlerLock.requiresLocking(plugin)) {
                // create new LinkCrawlerLock
                ret = new LinkCrawlerLock(plugin);
            }
            if (ret != null) {
                // share LinkCrawlerLock to all LinkCrawler roots
                for (final LinkCrawler linkCrawler : LOCKS.keySet()) {
                    addSequentialLockObject(linkCrawler.getRoot(), ret);
                }
            }
            return ret;
        }
    }

    protected void addSequentialLockObject(final LinkCrawler linkCrawler, final LinkCrawlerLock lock) {
        if (lock == null) {
            return;
        }
        synchronized (LOCKS) {
            final LinkCrawler root = linkCrawler != null ? linkCrawler : getRoot();
            Set<LinkCrawlerLock> locks = LOCKS.get(root);
            if (locks == null) {
                locks = new HashSet<LinkCrawlerLock>();
                LOCKS.put(root, locks);
            }
            locks.add(lock);
        }
    }

    public void addSequentialLockObject(final LinkCrawlerLock lock) {
        addSequentialLockObject(getRoot(), lock);
    }

    public UniqueAlltimeID getUniqueAlltimeID() {
        return uniqueAlltimeID;
    }

    public LinkCrawler getParent() {
        return parentCrawler;
    }

    private final static LinkCrawlerConfig CONFIG = JsonConfig.create(LinkCrawlerConfig.class);

    public static LinkCrawlerConfig getConfig() {
        return CONFIG;
    }

    public void setDirectHTTPPermission(DirectHTTPPermission directHTTPPermission) {
        if (directHTTPPermission == null) {
            this.directHTTPPermission = DirectHTTPPermission.ALWAYS;
        } else {
            this.directHTTPPermission = directHTTPPermission;
        }
    }

    protected final static ScheduledExecutorService TIMINGQUEUE = DelayedRunnable.getNewScheduledExecutorService();

    public boolean isDoDuplicateFinderFinalCheck() {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.isDoDuplicateFinderFinalCheck();
        } else {
            return doDuplicateFinderFinalCheck;
        }
    }

    protected Long getDefaultAverageRuntime() {
        return null;
    }

    public static int getMaxThreads() {
        return MAX_THREADS;
    }

    static {
        MAX_THREADS = Math.max(CONFIG.getMaxThreads(), 1);
        final int keepAlive = Math.max(CONFIG.getThreadKeepAlive(), 100);
        /**
         * PriorityBlockingQueue leaks last Item for some java versions
         *
         * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7161229
         */
        threadPool = new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, keepAlive, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(100, new Comparator<Runnable>() {
            public int compare(Runnable o1, Runnable o2) {
                if (o1 == o2) {
                    return 0;
                }
                long l1 = ((LinkCrawlerRunnable) o1).getAverageRuntime();
                long l2 = ((LinkCrawlerRunnable) o2).getAverageRuntime();
                return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
            }
        }), new ThreadFactory() {
            public Thread newThread(Runnable r) {
                /*
                 * our thread factory so we have logger,browser settings available
                 */
                return new LinkCrawlerThread(r);
            }
        }, new ThreadPoolExecutor.AbortPolicy());
        threadPool.allowCoreThreadTimeOut(true);
    }
    private final static LinkCrawlerEventSender EVENTSENDER = new LinkCrawlerEventSender();

    public static LinkCrawlerEventSender getGlobalEventSender() {
        return EVENTSENDER;
    }

    public static LinkCrawler newInstance() {
        return newInstance(null, null);
    }

    public static LinkCrawler newInstance(final Boolean connectParent, final Boolean avoidDuplicates) {
        final LinkCrawler lc;
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            final LinkCrawlerThread thread = (LinkCrawlerThread) (Thread.currentThread());
            final Object owner = thread.getCurrentOwner();
            final CrawledLink source;
            if (owner instanceof PluginForDecrypt) {
                source = ((PluginForDecrypt) owner).getCurrentLink();
            } else if (owner instanceof PluginsC) {
                source = ((PluginsC) owner).getCurrentLink();
            } else {
                source = null;
            }
            final LinkCrawler parent = thread.getCurrentLinkCrawler();
            lc = new LinkCrawler(connectParent == null ? false : connectParent.booleanValue(), avoidDuplicates == null ? false : avoidDuplicates.booleanValue()) {
                @Override
                protected void attachLinkCrawler(final LinkCrawler linkCrawler) {
                    if (linkCrawler != null && linkCrawler != this) {
                        if (parent != null) {
                            parent.attachLinkCrawler(linkCrawler);
                        }
                        synchronized (children) {
                            children.put(linkCrawler, Boolean.TRUE);
                        }
                    }
                }

                @Override
                protected CrawledLink crawledLinkFactorybyURL(final CharSequence url) {
                    final CrawledLink ret;
                    if (parent != null) {
                        ret = parent.crawledLinkFactorybyURL(url);
                    } else {
                        ret = new CrawledLink(url);
                    }
                    if (source != null) {
                        ret.setSourceLink(source);
                    }
                    return ret;
                }

                @Override
                protected boolean distributeCrawledLink(CrawledLink crawledLink) {
                    return crawledLink != null && crawledLink.getSourceUrls() == null;
                }

                @Override
                protected void postprocessFinalCrawledLink(CrawledLink link) {
                }
            };
            parent.attachLinkCrawler(lc);
        } else {
            lc = new LinkCrawler(connectParent == null ? true : connectParent.booleanValue(), avoidDuplicates == null ? true : avoidDuplicates.booleanValue());
        }
        return lc;
    }

    protected PluginClassLoaderChild getPluginClassLoaderChild() {
        return classLoader;
    }

    public boolean addLinkCrawlerRule(final LinkCrawlerRule newRule) {
        if (newRule == null) {
            return false;
        }
        boolean refresh = false;
        try {
            synchronized (LINKCRAWLERRULESLOCK) {
                List<LinkCrawlerRuleStorable> existingRules = CONFIG.getLinkCrawlerRules();
                if (existingRules == null) {
                    existingRules = new ArrayList<LinkCrawlerRuleStorable>();
                } else {
                    for (final LinkCrawlerRuleStorable existingRule : existingRules) {
                        if (existingRule.getId() == newRule.getId() || (existingRule.getRule() == newRule.getRule() && StringUtils.equals(existingRule.getPattern(), newRule.getPattern()))) {
                            return false;
                        }
                    }
                }
                existingRules.add(new LinkCrawlerRuleStorable(newRule));
                CONFIG.setLinkCrawlerRules(existingRules);
                refresh = true;
                return true;
            }
        } finally {
            if (refresh) {
                synchronized (linkCrawlerRules) {
                    linkCrawlerRules.set(null);
                }
            }
        }
    }

    private static final Object LINKCRAWLERRULESLOCK = new Object();

    public void updateLinkCrawlerRule(final LinkCrawlerRule updateRule) {
        boolean refresh = false;
        try {
            synchronized (LINKCRAWLERRULESLOCK) {
                final List<LinkCrawlerRuleStorable> existingRules = CONFIG.getLinkCrawlerRules();
                if (existingRules != null) {
                    for (final LinkCrawlerRuleStorable existingRule : existingRules) {
                        if (existingRule.getId() == updateRule.getId()) {
                            existingRule._set(updateRule);
                            CONFIG.setLinkCrawlerRules(new ArrayList<LinkCrawlerRuleStorable>(existingRules));
                            refresh = true;
                            return;
                        }
                    }
                }
            }
        } finally {
            if (refresh) {
                synchronized (linkCrawlerRules) {
                    linkCrawlerRules.set(null);
                }
            }
        }
    }

    public static LinkCrawlerRule getLinkCrawlerRule(final long ruleID) {
        synchronized (LINKCRAWLERRULESLOCK) {
            final List<LinkCrawlerRuleStorable> rules = CONFIG.getLinkCrawlerRules();
            if (rules == null || rules.size() == 0) {
                return null;
            } else {
                for (final LinkCrawlerRuleStorable rule : rules) {
                    if (rule.getId() == ruleID) {
                        return rule;
                    }
                }
                return null;
            }
        }
    }

    protected List<LinkCrawlerRule> listLinkCrawlerRules() {
        final ArrayList<LinkCrawlerRule> ret = new ArrayList<LinkCrawlerRule>();
        if (CONFIG.isLinkCrawlerRulesEnabled()) {
            synchronized (LINKCRAWLERRULESLOCK) {
                final List<LinkCrawlerRuleStorable> rules = CONFIG.getLinkCrawlerRules();
                if (rules != null) {
                    for (final LinkCrawlerRuleStorable rule : rules) {
                        try {
                            if (rule.isEnabled()) {
                                ret.add(rule);
                            }
                        } catch (final Throwable e) {
                            LogController.CL().log(e);
                        }
                    }
                }
            }
        }
        return ret;
    }

    protected void attachLinkCrawler(final LinkCrawler linkCrawler) {
        if (linkCrawler == null) {
            return;
        } else if (linkCrawler == this) {
            return;
        }
        final LinkCrawler parent = getParent();
        if (parent != null) {
            parent.attachLinkCrawler(linkCrawler);
        }
        synchronized (children) {
            children.put(linkCrawler, Boolean.TRUE);
        }
    }

    protected final AtomicReference<LazyHostPlugin> lazyDirect = new AtomicReference<LazyHostPlugin>();

    protected LazyHostPlugin getDirectHTTPPlugin() {
        if (parentCrawler != null) {
            return parentCrawler.getDirectHTTPPlugin();
        } else {
            LazyHostPlugin ret = lazyDirect.get();
            if (ret == null) {
                ret = HostPluginController.getInstance().get(DIRECT_HTTP);
                lazyDirect.set(ret);
            }
            return ret;
        }
    }

    protected final AtomicReference<LazyHostPlugin> lazyHttp = new AtomicReference<LazyHostPlugin>();

    protected LazyHostPlugin getGenericHttpPlugin() {
        if (parentCrawler != null) {
            return parentCrawler.getGenericHttpPlugin();
        } else {
            LazyHostPlugin ret = lazyHttp.get();
            if (ret == null) {
                ret = HostPluginController.getInstance().get(HTTP_LINKS);
                lazyHttp.set(ret);
            }
            return ret;
        }
    }

    protected final AtomicReference<LazyHostPlugin> lazyFtp = new AtomicReference<LazyHostPlugin>();

    protected LazyHostPlugin getGenericFtpPlugin() {
        if (parentCrawler != null) {
            return parentCrawler.getGenericFtpPlugin();
        } else {
            LazyHostPlugin ret = lazyFtp.get();
            if (ret == null) {
                ret = HostPluginController.getInstance().get("ftp");
                lazyFtp.set(ret);
            }
            return ret;
        }
    }

    protected final AtomicReference<LazyCrawlerPlugin> lazyDeepDecryptHelper = new AtomicReference<LazyCrawlerPlugin>();

    protected LazyCrawlerPlugin getDeepCrawlingPlugin() {
        if (parentCrawler != null) {
            return parentCrawler.getDeepCrawlingPlugin();
        } else {
            LazyCrawlerPlugin ret = lazyDeepDecryptHelper.get();
            if (ret == null) {
                final List<LazyCrawlerPlugin> lazyCrawlerPlugins = getSortedLazyCrawlerPlugins();
                final ListIterator<LazyCrawlerPlugin> it = lazyCrawlerPlugins.listIterator();
                while (it.hasNext()) {
                    final LazyCrawlerPlugin pDecrypt = it.next();
                    if (StringUtils.equals("linkcrawlerdeephelper", pDecrypt.getDisplayName())) {
                        lazyDeepDecryptHelper.set(pDecrypt);
                        return pDecrypt;
                    }
                }
            }
            return ret;
        }
    }

    protected final AtomicReference<LazyCrawlerPlugin> lazyGenericHttpDirectoryCrawlerPlugin = new AtomicReference<LazyCrawlerPlugin>();

    protected LazyCrawlerPlugin getLazyGenericHttpDirectoryCrawlerPlugin() {
        if (parentCrawler != null) {
            return parentCrawler.getLazyGenericHttpDirectoryCrawlerPlugin();
        } else {
            final LazyCrawlerPlugin ret = lazyGenericHttpDirectoryCrawlerPlugin.get();
            if (ret == null) {
                final List<LazyCrawlerPlugin> lazyCrawlerPlugins = getSortedLazyCrawlerPlugins();
                final ListIterator<LazyCrawlerPlugin> it = lazyCrawlerPlugins.listIterator();
                while (it.hasNext()) {
                    final LazyCrawlerPlugin pDecrypt = it.next();
                    if ("httpdirectorycrawler".equals(pDecrypt.getDisplayName())) {
                        lazyGenericHttpDirectoryCrawlerPlugin.set(pDecrypt);
                        return pDecrypt;
                    }
                }
            }
            return ret;
        }
    }

    public LinkCrawler(final boolean connectParentCrawler, final boolean avoidDuplicates) {
        setFilter(defaultFilterFactory());
        final LinkCrawlerThread thread = getCurrentLinkCrawlerThread();
        if (connectParentCrawler && thread != null) {
            /* forward crawlerGeneration from parent to this child */
            this.parentCrawler = thread.getCurrentLinkCrawler();
            this.parentCrawler.attachLinkCrawler(this);
            this.classLoader = parentCrawler.getPluginClassLoaderChild();
            this.directHTTPPermission = parentCrawler.directHTTPPermission;
            this.defaultDownloadFolder = parentCrawler.defaultDownloadFolder;
            this.duplicateFinderContainer = parentCrawler.duplicateFinderContainer;
            this.duplicateFinderCrawler = parentCrawler.duplicateFinderCrawler;
            this.duplicateFinderFinal = parentCrawler.duplicateFinderFinal;
            this.duplicateFinderDeep = parentCrawler.duplicateFinderDeep;
            this.loopPreventionEmbedded = parentCrawler.loopPreventionEmbedded;
            setHandler(parentCrawler.getHandler());
            setDeepInspector(parentCrawler.getDeepInspector());
        } else {
            duplicateFinderContainer = new HashMap<String, Object>();
            duplicateFinderCrawler = new HashMap<LazyCrawlerPlugin, Set<String>>();
            duplicateFinderFinal = new HashMap<String, CrawledLink>();
            duplicateFinderDeep = new HashMap<String, Object>();
            loopPreventionEmbedded = new HashMap<CrawledLink, Object>();
            setHandler(defaultHandlerFactory());
            setDeepInspector(defaultDeepInspector());
            defaultDownloadFolder = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
            parentCrawler = null;
            classLoader = PluginClassLoader.getInstance().getChild();
        }
        this.created = System.currentTimeMillis();
        this.doDuplicateFinderFinalCheck = avoidDuplicates;
    }

    public long getCreated() {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.getCreated();
        }
        return created;
    }

    protected CrawledLink crawledLinkFactorybyURL(final CharSequence url) {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.crawledLinkFactorybyURL(url);
        } else {
            return new CrawledLink(url);
        }
    }

    protected CrawledLink crawledLinkFactorybyDownloadLink(final DownloadLink link) {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.crawledLinkFactorybyDownloadLink(link);
        } else {
            return new CrawledLink(link);
        }
    }

    protected CrawledLink crawledLinkFactorybyCryptedLink(final CryptedLink link) {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.crawledLinkFactorybyCryptedLink(link);
        } else {
            return new CrawledLink(link);
        }
    }

    protected CrawledLink crawledLinkFactory(final Object link) {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.crawledLinkFactory(link);
        } else {
            if (link instanceof DownloadLink) {
                return crawledLinkFactorybyDownloadLink((DownloadLink) link);
            } else if (link instanceof CryptedLink) {
                return crawledLinkFactorybyCryptedLink((CryptedLink) link);
            } else if (link instanceof CharSequence) {
                return crawledLinkFactorybyURL((CharSequence) link);
            } else {
                throw new IllegalArgumentException("Unsupported:" + link);
            }
        }
    }

    public void crawl(String text) {
        crawl(text, null, false);
    }

    private volatile Set<String> crawlerPluginBlacklist = new HashSet<String>();
    private volatile Set<String> hostPluginBlacklist    = new HashSet<String>();

    public void setCrawlerPluginBlacklist(String[] list) {
        HashSet<String> lcrawlerPluginBlacklist = new HashSet<String>();
        if (list != null) {
            for (String s : list) {
                lcrawlerPluginBlacklist.add(s);
            }
        }
        this.crawlerPluginBlacklist = lcrawlerPluginBlacklist;
    }

    public boolean isBlacklisted(LazyCrawlerPlugin plugin) {
        final LinkCrawler parent = getParent();
        if (parent != null && parent.isBlacklisted(plugin)) {
            return true;
        }
        return crawlerPluginBlacklist.contains(plugin.getDisplayName());
    }

    public boolean isBlacklisted(LazyHostPlugin plugin) {
        final LinkCrawler parent = getParent();
        if (parent != null && parent.isBlacklisted(plugin)) {
            return true;
        }
        return hostPluginBlacklist.contains(plugin.getDisplayName());
    }

    public void setHostPluginBlacklist(String[] list) {
        final HashSet<String> lhostPluginBlacklist = new HashSet<String>();
        if (list != null) {
            for (String s : list) {
                lhostPluginBlacklist.add(s);
            }
        }
        this.hostPluginBlacklist = lhostPluginBlacklist;
    }

    public void crawl(final String text, final String url, final boolean allowDeep) {
        final LinkCrawlerTask task;
        if (StringUtils.isEmpty(text)) {
            return;
        }
        final LinkCrawlerGeneration generation = getValidLinkCrawlerGeneration();
        if ((task = checkStartNotify(generation, "crawlText")) == null) {
            return;
        }
        try {
            if (insideCrawlerPlugin()) {
                final List<CrawledLink> links = find(generation, null, text, url, allowDeep, true);
                crawl(generation, links);
            } else {
                final LinkCrawlerTask innerTask;
                if ((innerTask = checkStartNotify(generation, task.getTaskID() + "|crawlTextPool")) != null) {
                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                        @Override
                        public long getAverageRuntime() {
                            final Long ret = getDefaultAverageRuntime();
                            if (ret != null) {
                                return ret;
                            } else {
                                return super.getAverageRuntime();
                            }
                        }

                        @Override
                        void crawling() {
                            final java.util.List<CrawledLink> links = find(generation, null, text, url, allowDeep, true);
                            crawl(generation, links);
                        }
                    });
                }
            }
        } finally {
            checkFinishNotify(task);
        }
    }

    public List<CrawledLink> find(final LinkCrawlerGeneration generation, final CrawledLink source, String text, String baseURL, final boolean allowDeep, final boolean allowInstantCrawl) {
        final CrawledLink baseLink;
        if (StringUtils.isNotEmpty(baseURL)) {
            baseLink = crawledLinkFactorybyURL(baseURL);
        } else {
            baseLink = null;
        }
        final HtmlParserResultSet resultSet;
        if (allowInstantCrawl && getCurrentLinkCrawlerThread() != null && generation != null) {
            resultSet = new HtmlParserResultSet() {
                private final HashSet<HtmlParserCharSequence> fastResults = new HashSet<HtmlParserCharSequence>();

                @Override
                public boolean add(HtmlParserCharSequence e) {
                    if (!generation.isValid()) {
                        throw new RuntimeException("Abort");
                    } else {
                        final boolean ret = super.add(e);
                        if (ret && (!e.contains("...") && ((getBaseURL() != null && !e.equals(getBaseURL())) || Boolean.TRUE.equals(isSkipBaseURL())))) {
                            fastResults.add(e);
                            final CrawledLink crawledLink;
                            if (true || e.getRetainedLength() > 10) {
                                crawledLink = crawledLinkFactorybyURL(e.toURL());
                            } else {
                                crawledLink = crawledLinkFactorybyURL(e.toCharSequenceURL());
                            }
                            crawledLink.setCrawlDeep(allowDeep);
                            if (crawledLink.getSourceLink() == null) {
                                crawledLink.setSourceLink(baseLink);
                            }
                            final ArrayList<CrawledLink> crawledLinks = new ArrayList<CrawledLink>(1);
                            crawledLinks.add(crawledLink);
                            crawl(generation, crawledLinks);
                        }
                        return ret;
                    }
                };

                private LogSource logger = null;

                @Override
                public LogInterface getLogger() {
                    if (logger == null) {
                        logger = LogController.getInstance().getClassLogger(LinkCrawler.class);
                    }
                    return logger;
                }

                @Override
                protected Collection<String> exportResults() {
                    final ArrayList<String> ret = new ArrayList<String>();
                    outerLoop: for (final HtmlParserCharSequence result : this.getResults()) {
                        if (!fastResults.contains(result)) {
                            final int index = result.indexOf("...");
                            if (index > 0) {
                                final HtmlParserCharSequence check = result.subSequence(0, index);
                                for (final HtmlParserCharSequence fastResult : fastResults) {
                                    if (fastResult.startsWith(check) && result != fastResult && !fastResult.contains("...")) {
                                        continue outerLoop;
                                    }
                                }
                            }
                            ret.add(result.toURL());
                        }
                    }
                    return ret;
                }
            };
        } else {
            resultSet = new HtmlParserResultSet() {
                private LogSource logger = null;

                @Override
                public boolean add(HtmlParserCharSequence e) {
                    if (!generation.isValid()) {
                        throw new RuntimeException("Abort");
                    } else {
                        return super.add(e);
                    }
                }

                @Override
                public LogInterface getLogger() {
                    if (logger == null) {
                        logger = LogController.getInstance().getClassLogger(LinkCrawler.class);
                    }
                    return logger;
                }
            };
        }
        final LinkCrawlerTask task = checkStartNotify(generation, "find");
        if (task == null) {
            return null;
        }
        try {
            final String[] possibleLinks = HTMLParser.getHttpLinks(preprocessFind(text, baseURL, allowDeep), baseURL, resultSet);
            if (possibleLinks == null || possibleLinks.length == 0) {
                return null;
            }
            final List<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(possibleLinks.length);
            for (final String possibleLink : possibleLinks) {
                final CrawledLink crawledLink = crawledLinkFactorybyURL(possibleLink);
                crawledLink.setCrawlDeep(allowDeep);
                if (crawledLink.getSourceLink() == null) {
                    crawledLink.setSourceLink(baseLink);
                }
                possibleCryptedLinks.add(crawledLink);
            }
            return possibleCryptedLinks;
        } catch (final RuntimeException e) {
            if (generation.isValid()) {
                resultSet.getLogger().log(e);
            }
        } finally {
            checkFinishNotify(task);
        }
        return null;
    }

    public String preprocessFind(final String in, String url, final boolean allowDeep) {
        if (in == null) {
            return null;
        }
        String out = in.replaceAll("(?i)/\\s*Sharecode\\[\\?\\]:\\s*/", "/");
        out = out.replaceAll("(?i)\\s*Sharecode\\[\\?\\]:\\s*", "");
        out = out.replaceAll("(?i)/?\\s*Sharecode:\\s*/?", "/");
        // fix <a href="https://mega.nz/folder/XYZ">https://mega.nz/folder/XYZ</a>#KEY
        out = out.replaceAll("(?i)<a[^>]href\\s*=\\s*\"(.*?)\"[^>]*>\\s*\\1\\s*</a>\\s*(#[^< ]*)", "<a href=\"$1$2\"</a>");
        return out;
    }

    public void crawl(final List<CrawledLink> possibleCryptedLinks) {
        crawl(getValidLinkCrawlerGeneration(), possibleCryptedLinks);
    }

    protected void crawl(final LinkCrawlerGeneration generation, final List<CrawledLink> possibleCryptedLinks) {
        final LinkCrawlerTask task;
        if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) {
            return;
        } else if ((task = checkStartNotify(generation, "crawlLinks")) == null) {
            return;
        }
        try {
            if (insideCrawlerPlugin()) {
                /*
                 * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin waiting
                 * for linkcrawler results
                 */
                distribute(generation, possibleCryptedLinks);
            } else {
                /*
                 * enqueue this cryptedLink for decrypting
                 */
                final LinkCrawlerTask innerTask;
                if ((innerTask = checkStartNotify(generation, task.getTaskID() + "|crawlLinksPool")) != null) {
                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                        @Override
                        public long getAverageRuntime() {
                            final Long ret = getDefaultAverageRuntime();
                            if (ret != null) {
                                return ret;
                            } else {
                                return super.getAverageRuntime();
                            }
                        }

                        @Override
                        void crawling() {
                            distribute(generation, possibleCryptedLinks);
                        }
                    });
                }
            }
        } finally {
            checkFinishNotify(task);
        }
    }

    public static boolean insideCrawlerPlugin() {
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            final Object owner = ((LinkCrawlerThread) Thread.currentThread()).getCurrentOwner();
            if (owner != null && owner instanceof PluginForDecrypt) {
                return true;
            }
        }
        return false;
    }

    public static boolean insideHosterPlugin() {
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            final Object owner = ((LinkCrawlerThread) Thread.currentThread()).getCurrentOwner();
            if (owner != null && owner instanceof PluginForHost) {
                return true;
            }
        }
        return false;
    }

    /*
     * check if all known crawlers are done and notify all waiting listener + cleanup DuplicateFinder
     */
    protected static void checkFinishNotify(final LinkCrawlerTask task) {
        if (task == null) {
            return;
        } else if (!task.invalidate()) {
            return;
        }
        final LinkCrawler linkCrawler = task.getCrawler();
        /* this LinkCrawler instance stopped, notify static counter */
        final boolean finished;
        final boolean stopped;
        synchronized (CRAWLER) {
            linkCrawler.tasks.remove(task);
            final boolean crawling = linkCrawler.tasks.size() > 0;
            if (crawling) {
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    System.out.println("LinkCrawler:checkFinishNotify:" + linkCrawler + "|Task:(" + task.getTaskID() + ")|Crawling:" + linkCrawler.tasks.size());
                }
                return;
            } else {
                stopped = CRAWLER.remove(linkCrawler);
                finished = CRAWLER.size() == 0;
            }
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                System.out.println("LinkCrawler:checkFinishNotify:" + linkCrawler + "|Task:(" + task.getTaskID() + ")|Stopped:" + stopped + "|Finished:" + finished);
            }
        }
        if (stopped) {
            synchronized (WAIT) {
                WAIT.notifyAll();
            }
            if (linkCrawler.getParent() == null) {
                linkCrawler.cleanup();
            }
            EVENTSENDER.fireEvent(new LinkCrawlerEvent(linkCrawler, LinkCrawlerEvent.Type.STOPPED));
            linkCrawler.crawlerStopped();
        }
        if (finished) {
            synchronized (WAIT) {
                WAIT.notifyAll();
            }
            linkCrawler.cleanup();
            EVENTSENDER.fireEvent(new LinkCrawlerEvent(linkCrawler, LinkCrawlerEvent.Type.FINISHED));
            linkCrawler.crawlerFinished();
        }
    }

    protected void cleanup() {
        /*
         * all tasks are done , we can now cleanup our duplicateFinder
         */
        synchronized (duplicateFinderContainer) {
            duplicateFinderContainer.clear();
        }
        synchronized (duplicateFinderCrawler) {
            duplicateFinderCrawler.clear();
        }
        synchronized (duplicateFinderFinal) {
            duplicateFinderFinal.clear();
        }
        synchronized (duplicateFinderDeep) {
            duplicateFinderDeep.clear();
        }
        synchronized (loopPreventionEmbedded) {
            loopPreventionEmbedded.clear();
        }
    }

    protected void crawlerStopped() {
        synchronized (CRAWLER_CACHE) {
            CRAWLER_CACHE.remove(this);
        }
    }

    protected void crawlerStarted() {
    }

    protected void crawlerFinished() {
    }

    protected LinkCrawlerTask checkStartNotify(final LinkCrawlerGeneration generation, final String taskID) {
        if (generation == null) {
            return null;
        } else if (!generation.isValid()) {
            return null;
        }
        final LinkCrawlerTask task = new LinkCrawlerTask(this, generation, taskID);
        boolean event;
        synchronized (CRAWLER) {
            final LinkCrawler linkCrawler = task.getCrawler();
            final boolean start = linkCrawler.tasks.size() == 0;
            linkCrawler.tasks.add(task);
            event = CRAWLER.add(linkCrawler) && start;
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                System.out.println("LinkCrawler:checkStartNotify:" + linkCrawler + "|Task:(" + task.getTaskID() + ")|Start:" + start + "|Crawler:" + event + "|Crawling:" + linkCrawler.tasks.size());
            }
        }
        try {
            if (event) {
                EVENTSENDER.fireEvent(new LinkCrawlerEvent(task.getCrawler(), LinkCrawlerEvent.Type.STARTED));
                task.getCrawler().crawlerStarted();
            }
        } catch (RuntimeException e) {
            checkFinishNotify(task);
            throw e;
        }
        return task;
    }

    protected interface LazyCrawlerPluginInvokation<T> {
        public T invoke(PluginForDecrypt plugin) throws Exception;
    }

    protected <T> T invokeLazyCrawlerPlugin(final LinkCrawlerGeneration generation, LogInterface logger, final LazyCrawlerPlugin lazyC, final CrawledLink link, final LazyCrawlerPluginInvokation<T> invoker) throws Exception {
        final LinkCrawlerTask task = checkStartNotify(generation, "invokeLazyCrawlerPlugin:" + lazyC + "|" + link.getURL());
        if (task == null) {
            return null;
        }
        try {
            final boolean newLogger = logger == null;
            final PluginForDecrypt wplg = lazyC.newInstance(getPluginClassLoaderChild());
            final AtomicReference<LinkCrawler> nextLinkCrawler = new AtomicReference<LinkCrawler>(this);
            wplg.setBrowser(wplg.createNewBrowserInstance());
            if (logger != null) {
                logger = LogController.getFastPluginLogger(wplg.getCrawlerLoggerID(link));
            }
            wplg.setLogger(logger);
            wplg.init();
            LogInterface oldLogger = null;
            boolean oldVerbose = false;
            boolean oldDebug = false;
            /* now we run the plugin and let it find some links */
            final LinkCrawlerThread lct = getCurrentLinkCrawlerThread();
            Object owner = null;
            LinkCrawler previousCrawler = null;
            try {
                if (lct != null) {
                    /* mark thread to be used by decrypter plugin */
                    owner = lct.getCurrentOwner();
                    lct.setCurrentOwner(wplg);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                    /* save old logger/states */
                    oldLogger = lct.getLogger();
                    oldDebug = lct.isDebug();
                    oldVerbose = lct.isVerbose();
                    /* set new logger and set verbose/debug true */
                    lct.setLogger(logger);
                    lct.setVerbose(true);
                    lct.setDebug(true);
                }
                final long startTime = System.currentTimeMillis();
                try {
                    wplg.setCrawler(this);
                    wplg.setLinkCrawlerGeneration(generation);
                    wplg.setCurrentLink(link);
                    final LinkCrawler pluginNextLinkCrawler = wplg.getCustomNextCrawler();
                    if (pluginNextLinkCrawler != null) {
                        nextLinkCrawler.set(pluginNextLinkCrawler);
                    }
                    return invoker.invoke(wplg);
                } finally {
                    wplg.clean();
                    wplg.setLinkCrawlerGeneration(null);
                    wplg.setCurrentLink(null);
                    final long endTime = System.currentTimeMillis() - startTime;
                    lazyC.updateCrawlRuntime(endTime);
                }
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setCurrentOwner(owner);
                    lct.setCurrentLinkCrawler(previousCrawler);
                    lct.setLogger(oldLogger);
                    lct.setVerbose(oldVerbose);
                    lct.setDebug(oldDebug);
                }
                /* close the logger */
                if (newLogger && logger instanceof ClosableLogInterface) {
                    ((ClosableLogInterface) logger).close();
                }
            }
        } finally {
            /* restore old ClassLoader for current Thread */
            checkFinishNotify(task);
        }
    }

    protected URLConnectionAdapter openCrawlDeeperConnection(final LinkCrawlerGeneration generation, final LogInterface logger, final LinkCrawlerRule matchingRule, final Browser br, final CrawledLink link) throws Exception {
        final LazyCrawlerPlugin lazyC = getDeepCrawlingPlugin();
        if (lazyC == null) {
            throw new UpdateRequiredClassNotFoundException("could not find 'LinkCrawlerDeepHelper' crawler plugin");
        } else {
            return invokeLazyCrawlerPlugin(generation, logger, lazyC, link, new LazyCrawlerPluginInvokation<URLConnectionAdapter>() {
                @Override
                public URLConnectionAdapter invoke(PluginForDecrypt plugin) throws Exception {
                    plugin.setBrowser(br);
                    return ((LinkCrawlerDeepHelperInterface) plugin).openConnection(matchingRule, br, link);
                }
            });
        }
    }

    protected boolean isCrawledLinkDuplicated(final Map<String, Object> map, CrawledLink link) {
        if (link instanceof BrowserCrawledLink) {
            return false;
        }
        final String url = link.getURL();
        final String urlDecodedURL = Encoding.urlDecode(url, false);
        final String value;
        if (StringUtils.equals(url, urlDecodedURL)) {
            value = url;
        } else {
            value = urlDecodedURL;
        }
        synchronized (map) {
            if (map.containsKey(value)) {
                return true;
            } else {
                map.put(value, this);
                return false;
            }
        }
    }

    public DownloadLink createDirectHTTPDownloadLink(final Request sourceRequest, final URLConnectionAdapter con) {
        final Request request = con.getRequest();
        Request redirectOrigin = request.getRedirectOrigin();
        while (redirectOrigin != null) {
            final Request nextRedirectOrigin = redirectOrigin.getRedirectOrigin();
            if (nextRedirectOrigin != null) {
                redirectOrigin = nextRedirectOrigin;
            } else {
                break;
            }
        }
        final String startURL;
        if (request instanceof PostRequest) {
            startURL = request.getURL().toExternalForm();
        } else if (sourceRequest != null) {
            // previous URL is leading/redirecting to this download, so let's use this URL instead
            // for example the current URL might expire
            startURL = sourceRequest.getURL().toExternalForm();
        } else if (redirectOrigin != null) {
            startURL = redirectOrigin.getURL().toExternalForm();
        } else {
            startURL = request.getURL().toExternalForm();
        }
        final DownloadLink link = new DownloadLink(null, null, "DirectHTTP", "directhttp://" + startURL, true);
        final String cookie = con.getRequestProperty("Cookie");
        if (StringUtils.isNotEmpty(cookie)) {
            link.setProperty("COOKIES", cookie);
        }
        final long contentLength = con.getCompleteContentLength();
        if (contentLength > 0) {
            if (con.isContentDecoded()) {
                link.setDownloadSize(contentLength);
            } else {
                link.setVerifiedFileSize(contentLength);
            }
        }
        String fileName = null;
        final DispositionHeader dispositionHeader = Plugin.parseDispositionHeader(con);
        if (dispositionHeader != null && StringUtils.isNotEmpty(dispositionHeader.getFilename())) {
            // trust given filename extension via Content-Disposition header
            fileName = dispositionHeader.getFilename();
            if (dispositionHeader.getEncoding() == null) {
                try {
                    fileName = SimpleFTP.BestEncodingGuessingURLDecode(fileName);
                } catch (final IllegalArgumentException ignore) {
                } catch (final UnsupportedEncodingException ignore) {
                } catch (final IOException ignore) {
                }
            }
        }
        if (StringUtils.isEmpty(fileName)) {
            fileName = Plugin.extractFileNameFromURL(con.getRequest().getUrl());
        }
        link.setFinalFileName(fileName);
        /* save filename in property so we can restore in reset case */
        link.setProperty("fixName", fileName);
        link.setAvailable(true);
        final String requestRef = request.getHeaders().getValue(HTTPConstants.HEADER_REQUEST_REFERER);
        if (requestRef != null && !StringUtils.equals(requestRef, request.getURL().toExternalForm())) {
            link.setProperty(PROPERTY_AUTO_REFERER, requestRef);
        }
        if (request instanceof PostRequest) {
            final String postString = ((PostRequest) request).getPostDataString();
            if (postString != null) {
                link.setProperty("post", postString);
            }
        }
        return link;
    }

    public CrawledLink createDirectHTTPCrawledLink(CrawledLink source, Request sourceRequest, URLConnectionAdapter con) {
        final DownloadLink link = createDirectHTTPDownloadLink(sourceRequest, con);
        final CrawledLink directHTTP = crawledLinkFactorybyDownloadLink(link);
        final LinkCrawlerRule rule = source.getMatchingRule();
        if (rule != null) {
            link.setProperty("lcrID", rule.getId());
            directHTTP.setMatchingRule(rule);
        }
        return directHTTP;
    }

    protected static interface DeeperOrMatchingRuleModifier extends CrawledLinkModifier {
        public CrawledLinkModifier getSourceCrawledLinkModifier();
    }

    protected class BrowserCrawledLink extends CrawledLink {
        private final Browser       br;
        private final Request       next;
        private final Request       last;
        private final List<Request> previousRequests = new ArrayList<Request>();

        protected List<Request> getPreviousRequests() {
            return previousRequests;
        }

        protected BrowserCrawledLink(Browser br, final Request nextRequest) {
            this(br, null, nextRequest);
        }

        protected BrowserCrawledLink(Browser br, final List<Request> previousRequests) {
            this(br, previousRequests, null);
        }

        protected BrowserCrawledLink(Browser br, final List<Request> previousRequests, final Request nextRequest) {
            this.br = br;
            last = br.getRequest();
            next = nextRequest;
            if (previousRequests != null) {
                this.previousRequests.addAll(previousRequests);
            }
        }

        @Override
        public boolean isCrawlDeep() {
            return true;
        }

        protected Browser getBrowser() {
            return br;
        }

        @Override
        protected void linkToString(final StringBuilder sb, final Object link) {
            if (link == null) {
                if (next != null) {
                    sb.append("NextRequest:" + next.getUrl());
                } else if (last != null) {
                    sb.append("|Request:" + last.getUrl());
                }
            }
        }

        protected Request getLastRequest() {
            return last;
        }

        protected Request getNextRequest() {
            return next;
        }

        @Override
        public String getURL() {
            if (next != null) {
                return next.getURL().toExternalForm();
            } else {
                return last.getURL().toExternalForm();
            }
        }
    }

    protected void crawlDeeperOrMatchingRule(final LinkCrawlerGeneration generation, final CrawledLink source) {
        final CrawledLinkModifier sourceLinkModifier;
        if (source.getCustomCrawledLinkModifier() instanceof DeeperOrMatchingRuleModifier) {
            CrawledLinkModifier modifier = source.getCustomCrawledLinkModifier();
            while (modifier instanceof DeeperOrMatchingRuleModifier) {
                modifier = ((DeeperOrMatchingRuleModifier) modifier).getSourceCrawledLinkModifier();
            }
            sourceLinkModifier = modifier;
        } else {
            sourceLinkModifier = source.getCustomCrawledLinkModifier();
        }
        source.setCustomCrawledLinkModifier(null);
        source.setBrokenCrawlerHandler(null);
        final LinkCrawlerTask task;
        if (source == null || source.getURL() == null) {
            return;
        } else if (isCrawledLinkDuplicated(duplicateFinderDeep, source)) {
            onCrawledLinkDuplicate(source, DUPLICATE.DEEP);
            return;
        } else if (this.isCrawledLinkFiltered(source)) {
            return;
        } else if ((task = checkStartNotify(generation, "crawlDeeperOrMatchingRule:" + source.getURL())) == null) {
            /* Do nothing */
            return;
        }
        final LinkCrawlerRule matchingRule = source.getMatchingRule();
        final List<CrawledLinkModifier> additionalModifier = new ArrayList<CrawledLinkModifier>();
        final CrawledLinkModifier lm = new DeeperOrMatchingRuleModifier() {
            public CrawledLinkModifier getSourceCrawledLinkModifier() {
                return sourceLinkModifier;
            }

            public boolean modifyCrawledLink(CrawledLink link) {
                final boolean setContainerURL = link.getDownloadLink() != null && link.getDownloadLink().getContainerUrl() == null;
                boolean ret = false;
                if (sourceLinkModifier != null) {
                    if (sourceLinkModifier.modifyCrawledLink(link)) {
                        ret = true;
                    }
                }
                if (setContainerURL) {
                    link.getDownloadLink().setContainerUrl(source.getURL());
                    ret = true;
                }
                for (final CrawledLinkModifier modifier : additionalModifier) {
                    if (modifier.modifyCrawledLink(link)) {
                        ret = true;
                    }
                }
                return ret;
            }
        };
        try {
            Browser br = null;
            final LogInterface logger;
            if (matchingRule != null && matchingRule.isLogging()) {
                logger = LogController.getFastPluginLogger("LinkCrawlerRule." + matchingRule.getId());
            } else {
                logger = LogController.getFastPluginLogger("LinkCrawlerDeep." + CrossSystem.alleviatePathParts(source.getHost()));
            }
            processedLinksCounter.incrementAndGet();
            try {
                if (StringUtils.startsWithCaseInsensitive(source.getURL(), "file:/")) {
                    /* Crawl file contents */
                    // file:/ -> not authority -> all fine
                    // file://xy/ -> xy authority -> java.lang.IllegalArgumentException: URI has an authority component
                    // file:/// -> empty authority -> all fine
                    final String currentURI = source.getURL().replaceFirst("file:///?", "file:///");
                    final File file = new File(new URI(currentURI));
                    if (!file.exists()) {
                        logger.info("FILE: Invalid file path: File does not exist");
                        return;
                    } else if (!file.isFile()) {
                        logger.info("FILE: Invalid file path: File is not a file");
                        return;
                    }
                    final int limit = CONFIG.getDeepDecryptFileSizeLimit();
                    final int readLimit = limit == -1 ? -1 : Math.max(1 * 1024 * 1024, limit);
                    final String fileContent = new String(IO.readFile(file, readLimit), "UTF-8");
                    final List<CrawledLink> fileContentLinks = find(generation, source, fileContent, null, false, false);
                    if (fileContentLinks == null || fileContentLinks.isEmpty()) {
                        logger.info("FILE: Failed to find any results in file: " + file.getAbsolutePath());
                        return;
                    }
                    final String[] sourceURLs = getAndClearSourceURLs(source);
                    final boolean singleDest = fileContentLinks.size() == 1;
                    for (final CrawledLink fileContentLink : fileContentLinks) {
                        forwardCrawledLinkInfos(source, fileContentLink, lm, sourceURLs, singleDest);
                    }
                    crawl(generation, fileContentLinks);
                } else {
                    /* Crawl URL */
                    Request nextRequest = null;
                    if (source instanceof BrowserCrawledLink) {
                        final BrowserCrawledLink brc = (BrowserCrawledLink) source;
                        br = brc.getBrowser().cloneBrowser();
                        nextRequest = brc.getNextRequest();
                    } else {
                        br = new Browser();
                        nextRequest = br.createGetRequest(source.getURL());
                    }
                    br.setLogger(logger);
                    if (matchingRule != null && matchingRule.isLogging()) {
                        /* Enable logging if allowed by MatchingRule. */
                        br.setVerbose(true);
                        br.setDebug(true);
                    }
                    BrowserCrawledLink next = this.openCrawlDeeperConnectionV2(source, matchingRule, br, nextRequest);
                    if (next != null) {
                        forwardCrawledLinkInfos(source, next, lm, getAndClearSourceURLs(source), true);
                        crawl(generation, Arrays.asList(new CrawledLink[] { next }));
                        return;
                    }
                    final CrawledLink deeperSource;
                    final String[] sourceURLs;
                    final String current_url = br._getURL().toExternalForm();
                    final String source_url = source.getURL();
                    if (StringUtils.equals(current_url, source_url) || source instanceof BrowserCrawledLink) {
                        /* Same URL or BrowserCrawledLink */
                        deeperSource = source;
                        sourceURLs = getAndClearSourceURLs(source);
                    } else {
                        /* Different URL */
                        deeperSource = crawledLinkFactorybyURL(current_url);
                        forwardCrawledLinkInfos(source, deeperSource, lm, getAndClearSourceURLs(source), true);
                        sourceURLs = getAndClearSourceURLs(deeperSource);
                    }
                    final LinkCrawlerDeepInspector lDeepInspector = this.getDeepInspector();
                    final List<CrawledLink> inspectedLinks = lDeepInspector.deepInspect(this, generation, br, br.getHttpConnection(), deeperSource);
                    if (inspectedLinks != null) {
                        if (inspectedLinks.size() >= 0) {
                            final boolean singleDest = inspectedLinks.size() == 1;
                            for (final CrawledLink possibleCryptedLink : inspectedLinks) {
                                forwardCrawledLinkInfos(deeperSource, possibleCryptedLink, lm, sourceURLs, singleDest);
                            }
                            crawl(generation, inspectedLinks);
                        }
                        return;
                    }
                    String finalPackageName = null;
                    if (matchingRule != null) {
                        if (matchingRule._getPackageNamePattern() != null) {
                            /* Obtain package name by regex given in rule. */
                            final String packageName = br.getRegex(matchingRule._getPackageNamePattern()).getMatch(0);
                            if (StringUtils.isNotEmpty(packageName)) {
                                finalPackageName = Encoding.htmlDecode(packageName).trim();
                            }
                        }
                        if (matchingRule._getPasswordPattern() != null) {
                            /* Find extract passwords by password pattern of rule */
                            final String[][] matches = br.getRegex(matchingRule._getPasswordPattern()).getMatches();
                            if (matches != null && matches.length > 0) {
                                final HashSet<String> passwords = new HashSet<String>();
                                for (final String matcharray[] : matches) {
                                    for (final String match : matcharray) {
                                        if (StringUtils.isNotEmpty(match)) {
                                            passwords.add(match);
                                        }
                                    }
                                }
                                if (passwords.size() > 0) {
                                    additionalModifier.add(new CrawledLinkModifier() {
                                        @Override
                                        public boolean modifyCrawledLink(CrawledLink link) {
                                            for (final String password : passwords) {
                                                link.getArchiveInfo().addExtractionPassword(password);
                                            }
                                            return true;
                                        }
                                    });
                                }
                                // TODO: Add logger for when password pattern is given but no passwords are found
                            }
                        }
                    }
                    /* Load the webpage and find links on it */
                    if (matchingRule != null && LinkCrawlerRule.RULE.SUBMITFORM.equals(matchingRule.getRule())) {
                        final Pattern formPattern = matchingRule._getFormPattern();
                        if (formPattern == null) {
                            logger.info("SUBMITFORM: Cannot process Form handling because: Form Pattern is null");
                            return;
                        }
                        final Form[] allForms = br.getForms();
                        if (allForms == null || allForms.length == 0) {
                            logger.info("SUBMITFORM: Cannot process Form handling because: There are no forms available");
                            return;
                        }
                        Form targetform = null;
                        int index = 0;
                        for (final Form form : allForms) {
                            if ((StringUtils.isNotEmpty(form.getAction()) && formPattern.matcher(form.getAction()).matches()) || form.containsHTML(formPattern.pattern())) {
                                targetform = form;
                                break;
                            }
                            index++;
                        }
                        if (targetform == null) {
                            logger.info("Failed to find any form matching given Pattern");
                            return;
                        }
                        logger.info("SUBMITFORM: Submitting form from index [" + index + "]");
                        nextRequest = br.createFormRequest(targetform);
                        next = new BrowserCrawledLink(br, nextRequest);
                        forwardCrawledLinkInfos(source, next, lm, getAndClearSourceURLs(source), true);
                        if (finalPackageName != null) {
                            PackageInfo.setName(next, finalPackageName);
                        }
                        crawl(generation, Arrays.asList(new CrawledLink[] { next }));
                        return;
                    }
                    /* Deep-decrypt (generic and via DEEPDECRYPT LinkCrawler rule). */
                    final Request request = br.getRequest();
                    final String brURL = request.getURL().toExternalForm();
                    final List<CrawledLink> possibleCryptedLinks = find(generation, source, brURL, null, false, false);
                    if (possibleCryptedLinks == null) {
                        return;
                    }
                    final boolean singleDest = possibleCryptedLinks.size() == 1;
                    for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                        forwardCrawledLinkInfos(deeperSource, possibleCryptedLink, lm, sourceURLs, singleDest);
                        if (finalPackageName != null) {
                            PackageInfo.setName(possibleCryptedLink, finalPackageName);
                        }
                    }
                    final CrawledLink deepLink;
                    if (singleDest) {
                        deepLink = possibleCryptedLinks.get(0);
                    } else {
                        CrawledLink deep = null;
                        for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                            if (StringUtils.equalsIgnoreCase(possibleCryptedLink.getURL(), source.getURL())) {
                                deep = possibleCryptedLink;
                                break;
                            }
                        }
                        deepLink = deep;
                    }
                    if (deepLink == null) {
                        // TODO: This should never happen
                        return;
                    }
                    final String finalBaseUrl = new Regex(brURL, "(https?://.*?)(\\?|$)").getMatch(0);
                    final boolean deepPatternContent;
                    final List<CrawledLink> possibleDeepCryptedLinks;
                    if (matchingRule != null && matchingRule._getDeepPattern() != null) {
                        /* Crawl links according to pattern of rule. */
                        final String[][] matches = new Regex(request.getHtmlCode(), matchingRule._getDeepPattern()).getMatches();
                        if (matches == null || matches.length == 0) {
                            /*
                             * Users' deep pattern is bad and/or currently processed link is broken/offline and thus we get no results.
                             */
                            if (matchingRule.isLogging()) {
                                final LogInterface ruleLogger = LogController.getFastPluginLogger("LinkCrawlerRule." + matchingRule.getId());
                                ruleLogger.info("Got no matches based on user defined DeepPattern");
                            }
                            return;
                        }
                        final HashSet<String> dups = new HashSet<String>();
                        final StringBuilder sb = new StringBuilder();
                        for (final String matcharray[] : matches) {
                            for (final String match : matcharray) {
                                if (StringUtils.isNotEmpty(match) && !brURL.equals(match) && dups.add(match)) {
                                    if (sb.length() > 0) {
                                        sb.append("\r\n");
                                    }
                                    sb.append(match);
                                    if (match.matches("^[^<>\"]+$")) {
                                        try {
                                            final String url = br.getURL(match).toExternalForm();
                                            if (dups.add(url)) {
                                                sb.append("\r\n").append(url);
                                            }
                                        } catch (final Throwable e) {
                                        }
                                    }
                                }
                            }
                        }
                        deepPatternContent = true;
                        possibleDeepCryptedLinks = find(generation, source, sb.toString(), finalBaseUrl, false, false);
                    } else {
                        deepPatternContent = false;
                        possibleDeepCryptedLinks = find(generation, source, request.getHtmlCode(), finalBaseUrl, false, false);
                    }
                    if (possibleDeepCryptedLinks == null || possibleDeepCryptedLinks.size() == 0) {
                        return;
                    }
                    final boolean singleDeepCryptedDest = possibleDeepCryptedLinks.size() == 1;
                    for (final CrawledLink possibleDeepCryptedLink : possibleDeepCryptedLinks) {
                        forwardCrawledLinkInfos(deeperSource, possibleDeepCryptedLink, lm, sourceURLs, singleDeepCryptedDest);
                        if (finalPackageName != null) {
                            PackageInfo.setName(possibleDeepCryptedLink, finalPackageName);
                        }
                    }
                    if (deepPatternContent && StringUtils.startsWithCaseInsensitive(source.getURL(), deepLink.getURL())) {
                        /*
                         * deepLink is our source and a matching deepPattern, crawl the links directly and don't wait for
                         * UnknownCrawledLinkHandler
                         */
                        crawl(generation, possibleDeepCryptedLinks);
                    } else {
                        /* first check if the url itself can be handled */
                        deepLink.setUnknownHandler(new UnknownCrawledLinkHandler() {
                            @Override
                            public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc) {
                                /* unhandled url, lets parse the content on it */
                                lc.crawl(generation, possibleDeepCryptedLinks);
                            }
                        });
                        crawl(generation, possibleCryptedLinks);
                    }
                }
            } catch (Throwable e) {
                LogController.CL().log(e);
                logger.log(e);
            } finally {
                if (br != null) {
                    br.disconnect();
                }
                if (logger != null && logger instanceof ClosableLogInterface) {
                    ((ClosableLogInterface) logger).close();
                }
            }
        } finally {
            checkFinishNotify(task);
        }
    }

    /** Opens connection */
    protected final BrowserCrawledLink openCrawlDeeperConnectionV2(final CrawledLink source, final LinkCrawlerRule matchingRule, final Browser br, final Request req) throws Exception {
        if (req == null) {
            return null;
        } else if (req.isRequested()) {
            return null;
        }
        br.setFollowRedirects(false);
        final List<Request> previousRequests = new ArrayList<Request>();
        if (!(source instanceof BrowserCrawledLink)) {
            final CrawledLink sourceLink = source != null ? source.getSourceLink() : null;
            // TODO: Remove check for 'http' prefix?
            if (sourceLink != null && StringUtils.startsWithCaseInsensitive(sourceLink.getURL(), "http")) {
                /* Set referer */
                br.setCurrentURL(sourceLink.getURL());
            }
        } else if (source instanceof BrowserCrawledLink) {
            previousRequests.addAll(((BrowserCrawledLink) source).getPreviousRequests());
        }
        if (previousRequests.size() == 0 && matchingRule != null) {
            matchingRule.applyCookies(br, req.getUrl(), false);
        }
        previousRequests.add(req);
        URLConnectionAdapter con = null;
        try {
            final List<AuthenticationFactory> authenticationFactories = AuthenticationController.getInstance().buildAuthenticationFactories(req.getURL(), null);
            authloop: for (AuthenticationFactory authenticationFactory : authenticationFactories) {
                br.setCustomAuthenticationFactory(authenticationFactory);
                con = br.openRequestConnection(req);
                if ((con.getResponseCode() == 401 || con.getResponseCode() == 403) && con.getHeaderField(HTTPConstants.HEADER_RESPONSE_WWW_AUTHENTICATE) != null) {
                    /* Invalid or missing auth */
                    br.followConnection(true);
                    con = null;
                    continue authloop;
                } else {
                    break authloop;
                }
            }
            if (con == null) {
                throw new IOException("could not open connection due to missing/wrong authentication");
            }
            final BrowserCrawledLink next;
            if (req.getLocation() != null) {
                br.followConnection(true);
                final Request nextRequest = br.createRedirectFollowingRequest(br.getRequest());
                next = new BrowserCrawledLink(br, previousRequests, nextRequest);
            } else {
                if (RequestMethod.HEAD.equals(con.getRequestMethod()) || !getDeepInspector().looksLikeDownloadableContent(con)) {
                    br.followConnection(true);
                } else {
                    con.disconnect();
                }
                next = new BrowserCrawledLink(br, previousRequests);
            }
            if (matchingRule != null) {
                if (matchingRule.updateCookies(br, req.getUrl(), false, false)) {
                    updateLinkCrawlerRule(matchingRule);
                }
                next.setMatchingRule(matchingRule);
            }
            return next;
        } finally {
            con.disconnect();
        }
    }

    protected boolean distributeCrawledLink(CrawledLink crawledLink) {
        return crawledLink != null && crawledLink.gethPlugin() == null;
    }

    public boolean canHandle(final LazyPlugin<? extends Plugin> lazyPlugin, final String url, final CrawledLink link) {
        try {
            if (lazyPlugin.canHandle(url)) {
                final Plugin plugin = lazyPlugin.getPrototype(getPluginClassLoaderChild(), false);
                return plugin != null && plugin.canHandle(url);
            }
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return false;
    }

    protected DISTRIBUTE distributePluginForHost(final LazyHostPlugin pluginForHost, final LinkCrawlerGeneration generation, final String url, final CrawledLink link) {
        try {
            if (canHandle(pluginForHost, url, link)) {
                if (isBlacklisted(pluginForHost)) {
                    if (LogController.getInstance().isDebugMode()) {
                        LogController.CL().info("blacklisted! " + pluginForHost);
                    }
                    return DISTRIBUTE.BLACKLISTED;
                }
                if (insideCrawlerPlugin()) {
                    if (!generation.isValid()) {
                        /* LinkCrawler got aborted! */
                        return DISTRIBUTE.STOP;
                    }
                    processHostPlugin(generation, pluginForHost, link);
                } else {
                    final LinkCrawlerTask innerTask = checkStartNotify(generation, "distributePluginForHost:" + pluginForHost + "|" + link.getURL());
                    if (innerTask == null) {
                        /* LinkCrawler got aborted! */
                        return DISTRIBUTE.STOP;
                    }
                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                        @Override
                        public long getAverageRuntime() {
                            final Long ret = getDefaultAverageRuntime();
                            if (ret != null) {
                                return ret;
                            } else {
                                return pluginForHost.getAverageParseRuntime();
                            }
                        }

                        @Override
                        void crawling() {
                            processHostPlugin(generation, pluginForHost, link);
                        }
                    });
                }
                return DISTRIBUTE.NEXT;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return DISTRIBUTE.CONTINUE;
    }

    /**
     * break PluginForDecrypt loop when PluginForDecrypt and PluginForHost listen on same urls
     *
     * @param pDecrypt
     * @param link
     * @return
     */
    public boolean breakPluginForDecryptLoop(final LazyCrawlerPlugin pDecrypt, final CrawledLink link) {
        final boolean canHandle = canHandle(pDecrypt, link.getURL(), link.getSourceLink());
        if (!canHandle) {
            return false;
        } else if (!AvailableLinkState.UNKNOWN.equals(link.getLinkState())) {
            return true;
        }
        CrawledLink source = link.getSourceLink();
        final HashSet<String> dontRetry = new HashSet<String>();
        while (source != null) {
            if (source.getCryptedLink() != null) {
                if (StringUtils.equals(link.getURL(), source.getURL())) {
                    final LazyCrawlerPlugin lazyC = source.getCryptedLink().getLazyC();
                    dontRetry.add(lazyC.getDisplayName() + lazyC.getClassName());
                }
            }
            source = source.getSourceLink();
        }
        final boolean ret = dontRetry.contains(pDecrypt.getDisplayName() + pDecrypt.getClassName());
        return ret;
    }

    protected DISTRIBUTE distributePluginForDecrypt(final LazyCrawlerPlugin pDecrypt, final LinkCrawlerGeneration generation, final String url, final CrawledLink link) {
        try {
            if (canHandle(pDecrypt, url, link)) {
                if (isBlacklisted(pDecrypt)) {
                    if (LogController.getInstance().isDebugMode()) {
                        LogController.CL().info("blacklisted! " + pDecrypt);
                    }
                    return DISTRIBUTE.BLACKLISTED;
                }
                if (breakPluginForDecryptLoop(pDecrypt, link)) {
                    return DISTRIBUTE.CONTINUE;
                }
                final List<CrawledLink> cryptedLinks = new ArrayList<CrawledLink>();
                final DISTRIBUTE result = getCryptedLinks(cryptedLinks, pDecrypt, link, link.getCustomCrawledLinkModifier());
                if (cryptedLinks == null || cryptedLinks.size() == 0) {
                    return result;
                }
                if (insideCrawlerPlugin()) {
                    /*
                     * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin
                     * waiting for linkcrawler results
                     */
                    for (final CrawledLink decryptThis : cryptedLinks) {
                        if (!generation.isValid()) {
                            /* LinkCrawler got aborted! */
                            return DISTRIBUTE.STOP;
                        }
                        crawl(generation, pDecrypt, decryptThis);
                    }
                } else {
                    /*
                     * enqueue these cryptedLinks for decrypting
                     */
                    for (final CrawledLink decryptThis : cryptedLinks) {
                        final LinkCrawlerTask innerTask = checkStartNotify(generation, "distributePluginForDecrypt:" + pDecrypt + "|" + link.getURL() + "|" + decryptThis.getURL());
                        if (innerTask == null) {
                            /* LinkCrawler got aborted! */
                            return DISTRIBUTE.STOP;
                        }
                        threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                            public long getAverageRuntime() {
                                final Long ret = getDefaultAverageRuntime();
                                if (ret != null) {
                                    return ret;
                                } else {
                                    return pDecrypt.getAverageCrawlRuntime();
                                }
                            }

                            @Override
                            protected LinkCrawlerLock getLinkCrawlerLock() {
                                return LinkCrawler.this.getLinkCrawlerLock(pDecrypt, decryptThis);
                            }

                            @Override
                            void crawling() {
                                crawl(generation, pDecrypt, decryptThis);
                            }
                        });
                    }
                }
                return result;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return DISTRIBUTE.CONTINUE;
    }

    protected Boolean distributePluginC(final PluginsC pluginC, final LinkCrawlerGeneration generation, final String url, final CrawledLink link) {
        try {
            if (!pluginC.canHandle(url)) {
                return null;
            }
            final CrawledLinkModifier originalModifier = link.getCustomCrawledLinkModifier();
            final CrawledLinkModifier lm;
            if (pluginC.hideLinks()) {
                final ArrayList<CrawledLinkModifier> modifiers = new ArrayList<CrawledLinkModifier>();
                if (originalModifier != null) {
                    modifiers.add(originalModifier);
                }
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public boolean modifyCrawledLink(CrawledLink link) {
                        /* we hide the links */
                        final DownloadLink dl = link.getDownloadLink();
                        if (dl != null) {
                            dl.setUrlProtection(UrlProtection.PROTECTED_CONTAINER);
                            return true;
                        }
                        return false;
                    }
                });
                lm = new CrawledLinkModifier() {
                    @Override
                    public boolean modifyCrawledLink(CrawledLink link) {
                        boolean ret = false;
                        for (CrawledLinkModifier mod : modifiers) {
                            if (mod.modifyCrawledLink(link)) {
                                ret = true;
                            }
                        }
                        return ret;
                    }
                };
            } else {
                lm = originalModifier;
            }
            final java.util.List<CrawledLink> allPossibleCryptedLinks = getCrawledLinks(pluginC.getSupportedLinks(), link, lm);
            if (allPossibleCryptedLinks == null) {
                return true;
            }
            if (insideCrawlerPlugin()) {
                /*
                 * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin waiting
                 * for linkcrawler results
                 */
                for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                    if (!generation.isValid()) {
                        /* LinkCrawler got aborted! */
                        return false;
                    }
                    container(generation, pluginC, decryptThis);
                }
            } else {
                /*
                 * enqueue these cryptedLinks for decrypting
                 */
                for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                    final LinkCrawlerTask innerTask = checkStartNotify(generation, "distributePluginC:" + pluginC.getName() + "|" + link.getURL() + "|" + decryptThis.getURL());
                    if (innerTask == null) {
                        /* LinkCrawler got aborted! */
                        return false;
                    }
                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                        @Override
                        public long getAverageRuntime() {
                            final Long ret = getDefaultAverageRuntime();
                            if (ret != null) {
                                return ret;
                            } else {
                                return super.getAverageRuntime();
                            }
                        }

                        @Override
                        void crawling() {
                            container(generation, pluginC, decryptThis);
                        }
                    });
                }
            }
            return true;
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return null;
    }

    protected DISTRIBUTE rewrite(final LinkCrawlerGeneration generation, final String url, final CrawledLink source) {
        try {
            final LinkCrawlerRule rule = getFirstMatchingRule(source, url, LinkCrawlerRule.RULE.REWRITE);
            if (rule != null && rule.getRewriteReplaceWith() != null) {
                source.setMatchingRule(rule);
                final String newURL = url.replaceAll(rule.getPattern(), rule.getRewriteReplaceWith());
                final LinkCrawlerTask innerTask;
                if (!url.equals(newURL)) {
                    final CrawledLinkModifier lm = source.getCustomCrawledLinkModifier();
                    source.setCustomCrawledLinkModifier(null);
                    source.setBrokenCrawlerHandler(null);
                    final CrawledLink rewritten = crawledLinkFactorybyURL(newURL);
                    forwardCrawledLinkInfos(source, rewritten, lm, getAndClearSourceURLs(source), true);
                    if (insideCrawlerPlugin()) {
                        if (!generation.isValid()) {
                            /* LinkCrawler got aborted! */
                            return DISTRIBUTE.STOP;
                        }
                        distribute(generation, rewritten);
                        return DISTRIBUTE.NEXT;
                    } else if ((innerTask = checkStartNotify(generation, "rewritePool:" + source.getURL() + "|" + rewritten.getURL())) != null) {
                        threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                            @Override
                            public long getAverageRuntime() {
                                final Long ret = getDefaultAverageRuntime();
                                if (ret != null) {
                                    return ret;
                                } else {
                                    return super.getAverageRuntime();
                                }
                            }

                            @Override
                            void crawling() {
                                distribute(generation, rewritten);
                            }
                        });
                        return DISTRIBUTE.NEXT;
                    } else {
                        return DISTRIBUTE.STOP;
                    }
                }
            }
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return DISTRIBUTE.CONTINUE;
    }

    protected Boolean distributeDeeperOrMatchingRule(final LinkCrawlerGeneration generation, final String url, final CrawledLink link) {
        try {
            LinkCrawlerRule rule = null;
            /* do not change order, it is important to check redirect first */
            if ((rule = getFirstMatchingRule(link, url, LinkCrawlerRule.RULE.SUBMITFORM, LinkCrawlerRule.RULE.FOLLOWREDIRECT, LinkCrawlerRule.RULE.DEEPDECRYPT)) != null || link.isCrawlDeep()) {
                if (rule != null) {
                    link.setMatchingRule(rule);
                }
                /* the link is allowed to crawlDeep */
                if (insideCrawlerPlugin()) {
                    if (!generation.isValid()) {
                        /* LinkCrawler got aborted! */
                        return false;
                    }
                    crawlDeeperOrMatchingRule(generation, link);
                } else {
                    final LinkCrawlerTask innerTask = checkStartNotify(generation, "distributeDeeperOrMatchingRulePool:" + link.getURL());
                    if (innerTask == null) {
                        return false;
                    }
                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                        @Override
                        public long getAverageRuntime() {
                            final Long ret = getDefaultAverageRuntime();
                            if (ret != null) {
                                return ret;
                            } else {
                                return super.getAverageRuntime();
                            }
                        }

                        @Override
                        void crawling() {
                            crawlDeeperOrMatchingRule(generation, link);
                        }
                    });
                }
                return true;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return null;
    }

    protected void distribute(final LinkCrawlerGeneration generation, CrawledLink... possibleCryptedLinks) {
        if (possibleCryptedLinks == null || possibleCryptedLinks.length == 0) {
            return;
        }
        distribute(generation, Arrays.asList(possibleCryptedLinks));
    }

    protected CrawledLink createCopyOf(CrawledLink source) {
        final CrawledLink ret;
        if (source.getDownloadLink() != null) {
            ret = new CrawledLink(source.getDownloadLink());
        } else if (source.getCryptedLink() != null) {
            ret = new CrawledLink(source.getCryptedLink());
        } else {
            ret = new CrawledLink(source.getURL());
        }
        ret.setSourceLink(source);
        if (source.hasCollectingInfo()) {
            ret.setCollectingInfo(source.getCollectingInfo());
        }
        ret.setSourceJob(source.getSourceJob());
        ret.setSourceUrls(source.getSourceUrls());
        ret.setOrigin(source.getOrigin());
        ret.setCrawlDeep(source.isCrawlDeep());
        if (source.hasArchiveInfo()) {
            ret.setArchiveInfo(source.getArchiveInfo());
        }
        ret.setCustomCrawledLinkModifier(source.getCustomCrawledLinkModifier());
        ret.setBrokenCrawlerHandler(source.getBrokenCrawlerHandler());
        ret.setUnknownHandler(source.getUnknownHandler());
        ret.setMatchingFilter(source.getMatchingFilter());
        ret.setMatchingRule(source.getMatchingRule());
        // ret.setCreated(source.getCreated());#set in handleFinalCrawledLink
        ret.setEnabled(source.isEnabled());
        ret.setForcedAutoStartEnabled(source.isForcedAutoStartEnabled());
        ret.setAutoConfirmEnabled(source.isAutoConfirmEnabled());
        ret.setAutoStartEnabled(source.isAutoStartEnabled());
        if (source.isNameSet()) {
            ret.setName(source._getName());
        }
        ret.setDesiredPackageInfo(source.getDesiredPackageInfo());
        ret.setParentNode(source.getParentNode());
        return ret;
    }

    protected boolean distributeFinalCrawledLink(final LinkCrawlerGeneration generation, final CrawledLink crawledLink) {
        if (generation != null && generation.isValid() && crawledLink != null) {
            this.handleFinalCrawledLink(generation, crawledLink);
            return true;
        } else {
            return false;
        }
    }

    protected void distribute(final LinkCrawlerGeneration generation, List<CrawledLink> possibleCryptedLinks) {
        final LinkCrawlerTask task;
        if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) {
            return;
        } else if ((task = checkStartNotify(generation, "distributeLinks")) == null) {
            return;
        }
        try {
            mainloop: for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                if (!generation.isValid()) {
                    /* LinkCrawler got aborted! */
                    return;
                }
                mainloopretry: while (true) {
                    final UnknownCrawledLinkHandler unnknownHandler = possibleCryptedLink.getUnknownHandler();
                    possibleCryptedLink.setUnknownHandler(null);
                    if (!distributeCrawledLink(possibleCryptedLink)) {
                        // direct forward, if we already have a final link.
                        distributeFinalCrawledLink(generation, possibleCryptedLink);
                        continue mainloop;
                    }
                    final String url = possibleCryptedLink.getURL();
                    if (url == null) {
                        /* WTF, no URL?! let's continue */
                        continue mainloop;
                    } else {
                        final DISTRIBUTE ret = rewrite(generation, url, possibleCryptedLink);
                        switch (ret) {
                        case STOP:
                            return;
                        case CONTINUE:
                            break;
                        case BLACKLISTED:
                            continue mainloop;
                        case NEXT:
                            handleUnhandledCryptedLink(possibleCryptedLink);
                            continue mainloop;
                        default:
                            LogController.CL().log(new IllegalStateException(ret.name()));
                            break;
                        }
                    }
                    final boolean isDirect = url.startsWith("directhttp://");
                    final boolean isFtp = url.startsWith("ftp://") || url.startsWith("ftpviajd://");
                    final boolean isFile = url.startsWith("file:/");
                    final boolean isHttpJD = url.startsWith("httpviajd://") || url.startsWith("httpsviajd://");
                    if (isFile) {
                        /*
                         * first we will walk through all available container plugins
                         */
                        for (final PluginsC pCon : ContainerPluginController.getInstance().list()) {
                            final Boolean ret = distributePluginC(pCon, generation, url, possibleCryptedLink);
                            if (Boolean.FALSE.equals(ret)) {
                                return;
                            } else if (Boolean.TRUE.equals(ret)) {
                                continue mainloop;
                            }
                        }
                    } else if (!isDirect && !isHttpJD) {
                        {
                            /*
                             * first we will walk through all available decrypter plugins
                             */
                            final List<LazyCrawlerPlugin> lazyCrawlerPlugins = getSortedLazyCrawlerPlugins();
                            final ListIterator<LazyCrawlerPlugin> it = lazyCrawlerPlugins.listIterator();
                            lazyCrawlerPlugin: while (it.hasNext()) {
                                final LazyCrawlerPlugin pDecrypt = it.next();
                                final DISTRIBUTE ret = distributePluginForDecrypt(pDecrypt, generation, url, possibleCryptedLink);
                                switch (ret) {
                                case PARTIAL_MATCH:
                                    if (it.previousIndex() > lazyCrawlerPlugins.size() / 50) {
                                        resetSortedLazyCrawlerPlugins(lazyCrawlerPlugins);
                                    }
                                    continue lazyCrawlerPlugin;
                                case STOP:
                                    return;
                                case CONTINUE:
                                    continue lazyCrawlerPlugin;
                                case BLACKLISTED:
                                    continue mainloop;
                                case NEXT:
                                    if (it.previousIndex() > lazyCrawlerPlugins.size() / 50) {
                                        resetSortedLazyCrawlerPlugins(lazyCrawlerPlugins);
                                    }
                                    continue mainloop;
                                default:
                                    LogController.CL().log(new IllegalStateException(ret.name()));
                                    break;
                                }
                            }
                        }
                        {
                            /* now we will walk through all available hoster plugins */
                            final List<LazyHostPlugin> sortedLazyHostPlugins = getSortedLazyHostPlugins();
                            final ListIterator<LazyHostPlugin> it = sortedLazyHostPlugins.listIterator();
                            lazyHosterPlugin: while (it.hasNext()) {
                                final LazyHostPlugin pHost = it.next();
                                final DISTRIBUTE ret = distributePluginForHost(pHost, generation, url, possibleCryptedLink);
                                switch (ret) {
                                case STOP:
                                    return;
                                case CONTINUE:
                                    continue lazyHosterPlugin;
                                case BLACKLISTED:
                                    continue mainloop;
                                case NEXT:
                                    if (it.previousIndex() > sortedLazyHostPlugins.size() / 50) {
                                        resetSortedLazyHostPlugins(sortedLazyHostPlugins);
                                    }
                                    continue mainloop;
                                default:
                                    LogController.CL().log(new IllegalStateException(ret.name()));
                                    break;
                                }
                            }
                        }
                    }
                    if (isFtp) {
                        final LazyHostPlugin ftpPlugin = getGenericFtpPlugin();
                        if (ftpPlugin != null) {
                            /* now we will check for generic ftp links */
                            final DISTRIBUTE ret = distributePluginForHost(ftpPlugin, generation, url, possibleCryptedLink);
                            switch (ret) {
                            case STOP:
                                return;
                            case CONTINUE:
                                break;
                            case BLACKLISTED:
                            case NEXT:
                                continue mainloop;
                            default:
                                LogController.CL().log(new IllegalStateException(ret.name()));
                                break;
                            }
                        }
                    } else if (!isFile) {
                        final DirectHTTPPermission directHTTPPermission = getDirectHTTPPermission();
                        final LazyHostPlugin directPlugin = getDirectHTTPPlugin();
                        if (directPlugin != null) {
                            LinkCrawlerRule rule = null;
                            if (isDirect) {
                                rule = possibleCryptedLink.getMatchingRule();
                                if (DirectHTTPPermission.ALWAYS.equals(directHTTPPermission) || (DirectHTTPPermission.RULES_ONLY.equals(directHTTPPermission) && (rule != null && LinkCrawlerRule.RULE.DIRECTHTTP.equals(rule.getRule())))) {
                                    /* now we will check for directPlugin links */
                                    final DISTRIBUTE ret = distributePluginForHost(directPlugin, generation, url, possibleCryptedLink);
                                    switch (ret) {
                                    case STOP:
                                        return;
                                    case CONTINUE:
                                        break;
                                    case BLACKLISTED:
                                    case NEXT:
                                        continue mainloop;
                                    default:
                                        LogController.CL().log(new IllegalStateException(ret.name()));
                                        break;
                                    }
                                } else {
                                    // DirectHTTPPermission.FORBIDDEN
                                    continue mainloop;
                                }
                            } else if ((rule = getFirstMatchingRule(possibleCryptedLink, url, LinkCrawlerRule.RULE.DIRECTHTTP)) != null) {
                                if (DirectHTTPPermission.FORBIDDEN.equals(directHTTPPermission)) {
                                    continue mainloop;
                                }
                                final CrawledLink copy = createCopyOf(possibleCryptedLink);
                                final CrawledLinkModifier linkModifier = copy.getCustomCrawledLinkModifier();
                                copy.setCustomCrawledLinkModifier(null);
                                final DownloadLink link = new DownloadLink(null, null, null, "directhttp://" + url, true);
                                final CrawledLink directHTTP = crawledLinkFactorybyDownloadLink(link);
                                if (rule != null) {
                                    link.setProperty("lcrID", rule.getId());
                                    directHTTP.setMatchingRule(rule);
                                }
                                forwardCrawledLinkInfos(copy, directHTTP, linkModifier, getAndClearSourceURLs(copy), true);
                                // modify sourceLink because directHTTP arise from possibleCryptedLink(convert to directhttp)
                                directHTTP.setSourceLink(possibleCryptedLink.getSourceLink());
                                final DISTRIBUTE ret = distributePluginForHost(directPlugin, generation, directHTTP.getURL(), directHTTP);
                                switch (ret) {
                                case STOP:
                                    return;
                                case CONTINUE:
                                    break;
                                case BLACKLISTED:
                                case NEXT:
                                    continue mainloop;
                                default:
                                    LogController.CL().log(new IllegalStateException(ret.name()));
                                    break;
                                }
                            }
                        }
                        final LazyHostPlugin httpPlugin = getGenericHttpPlugin();
                        if (httpPlugin != null && url.startsWith("http")) {
                            try {
                                if (canHandle(httpPlugin, url, possibleCryptedLink) && getFirstMatchingRule(possibleCryptedLink, url.replaceFirst("(https?)(viajd)://", "$1://"), LinkCrawlerRule.RULE.SUBMITFORM, LinkCrawlerRule.RULE.FOLLOWREDIRECT, LinkCrawlerRule.RULE.DEEPDECRYPT) == null) {
                                    synchronized (loopPreventionEmbedded) {
                                        if (!loopPreventionEmbedded.containsKey(possibleCryptedLink)) {
                                            final UnknownCrawledLinkHandler unknownLinkHandler = new UnknownCrawledLinkHandler() {
                                                @Override
                                                public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc) {
                                                    lc.distribute(generation, Arrays.asList(new CrawledLink[] { possibleCryptedLink }));
                                                }
                                            };
                                            final DISTRIBUTE ret = distributeEmbeddedLink(generation, url, createCopyOf(possibleCryptedLink), unknownLinkHandler);
                                            switch (ret) {
                                            case STOP:
                                                return;
                                            case CONTINUE:
                                                break;
                                            case BLACKLISTED:
                                                continue mainloop;
                                            case NEXT:
                                                loopPreventionEmbedded.put(possibleCryptedLink, this);
                                                handleUnhandledCryptedLink(possibleCryptedLink);
                                                continue mainloop;
                                            default:
                                                LogController.CL().log(new IllegalStateException(ret.name()));
                                                break;
                                            }
                                        }
                                    }
                                    if (DirectHTTPPermission.ALWAYS.equals(directHTTPPermission)) {
                                        final DISTRIBUTE ret = distributePluginForHost(httpPlugin, generation, url, createCopyOf(possibleCryptedLink));
                                        switch (ret) {
                                        case STOP:
                                            return;
                                        case CONTINUE:
                                            break;
                                        case NEXT:
                                        case BLACKLISTED:
                                            continue mainloop;
                                        default:
                                            LogController.CL().log(new IllegalStateException(ret.name()));
                                            break;
                                        }
                                    } else {
                                        // DirectHTTPPermission.FORBIDDEN
                                        continue mainloop;
                                    }
                                }
                            } catch (final Throwable e) {
                                LogController.CL().log(e);
                            }
                        }
                    }
                    if (unnknownHandler != null) {
                        /*
                         * CrawledLink is unhandled till now , but has an UnknownHandler set, lets call it, maybe it makes the Link handable
                         * by a Plugin
                         */
                        try {
                            unnknownHandler.unhandledCrawledLink(possibleCryptedLink, this);
                        } catch (final Throwable e) {
                            LogController.CL().log(e);
                        }
                        /* lets retry this crawledLink */
                        continue mainloopretry;
                    }
                    if (!isFtp && !isHttpJD && !isDirect) {
                        // only process non directhttp/https?viajd/ftp
                        final Boolean deeperOrFollow = distributeDeeperOrMatchingRule(generation, url, possibleCryptedLink);
                        if (Boolean.FALSE.equals(deeperOrFollow)) {
                            return;
                        } else {
                            synchronized (loopPreventionEmbedded) {
                                if (!loopPreventionEmbedded.containsKey(possibleCryptedLink)) {
                                    final DISTRIBUTE ret = distributeEmbeddedLink(generation, url, possibleCryptedLink, null);
                                    switch (ret) {
                                    case STOP:
                                        return;
                                    case CONTINUE:
                                        break;
                                    case BLACKLISTED:
                                        continue mainloop;
                                    case NEXT:
                                        loopPreventionEmbedded.put(possibleCryptedLink, this);
                                        handleUnhandledCryptedLink(possibleCryptedLink);
                                        continue mainloop;
                                    default:
                                        LogController.CL().log(new IllegalStateException(ret.name()));
                                        break;
                                    }
                                }
                            }
                            if (Boolean.TRUE.equals(deeperOrFollow)) {
                                continue mainloop;
                            }
                        }
                    }
                    break mainloopretry;
                }
                handleUnhandledCryptedLink(possibleCryptedLink);
            }
        } finally {
            checkFinishNotify(task);
        }
    }

    protected DISTRIBUTE distributeEmbeddedLink(final LinkCrawlerGeneration generation, final String url, final CrawledLink source, UnknownCrawledLinkHandler unknownCrawledLinkHandler) {
        final LinkedHashSet<String> possibleEmbeddedLinks = new LinkedHashSet<String>();
        try {
            final String sourceURL = source.getURL();
            final String queryString = new Regex(sourceURL, "\\?(.+)$").getMatch(0);
            if (StringUtils.isNotEmpty(queryString)) {
                final String[] parameters = queryString.split("\\&(?!#)", -1);
                for (final String parameter : parameters) {
                    try {
                        final String params[] = parameter.split("=", 2);
                        final String checkParam;
                        if (params.length == 1) {
                            checkParam = URLDecoder.decode(params[0], "UTF-8");
                        } else {
                            checkParam = URLDecoder.decode(params[1], "UTF-8");
                        }
                        if (checkParam.startsWith("aHR0c") || checkParam.startsWith("ZnRwOi")) {
                            String base64 = checkParam;
                            /* base64 http and ftp */
                            while (true) {
                                if (base64.length() % 4 != 0) {
                                    base64 += "=";
                                } else {
                                    break;
                                }
                            }
                            final byte[] decoded = Base64.decode(base64);
                            if (decoded != null) {
                                String possibleURLs = new String(decoded, "UTF-8");
                                if (HTMLParser.getProtocol(possibleURLs) == null) {
                                    possibleURLs = URLDecoder.decode(possibleURLs, "UTF-8");
                                }
                                if (HTMLParser.getProtocol(possibleURLs) != null) {
                                    possibleEmbeddedLinks.add(possibleURLs);
                                }
                            }
                        } else {
                            try {
                                final String maybeURL;
                                if (checkParam.contains("%3")) {
                                    maybeURL = URLDecoder.decode(checkParam, "UTF-8").replaceFirst("^:?/?/?", "");
                                } else {
                                    maybeURL = checkParam.replaceFirst("^:?/?/?", "");
                                }
                                final URL dummyURL;
                                if (HTMLParser.getProtocol(maybeURL) == null) {
                                    dummyURL = new URL("http://" + maybeURL.replaceFirst("^(.+?://)", ""));
                                } else {
                                    dummyURL = new URL(maybeURL);
                                }
                                if (dummyURL != null && dummyURL.getHost() != null && dummyURL.getHost().contains(".") && (StringUtils.isNotEmpty(dummyURL.getFile()) || StringUtils.isNotEmpty(dummyURL.getRef()))) {
                                    possibleEmbeddedLinks.add(dummyURL.toString());
                                }
                            } catch (final MalformedURLException e) {
                            }
                        }
                    } catch (final Throwable e) {
                        LogController.CL().log(e);
                    }
                }
            }
            if (StringUtils.contains(sourceURL, "aHR0c") || StringUtils.contains(sourceURL, "ZnRwOi")) {
                String base64 = new Regex(sourceURL, "(aHR0c[0-9a-zA-Z\\+\\/]+(%3D|=){0,2})").getMatch(0);// http
                if (base64 == null) {
                    base64 = new Regex(sourceURL, "(ZnRwOi[0-9a-zA-Z\\+\\/]+(%3D|=){0,2})").getMatch(0);// ftp
                }
                if (base64 != null) {
                    if (base64.contains("%3D")) {
                        base64 = URLDecoder.decode(base64, "UTF-8");
                    }
                    while (true) {
                        if (base64.length() % 4 != 0) {
                            base64 += "=";
                        } else {
                            break;
                        }
                    }
                    final byte[] decoded = Base64.decode(base64);
                    if (decoded != null) {
                        String possibleURLs = new String(decoded, "UTF-8");
                        if (HTMLParser.getProtocol(possibleURLs) == null) {
                            possibleURLs = URLDecoder.decode(possibleURLs, "UTF-8");
                        }
                        if (HTMLParser.getProtocol(possibleURLs) != null) {
                            possibleEmbeddedLinks.add(possibleURLs);
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        if (possibleEmbeddedLinks.size() > 0) {
            final ArrayList<CrawledLink> embeddedLinks = new ArrayList<CrawledLink>();
            for (final String possibleURL : possibleEmbeddedLinks) {
                final List<CrawledLink> links = find(generation, source, possibleURL, null, source.isCrawlDeep(), false);
                if (links != null) {
                    embeddedLinks.addAll(links);
                }
            }
            if (embeddedLinks.size() > 0) {
                final boolean singleDest = embeddedLinks.size() == 1;
                final String[] sourceURLs = getAndClearSourceURLs(source);
                final CrawledLinkModifier sourceLinkModifier = source.getCustomCrawledLinkModifier();
                source.setCustomCrawledLinkModifier(null);
                source.setBrokenCrawlerHandler(null);
                for (final CrawledLink embeddedLink : embeddedLinks) {
                    embeddedLink.setUnknownHandler(unknownCrawledLinkHandler);
                    forwardCrawledLinkInfos(source, embeddedLink, sourceLinkModifier, sourceURLs, singleDest);
                }
                crawl(generation, embeddedLinks);
                return DISTRIBUTE.NEXT;
            }
        }
        return DISTRIBUTE.CONTINUE;
    }

    public List<LinkCrawlerRule> getLinkCrawlerRules() {
        List<LinkCrawlerRule> ret = linkCrawlerRules.get();
        if (ret == null) {
            synchronized (LINKCRAWLERRULESLOCK) {
                ret = linkCrawlerRules.get();
                if (ret == null) {
                    linkCrawlerRules.set(listLinkCrawlerRules());
                    return getLinkCrawlerRules();
                }
            }
        }
        if (ret.size() == 0) {
            return null;
        } else {
            return ret;
        }
    }

    protected LinkCrawlerRule getFirstMatchingRule(CrawledLink link, String url, LinkCrawlerRule.RULE... ruleTypes) {
        final List<LinkCrawlerRule> rules = getLinkCrawlerRules();
        if (rules == null) {
            return null;
        }
        if (StringUtils.startsWithCaseInsensitive(url, "file:/") || StringUtils.startsWithCaseInsensitive(url, "http://") || StringUtils.startsWithCaseInsensitive(url, "https://")) {
            for (final LinkCrawlerRule.RULE ruleType : ruleTypes) {
                for (final LinkCrawlerRule rule : rules) {
                    if (ruleType.equals(rule.getRule()) && rule.matches(url)) {
                        if (rule.getMaxDecryptDepth() == -1) {
                            return rule;
                        } else {
                            final Iterator<CrawledLink> it = link.iterator();
                            int depth = 0;
                            while (it.hasNext()) {
                                final CrawledLink next = it.next();
                                final LinkCrawlerRule matchingRule = next.getMatchingRule();
                                if (matchingRule != null && matchingRule.getId() == rule.getId()) {
                                    depth++;
                                }
                            }
                            if (depth <= rule.getMaxDecryptDepth()) {
                                // okay
                                return rule;
                            } else {
                                // too deep
                                continue;
                            }
                        }
                    }
                }
            }
            // no matching LinkCrawlerRule
            if (link instanceof BrowserCrawledLink) {
                final LinkCrawlerRule matchedRule = link.getMatchingRule();
                if (matchedRule != null) {
                    for (final LinkCrawlerRule.RULE ruleType : ruleTypes) {
                        if (matchedRule != null && ruleType.equals(matchedRule.getRule())) {
                            // return previous matching LinkCrawlerRule for BrowserCrawledLink
                            return matchedRule;
                        }
                    }
                }
            }
        }
        return null;
    }

    public List<LazyCrawlerPlugin> getSortedLazyCrawlerPlugins() {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.getSortedLazyCrawlerPlugins();
        } else {
            if (unsortedLazyCrawlerPlugins == null) {
                unsortedLazyCrawlerPlugins = CrawlerPluginController.getInstance().list();
            }
            List<LazyCrawlerPlugin> ret = sortedLazyCrawlerPlugins.get();
            if (ret == null) {
                synchronized (sortedLazyCrawlerPlugins) {
                    ret = sortedLazyCrawlerPlugins.get();
                    if (ret == null) {
                        /* sort cHosts according to their usage */
                        ret = new ArrayList<LazyCrawlerPlugin>(unsortedLazyCrawlerPlugins.size());
                        final List<LazyCrawlerPlugin> allPlugins = new ArrayList<LazyCrawlerPlugin>(unsortedLazyCrawlerPlugins);
                        try {
                            final Map<String, Object> pluginMap = new HashMap<String, Object>();
                            for (final LazyCrawlerPlugin plugin : allPlugins) {
                                final Object entry = pluginMap.get(plugin.getDisplayName());
                                if (entry == null) {
                                    pluginMap.put(plugin.getDisplayName(), plugin);
                                } else if (entry instanceof List) {
                                    ((List<LazyCrawlerPlugin>) entry).add(plugin);
                                } else {
                                    final ArrayList<LazyCrawlerPlugin> list = new ArrayList<LazyCrawlerPlugin>();
                                    list.add((LazyCrawlerPlugin) entry);
                                    list.add(plugin);
                                    pluginMap.put(plugin.getDisplayName(), list);
                                }
                            }
                            Collections.sort(allPlugins, new Comparator<LazyCrawlerPlugin>() {
                                public final int compare(final long x, final long y) {
                                    return (x < y) ? 1 : ((x == y) ? 0 : -1);
                                }

                                public final int compare(final boolean x, final boolean y) {
                                    return (x == y) ? 0 : (x ? 1 : -1);
                                }

                                @Override
                                public int compare(LazyCrawlerPlugin o1, LazyCrawlerPlugin o2) {
                                    final int ret = compare(o1.getPluginUsage(), o2.getPluginUsage());
                                    if (ret == 0) {
                                        return compare(o1.hasFeature(FEATURE.GENERIC), o2.hasFeature(FEATURE.GENERIC));
                                    } else {
                                        return ret;
                                    }
                                }
                            });
                            for (final LazyCrawlerPlugin plugin : allPlugins) {
                                final Object entry = pluginMap.remove(plugin.getDisplayName());
                                if (entry == null) {
                                    if (pluginMap.isEmpty()) {
                                        break;
                                    } else {
                                        continue;
                                    }
                                } else if (entry instanceof LazyCrawlerPlugin) {
                                    ret.add((LazyCrawlerPlugin) entry);
                                } else {
                                    final List<LazyCrawlerPlugin> list = (List<LazyCrawlerPlugin>) entry;
                                    sortLazyCrawlerPluginByInterfaceVersion(list);
                                    ret.addAll(list);
                                }
                            }
                        } catch (final Throwable e) {
                            LogController.CL(true).log(e);
                        }
                        if (ret == null || ret.size() == 0) {
                            ret = allPlugins;
                        }
                        sortedLazyCrawlerPlugins.compareAndSet(null, ret);
                    }
                }
            }
            return ret;
        }
    }

    protected void sortLazyCrawlerPluginByInterfaceVersion(final List<LazyCrawlerPlugin> plugins) {
        Collections.sort(plugins, new Comparator<LazyCrawlerPlugin>() {
            @Override
            public final int compare(final LazyCrawlerPlugin lazyCrawlerPlugin1, final LazyCrawlerPlugin lazyCrawlerPlugin2) {
                final int i1 = lazyCrawlerPlugin1.getLazyPluginClass().getInterfaceVersion();
                final int i2 = lazyCrawlerPlugin2.getLazyPluginClass().getInterfaceVersion();
                if (i1 == i2) {
                    return 0;
                } else if (i1 > i2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
    }

    public List<LazyHostPlugin> getSortedLazyHostPlugins() {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.getSortedLazyHostPlugins();
        } else {
            /* sort pHosts according to their usage */
            List<LazyHostPlugin> ret = sortedLazyHostPlugins.get();
            if (ret == null) {
                synchronized (sortedLazyHostPlugins) {
                    ret = sortedLazyHostPlugins.get();
                    if (ret == null) {
                        ret = new ArrayList<LazyHostPlugin>();
                        for (final LazyHostPlugin lazyHostPlugin : HostPluginController.getInstance().list()) {
                            if (!HTTP_LINKS.equals(lazyHostPlugin.getDisplayName()) && !"ftp".equals(lazyHostPlugin.getDisplayName()) && !DIRECT_HTTP.equals(lazyHostPlugin.getDisplayName())) {
                                ret.add(lazyHostPlugin);
                            }
                        }
                        try {
                            Collections.sort(ret, new Comparator<LazyHostPlugin>() {
                                public final int compare(long x, long y) {
                                    return (x < y) ? 1 : ((x == y) ? 0 : -1);
                                }

                                @Override
                                public final int compare(LazyHostPlugin o1, LazyHostPlugin o2) {
                                    return compare(o1.getPluginUsage(), o2.getPluginUsage());
                                }
                            });
                        } catch (final Throwable e) {
                            LogController.CL(true).log(e);
                        }
                        sortedLazyHostPlugins.compareAndSet(null, ret);
                    }
                }
            }
            return ret;
        }
    }

    protected boolean resetSortedLazyCrawlerPlugins(List<LazyCrawlerPlugin> resetSortedLazyCrawlerPlugins) {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.resetSortedLazyCrawlerPlugins(resetSortedLazyCrawlerPlugins);
        } else {
            return sortedLazyCrawlerPlugins.compareAndSet(resetSortedLazyCrawlerPlugins, null);
        }
    }

    protected boolean resetSortedLazyHostPlugins(List<LazyHostPlugin> lazyHostPlugins) {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.resetSortedLazyHostPlugins(lazyHostPlugins);
        } else {
            return sortedLazyHostPlugins.compareAndSet(lazyHostPlugins, null);
        }
    }

    protected DirectHTTPPermission getDirectHTTPPermission() {
        return directHTTPPermission;
    }

    public DISTRIBUTE getCryptedLinks(List<CrawledLink> results, LazyCrawlerPlugin lazyC, CrawledLink source, CrawledLinkModifier modifier) {
        final String[] matches = getMatchingLinks(lazyC.getPattern(), source, modifier);
        if (matches == null || matches.length == 0) {
            return DISTRIBUTE.NEXT;
        }
        DISTRIBUTE result = null;
        final String sourceURL = source.getURL();
        for (final String match : matches) {
            final CryptedLink cryptedLink;
            if (matches.length == 1 && match.equals(sourceURL)) {
                cryptedLink = new CryptedLink(source);
            } else {
                cryptedLink = new CryptedLink(match, source);
            }
            if (result == null) {
                if (match.equals(source.getURL())) {
                    result = DISTRIBUTE.NEXT;
                } else {
                    final Matcher firstHttp = Pattern.compile("https?://").matcher(sourceURL);
                    final int firstHttpIndex = firstHttp.find() ? firstHttp.start() : -1;
                    if (firstHttpIndex == sourceURL.indexOf(match)) {
                        result = DISTRIBUTE.NEXT;
                    } else {
                        // match is part of another URL(eg: http://site1.com/dosomethingwith_/http://site2.com
                        // site2.com is match here, but we want to continue, maybe another plugin can handle full site1+site2 URL
                        result = DISTRIBUTE.PARTIAL_MATCH;
                    }
                }
            }
            cryptedLink.setLazyC(lazyC);
            final CrawledLink link = crawledLinkFactorybyCryptedLink(cryptedLink);
            forwardCrawledLinkInfos(source, link, modifier, null, null);
            if ((source.getUrlLink() == null || StringUtils.equals(source.getUrlLink(), link.getURL())) && (source.getDownloadLink() == null || source.getDownloadLink().getProperties().isEmpty())) {
                // modify sourceLink because link arise from source(getMatchingLinks)
                //
                // keep DownloadLinks with non empty properties
                link.setCrawlDeep(source.isCrawlDeep());
                link.setSourceLink(source.getSourceLink());
                if (!(cryptedLink.getSource() instanceof String)) {
                    cryptedLink.setCryptedUrl(match);
                }
                cryptedLink.setSourceLink(source.getSourceLink());
                final LinkCrawlerRule sourceMatchingRule = source.getMatchingRule();
                if (sourceMatchingRule != null) {
                    link.setMatchingRule(sourceMatchingRule);
                }
            }
            results.add(link);
        }
        return result;
    }

    protected String[] getMatchingLinks(Pattern pattern, CrawledLink source, CrawledLinkModifier modifier) {
        final String[] ret = new Regex(source.getURL(), pattern).getColumn(-1);
        if (ret == null || ret.length == 0) {
            return null;
        }
        for (int index = 0; index < ret.length; index++) {
            String match = ret[index];
            match = match.trim();
            while (match.length() > 2 && match.charAt(0) == '<' && match.charAt(match.length() - 1) == '>') {
                match = match.substring(1, match.length() - 1);
            }
            while (match.length() > 2 && match.charAt(0) == '\"' && match.charAt(match.length() - 1) == '\"') {
                match = match.substring(1, match.length() - 1);
            }
            ret[index] = match.trim();
            if (StringUtils.equals(source.getURL(), ret[index])) {
                ret[index] = source.getURL();
            }
        }
        return ret;
    }

    public List<CrawledLink> getCrawledLinks(Pattern pattern, CrawledLink source, CrawledLinkModifier modifier) {
        final String[] matches = getMatchingLinks(pattern, source, modifier);
        if (matches == null || matches.length == 0) {
            return null;
        }
        final ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
        for (final String match : matches) {
            final CrawledLink link = crawledLinkFactorybyURL(match);
            forwardCrawledLinkInfos(source, link, modifier, null, null);
            if ((source.getUrlLink() == null || StringUtils.equals(source.getUrlLink(), link.getURL())) && (source.getDownloadLink() == null || source.getDownloadLink().getProperties().isEmpty())) {
                // modify sourceLink because link arise from source(getMatchingLinks)
                //
                // keep DownloadLinks with non empty properties
                link.setSourceLink(source.getSourceLink());
                if (source.getMatchingRule() != null) {
                    link.setMatchingRule(source.getMatchingRule());
                }
            }
            ret.add(link);
        }
        return ret;
    }

    protected void processHostPlugin(final LinkCrawlerGeneration generation, LazyHostPlugin pHost, CrawledLink possibleCryptedLink) {
        final CrawledLinkModifier parentLinkModifier = possibleCryptedLink.getCustomCrawledLinkModifier();
        possibleCryptedLink.setCustomCrawledLinkModifier(null);
        possibleCryptedLink.setBrokenCrawlerHandler(null);
        final LinkCrawlerTask task;
        if (pHost == null) {
            return;
        } else if (possibleCryptedLink.getURL() == null) {
            return;
        } else if (this.isCrawledLinkFiltered(possibleCryptedLink)) {
            return;
        } else if ((task = checkStartNotify(generation, "processHostPlugin:" + pHost + "|" + possibleCryptedLink.getURL())) == null) {
            return;
        }
        try {
            final String[] sourceURLs = getAndClearSourceURLs(possibleCryptedLink);
            /*
             * use a new PluginClassLoader here
             */
            final PluginForHost wplg = pHost.newInstance(getPluginClassLoaderChild());
            if (wplg == null) {
                LogController.CL().info("Hoster Plugin not available:" + pHost.getDisplayName());
                return;
            }
            /* now we run the plugin and let it find some links */
            final LinkCrawlerThread lct = getCurrentLinkCrawlerThread();
            Object owner = null;
            LinkCrawler previousCrawler = null;
            boolean oldDebug = false;
            boolean oldVerbose = false;
            LogInterface oldLogger = null;
            try {
                final LogInterface logger = LogController.getFastPluginLogger(wplg.getCrawlerLoggerID(possibleCryptedLink));
                logger.info("Processing: " + possibleCryptedLink.getURL());
                if (lct != null) {
                    /* mark thread to be used by crawler plugin */
                    owner = lct.getCurrentOwner();
                    lct.setCurrentOwner(wplg);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                    /* save old logger/states */
                    oldLogger = lct.getLogger();
                    oldDebug = lct.isDebug();
                    oldVerbose = lct.isVerbose();
                    /* set new logger and set verbose/debug true */
                    lct.setLogger(logger);
                    lct.setVerbose(true);
                    lct.setDebug(true);
                }
                final Browser br = wplg.createNewBrowserInstance();
                wplg.setBrowser(br);
                wplg.setLogger(logger);
                wplg.init();
                String url = possibleCryptedLink.getURL();
                FilePackage sourcePackage = null;
                if (possibleCryptedLink.getDownloadLink() != null) {
                    sourcePackage = possibleCryptedLink.getDownloadLink().getFilePackage();
                    if (FilePackage.isDefaultFilePackage(sourcePackage)) {
                        /* we don't want the various filePackage getting used */
                        sourcePackage = null;
                    }
                }
                final long startTime = Time.systemIndependentCurrentJVMTimeMillis();
                final List<CrawledLink> crawledLinks = new ArrayList<CrawledLink>();
                try {
                    wplg.setCurrentLink(possibleCryptedLink);
                    final List<DownloadLink> hosterLinks = wplg.getDownloadLinks(possibleCryptedLink, url, sourcePackage);
                    if (hosterLinks != null) {
                        final UrlProtection protection = wplg.getUrlProtection(hosterLinks);
                        if (protection != null && protection != UrlProtection.UNSET) {
                            for (DownloadLink dl : hosterLinks) {
                                if (dl.getUrlProtection() == UrlProtection.UNSET) {
                                    dl.setUrlProtection(protection);
                                }
                            }
                        }
                        for (final DownloadLink hosterLink : hosterLinks) {
                            try {
                                wplg.correctDownloadLink(hosterLink);
                            } catch (final Throwable e) {
                                LogController.CL().log(e);
                            }
                            crawledLinks.add(wplg.convert(hosterLink));
                        }
                    }
                    /* in case the function returned without exceptions, we can clear log */
                    if (logger instanceof ClearableLogInterface) {
                        ((ClearableLogInterface) logger).clear();
                    }
                } finally {
                    wplg.setCurrentLink(null);
                    final long endTime = Time.systemIndependentCurrentJVMTimeMillis() - startTime;
                    pHost.updateParseRuntime(endTime);
                    /* close the logger */
                    if (logger instanceof ClosableLogInterface) {
                        ((ClosableLogInterface) logger).close();
                    }
                }
                if (crawledLinks.size() > 0) {
                    final boolean singleDest = crawledLinks.size() == 1;
                    for (final CrawledLink crawledLink : crawledLinks) {
                        forwardCrawledLinkInfos(possibleCryptedLink, crawledLink, parentLinkModifier, sourceURLs, singleDest);
                        if (possibleCryptedLink.getUrlLink() == null || StringUtils.equals(possibleCryptedLink.getUrlLink(), crawledLink.getURL())) {
                            // modify sourceLink because crawledLink arise from possibleCryptedLink(wplg.getDownloadLinks)
                            crawledLink.setSourceLink(possibleCryptedLink.getSourceLink());
                            final LinkCrawlerRule matchingRule = possibleCryptedLink.getMatchingRule();
                            if (matchingRule != null) {
                                crawledLink.setMatchingRule(matchingRule);
                            }
                        }
                        distributeFinalCrawledLink(generation, crawledLink);
                    }
                }
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setCurrentOwner(owner);
                    lct.setCurrentLinkCrawler(previousCrawler);
                    lct.setLogger(oldLogger);
                    lct.setVerbose(oldVerbose);
                    lct.setDebug(oldDebug);
                }
            }
        } catch (Throwable e) {
            LogController.CL().log(e);
        } finally {
            /* restore old ClassLoader for current Thread */
            checkFinishNotify(task);
        }
    }

    public String[] getAndClearSourceURLs(final CrawledLink link) {
        final ArrayList<String> sources = new ArrayList<String>();
        CrawledLink next = link;
        CrawledLink previous = link;
        while (next != null) {
            final CrawledLink current = next;
            next = current.getSourceLink();
            final String currentURL = cleanURL(current.getURL());
            if (currentURL != null) {
                if (sources.size() == 0) {
                    sources.add(currentURL);
                } else {
                    if (current.getMatchingRule() == null || current.getMatchingRule() != previous.getMatchingRule()) {
                        final String previousURL = sources.get(sources.size() - 1);
                        if (!StringUtils.equals(currentURL, previousURL)) {
                            sources.add(currentURL);
                        }
                    }
                }
            }
            previous = current;
        }
        link.setSourceUrls(null);
        final String customSourceUrl = getReferrerUrl(link);
        if (customSourceUrl != null) {
            sources.add(customSourceUrl);
        }
        if (sources.size() == 0) {
            return null;
        } else {
            return sources.toArray(new String[] {});
        }
    }

    public String getReferrerUrl(final CrawledLink link) {
        if (link != null) {
            final LinkCollectingJob job = link.getSourceJob();
            if (job != null) {
                final String customSourceUrl = job.getCustomSourceUrl();
                if (customSourceUrl != null) {
                    return customSourceUrl;
                }
            }
        }
        if (this instanceof JobLinkCrawler) {
            final LinkCollectingJob job = ((JobLinkCrawler) this).getJob();
            if (job != null) {
                return job.getCustomSourceUrl();
            }
        }
        return null;
    }

    public static String getUnsafeName(String unsafeName, String currentName) {
        if (unsafeName == null) {
            return null;
        }
        String extension = Files.getExtension(unsafeName);
        if (extension == null && unsafeName.indexOf('.') < 0) {
            String unsafeSourceModified = null;
            if (unsafeName.indexOf('_') > 0) {
                unsafeSourceModified = unsafeName.replaceAll("_", ".");
                extension = Files.getExtension(unsafeSourceModified);
            }
            if (extension == null && unsafeName.indexOf('-') > 0) {
                unsafeSourceModified = unsafeName.replaceAll("-", ".");
                extension = Files.getExtension(unsafeSourceModified);
            }
            if (extension != null) {
                unsafeName = unsafeSourceModified;
            }
        }
        if (extension != null && !StringUtils.equals(currentName, unsafeName)) {
            return unsafeName;
        }
        return null;
    }

    /**
     * in case link contains rawURL/CryptedLink we return downloadLink from sourceLink
     *
     * @param link
     * @return
     */
    private DownloadLink getLatestDownloadLink(CrawledLink link) {
        final DownloadLink ret = link.getDownloadLink();
        if (ret == null && link.getSourceLink() != null) {
            return link.getSourceLink().getDownloadLink();
        } else {
            return ret;
        }
    }

    private CryptedLink getLatestCryptedLink(CrawledLink link) {
        final CryptedLink ret = link.getCryptedLink();
        if (ret == null && link.getSourceLink() != null) {
            return link.getSourceLink().getCryptedLink();
        } else {
            return ret;
        }
    }

    private void forwardCryptedLinkInfos(final CrawledLink sourceCrawledLink, final CryptedLink destCryptedLink) {
        if (sourceCrawledLink == null) {
            return;
        } else if (destCryptedLink == null) {
            return;
        }
        String pw = null;
        final DownloadLink latestDownloadLink = getLatestDownloadLink(sourceCrawledLink);
        if (latestDownloadLink != null) {
            pw = latestDownloadLink.getDownloadPassword();
        }
        if (StringUtils.isEmpty(pw)) {
            final CryptedLink latestCryptedLink = getLatestCryptedLink(sourceCrawledLink);
            if (latestCryptedLink != null) {
                pw = latestCryptedLink.getDecrypterPassword();
            }
        }
        if (StringUtils.isEmpty(pw) && LinkCrawler.this instanceof JobLinkCrawler && ((JobLinkCrawler) LinkCrawler.this).getJob() != null) {
            pw = ((JobLinkCrawler) LinkCrawler.this).getJob().getCrawlerPassword();
        }
        destCryptedLink.setDecrypterPassword(pw);
    }

    protected void forwardCrawledLinkInfos(final CrawledLink sourceCrawledLink, final CrawledLink destCrawledLink, final CrawledLinkModifier sourceLinkModifier, final String sourceURLs[], final Boolean singleDestCrawledLink) {
        if (sourceCrawledLink == null || destCrawledLink == null || sourceCrawledLink == destCrawledLink) {
            /* Guard clause -> Do nothing */
            return;
        }
        destCrawledLink.setSourceLink(sourceCrawledLink);
        destCrawledLink.setOrigin(sourceCrawledLink.getOrigin());
        destCrawledLink.setSourceUrls(sourceURLs);
        destCrawledLink.setMatchingFilter(sourceCrawledLink.getMatchingFilter());
        forwardCryptedLinkInfos(sourceCrawledLink, destCrawledLink.getCryptedLink());
        forwardDownloadLinkInfos(getLatestDownloadLink(sourceCrawledLink), destCrawledLink.getDownloadLink(), singleDestCrawledLink);
        if (Boolean.TRUE.equals(singleDestCrawledLink) && sourceCrawledLink.isNameSet()) {
            // forward customized name, eg from container plugins
            destCrawledLink.setName(sourceCrawledLink.getName());
        }
        final CrawledLinkModifier destCustomModifier = destCrawledLink.getCustomCrawledLinkModifier();
        if (destCustomModifier == null) {
            destCrawledLink.setCustomCrawledLinkModifier(sourceLinkModifier);
        } else if (sourceLinkModifier != null) {
            final List<CrawledLinkModifier> modifiers = new ArrayList<CrawledLinkModifier>();
            modifiers.add(sourceLinkModifier);
            modifiers.add(destCustomModifier);
            destCrawledLink.setCustomCrawledLinkModifier(new CrawledLinkModifiers(modifiers));
        }
        // if we decrypted a dlc,source.getDesiredPackageInfo() is null, and dest might already have package infos from the container.
        // maybe it would be even better to merge the packageinfos
        // However. if there are crypted links in the container, it may be up to the decrypterplugin to decide
        // example: share-links.biz uses CNL to post links to localhost. the dlc origin get's lost on such a way
        final PackageInfo dpi = sourceCrawledLink.getDesiredPackageInfo();
        if (dpi != null) {
            destCrawledLink.setDesiredPackageInfo(dpi.getCopy());
        }
        final ArchiveInfo destArchiveInfo;
        if (destCrawledLink.hasArchiveInfo()) {
            destArchiveInfo = destCrawledLink.getArchiveInfo();
        } else {
            destArchiveInfo = null;
        }
        if (sourceCrawledLink.hasArchiveInfo()) {
            if (destArchiveInfo == null) {
                destCrawledLink.setArchiveInfo(new ArchiveInfo().migrate(sourceCrawledLink.getArchiveInfo()));
            } else {
                destArchiveInfo.migrate(sourceCrawledLink.getArchiveInfo());
            }
        }
        convertFilePackageInfos(destCrawledLink);
        permanentOffline(destCrawledLink);
    }

    private PackageInfo convertFilePackageInfos(final CrawledLink link) {
        if (link.getDownloadLink() == null) {
            return null;
        }
        final FilePackage fp = link.getDownloadLink().getFilePackage();
        if (FilePackage.isDefaultFilePackage(fp)) {
            return null;
        }
        fp.remove(link.getDownloadLink());
        if (link.getDesiredPackageInfo() != null && Boolean.TRUE.equals(link.getDesiredPackageInfo().isAllowInheritance())) {
            final Boolean allowInheritance = fp.isAllowInheritance();
            if (allowInheritance == null || allowInheritance == Boolean.FALSE) {
                return link.getDesiredPackageInfo();
            }
        }
        PackageInfo fpi = null;
        if (StringUtils.isNotEmpty(fp.getDownloadDirectory()) && !fp.getDownloadDirectory().equals(defaultDownloadFolder)) {
            // do not set downloadfolder if it is the defaultfolder
            if (fpi == null && (fpi = link.getDesiredPackageInfo()) == null) {
                fpi = new PackageInfo();
            }
            fpi.setDestinationFolder(CrossSystem.fixPathSeparators(fp.getDownloadDirectory() + File.separator));
        }
        final String name = fp.getName();
        if (StringUtils.isNotEmpty(name)) {
            if (fpi == null && (fpi = link.getDesiredPackageInfo()) == null) {
                fpi = new PackageInfo();
            }
            fpi.setName(name);
        }
        final Boolean allowMerge = fp.isAllowMerge();
        if (allowMerge != null) {
            if (allowMerge == Boolean.TRUE) {
                if (fpi != null || (fpi = link.getDesiredPackageInfo()) != null) {
                    fpi.setUniqueId(null);
                }
            } else {
                if (fpi == null && (fpi = link.getDesiredPackageInfo()) == null) {
                    fpi = new PackageInfo();
                }
                fpi.setUniqueId(fp.getUniqueID());
            }
        }
        final String packageKey = fp.getPackageKey();
        if (packageKey != null) {
            if (fpi == null && (fpi = link.getDesiredPackageInfo()) == null) {
                fpi = new PackageInfo();
            }
            fpi.setPackageKey(packageKey);
        }
        final Boolean ignoreVarious = fp.isIgnoreVarious();
        if (ignoreVarious != null) {
            if (fpi == null && (fpi = link.getDesiredPackageInfo()) == null) {
                fpi = new PackageInfo();
            }
            fpi.setIgnoreVarious(ignoreVarious);
        }
        final Boolean allowInheritance = fp.isAllowInheritance();
        if (allowInheritance != null) {
            if (fpi == null && (fpi = link.getDesiredPackageInfo()) == null) {
                fpi = new PackageInfo();
            }
            fpi.setAllowInheritance(allowInheritance);
        }
        if (StringUtils.isNotEmpty(fp.getComment())) {
            if (fpi == null && (fpi = link.getDesiredPackageInfo()) == null) {
                fpi = new PackageInfo();
            }
            fpi.setComment(fp.getComment());
        }
        if (fpi != null) {
            link.setDesiredPackageInfo(fpi);
        }
        return fpi;
    }

    private void permanentOffline(CrawledLink link) {
        final DownloadLink dl = link.getDownloadLink();
        if (dl == null) {
            return;
        }
        try {
            if (dl.getDefaultPlugin().getLazyP().isOfflinePlugin()) {
                final PackageInfo dpi;
                if (link.getDesiredPackageInfo() == null) {
                    dpi = new PackageInfo();
                } else {
                    dpi = link.getDesiredPackageInfo();
                }
                dpi.setUniqueId(PERMANENT_OFFLINE_ID);
                link.setDesiredPackageInfo(dpi);
            }
        } catch (final Throwable e) {
        }
    }

    protected void forwardDownloadLinkInfos(final DownloadLink sourceDownloadLink, final DownloadLink destDownloadLink, final Boolean singleDestDownloadLink) {
        if (sourceDownloadLink == null || destDownloadLink == null || sourceDownloadLink == destDownloadLink) {
            return;
        }
        /* create a copy of ArrayList */
        final List<String> srcPWs = sourceDownloadLink.getSourcePluginPasswordList();
        if (srcPWs != null && srcPWs.size() > 0) {
            destDownloadLink.setSourcePluginPasswordList(new ArrayList<String>(srcPWs));
        }
        if (sourceDownloadLink.getComment() != null && destDownloadLink.getComment() == null) {
            destDownloadLink.setComment(sourceDownloadLink.getComment());
        }
        if (sourceDownloadLink.getContainerUrl() != null && destDownloadLink.getContainerUrl() == null) {
            destDownloadLink.setContainerUrl(sourceDownloadLink.getContainerUrl());
        }
        if (destDownloadLink.getUrlProtection() == UrlProtection.UNSET && sourceDownloadLink.getUrlProtection() != UrlProtection.UNSET) {
            destDownloadLink.setUrlProtection(sourceDownloadLink.getUrlProtection());
        }
        if (Boolean.TRUE.equals(singleDestDownloadLink)) {
            if (!destDownloadLink.isNameSet()) {
                if (sourceDownloadLink.isNameSet()) {
                    destDownloadLink.setName(sourceDownloadLink.getName());
                } else {
                    final String name = getUnsafeName(sourceDownloadLink.getName(), destDownloadLink.getName());
                    if (name != null) {
                        destDownloadLink.setName(name);
                    }
                }
            }
            if (sourceDownloadLink.getForcedFileName() != null && destDownloadLink.getForcedFileName() == null) {
                destDownloadLink.setForcedFileName(sourceDownloadLink.getForcedFileName());
            }
            if (sourceDownloadLink.getFinalFileName() != null && destDownloadLink.getFinalFileName() == null) {
                destDownloadLink.setFinalFileName(sourceDownloadLink.getFinalFileName());
            }
            if (sourceDownloadLink.isAvailabilityStatusChecked() && sourceDownloadLink.getAvailableStatus() != destDownloadLink.getAvailableStatus() && !destDownloadLink.isAvailabilityStatusChecked()) {
                destDownloadLink.setAvailableStatus(sourceDownloadLink.getAvailableStatus());
            }
            if (sourceDownloadLink.getContentUrl() != null && destDownloadLink.getContentUrl() == null) {
                destDownloadLink.setContentUrl(sourceDownloadLink.getContentUrl());
            }
            if (sourceDownloadLink.getVerifiedFileSize() >= 0 && destDownloadLink.getVerifiedFileSize() < 0) {
                destDownloadLink.setVerifiedFileSize(sourceDownloadLink.getVerifiedFileSize());
            }
            if (sourceDownloadLink.hasTempProperties()) {
                destDownloadLink.getTempProperties().setProperties(sourceDownloadLink.getTempProperties().getProperties());
            }
            final Map<String, Object> sourceProperties = sourceDownloadLink.getProperties();
            if (sourceProperties != null && !sourceProperties.isEmpty()) {
                final Map<String, Object> destProperties = destDownloadLink.getProperties();
                if (destProperties == null || destProperties.isEmpty()) {
                    destDownloadLink.setProperties(sourceProperties);
                } else {
                    for (Entry<String, Object> property : sourceProperties.entrySet()) {
                        if (!destDownloadLink.hasProperty(property.getKey())) {
                            destDownloadLink.setProperty(property.getKey(), property.getValue());
                        }
                    }
                }
            }
            if (sourceDownloadLink.getView().getBytesTotal() >= 0 && destDownloadLink.getKnownDownloadSize() < 0) {
                destDownloadLink.setDownloadSize(sourceDownloadLink.getView().getBytesTotal());
            }
        }
    }

    public void stopCrawling() {
        stopCrawling(true);
    }

    public void stopCrawling(final boolean stopChildren) {
        final LinkCrawlerGeneration generation = linkCrawlerGeneration.getAndSet(null);
        if (generation != null) {
            generation.invalidate();
        }
        if (stopChildren) {
            for (final LinkCrawler child : getChildren()) {
                child.stopCrawling(true);
            }
        }
    }

    public boolean waitForCrawling() {
        return waitForCrawling(true);
    }

    private final static Object WAIT = new Object();

    public boolean waitForCrawling(final boolean waitForChildren) {
        while (isRunning(waitForChildren)) {
            synchronized (WAIT) {
                if (isRunning(waitForChildren)) {
                    try {
                        WAIT.wait(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
        return isRunning(waitForChildren) == false;
    }

    public List<LinkCrawler> getChildren() {
        synchronized (children) {
            return new ArrayList<LinkCrawler>(children.keySet());
        }
    }

    public LinkCrawler getRoot() {
        final LinkCrawler parent = getParent();
        if (parent != null) {
            return parent.getRoot();
        } else {
            return this;
        }
    }

    public boolean isRunning() {
        return isRunning(true);
    }

    public boolean isRunning(final boolean checkChildren) {
        synchronized (CRAWLER) {
            if (tasks.size() > 0) {
                return true;
            }
        }
        if (checkChildren) {
            for (final LinkCrawler child : getChildren()) {
                if (child.isRunning(true)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCrawling() {
        synchronized (CRAWLER) {
            return CRAWLER.size() > 0;
        }
    }

    protected void container(final LinkCrawlerGeneration generation, final PluginsC oplg, final CrawledLink cryptedLink) {
        final CrawledLinkModifier parentLinkModifier = cryptedLink.getCustomCrawledLinkModifier();
        cryptedLink.setCustomCrawledLinkModifier(null);
        cryptedLink.setBrokenCrawlerHandler(null);
        final LinkCrawlerTask task;
        if (oplg == null || cryptedLink.getURL() == null) {
            return;
        } else if (isCrawledLinkDuplicated(duplicateFinderContainer, cryptedLink)) {
            onCrawledLinkDuplicate(cryptedLink, DUPLICATE.CONTAINER);
            return;
        } else if (this.isCrawledLinkFiltered(cryptedLink)) {
            return;
        } else if ((task = checkStartNotify(generation, "containerPlugin:" + oplg.getName() + "|" + cryptedLink.getURL())) == null) {
            return;
        }
        try {
            final String[] sourceURLs = getAndClearSourceURLs(cryptedLink);
            processedLinksCounter.incrementAndGet();
            /* set new PluginClassLoaderChild because ContainerPlugin maybe uses Hoster/Crawler */
            final PluginsC plg;
            try {
                plg = oplg.newPluginInstance();
            } catch (final Throwable e) {
                LogController.CL().log(e);
                return;
            }
            /* now we run the plugin and let it find some links */
            final LinkCrawlerThread lct = getCurrentLinkCrawlerThread();
            Object owner = null;
            LinkCrawler previousCrawler = null;
            boolean oldDebug = false;
            boolean oldVerbose = false;
            LogInterface oldLogger = null;
            try {
                final LogInterface logger = LogController.getFastPluginLogger(plg.getName());
                if (lct != null) {
                    /* mark thread to be used by crawler plugin */
                    owner = lct.getCurrentOwner();
                    lct.setCurrentOwner(plg);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                    /* save old logger/states */
                    oldLogger = lct.getLogger();
                    oldDebug = lct.isDebug();
                    oldVerbose = lct.isVerbose();
                    /* set new logger and set verbose/debug true */
                    lct.setLogger(logger);
                    lct.setVerbose(true);
                    lct.setDebug(true);
                }
                plg.setLogger(logger);
                try {
                    final List<CrawledLink> decryptedPossibleLinks = plg.decryptContainer(cryptedLink);
                    /* in case the function returned without exceptions, we can clear log */
                    if (logger instanceof ClearableLogInterface) {
                        ((ClearableLogInterface) logger).clear();
                    }
                    if (decryptedPossibleLinks == null || decryptedPossibleLinks.size() == 0) {
                        return;
                    }
                    /* we found some links, distribute them */
                    final boolean singleDest = decryptedPossibleLinks.size() == 1;
                    for (CrawledLink decryptedPossibleLink : decryptedPossibleLinks) {
                        forwardCrawledLinkInfos(cryptedLink, decryptedPossibleLink, parentLinkModifier, sourceURLs, singleDest);
                    }
                    if (insideCrawlerPlugin()) {
                        /*
                         * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin
                         * waiting for linkcrawler results
                         */
                        if (!generation.isValid()) {
                            /* LinkCrawler got aborted! */
                            return;
                        }
                        distribute(generation, decryptedPossibleLinks);
                    } else {
                        final LinkCrawlerTask innerTask = checkStartNotify(generation, task.getTaskID() + "|containerPool");
                        if (innerTask == null) {
                            return;
                        }
                        /* enqueue distributing of the links */
                        threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                            @Override
                            public long getAverageRuntime() {
                                final Long ret = getDefaultAverageRuntime();
                                if (ret != null) {
                                    return ret;
                                } else {
                                    return super.getAverageRuntime();
                                }
                            }

                            @Override
                            void crawling() {
                                LinkCrawler.this.distribute(generation, decryptedPossibleLinks);
                            }
                        });
                    }
                } finally {
                    /* close the logger */
                    if (logger instanceof ClosableLogInterface) {
                        ((ClosableLogInterface) logger).close();
                    }
                }
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setCurrentOwner(owner);
                    lct.setCurrentLinkCrawler(previousCrawler);
                    lct.setLogger(oldLogger);
                    lct.setVerbose(oldVerbose);
                    lct.setDebug(oldDebug);
                }
            }
        } finally {
            /* restore old ClassLoader for current Thread */
            checkFinishNotify(task);
        }
    }

    private boolean isDuplicatedCrawling(final LazyCrawlerPlugin lazyC, final CrawledLink cryptedLink) {
        String url = cryptedLink.getURL();
        try {
            final URL tmp = URLHelper.createURL(url);
            final String urlDecodedPath = URLDecoder.decode(tmp.getPath(), "UTF-8");
            if (!StringUtils.equals(tmp.getPath(), urlDecodedPath)) {
                url = URLHelper.createURL(tmp.getProtocol(), tmp.getUserInfo(), tmp.getHost(), tmp.getPort(), urlDecodedPath, tmp.getQuery(), tmp.getRef());
            } else {
                if (!StringUtils.contains(url, tmp.getHost())) {
                    url = tmp.toString();
                }
            }
        } catch (Exception ignore) {
        }
        synchronized (duplicateFinderCrawler) {
            Set<String> set = duplicateFinderCrawler.get(lazyC);
            if (set == null) {
                set = new HashSet<String>();
                duplicateFinderCrawler.put(lazyC, set);
            }
            final boolean ret = !set.add(url);
            return ret;
        }
    }

    protected LinkCrawlerThread getCurrentLinkCrawlerThread() {
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof LinkCrawlerThread) {
            return (LinkCrawlerThread) currentThread;
        } else {
            return null;
        }
    }

    protected void crawl(final LinkCrawlerGeneration generation, final LazyCrawlerPlugin lazyC, final CrawledLink cryptedLink) {
        final CrawledLinkModifier parentLinkModifier = cryptedLink.getCustomCrawledLinkModifier();
        cryptedLink.setCustomCrawledLinkModifier(null);
        final BrokenCrawlerHandler brokenCrawler = cryptedLink.getBrokenCrawlerHandler();
        cryptedLink.setBrokenCrawlerHandler(null);
        final LinkCrawlerTask task;
        if (lazyC == null) {
            return;
        } else if (cryptedLink.getCryptedLink() == null) {
            return;
        } else if (isDuplicatedCrawling(lazyC, cryptedLink)) {
            onCrawledLinkDuplicate(cryptedLink, DUPLICATE.CRAWLER);
            return;
        } else if (this.isCrawledLinkFiltered(cryptedLink)) {
            return;
        } else if ((task = checkStartNotify(generation, "crawlPlugin:" + lazyC + "|" + cryptedLink.getURL())) == null) {
            return;
        }
        try {
            final String[] sourceURLs = getAndClearSourceURLs(cryptedLink);
            processedLinksCounter.incrementAndGet();
            final PluginForDecrypt wplg;
            /*
             * we want a fresh pluginClassLoader here
             */
            try {
                wplg = lazyC.newInstance(getPluginClassLoaderChild());
            } catch (final UpdateRequiredClassNotFoundException e1) {
                LogController.CL().log(e1);
                return;
            }
            final AtomicReference<LinkCrawler> nextLinkCrawler = new AtomicReference<LinkCrawler>(this);
            final Browser br = wplg.createNewBrowserInstance();
            wplg.setBrowser(br);
            LogInterface oldLogger = null;
            boolean oldVerbose = false;
            boolean oldDebug = false;
            final LogInterface logger = LogController.getFastPluginLogger(wplg.getCrawlerLoggerID(cryptedLink));
            logger.info("Crawling: " + cryptedLink.getURL());
            wplg.setLogger(logger);
            wplg.init();
            /* now we run the plugin and let it find some links */
            final LinkCrawlerThread lct = getCurrentLinkCrawlerThread();
            Object owner = null;
            LinkCrawlerDistributer dist = null;
            LinkCrawler previousCrawler = null;
            List<DownloadLink> decryptedPossibleLinks = null;
            try {
                final boolean useDelay = wplg.getDistributeDelayerMinimum() > 0;
                final DelayedRunnable finalLinkCrawlerDistributerDelayer;
                final List<CrawledLink> distributedLinks;
                if (useDelay) {
                    distributedLinks = new ArrayList<CrawledLink>();
                    final int minimumDelay = Math.max(10, wplg.getDistributeDelayerMinimum());
                    int maximumDelay = wplg.getDistributeDelayerMaximum();
                    if (maximumDelay == 0) {
                        maximumDelay = -1;
                    }
                    finalLinkCrawlerDistributerDelayer = new DelayedRunnable(TIMINGQUEUE, minimumDelay, maximumDelay) {
                        @Override
                        public String getID() {
                            return "LinkCrawler";
                        }

                        @Override
                        public void delayedrun() {
                            /* we are now in IOEQ, thats why we create copy and then push work back into LinkCrawler */
                            final List<CrawledLink> linksToDistribute;
                            synchronized (distributedLinks) {
                                if (distributedLinks.size() == 0) {
                                    return;
                                } else {
                                    linksToDistribute = new ArrayList<CrawledLink>(distributedLinks);
                                    distributedLinks.clear();
                                }
                            }
                            final LinkCrawlerTask innerTask;
                            if ((innerTask = checkStartNotify(generation, task.getTaskID() + "|crawlPool(1)")) != null) {
                                /* enqueue distributing of the links */
                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                                    @Override
                                    public long getAverageRuntime() {
                                        final Long ret = getDefaultAverageRuntime();
                                        if (ret != null) {
                                            return ret;
                                        } else {
                                            return super.getAverageRuntime();
                                        }
                                    }

                                    @Override
                                    void crawling() {
                                        nextLinkCrawler.get().distribute(generation, linksToDistribute);
                                    }
                                });
                            }
                        }
                    };
                } else {
                    finalLinkCrawlerDistributerDelayer = null;
                    distributedLinks = null;
                }
                /*
                 * set LinkCrawlerDistributer in case the plugin wants to add links in realtime
                 */
                wplg.setDistributer(dist = new LinkCrawlerDistributer() {
                    final HashSet<DownloadLink> fastDuplicateDetector = new HashSet<DownloadLink>();
                    final AtomicInteger         distributed           = new AtomicInteger(0);
                    final HashSet<DownloadLink> distribute            = new HashSet<DownloadLink>();

                    public synchronized void distribute(DownloadLink... links) {
                        if (links == null || (links.length == 0 && wplg.getDistributer() != null)) {
                            return;
                        }
                        for (final DownloadLink link : links) {
                            if (link != null && link.getPluginPatternMatcher() != null && !fastDuplicateDetector.contains(link)) {
                                distribute.add(link);
                            }
                        }
                        if (wplg.getDistributer() != null && (distribute.size() + distributed.get()) <= 1) {
                            /**
                             * crawler is still running, wait for finish or multiple distributed
                             */
                            return;
                        }
                        final List<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(distribute.size());
                        final boolean distributeMultipleLinks = (distribute.size() + distributed.get()) > 1;
                        final String cleanURL = cleanURL(cryptedLink.getCryptedLink().getCryptedUrl());
                        for (final DownloadLink link : distribute) {
                            if (link.getPluginPatternMatcher() != null && fastDuplicateDetector.add(link)) {
                                distributed.incrementAndGet();
                                if (cleanURL != null) {
                                    if (isTempDecryptedURL(link.getPluginPatternMatcher())) {
                                        /**
                                         * some plugins have same regex for hoster/decrypter, so they add decrypted.com at the end
                                         */
                                        if (distributeMultipleLinks) {
                                            if (link.getContainerUrl() == null) {
                                                link.setContainerUrl(cleanURL);
                                            }
                                        } else {
                                            if (link.getContentUrl() == null) {
                                                link.setContentUrl(cleanURL);
                                            }
                                        }
                                    } else {
                                        /**
                                         * this plugin returned multiple links, so we set containerURL (if not set yet)
                                         */
                                        if (distributeMultipleLinks && link.getContainerUrl() == null) {
                                            link.setContainerUrl(cleanURL);
                                        }
                                    }
                                }
                                final CrawledLink crawledLink = wplg.convert(link);
                                forwardCrawledLinkInfos(cryptedLink, crawledLink, parentLinkModifier, sourceURLs, !distributeMultipleLinks);
                                possibleCryptedLinks.add(crawledLink);
                            }
                        }
                        distribute.clear();
                        if (useDelay && wplg.getDistributer() != null) {
                            /* we delay the distribute */
                            synchronized (distributedLinks) {
                                /* synchronized adding */
                                distributedLinks.addAll(possibleCryptedLinks);
                            }
                            /* restart delayer to distribute links */
                            finalLinkCrawlerDistributerDelayer.run();
                        } else if (possibleCryptedLinks.size() > 0) {
                            /* we do not delay the distribute */
                            final LinkCrawlerTask innerTask;
                            if ((innerTask = checkStartNotify(generation, task.getTaskID() + "|crawlPool(2)")) != null) {
                                /* enqueue distributing of the links */
                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation, innerTask) {
                                    @Override
                                    public long getAverageRuntime() {
                                        final Long ret = getDefaultAverageRuntime();
                                        if (ret != null) {
                                            return ret;
                                        } else {
                                            return super.getAverageRuntime();
                                        }
                                    }

                                    @Override
                                    void crawling() {
                                        nextLinkCrawler.get().distribute(generation, possibleCryptedLinks);
                                    }
                                });
                            }
                        }
                    }
                });
                if (lct != null) {
                    /* mark thread to be used by decrypter plugin */
                    owner = lct.getCurrentOwner();
                    lct.setCurrentOwner(wplg);
                    previousCrawler = lct.getCurrentLinkCrawler();
                    lct.setCurrentLinkCrawler(this);
                    /* save old logger/states */
                    oldLogger = lct.getLogger();
                    oldDebug = lct.isDebug();
                    oldVerbose = lct.isVerbose();
                    /* set new logger and set verbose/debug true */
                    lct.setLogger(logger);
                    lct.setVerbose(true);
                    lct.setDebug(true);
                }
                final long startTime = Time.systemIndependentCurrentJVMTimeMillis();
                try {
                    wplg.setCrawler(this);
                    wplg.setLinkCrawlerGeneration(generation);
                    final LinkCrawler pluginNextLinkCrawler = wplg.getCustomNextCrawler();
                    if (pluginNextLinkCrawler != null) {
                        nextLinkCrawler.set(pluginNextLinkCrawler);
                    }
                    decryptedPossibleLinks = wplg.decryptLink(cryptedLink);
                    /* remove distributer from plugin to process remaining/returned links */
                    wplg.setDistributer(null);
                    if (finalLinkCrawlerDistributerDelayer != null) {
                        finalLinkCrawlerDistributerDelayer.setDelayerEnabled(false);
                        /* make sure we dont have any unprocessed delayed Links */
                        finalLinkCrawlerDistributerDelayer.delayedrun();
                    }
                    if (decryptedPossibleLinks != null) {
                        /* distribute remaining/returned links */
                        dist.distribute(decryptedPossibleLinks.toArray(new DownloadLink[decryptedPossibleLinks.size()]));
                    }
                    /* in case we return normally, clear the logger */
                    if (logger instanceof ClearableLogInterface) {
                        ((ClearableLogInterface) logger).clear();
                    }
                } finally {
                    /* close the logger */
                    wplg.setLinkCrawlerGeneration(null);
                    wplg.setCurrentLink(null);
                    final long endTime = Time.systemIndependentCurrentJVMTimeMillis() - startTime;
                    lazyC.updateCrawlRuntime(endTime);
                    if (logger instanceof ClosableLogInterface) {
                        ((ClosableLogInterface) logger).close();
                    }
                }
            } finally {
                if (lct != null) {
                    /* reset thread to last known used state */
                    lct.setCurrentOwner(owner);
                    lct.setCurrentLinkCrawler(previousCrawler);
                    lct.setLogger(oldLogger);
                    lct.setVerbose(oldVerbose);
                    lct.setDebug(oldDebug);
                }
            }
            if (decryptedPossibleLinks == null) {
                this.handleBrokenCrawledLink(cryptedLink);
            }
            if (brokenCrawler != null) {
                try {
                    brokenCrawler.brokenCrawler(cryptedLink, this);
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
        } finally {
            /* restore old ClassLoader for current Thread */
            checkFinishNotify(task);
        }
    }

    public java.util.List<CrawledLink> getCrawledLinks() {
        return crawledLinks;
    }

    public java.util.List<CrawledLink> getFilteredLinks() {
        return filteredLinks;
    }

    public java.util.List<CrawledLink> getBrokenLinks() {
        return brokenLinks;
    }

    public java.util.List<CrawledLink> getUnhandledLinks() {
        return unhandledLinks;
    }

    protected void handleBrokenCrawledLink(CrawledLink link) {
        this.brokenLinksCounter.incrementAndGet();
        getHandler().handleBrokenLink(link);
    }

    protected void handleUnhandledCryptedLink(CrawledLink link) {
        this.unhandledLinksCounter.incrementAndGet();
        getHandler().handleUnHandledLink(link);
    }

    private String getContentURL(final CrawledLink link) {
        final DownloadLink downloadLink = link.getDownloadLink();
        final PluginForHost plugin = downloadLink.getDefaultPlugin();
        if (downloadLink == null || plugin == null) {
            return null;
        }
        final String pluginURL = downloadLink.getPluginPatternMatcher();
        final Iterator<CrawledLink> it = link.iterator();
        while (it.hasNext()) {
            final CrawledLink next = it.next();
            if (next == link) {
                continue;
            }
            if (next.getDownloadLink() != null || next.getCryptedLink() == null) {
                final String nextURL = cleanURL(next.getURL());
                if (nextURL != null && !StringUtils.equals(pluginURL, nextURL)) {
                    final String[] hits = new Regex(nextURL, plugin.getSupportedLinks()).getColumn(-1);
                    if (hits != null) {
                        try {
                            if (hits.length == 1 && hits[0] != null && plugin.isValidURL(hits[0]) && !StringUtils.equals(pluginURL, hits[0]) && new URL(hits[0]).getPath().length() > 1) {
                                return hits[0];
                            } else {
                                return null;
                            }
                        } catch (IOException e) {
                        }
                    }
                }
                if (next.getDownloadLink() != null) {
                    continue;
                }
            }
            break;
        }
        return null;
    }

    private String getOriginURL(final CrawledLink link) {
        final DownloadLink downloadLink = link.getDownloadLink();
        if (downloadLink == null) {
            return null;
        }
        final String pluginURL = downloadLink.getPluginPatternMatcher();
        final Iterator<CrawledLink> it = link.iterator();
        String originURL = null;
        while (it.hasNext()) {
            final CrawledLink next = it.next();
            if (next == link || (next.getDownloadLink() != null && next.getDownloadLink().getUrlProtection() != UrlProtection.UNSET)) {
                originURL = null;
                continue;
            }
            final String nextURL = cleanURL(next.getURL());
            if (nextURL != null && !StringUtils.equals(pluginURL, nextURL)) {
                originURL = nextURL;
            }
        }
        return originURL;
    }

    protected void postprocessFinalCrawledLink(CrawledLink link) {
        final DownloadLink downloadLink = link.getDownloadLink();
        if (downloadLink == null) {
            return;
        }
        final HashSet<String> knownURLs = new HashSet<String>();
        knownURLs.add(downloadLink.getPluginPatternMatcher());
        if (downloadLink.getContentUrl() != null) {
            if (StringUtils.equals(downloadLink.getPluginPatternMatcher(), downloadLink.getContentUrl())) {
                downloadLink.setContentUrl(null);
            }
            knownURLs.add(downloadLink.getContentUrl());
        } else if (true || downloadLink.getContainerUrl() == null) {
            /**
             * remove true in case we don't want a contentURL when containerURL is already set
             */
            final String contentURL = getContentURL(link);
            if (contentURL != null && knownURLs.add(contentURL)) {
                downloadLink.setContentUrl(contentURL);
            }
        }
        if (downloadLink.getContainerUrl() != null) {
            /**
             * containerURLs are only set by crawl or crawlDeeper or manually
             */
            knownURLs.add(downloadLink.getContainerUrl());
        }
        if (downloadLink.getOriginUrl() != null) {
            knownURLs.add(downloadLink.getOriginUrl());
        } else {
            final String originURL = getOriginURL(link);
            if (originURL != null && knownURLs.add(originURL)) {
                downloadLink.setOriginUrl(originURL);
            }
        }
        if (StringUtils.equals(downloadLink.getOriginUrl(), downloadLink.getContainerUrl())) {
            downloadLink.setContainerUrl(null);
        }
        if (downloadLink.getReferrerUrl() == null) {
            final String referrerURL = getReferrerUrl(link);
            if (referrerURL != null && knownURLs.add(referrerURL)) {
                downloadLink.setReferrerUrl(referrerURL);
            }
        }
    }

    public static boolean isTempDecryptedURL(final String url) {
        if (url == null) {
            return false;
        }
        final String host = Browser.getHost(url, true);
        if (StringUtils.containsIgnoreCase(host, "decrypted") || StringUtils.containsIgnoreCase(host, "yt.not.allowed")) {
            return true;
        } else {
            return false;
        }
    }

    public static String cleanURL(String cUrl) {
        final boolean isSupportedProtocol = HTMLParser.isSupportedProtocol(cUrl);
        if (!isSupportedProtocol) {
            return null;
        }
        final String host = Browser.getHost(cUrl, true);
        if (!StringUtils.containsIgnoreCase(host, "decrypted") && !StringUtils.containsIgnoreCase(host, "dummydirect.jdownloader.org") && !StringUtils.containsIgnoreCase(host, "dummycnl.jdownloader.org") && !StringUtils.containsIgnoreCase(host, "yt.not.allowed")) {
            if (cUrl.startsWith("http://") || cUrl.startsWith("https://") || cUrl.startsWith("ftp://") || cUrl.startsWith("file:/")) {
                return cUrl;
            } else if (cUrl.startsWith("m3u8://")) {
                return cUrl.substring("m3u8://".length());
            } else if (cUrl.startsWith("directhttp://")) {
                return cUrl.substring("directhttp://".length());
            } else if (cUrl.startsWith("httpviajd://")) {
                return "http://".concat(cUrl.substring("httpviajd://".length()));
            } else if (cUrl.startsWith("httpsviajd://")) {
                return "https://".concat(cUrl.substring("httpsviajd://".length()));
            } else if (cUrl.startsWith("ftpviajd://")) {
                return "ftp://".concat(cUrl.substring("ftpviajd://".length()));
            } else if (cUrl.startsWith("jd://")) {
                return cUrl.replaceFirst("(?i)^jd://[^/:]+://", "");
            }
        }
        return null;
    }

    protected void handleFinalCrawledLink(LinkCrawlerGeneration generation, CrawledLink link) {
        if (link == null) {
            return;
        }
        final LinkCrawlerRule rule;
        if (link.getDownloadLink() != null && (rule = link.getMatchingRule()) != null) {
            link.getDownloadLink().setProperty("lcrID", rule.getId());
        }
        final CrawledLink origin = link.getOriginLink();
        if (link.getCreated() == -1) {
            link.setCreated(getCreated());
            final CrawledLinkModifier customModifier = link.getCustomCrawledLinkModifier();
            if (customModifier != null) {
                link.setCustomCrawledLinkModifier(null);
                try {
                    customModifier.modifyCrawledLink(link);
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
            postprocessFinalCrawledLink(link);
            /* clean up some references */
            link.setBrokenCrawlerHandler(null);
            link.setUnknownHandler(null);
        }
        if (isDoDuplicateFinderFinalCheck()) {
            /* specialHandling: Crypted A - > B - > Final C , and A equals C */
            // if link comes from flashgot, origin might be null
            final boolean specialHandling = origin != null && (origin != link) && (StringUtils.equals(origin.getLinkID(), link.getLinkID()));
            if (!specialHandling) {
                CrawledLink existing = null;
                synchronized (duplicateFinderFinal) {
                    final String key = Encoding.urlDecode(link.getLinkID(), false);
                    existing = duplicateFinderFinal.get(key);
                    if (existing == null) {
                        duplicateFinderFinal.put(key, link);
                    }
                }
                if (existing != null) {
                    final PluginForHost hPlugin = link.gethPlugin();
                    if (hPlugin == null || hPlugin.onLinkCrawlerDupeFilterEnabled(existing, link)) {
                        onCrawledLinkDuplicate(link, DUPLICATE.FINAL);
                        return;
                    }
                }
            }
        }
        enqueueFinalCrawledLink(generation, link);
    }

    protected void enqueueFinalCrawledLink(LinkCrawlerGeneration generation, CrawledLink link) {
        if (isCrawledLinkFiltered(link) == false) {
            /* link is not filtered, so we can process it normally */
            crawledLinksCounter.incrementAndGet();
            getHandler().handleFinalLink(link);
        }
    }

    protected boolean isCrawledLinkFiltered(CrawledLink link) {
        final LinkCrawler parent = getParent();
        if (parent != null && getFilter() != parent.getFilter()) {
            if (parent.isCrawledLinkFiltered(link)) {
                return true;
            }
        }
        if (getFilter().dropByUrl(link)) {
            filteredLinksCounter.incrementAndGet();
            getHandler().handleFilteredLink(link);
            return true;
        } else {
            return false;
        }
    }

    protected void onCrawledLinkDuplicate(CrawledLink link, DUPLICATE duplicate) {
    }

    public int getCrawledLinksFoundCounter() {
        return crawledLinksCounter.get();
    }

    public int getFilteredLinksFoundCounter() {
        return filteredLinksCounter.get();
    }

    public int getBrokenLinksFoundCounter() {
        return brokenLinksCounter.get();
    }

    public int getUnhandledLinksFoundCounter() {
        return unhandledLinksCounter.get();
    }

    public int getProcessedLinksCounter() {
        return crawledLinksCounter.get() + filteredLinksCounter.get() + brokenLinksCounter.get() + processedLinksCounter.get();
    }

    public LinkCrawlerDeepInspector defaultDeepInspector() {
        return new LinkCrawlerDeepInspector() {
            @Override
            public List<CrawledLink> deepInspect(LinkCrawler lc, final LinkCrawlerGeneration generation, final Browser br, final URLConnectionAdapter urlConnection, final CrawledLink link) throws Exception {
                final int limit = Math.max(1 * 1024 * 1024, CONFIG.getDeepDecryptLoadLimit());
                if (br != null) {
                    br.setLoadLimit(limit);
                }
                final LinkCrawlerRule rule = link.getMatchingRule();
                if (rule == null && !urlConnection.isContentDisposition()) {
                    final boolean hasContentType = urlConnection.getHeaderField(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE) != null;
                    if (urlConnection.getRequest().getLocation() == null && urlConnection.getResponseCode() == 200 && !isTextContent(urlConnection) || urlConnection.getCompleteContentLength() > limit) {
                        if (!hasContentType) {
                            try {
                                br.followConnection();
                                if (br.containsHTML("<!DOCTYPE html>") || (br.containsHTML("</html") && br.containsHTML("<html"))) {
                                    return null;
                                }
                            } catch (final IOException e) {
                                final LogInterface log = br.getLogger();
                                if (log != null) {
                                    log.log(e);
                                }
                            }
                        }
                        urlConnection.disconnect();
                        final ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
                        final CrawledLink direct = createDirectHTTPCrawledLink(link, null, urlConnection);
                        if (direct != null) {
                            ret.add(direct);
                        }
                        return ret;
                    }
                }
                if (looksLikeDownloadableContent(urlConnection)) {
                    if (rule != null && RULE.DEEPDECRYPT.equals(rule.getRule()) && isTextContent(urlConnection)) {
                        br.followConnection();
                        return null;
                    }
                    urlConnection.disconnect();
                    final ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
                    final CrawledLink direct = lc.createDirectHTTPCrawledLink(link, null, urlConnection);
                    if (direct != null) {
                        ret.add(direct);
                    }
                    return ret;
                } else {
                    br.followConnection();
                    if (br.containsHTML("^#EXTM3U")) {
                        /* auto m3u8 handling of URLs without .m3u8 in URL */
                        final ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
                        ret.add(lc.crawledLinkFactorybyURL("m3u8://" + br._getURL().toExternalForm()));
                        return ret;
                    } else if (rule != null && RULE.DEEPDECRYPT.equals(rule.getRule())) {
                        return null;
                    }
                    try {
                        final LazyCrawlerPlugin lazyC = lc.getLazyGenericHttpDirectoryCrawlerPlugin();
                        if (lazyC == null) {
                            throw new UpdateRequiredClassNotFoundException("could not find 'GenericHttpDirectoryCrawlerPlugin' crawler plugin");
                        }
                        final ArrayList<DownloadLink> directoryContent = lc.invokeLazyCrawlerPlugin(generation, null, lazyC, link, new LazyCrawlerPluginInvokation<ArrayList<DownloadLink>>() {
                            @Override
                            public ArrayList<DownloadLink> invoke(PluginForDecrypt plugin) throws Exception {
                                plugin.setBrowser(br);
                                return ((abstractGenericHTTPDirectoryIndexCrawler) plugin).parseHTTPDirectory(new CryptedLink(br.getURL(), link), br);
                            }
                        });
                        if (directoryContent != null && directoryContent.size() > 0) {
                            /* Let http directory crawler process this link. */
                            final ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
                            ret.add(lc.crawledLinkFactorybyURL("jd://directoryindex://" + br._getURL().toExternalForm()));
                            return ret;
                        }
                    } catch (final Throwable e) {
                        LogController.CL().log(e);
                    }
                    return null;
                }
            }
        };
    }

    public LinkCrawlerHandler defaultHandlerFactory() {
        return new LinkCrawlerHandler() {
            public void handleFinalLink(CrawledLink link) {
                synchronized (crawledLinks) {
                    crawledLinks.add(link);
                }
            }

            public void handleFilteredLink(CrawledLink link) {
                synchronized (filteredLinks) {
                    filteredLinks.add(link);
                }
            }

            @Override
            public void handleBrokenLink(CrawledLink link) {
                synchronized (brokenLinks) {
                    brokenLinks.add(link);
                }
            }

            @Override
            public void handleUnHandledLink(CrawledLink link) {
                synchronized (unhandledLinks) {
                    unhandledLinks.add(link);
                }
            }
        };
    }

    public LinkCrawlerFilter defaultFilterFactory() {
        return new LinkCrawlerFilter() {
            public boolean dropByUrl(CrawledLink link) {
                return false;
            }

            public boolean dropByFileProperties(CrawledLink link) {
                return false;
            };
        };
    }

    public void setFilter(final LinkCrawlerFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter is null");
        }
        this.filter = filter;
    }

    public void setHandler(final LinkCrawlerHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        this.handler = handler;
    }

    public void setDeepInspector(final LinkCrawlerDeepInspector deepInspector) {
        if (deepInspector == null) {
            throw new IllegalArgumentException("deepInspector is null");
        }
        this.deepInspector = deepInspector;
    }

    public LinkCrawlerFilter getFilter() {
        return filter;
    }

    public LinkCrawlerHandler getHandler() {
        return this.handler;
    }

    public LinkCrawlerDeepInspector getDeepInspector() {
        return this.deepInspector;
    }
}
