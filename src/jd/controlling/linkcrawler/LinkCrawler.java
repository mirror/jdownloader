package jd.controlling.linkcrawler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.controlling.HTACCESSController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
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

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialogInterface;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.auth.BasicAuth;
import org.jdownloader.auth.InvalidBasicAuthFormatException;
import org.jdownloader.auth.Login;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.UrlProtection;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPluginClass;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.container.ContainerPluginController;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class LinkCrawler {

    private final static String                            DIRECT_HTTP                 = "directhttp";
    private final static String                            HTTP_LINKS                  = "http links";
    private final LazyHostPlugin                           httpPlugin;
    private final LazyHostPlugin                           directPlugin;
    private final LazyHostPlugin                           ftpPlugin;
    private java.util.List<CrawledLink>                    crawledLinks                = new ArrayList<CrawledLink>();
    private AtomicInteger                                  crawledLinksCounter         = new AtomicInteger(0);
    private java.util.List<CrawledLink>                    filteredLinks               = new ArrayList<CrawledLink>();
    private AtomicInteger                                  filteredLinksCounter        = new AtomicInteger(0);
    private java.util.List<CrawledLink>                    brokenLinks                 = new ArrayList<CrawledLink>();
    private AtomicInteger                                  brokenLinksCounter          = new AtomicInteger(0);
    private java.util.List<CrawledLink>                    unhandledLinks              = new ArrayList<CrawledLink>();
    private final AtomicInteger                            unhandledLinksCounter       = new AtomicInteger(0);
    private final AtomicInteger                            processedLinksCounter       = new AtomicInteger(0);

    private final AtomicBoolean                            runningState                = new AtomicBoolean(false);
    private final AtomicInteger                            crawler                     = new AtomicInteger(0);
    private final static AtomicInteger                     CRAWLER                     = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Object>        duplicateFinderContainer;
    private final ConcurrentHashMap<String, Object>        duplicateFinderCrawler;
    private final ConcurrentHashMap<String, Object>        duplicateFinderFinal;
    private final ConcurrentHashMap<String, Object>        duplicateFinderDeep;
    private LinkCrawlerHandler                             handler                     = null;
    protected static final ThreadPoolExecutor              threadPool;

    private LinkCrawlerFilter                              filter                      = null;
    private final AtomicBoolean                            allowCrawling               = new AtomicBoolean(true);
    protected final AtomicInteger                          crawlerGeneration           = new AtomicInteger(0);
    private final LinkCrawler                              parentCrawler;
    private final long                                     created;

    public final static String                             PACKAGE_ALLOW_MERGE         = "ALLOW_MERGE";
    public final static String                             PACKAGE_CLEANUP_NAME        = "CLEANUP_NAME";
    public final static String                             PACKAGE_IGNORE_VARIOUS      = "PACKAGE_IGNORE_VARIOUS";
    public static final UniqueAlltimeID                    PERMANENT_OFFLINE_ID        = new UniqueAlltimeID();
    private boolean                                        doDuplicateFinderFinalCheck = true;
    private final List<LazyHostPlugin>                     unsortedLazyHostPlugins;
    private List<LazyCrawlerPlugin>                        unsortedLazyCrawlerPlugins;
    protected final PluginClassLoaderChild                 classLoader;
    private boolean                                        directHttpEnabled           = true;
    private final String                                   defaultDownloadFolder;
    private final AtomicReference<List<LazyCrawlerPlugin>> sortedLazyCrawlerPlugins    = new AtomicReference<List<LazyCrawlerPlugin>>();
    private final AtomicReference<List<LazyHostPlugin>>    sortedLazyHostPlugins       = new AtomicReference<List<LazyHostPlugin>>();

    private final List<LinkCrawlerRule>                    linkCrawlerRules;
    private LinkCrawlerDeepInspector                       deepInspector               = null;

    protected List<LinkCrawlerRule> getLinkCrawlerRules() {
        return listLinkCrawlerRules();
    }

    private final static LinkCrawlerConfig CONFIG = JsonConfig.create(LinkCrawlerConfig.class);

    public static LinkCrawlerConfig getConfig() {
        return CONFIG;
    }

    public void setDirectHttpEnabled(boolean directHttpEnabled) {
        this.directHttpEnabled = directHttpEnabled;
    }

    protected final static ScheduledExecutorService TIMINGQUEUE = DelayedRunnable.getNewScheduledExecutorService();

    public boolean isDoDuplicateFinderFinalCheck() {
        if (parentCrawler != null) {
            parentCrawler.isDoDuplicateFinderFinalCheck();
        }
        return doDuplicateFinderFinalCheck;
    }

    protected Long getDefaultAverageRuntime() {
        return null;
    }

    /*
     * customized comparator we use to prefer faster decrypter plugins over slower ones
     */

    static {
        final int maxThreads = Math.max(CONFIG.getMaxThreads(), 1);
        final int keepAlive = Math.max(CONFIG.getThreadKeepAlive(), 100);
        /**
         * PriorityBlockingQueue leaks last Item for some java versions
         *
         * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7161229
         */
        threadPool = new ThreadPoolExecutor(0, maxThreads, keepAlive, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(100, new Comparator<Runnable>() {
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

        }, new ThreadPoolExecutor.AbortPolicy()) {

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                /*
                 * WORKAROUND for stupid SUN /ORACLE way of "how a threadpool should work" !
                 */
                int working = threadPool.getActiveCount();
                int active = threadPool.getPoolSize();
                int max = threadPool.getMaximumPoolSize();
                if (active < max) {
                    if (working == active) {
                        /*
                         * we can increase max pool size so new threads get started
                         */
                        threadPool.setCorePoolSize(Math.min(max, active + 1));
                    }
                }
            }

        };
        threadPool.allowCoreThreadTimeOut(true);
    }

    private static LinkCrawlerEventSender EVENTSENDER = new LinkCrawlerEventSender();

    public static LinkCrawlerEventSender getGlobalEventSender() {
        return EVENTSENDER;
    }

    public LinkCrawler() {
        this(true, true);
    }

    public static LinkCrawler newInstance() {
        final LinkCrawler lc;
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            final LinkCrawlerThread thread = (LinkCrawlerThread) (Thread.currentThread());
            Object owner = thread.getCurrentOwner();
            final CrawledLink source;
            if (owner instanceof PluginForDecrypt) {
                source = ((PluginForDecrypt) owner).getCurrentLink();
            } else {
                source = null;
            }
            final LinkCrawler parent = thread.getCurrentLinkCrawler();
            lc = new LinkCrawler(false, false) {

                @Override
                protected CrawledLink crawledLinkFactorybyURL(String url) {
                    final CrawledLink ret = new CrawledLink(url);
                    if (source != null) {
                        ret.setSourceLink(source);
                    }
                    return ret;
                }

                @Override
                public int getCrawlerGeneration(boolean thisGeneration) {
                    if (!thisGeneration && parent != null) {
                        return Math.max(crawlerGeneration.get(), parent.getCrawlerGeneration(false));
                    }
                    return crawlerGeneration.get();
                }

                @Override
                protected boolean distributeCrawledLink(CrawledLink crawledLink) {
                    return crawledLink != null && crawledLink.getSourceUrls() == null;
                }

                @Override
                protected void postprocessFinalCrawledLink(CrawledLink link) {
                }

                @Override
                protected void preprocessFinalCrawledLink(CrawledLink link) {
                }

            };
        } else {
            lc = new LinkCrawler(true, true);
        }
        return lc;
    }

    protected PluginClassLoaderChild getPluginClassLoaderChild() {
        return classLoader;
    }

    public static synchronized boolean addLinkCrawlerRule(LinkCrawlerRule rule) {
        if (rule != null) {
            List<LinkCrawlerRuleStorable> rules = CONFIG.getLinkCrawlerRules();
            if (rules == null) {
                rules = new ArrayList<LinkCrawlerRuleStorable>();
            }
            for (LinkCrawlerRuleStorable existingRule : rules) {
                if (existingRule.getId() == rule.getId() || (existingRule.getRule() == rule.getRule() && StringUtils.equals(existingRule.getPattern(), rule.getPattern()))) {
                    return false;
                }
            }
            rules.add(new LinkCrawlerRuleStorable(rule));
            CONFIG.setLinkCrawlerRules(rules);
            return true;
        }
        return false;
    }

    public static synchronized List<LinkCrawlerRule> listLinkCrawlerRules() {
        final ArrayList<LinkCrawlerRule> linkCrawlerRules = new ArrayList<LinkCrawlerRule>();
        final List<LinkCrawlerRuleStorable> rules = CONFIG.getLinkCrawlerRules();
        if (rules != null) {
            for (LinkCrawlerRuleStorable rule : rules) {
                try {
                    linkCrawlerRules.add(rule._getLinkCrawlerRule());
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
        }
        return linkCrawlerRules;
    }

    public LinkCrawler(boolean connectParentCrawler, boolean avoidDuplicates) {
        setFilter(defaultFilterFactory());
        if (connectParentCrawler && Thread.currentThread() instanceof LinkCrawlerThread) {
            /* forward crawlerGeneration from parent to this child */
            LinkCrawlerThread thread = (LinkCrawlerThread) Thread.currentThread();
            parentCrawler = thread.getCurrentLinkCrawler();
            classLoader = parentCrawler.getPluginClassLoaderChild();
            this.unsortedLazyHostPlugins = parentCrawler.unsortedLazyHostPlugins;
            this.directPlugin = parentCrawler.directPlugin;
            this.httpPlugin = parentCrawler.httpPlugin;
            this.ftpPlugin = parentCrawler.ftpPlugin;
            this.directHttpEnabled = parentCrawler.directHttpEnabled;
            this.defaultDownloadFolder = parentCrawler.defaultDownloadFolder;
            duplicateFinderContainer = parentCrawler.duplicateFinderContainer;
            duplicateFinderCrawler = parentCrawler.duplicateFinderCrawler;
            duplicateFinderFinal = parentCrawler.duplicateFinderFinal;
            duplicateFinderDeep = parentCrawler.duplicateFinderDeep;
            linkCrawlerRules = parentCrawler.linkCrawlerRules;
            setHandler(parentCrawler.getHandler());
            setDeepInspector(parentCrawler.getDeepInspector());
        } else {
            duplicateFinderContainer = new ConcurrentHashMap<String, Object>(8, 0.9f, 1);
            duplicateFinderCrawler = new ConcurrentHashMap<String, Object>(8, 0.9f, 1);
            duplicateFinderFinal = new ConcurrentHashMap<String, Object>(8, 0.9f, 1);
            duplicateFinderDeep = new ConcurrentHashMap<String, Object>(8, 0.9f, 1);
            if (CONFIG.isLinkCrawlerRulesEnabled()) {
                this.linkCrawlerRules = Collections.unmodifiableList(getLinkCrawlerRules());
            } else {
                linkCrawlerRules = null;
            }
            setHandler(defaulHandlerFactory());
            setDeepInspector(defaultDeepInspector());
            defaultDownloadFolder = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
            parentCrawler = null;
            classLoader = PluginClassLoader.getInstance().getChild();
            final Collection<LazyHostPlugin> lazyHostPlugins = HostPluginController.getInstance().list();
            this.unsortedLazyHostPlugins = new ArrayList<LazyHostPlugin>(lazyHostPlugins.size());
            LazyHostPlugin ftpPlugin = null;
            LazyHostPlugin httpPlugin = null;
            LazyHostPlugin directPlugin = null;
            for (LazyHostPlugin lazyHostPlugin : lazyHostPlugins) {
                if (ftpPlugin != null && httpPlugin != null && directPlugin != null) {
                    this.unsortedLazyHostPlugins.add(lazyHostPlugin);
                } else {
                    if (httpPlugin == null && HTTP_LINKS.equals(lazyHostPlugin.getDisplayName())) {
                        /* for direct access to the directhttp plugin */
                        // we have at least 2 directHTTP entries in pHost. each one listens to a different regex
                        // the one we found here listens to "https?viajd://[\\w\\.:\\-@]*/.*\\.(jdeatme|3gp|7zip|7z|abr...
                        // the other listens to directhttp://.+
                        httpPlugin = lazyHostPlugin;
                    } else if (ftpPlugin == null && "ftp".equals(lazyHostPlugin.getDisplayName())) {
                        /* for generic ftp sites */
                        ftpPlugin = lazyHostPlugin;
                    } else if (directPlugin == null && DIRECT_HTTP.equals(lazyHostPlugin.getDisplayName())) {
                        directPlugin = lazyHostPlugin;
                    } else {
                        this.unsortedLazyHostPlugins.add(lazyHostPlugin);
                    }
                }
            }
            this.ftpPlugin = ftpPlugin;
            this.directPlugin = directPlugin;
            this.httpPlugin = httpPlugin;
        }
        this.created = System.currentTimeMillis();
        this.doDuplicateFinderFinalCheck = avoidDuplicates;
    }

    public long getCreated() {
        if (parentCrawler != null) {
            return parentCrawler.getCreated();
        }
        return created;
    }

    /**
     * returns the generation of this LinkCrawler if thisGeneration is true.
     *
     * if a parent LinkCrawler does exist and thisGeneration is false, we return the older generation of the parent LinkCrawler or this
     * child
     *
     * @param thisGeneration
     * @return
     */
    public int getCrawlerGeneration(boolean thisGeneration) {
        if (!thisGeneration && parentCrawler != null) {
            return Math.max(crawlerGeneration.get(), parentCrawler.getCrawlerGeneration(false));
        }
        return crawlerGeneration.get();
    }

    protected CrawledLink crawledLinkFactorybyURL(String url) {
        return new CrawledLink(url);
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
        if (parentCrawler != null && parentCrawler.isBlacklisted(plugin)) {
            return true;
        }
        return crawlerPluginBlacklist.contains(plugin.getDisplayName());
    }

    public boolean isBlacklisted(LazyHostPlugin plugin) {
        if (parentCrawler != null && parentCrawler.isBlacklisted(plugin)) {
            return true;
        }
        return hostPluginBlacklist.contains(plugin.getDisplayName());
    }

    public void setHostPluginBlacklist(String[] list) {
        HashSet<String> lhostPluginBlacklist = new HashSet<String>();
        if (list != null) {
            for (String s : list) {
                lhostPluginBlacklist.add(s);
            }
        }
        this.hostPluginBlacklist = lhostPluginBlacklist;
    }

    public void crawl(final String text, final String url, final boolean allowDeep) {
        if (!StringUtils.isEmpty(text)) {
            final int generation = this.getCrawlerGeneration(true);
            if (checkStartNotify(generation)) {
                try {
                    if (insideCrawlerPlugin()) {
                        final List<CrawledLink> links = find(text, url, allowDeep, true);
                        crawl(generation, links);
                    } else {
                        if (checkStartNotify(generation)) {
                            threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                @Override
                                public long getAverageRuntime() {
                                    final Long ret = getDefaultAverageRuntime();
                                    if (ret != null) {
                                        return ret;
                                    }
                                    return super.getAverageRuntime();
                                }

                                @Override
                                void crawling() {
                                    java.util.List<CrawledLink> links = find(text, url, allowDeep, true);
                                    crawl(getGeneration(), links);
                                }
                            });
                        }
                    }
                } finally {
                    checkFinishNotify();
                }
            }
        }
    }

    public java.util.List<CrawledLink> find(String text, String url, final boolean allowDeep) {
        return find(text, url, allowDeep, false);
    }

    protected java.util.List<CrawledLink> find(String text, String url, final boolean allowDeep, final boolean allowInstantCrawl) {
        final HtmlParserResultSet resultSet;
        if (allowInstantCrawl && Thread.currentThread() instanceof LinkCrawlerThread) {
            final int generation = this.getCrawlerGeneration(true);
            resultSet = new HtmlParserResultSet() {
                private final HashSet<CharSequence> fastResults = new HashSet<CharSequence>();

                @Override
                public boolean add(HtmlParserCharSequence e) {
                    final boolean ret = super.add(e);
                    if (ret && (!e.contains("...") && !e.equals(getBaseURL()))) {
                        fastResults.add(e);
                        final CrawledLink crawledLink = crawledLinkFactorybyURL(e.toString());
                        crawledLink.setCrawlDeep(allowDeep);
                        final ArrayList<CrawledLink> crawledLinks = new ArrayList<CrawledLink>(1);
                        crawledLinks.add(crawledLink);
                        crawl(generation, crawledLinks);
                    }
                    return ret;
                };

                @Override
                protected LinkedHashSet<String> exportResults() {
                    final LinkedHashSet<String> ret = new LinkedHashSet<String>();
                    for (HtmlParserCharSequence result : this.getResults()) {
                        if (!fastResults.contains(result)) {
                            ret.add(result.toString());
                        }
                    }
                    return ret;
                }
            };
        } else {
            resultSet = new HtmlParserResultSet();
        }
        final String[] possibleLinks = HTMLParser.getHttpLinks(text, url, resultSet);
        if (possibleLinks != null && possibleLinks.length > 0) {
            final List<CrawledLink> possibleCryptedLinks = new ArrayList<CrawledLink>(possibleLinks.length);
            for (final String possibleLink : possibleLinks) {
                final CrawledLink link = crawledLinkFactorybyURL(possibleLink);
                link.setCrawlDeep(allowDeep);
                possibleCryptedLinks.add(link);
            }
            return possibleCryptedLinks;
        }
        return null;
    }

    public void crawl(final List<CrawledLink> possibleCryptedLinks) {
        crawl(this.getCrawlerGeneration(true), possibleCryptedLinks);
    }

    protected void crawl(int generation, final List<CrawledLink> possibleCryptedLinks) {
        if (possibleCryptedLinks != null && possibleCryptedLinks.size() > 0) {
            if (checkStartNotify(generation)) {
                try {
                    if (insideCrawlerPlugin()) {
                        /*
                         * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin
                         * waiting for linkcrawler results
                         */
                        distribute(generation, possibleCryptedLinks);
                    } else {
                        /*
                         * enqueue this cryptedLink for decrypting
                         */
                        if (checkStartNotify(generation)) {
                            threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                @Override
                                public long getAverageRuntime() {
                                    final Long ret = getDefaultAverageRuntime();
                                    if (ret != null) {
                                        return ret;
                                    }
                                    return super.getAverageRuntime();
                                }

                                @Override
                                void crawling() {
                                    distribute(getGeneration(), possibleCryptedLinks);
                                }
                            });
                        }

                    }
                } finally {
                    checkFinishNotify();
                }
            }
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
    protected void checkFinishNotify() {
        if (crawler.decrementAndGet() == 0) {
            /* this LinkCrawler instance stopped, notify static counter */
            final boolean event;
            synchronized (LinkCrawler.this) {
                if (crawler.get() == 0 && runningState.compareAndSet(true, false)) {
                    event = CRAWLER.decrementAndGet() == 0;
                } else {
                    event = false;
                }
            }
            if (event) {
                synchronized (this) {
                    this.notifyAll();
                }
                /*
                 * all tasks are done , we can now cleanup our duplicateFinder
                 */
                duplicateFinderContainer.clear();
                duplicateFinderCrawler.clear();
                duplicateFinderFinal.clear();
                duplicateFinderDeep.clear();
                EVENTSENDER.fireEvent(new LinkCrawlerEvent(this, LinkCrawlerEvent.Type.STOPPED));
                crawlerStopped();
            }
        }
    }

    protected void crawlerStopped() {
    }

    protected void crawlerStarted() {
    }

    private boolean checkStartNotify(int generation) {
        if (checkAllowStart(generation)) {
            if (crawler.getAndIncrement() == 0) {
                final boolean event;
                synchronized (LinkCrawler.this) {
                    event = runningState.compareAndSet(false, true);
                    if (event) {
                        CRAWLER.incrementAndGet();
                    }
                }
                if (event) {
                    EVENTSENDER.fireEvent(new LinkCrawlerEvent(this, LinkCrawlerEvent.Type.STARTED));
                    crawlerStarted();
                }
            }
            return true;
        }
        return false;
    }

    protected URLConnectionAdapter openCrawlDeeperConnection(Browser br, CrawledLink source) throws IOException {
        final HashSet<String> loopAvoid = new HashSet<String>();
        Request request = new GetRequest(source.getURL());
        loopAvoid.add(request.getUrl());
        URLConnectionAdapter connection = null;
        for (int i = 0; i < 10; i++) {
            final ArrayList<String> basicAuths = new ArrayList<String>();
            final String basicAuthinURL = new Regex(request.getUrl(), "https?://(.+)@.*?($|/)").getMatch(0);
            if (basicAuthinURL != null) {
                basicAuths.add("Basic " + Encoding.Base64Encode(basicAuthinURL));
            }
            final List<Login> knownAuths = AuthenticationController.getInstance().getSortedLoginsList(request.getUrl());
            if (knownAuths != null) {
                for (final Login knownAuth : knownAuths) {
                    final String basicAuth = knownAuth.toBasicAuth();
                    if (StringUtils.isNotEmpty(basicAuth)) {
                        basicAuths.add(basicAuth);
                    }
                }
            }
            basicAuths.add("");
            basicAuths.add(null);
            authLoop: for (String basicAuth : basicAuths) {
                if (connection != null) {
                    connection.setAllowedResponseCodes(new int[] { connection.getResponseCode() });
                    br.followConnection();
                }
                final boolean ask = basicAuth == null;
                boolean remember = false;
                if (ask) {
                    final LoginDialog loginDialog = new LoginDialog(UIOManager.LOGIC_COUNTDOWN, _GUI._.AskForPasswordDialog_AskForPasswordDialog_title_(), _JDT._.Plugin_requestLogins_message(), new AbstractIcon(IconKey.ICON_PASSWORD, 32));
                    loginDialog.setTimeout(60 * 1000);
                    final LoginDialogInterface handle = UIOManager.I().show(LoginDialogInterface.class, loginDialog);
                    final String userNameAndPassword;
                    if (handle.getCloseReason() == CloseReason.OK) {
                        userNameAndPassword = handle.getUsername() + ":" + handle.getPassword();
                        remember = handle.isRememberSelected();
                    } else {
                        userNameAndPassword = null;
                    }
                    if (StringUtils.isEmpty(userNameAndPassword)) {
                        return connection;
                    } else {
                        basicAuth = "Basic " + Encoding.Base64Encode(userNameAndPassword);
                    }
                }
                request = request.cloneRequest();
                if (StringUtils.isNotEmpty(basicAuth)) {
                    request.getHeaders().put("Authorization", basicAuth);
                } else {
                    request.getHeaders().remove("Authorization");
                }
                connection = br.openRequestConnection(request);
                if (connection.getResponseCode() == 401 || connection.getResponseCode() == 403) {
                    if (connection.getHeaderField("WWW-Authenticate") == null) {
                        return connection;
                    }
                    if (StringUtils.isNotEmpty(basicAuth)) {
                        try {
                            AuthenticationController.getInstance().invalidate(new BasicAuth(basicAuth), request.getUrl());
                        } catch (InvalidBasicAuthFormatException ignore) {
                        }
                    }
                    continue authLoop;
                } else if (connection.isOK()) {
                    if (StringUtils.isNotEmpty(basicAuth)) {
                        try {
                            final BasicAuth auth = new BasicAuth(basicAuth);
                            if (ask && remember) {
                                HTACCESSController.getInstance().addValidatedAuthentication(request.getUrl(), auth.getUsername(), auth.getPassword());
                            }
                            AuthenticationController.getInstance().validate(auth, request.getUrl());
                        } catch (InvalidBasicAuthFormatException ignore) {
                        }
                    }
                    break authLoop;
                } else {
                    return connection;
                }
            }
            final String location = request.getLocation();
            if (location != null) {
                try {
                    br.followConnection();
                } catch (Throwable e) {
                }
                if (loopAvoid.add(location) == false) {
                    return connection;
                }
                request = br.createRedirectFollowingRequest(request);
            } else {
                return connection;
            }
        }
        return connection;
    }

    protected void crawlDeeper(final int generation, final CrawledLink source) {
        final CrawledLinkModifier sourceLinkModifier = source.getCustomCrawledLinkModifier();
        source.setCustomCrawledLinkModifier(null);
        source.setBrokenCrawlerHandler(null);
        if (source == null || source.getURL() == null || duplicateFinderDeep.putIfAbsent(source.getURL(), this) != null || this.isCrawledLinkFiltered(source)) {
            return;
        }
        final CrawledLinkModifier lm = new CrawledLinkModifier() {
            /*
             * this modifier sets the BrowserURL if not set yet
             */
            public void modifyCrawledLink(CrawledLink link) {
                if (sourceLinkModifier != null) {
                    sourceLinkModifier.modifyCrawledLink(link);
                }
                if (link.getDownloadLink() != null && link.getDownloadLink().getContainerUrl() == null) {
                    link.getDownloadLink().setContainerUrl(source.getURL());
                }
            }

        };
        if (checkStartNotify(generation)) {
            final String[] sourceURLs = getAndClearSourceURLs(source);
            try {
                Browser br = null;
                try {
                    processedLinksCounter.incrementAndGet();
                    if (StringUtils.startsWithCaseInsensitive(source.getURL(), "file:/")) {
                        final File file = new File(new URI(source.getURL()));
                        if (file.exists() && file.isFile()) {
                            final int readLimit = Math.max(1 * 1024 * 1024, CONFIG.getDeepDecryptLoadLimit());
                            final String fileContent = new String(IO.readFile(file, readLimit), "UTF-8");
                            final List<CrawledLink> fileContentLinks = find(fileContent, null, false);
                            if (fileContentLinks != null) {
                                final boolean singleDest = fileContentLinks.size() == 1;
                                for (final CrawledLink fileContentLink : fileContentLinks) {
                                    forwardCrawledLinkInfos(source, fileContentLink, lm, sourceURLs, singleDest);
                                }
                                crawl(generation, fileContentLinks);
                            }
                        }
                    } else {
                        br = new Browser();
                        br.setFollowRedirects(false);
                        final URLConnectionAdapter connection = openCrawlDeeperConnection(br, source);
                        final LinkCrawlerRule matchingRule = source.getMatchingRule();
                        if (matchingRule != null && LinkCrawlerRule.RULE.FOLLOWREDIRECT.equals(matchingRule.getRule())) {
                            try {
                                br.getHttpConnection().disconnect();
                            } catch (Throwable e) {
                            }
                            final CrawledLink followRedirectLink = crawledLinkFactorybyURL(connection.getRequest().getUrl());
                            forwardCrawledLinkInfos(source, followRedirectLink, lm, sourceURLs, true);
                            final ArrayList<CrawledLink> followRedirectLinks = new ArrayList<CrawledLink>();
                            followRedirectLinks.add(followRedirectLink);
                            crawl(generation, followRedirectLinks);
                        } else {
                            final List<CrawledLink> inspectedLinks = getDeepInspector().deepInspect(this, br, connection, source);
                            /*
                             * downloadable content, we use directhttp and distribute the url
                             */
                            if (inspectedLinks != null) {
                                if (inspectedLinks.size() >= 0) {
                                    final boolean singleDest = inspectedLinks.size() == 1;
                                    for (final CrawledLink possibleCryptedLink : inspectedLinks) {
                                        forwardCrawledLinkInfos(source, possibleCryptedLink, lm, sourceURLs, singleDest);
                                    }
                                    crawl(generation, inspectedLinks);
                                }
                            } else {
                                /* try to load the webpage and find links on it */
                                // We need browser currentURL and not sourceURL, because of possible redirects will change domain and or
                                // relative
                                // path.
                                final String finalBaseUrl = new Regex(br.getURL(), "(https?://.*?)(\\?|$)").getMatch(0);
                                final String browserContent = br.toString();
                                final List<CrawledLink> possibleCryptedLinks = find(br.getURL(), null, false);
                                if (possibleCryptedLinks != null) {
                                    final boolean singleDest = possibleCryptedLinks.size() == 1;
                                    for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                                        forwardCrawledLinkInfos(source, possibleCryptedLink, lm, sourceURLs, singleDest);
                                    }
                                    if (possibleCryptedLinks.size() == 1) {
                                        /* first check if the url itself can be handled */
                                        final CrawledLink link = possibleCryptedLinks.get(0);
                                        link.setUnknownHandler(new UnknownCrawledLinkHandler() {

                                            @Override
                                            public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc) {
                                                /* unhandled url, lets parse the content on it */
                                                final List<CrawledLink> possibleCryptedLinks2 = lc.find(browserContent, finalBaseUrl, false);
                                                if (possibleCryptedLinks2 != null && possibleCryptedLinks2.size() > 0) {
                                                    final boolean singleDest = possibleCryptedLinks2.size() == 1;
                                                    for (final CrawledLink possibleCryptedLink : possibleCryptedLinks2) {
                                                        forwardCrawledLinkInfos(source, possibleCryptedLink, lm, sourceURLs, singleDest);
                                                    }
                                                    lc.crawl(generation, possibleCryptedLinks2);
                                                }
                                            }
                                        });
                                    }
                                    crawl(generation, possibleCryptedLinks);
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    LogController.CL().log(e);
                } finally {
                    try {
                        if (br != null) {
                            br.getHttpConnection().disconnect();
                        }
                    } catch (Throwable e) {
                    }
                }
            } finally {
                checkFinishNotify();
            }
        }
    }

    protected boolean distributeCrawledLink(CrawledLink crawledLink) {
        return crawledLink != null && crawledLink.gethPlugin() == null;
    }

    protected boolean canHandle(final LazyPlugin<? extends Plugin> lazyPlugin, final String url, final CrawledLink link) {
        if (lazyPlugin.canHandle(url)) {
            try {
                final Plugin plugin = lazyPlugin.newInstance(getPluginClassLoaderChild());
                if (plugin != null) {
                    /* now we run the plugin and let it find some links */
                    return plugin.canHandle(url);
                } else {
                    throw new WTFException("Plugin not available:" + lazyPlugin);
                }
            } catch (Throwable e) {
                LogController.CL().log(e);
            }
        }
        return false;
    }

    protected boolean checkAllowStart(int generation) {
        return isCrawlingAllowed() && getCrawlerGeneration(false) == generation;
    }

    protected Boolean distributePluginForHost(final LazyHostPlugin pluginForHost, final int generation, final String url, final CrawledLink link) {
        try {
            if (canHandle(pluginForHost, url, link)) {
                if (!isBlacklisted(pluginForHost)) {
                    if (insideCrawlerPlugin()) {
                        if (!checkAllowStart(generation)) {
                            /* LinkCrawler got aborted! */
                            return false;
                        }
                        processHostPlugin(generation, pluginForHost, link);
                    } else {
                        if (checkStartNotify(generation)) {
                            threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                @Override
                                public long getAverageRuntime() {
                                    final Long ret = getDefaultAverageRuntime();
                                    if (ret != null) {
                                        return ret;
                                    }
                                    return pluginForHost.getAverageParseRuntime();
                                }

                                @Override
                                void crawling() {
                                    processHostPlugin(getGeneration(), pluginForHost, link);
                                }
                            });
                        } else {
                            /* LinkCrawler got aborted! */
                            return false;
                        }
                    }
                }
                return true;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return null;
    }

    protected Boolean distributePluginForDecrypt(final LazyCrawlerPlugin pDecrypt, final int generation, final String url, final CrawledLink link) {
        try {
            if (canHandle(pDecrypt, url, link)) {
                if (!isBlacklisted(pDecrypt)) {
                    final java.util.List<CrawledLink> allPossibleCryptedLinks = getCrawlableLinks(pDecrypt.getPattern(), link, link.getCustomCrawledLinkModifier());
                    if (allPossibleCryptedLinks != null) {
                        if (insideCrawlerPlugin()) {
                            /*
                             * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on
                             * plugin waiting for linkcrawler results
                             */
                            for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                if (!checkAllowStart(generation)) {
                                    /* LinkCrawler got aborted! */
                                    return false;
                                }
                                crawl(generation, pDecrypt, decryptThis);
                            }
                        } else {
                            /*
                             * enqueue these cryptedLinks for decrypting
                             */
                            for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                                if (checkStartNotify(generation)) {
                                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {

                                        public long getAverageRuntime() {
                                            final Long ret = getDefaultAverageRuntime();
                                            if (ret != null) {
                                                return ret;
                                            }
                                            return pDecrypt.getAverageCrawlRuntime();
                                        }

                                        @Override
                                        protected Object sequentialLockingObject() {
                                            return pDecrypt.getDisplayName();
                                        }

                                        @Override
                                        protected int maxConcurrency() {
                                            return pDecrypt.getMaxConcurrentInstances();
                                        }

                                        @Override
                                        void crawling() {
                                            crawl(getGeneration(), pDecrypt, decryptThis);
                                        }
                                    });
                                } else {
                                    /* LinkCrawler got aborted! */
                                    return false;
                                }
                            }
                        }
                    }
                }
                return true;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return null;
    }

    protected Boolean distributePluginC(final PluginsC pluginC, final int generation, final String url, final CrawledLink link) {
        try {
            if (pluginC.canHandle(url)) {
                final CrawledLinkModifier originalModifier = link.getCustomCrawledLinkModifier();
                final CrawledLinkModifier lm;
                if (pluginC.hideLinks()) {
                    lm = new CrawledLinkModifier() {
                        /*
                         * set new LinkModifier, hides the url if needed
                         */
                        public void modifyCrawledLink(CrawledLink link) {
                            if (originalModifier != null) {
                                originalModifier.modifyCrawledLink(link);
                            }
                            /* we hide the links */
                            final DownloadLink dl = link.getDownloadLink();
                            if (dl != null) {
                                dl.setUrlProtection(UrlProtection.PROTECTED_CONTAINER);
                            }
                        }
                    };
                } else {
                    lm = originalModifier;
                }
                final java.util.List<CrawledLink> allPossibleCryptedLinks = getCrawlableLinks(pluginC.getSupportedLinks(), link, lm);
                if (allPossibleCryptedLinks != null) {
                    if (insideCrawlerPlugin()) {
                        /*
                         * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on plugin
                         * waiting for linkcrawler results
                         */
                        for (final CrawledLink decryptThis : allPossibleCryptedLinks) {
                            if (!checkAllowStart(generation)) {
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
                            if (checkStartNotify(generation)) {
                                threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                    @Override
                                    public long getAverageRuntime() {
                                        final Long ret = getDefaultAverageRuntime();
                                        if (ret != null) {
                                            return ret;
                                        }
                                        return super.getAverageRuntime();
                                    }

                                    @Override
                                    void crawling() {
                                        container(getGeneration(), pluginC, decryptThis);
                                    }
                                });
                            } else {
                                /* LinkCrawler got aborted! */
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return null;
    }

    protected Boolean distributeDeeperOrFollowRedirect(final int generation, final String url, final CrawledLink link) {
        try {
            new URL(link.getURL());
        } catch (MalformedURLException e) {
            return null;
        }
        try {
            LinkCrawlerRule rule = null;
            /* do not change order, it is important to check redirect first */
            if ((rule = matchesFollowRedirectRule(link, url)) != null || (link.isCrawlDeep() || (rule = matchesDeepDecryptRule(link, url)) != null)) {
                if (link != null) {
                    link.setMatchingRule(rule);
                }
                /* the link is allowed to crawlDeep */
                if (insideCrawlerPlugin()) {
                    if (!checkAllowStart(generation)) {
                        /* LinkCrawler got aborted! */
                        return false;
                    }
                    crawlDeeper(generation, link);
                } else {
                    if (checkStartNotify(generation)) {
                        threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                            @Override
                            public long getAverageRuntime() {
                                final Long ret = getDefaultAverageRuntime();
                                if (ret != null) {
                                    return ret;
                                }
                                return super.getAverageRuntime();
                            }

                            @Override
                            void crawling() {
                                crawlDeeper(getGeneration(), link);
                            }
                        });
                    } else {
                        return false;
                    }
                }
                return true;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return null;
    }

    protected void distribute(int generation, java.util.List<CrawledLink> possibleCryptedLinks) {
        if (possibleCryptedLinks == null || possibleCryptedLinks.size() == 0) {
            return;
        }
        if (checkStartNotify(generation)) {
            try {
                mainloop: for (final CrawledLink possibleCryptedLink : possibleCryptedLinks) {
                    if (!checkAllowStart(generation)) {
                        /* LinkCrawler got aborted! */
                        return;
                    }
                    mainloopretry: while (true) {
                        final UnknownCrawledLinkHandler unnknownHandler = possibleCryptedLink.getUnknownHandler();
                        possibleCryptedLink.setUnknownHandler(null);
                        if (!distributeCrawledLink(possibleCryptedLink)) {
                            // direct forward, if we already have a final link.
                            this.handleFinalCrawledLink(possibleCryptedLink);
                            continue mainloop;
                        }
                        final String url = possibleCryptedLink.getURL();
                        if (url == null) {
                            /* WTF, no URL?! let's continue */
                            continue mainloop;
                        }
                        if (url.startsWith("file:/")) {
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
                        } else {
                            if (!url.startsWith("directhttp://") && !url.startsWith("httpviajd://") && !url.startsWith("httpsviajd://")) {
                                {
                                    /*
                                     * first we will walk through all available decrypter plugins
                                     */
                                    final List<LazyCrawlerPlugin> lazyCrawlerPlugins = getSortedLazyCrawlerPlugins();
                                    final ListIterator<LazyCrawlerPlugin> it = lazyCrawlerPlugins.listIterator();
                                    while (it.hasNext()) {
                                        final LazyCrawlerPlugin pDecrypt = it.next();
                                        final Boolean ret = distributePluginForDecrypt(pDecrypt, generation, url, possibleCryptedLink);
                                        if (Boolean.FALSE.equals(ret)) {
                                            return;
                                        } else if (Boolean.TRUE.equals(ret)) {
                                            if (it.previousIndex() > lazyCrawlerPlugins.size() / 50) {
                                                resetSortedLazyCrawlerPlugins(lazyCrawlerPlugins);
                                            }
                                            continue mainloop;
                                        }
                                    }
                                }
                                {
                                    /* now we will walk through all available hoster plugins */
                                    final List<LazyHostPlugin> sortedLazyHostPlugins = getSortedLazyHostPlugins();
                                    final ListIterator<LazyHostPlugin> it = sortedLazyHostPlugins.listIterator();
                                    while (it.hasNext()) {
                                        final LazyHostPlugin pHost = it.next();
                                        final Boolean ret = distributePluginForHost(pHost, generation, url, possibleCryptedLink);
                                        if (Boolean.FALSE.equals(ret)) {
                                            return;
                                        } else if (Boolean.TRUE.equals(ret)) {
                                            if (it.previousIndex() > sortedLazyHostPlugins.size() / 50) {
                                                resetSortedLazyHostPlugins(sortedLazyHostPlugins);
                                            }
                                            continue mainloop;
                                        }
                                    }
                                }
                            }
                        }
                        if (ftpPlugin != null && url.startsWith("ftp://")) {
                            /* now we will check for generic ftp links */
                            final Boolean ret = distributePluginForHost(ftpPlugin, generation, url, possibleCryptedLink);
                            if (Boolean.FALSE.equals(ret)) {
                                return;
                            } else if (Boolean.TRUE.equals(ret)) {
                                continue mainloop;
                            }
                        } else if (isDirectHttpEnabled()) {
                            if (directPlugin != null) {
                                LinkCrawlerRule rule = null;
                                if (url.startsWith("directhttp://")) {
                                    /* now we will check for directPlugin links */
                                    final Boolean ret = distributePluginForHost(directPlugin, generation, url, possibleCryptedLink);
                                    if (Boolean.FALSE.equals(ret)) {
                                        return;
                                    } else if (Boolean.TRUE.equals(ret)) {
                                        continue mainloop;
                                    }
                                } else if ((rule = matchesDirectHTTPRule(possibleCryptedLink, url)) != null) {
                                    final String newURL = "directhttp://" + url;
                                    final CrawledLink modifiedPossibleCryptedLink = new CrawledLink(newURL);
                                    modifiedPossibleCryptedLink.setMatchingRule(rule);
                                    final String[] originalSourceURLS = possibleCryptedLink.getSourceUrls();
                                    final String[] sourceURLs = getAndClearSourceURLs(possibleCryptedLink);
                                    final CrawledLinkModifier parentLinkModifier = possibleCryptedLink.getCustomCrawledLinkModifier();
                                    possibleCryptedLink.setCustomCrawledLinkModifier(null);
                                    forwardCrawledLinkInfos(possibleCryptedLink, modifiedPossibleCryptedLink, parentLinkModifier, sourceURLs, true);
                                    final Boolean ret = distributePluginForHost(directPlugin, generation, newURL, modifiedPossibleCryptedLink);
                                    if (Boolean.FALSE.equals(ret)) {
                                        return;
                                    } else if (Boolean.TRUE.equals(ret)) {
                                        continue mainloop;
                                    }
                                    possibleCryptedLink.setSourceUrls(originalSourceURLS);
                                    possibleCryptedLink.setCustomCrawledLinkModifier(parentLinkModifier);
                                }
                            }
                            if (httpPlugin != null && url.startsWith("http")) {
                                /* now we will check for normal http links */
                                final String newURL = url.replaceFirst("https?://", (url.startsWith("https://") ? "httpsviajd://" : "httpviajd://"));
                                try {
                                    if (canHandle(httpPlugin, newURL, possibleCryptedLink)) {
                                        /* create new CrawledLink that holds the modified CrawledLink */
                                        final CrawledLink modifiedPossibleCryptedLink = new CrawledLink(newURL);
                                        final String[] originalSourceURLS = possibleCryptedLink.getSourceUrls();
                                        final String[] sourceURLs = getAndClearSourceURLs(possibleCryptedLink);
                                        final CrawledLinkModifier parentLinkModifier = possibleCryptedLink.getCustomCrawledLinkModifier();
                                        possibleCryptedLink.setCustomCrawledLinkModifier(null);
                                        forwardCrawledLinkInfos(possibleCryptedLink, modifiedPossibleCryptedLink, parentLinkModifier, sourceURLs, true);
                                        final Boolean ret = distributePluginForHost(httpPlugin, generation, newURL, modifiedPossibleCryptedLink);
                                        if (Boolean.FALSE.equals(ret)) {
                                            return;
                                        } else if (Boolean.TRUE.equals(ret)) {
                                            continue mainloop;
                                        }
                                        /**
                                         * restore possibleCryptedLink properties because it is still unhandled
                                         */
                                        possibleCryptedLink.setSourceUrls(originalSourceURLS);
                                        possibleCryptedLink.setCustomCrawledLinkModifier(parentLinkModifier);
                                    }
                                } catch (final Throwable e) {
                                    LogController.CL().log(e);
                                }
                            }
                        }
                        if (unnknownHandler != null) {
                            /*
                             * CrawledLink is unhandled till now , but has an UnknownHandler set, lets call it, maybe it makes the Link
                             * handable by a Plugin
                             */
                            try {
                                unnknownHandler.unhandledCrawledLink(possibleCryptedLink, this);
                            } catch (final Throwable e) {
                                LogController.CL().log(e);
                            }
                            /* lets retry this crawledLink */
                            continue mainloopretry;
                        }
                        final Boolean deeperOrFollow = distributeDeeperOrFollowRedirect(generation, url, possibleCryptedLink);
                        if (Boolean.FALSE.equals(deeperOrFollow)) {
                            return;
                        } else if (Boolean.TRUE.equals(deeperOrFollow)) {
                            continue mainloop;
                        }
                        final Boolean embedded = distributeEmbeddedLink(generation, url, possibleCryptedLink);
                        if (Boolean.FALSE.equals(embedded)) {
                            return;
                        } else if (Boolean.TRUE.equals(embedded)) {
                            continue mainloop;
                        }
                        break mainloopretry;
                    }
                    handleUnhandledCryptedLink(possibleCryptedLink);
                }
            } finally {
                checkFinishNotify();
            }
        }
    }

    protected Boolean distributeEmbeddedLink(final int generation, final String url, final CrawledLink source) {
        final LinkedHashSet<String> possibleEmbeddedLinks = new LinkedHashSet<String>();
        try {
            final String queryString = new Regex(source.getURL(), "\\?(.+)$").getMatch(0);
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
                            /* base64 http and ftp */
                            String possibleURLs = new String(Base64.decode(checkParam), "UTF-8");
                            if (HTMLParser.getProtocol(possibleURLs) == null) {
                                possibleURLs = URLDecoder.decode(possibleURLs, "UTF-8");
                            }
                            if (HTMLParser.getProtocol(possibleURLs) != null) {
                                possibleEmbeddedLinks.add(possibleURLs);
                            }
                        }
                    } catch (final Throwable e) {
                        LogController.CL().log(e);
                    }
                }
            }
            if (StringUtils.contains(source.getURL(), "aHR0c") || StringUtils.contains(source.getURL(), "ZnRwOi")) {
                String base64 = new Regex(source.getURL(), "(aHR0c[0-9a-zA-Z\\+\\/=]+(%3D){0,2})").getMatch(0);// http
                if (base64 == null) {
                    base64 = new Regex(source.getURL(), "(ZnRwOi[0-9a-zA-Z\\+\\/=]+(%3D){0,2})").getMatch(0);// ftp
                }
                if (base64 != null) {
                    if (base64.contains("%3D")) {
                        base64 = URLDecoder.decode(base64, "UTF-8");
                    }
                    String possibleURLs = new String(Base64.decode(base64), "UTF-8");
                    if (HTMLParser.getProtocol(possibleURLs) == null) {
                        possibleURLs = URLDecoder.decode(possibleURLs, "UTF-8");
                    }
                    if (HTMLParser.getProtocol(possibleURLs) != null) {
                        possibleEmbeddedLinks.add(possibleURLs);
                    }
                }
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        if (possibleEmbeddedLinks.size() > 0) {
            final ArrayList<CrawledLink> embeddedLinks = new ArrayList<CrawledLink>();
            for (final String possibleURL : possibleEmbeddedLinks) {
                final List<CrawledLink> links = find(possibleURL, null, false);
                embeddedLinks.addAll(links);
            }
            if (embeddedLinks.size() > 0) {
                final boolean singleDest = embeddedLinks.size() == 1;
                final String[] sourceURLs = getAndClearSourceURLs(source);
                final CrawledLinkModifier sourceLinkModifier = source.getCustomCrawledLinkModifier();
                source.setCustomCrawledLinkModifier(null);
                source.setBrokenCrawlerHandler(null);
                for (final CrawledLink embeddedLink : embeddedLinks) {
                    forwardCrawledLinkInfos(source, embeddedLink, sourceLinkModifier, sourceURLs, singleDest);
                }
                crawl(generation, embeddedLinks);
                return true;
            }
        }
        return null;
    }

    protected LinkCrawlerRule matchesDirectHTTPRule(CrawledLink link, String url) {
        if (linkCrawlerRules != null && (StringUtils.startsWithCaseInsensitive(url, "http://") || StringUtils.startsWithCaseInsensitive(url, "https://"))) {
            for (final LinkCrawlerRule rule : linkCrawlerRules) {
                if (rule.isEnabled() && LinkCrawlerRule.RULE.DIRECTHTTP.equals(rule.getRule()) && rule.matches(url)) {
                    return rule;
                }
            }
        }
        return null;
    }

    protected LinkCrawlerRule matchesFollowRedirectRule(CrawledLink link, String url) {
        if (linkCrawlerRules != null && (StringUtils.startsWithCaseInsensitive(url, "http://") || StringUtils.startsWithCaseInsensitive(url, "https://"))) {
            for (final LinkCrawlerRule rule : linkCrawlerRules) {
                if (rule.isEnabled() && LinkCrawlerRule.RULE.FOLLOWREDIRECT.equals(rule.getRule()) && rule.matches(url)) {
                    return rule;
                }
            }
        }
        return null;
    }

    protected LinkCrawlerRule matchesDeepDecryptRule(CrawledLink link, String url) {
        if (linkCrawlerRules != null && (StringUtils.startsWithCaseInsensitive(url, "file:/") || StringUtils.startsWithCaseInsensitive(url, "http://") || StringUtils.startsWithCaseInsensitive(url, "https://"))) {
            Integer knownDepth = null;
            for (final LinkCrawlerRule rule : linkCrawlerRules) {
                if (rule.isEnabled() && LinkCrawlerRule.RULE.DEEPDECRYPT.equals(rule.getRule()) && rule.matches(url)) {
                    if (rule.getMaxDecryptDepth() == -1) {
                        return rule;
                    } else {
                        if (knownDepth == null) {
                            Iterator<CrawledLink> it = link.iterator();
                            int depth = 0;
                            while (it.hasNext()) {
                                final CrawledLink next = it.next();
                                final LinkCrawlerRule matchingRule = next.getMatchingRule();
                                if (matchingRule != null && LinkCrawlerRule.RULE.DEEPDECRYPT.equals(rule.getRule())) {
                                    depth++;
                                }
                            }
                            knownDepth = depth;
                        }
                        if (knownDepth < rule.getMaxDecryptDepth()) {
                            return rule;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected List<LazyCrawlerPlugin> getSortedLazyCrawlerPlugins() {
        if (parentCrawler != null) {
            return parentCrawler.getSortedLazyCrawlerPlugins();
        }
        if (unsortedLazyCrawlerPlugins == null) {
            unsortedLazyCrawlerPlugins = CrawlerPluginController.getInstance().list();
        }
        List<LazyCrawlerPlugin> ret = sortedLazyCrawlerPlugins.get();
        if (ret == null) {
            /* sort cHosts according to their usage */
            ret = new ArrayList<LazyCrawlerPlugin>(unsortedLazyCrawlerPlugins);
            try {
                Collections.sort(ret, new Comparator<LazyCrawlerPlugin>() {

                    @Override
                    public final int compare(LazyCrawlerPlugin o1, LazyCrawlerPlugin o2) {
                        final LazyPluginClass l1 = o1.getLazyPluginClass();
                        final LazyPluginClass l2 = o2.getLazyPluginClass();
                        if (l1.getInterfaceVersion() == l2.getInterfaceVersion()) {
                            return 0;
                        }
                        if (l1.getInterfaceVersion() > l2.getInterfaceVersion()) {
                            return -1;
                        }
                        return 1;
                    }

                });
                Collections.sort(ret, new Comparator<LazyCrawlerPlugin>() {
                    public final int compare(long x, long y) {
                        return (x < y) ? 1 : ((x == y) ? 0 : -1);
                    }

                    @Override
                    public final int compare(LazyCrawlerPlugin o1, LazyCrawlerPlugin o2) {
                        return compare(o1.getPluginUsage(), o2.getPluginUsage());
                    }

                });
            } catch (final Throwable e) {
                LogController.CL(true).log(e);
            }
            sortedLazyCrawlerPlugins.compareAndSet(null, ret);
        }
        return ret;
    }

    protected List<LazyHostPlugin> getSortedLazyHostPlugins() {
        if (parentCrawler != null) {
            return parentCrawler.getSortedLazyHostPlugins();
        }
        /* sort pHosts according to their usage */
        List<LazyHostPlugin> ret = sortedLazyHostPlugins.get();
        if (ret == null) {
            ret = new ArrayList<LazyHostPlugin>(unsortedLazyHostPlugins);
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
        return ret;
    }

    protected boolean resetSortedLazyCrawlerPlugins(List<LazyCrawlerPlugin> resetSortedLazyCrawlerPlugins) {
        if (parentCrawler != null) {
            return parentCrawler.resetSortedLazyCrawlerPlugins(resetSortedLazyCrawlerPlugins);
        }
        return sortedLazyCrawlerPlugins.compareAndSet(resetSortedLazyCrawlerPlugins, null);
    }

    protected boolean resetSortedLazyHostPlugins(List<LazyHostPlugin> lazyHostPlugins) {
        if (parentCrawler != null) {
            return parentCrawler.resetSortedLazyHostPlugins(lazyHostPlugins);
        }
        return sortedLazyHostPlugins.compareAndSet(lazyHostPlugins, null);
    }

    public boolean isDirectHttpEnabled() {
        return directHttpEnabled;
    }

    public List<CrawledLink> getCrawlableLinks(Pattern pattern, CrawledLink source, CrawledLinkModifier modifier) {
        /*
         * we dont need memory optimization here as downloadlink, crypted link itself take care of this
         */
        final String[] hits = new Regex(source.getURL(), pattern).getColumn(-1);
        if (hits != null && hits.length > 0) {
            final ArrayList<CrawledLink> chits = new ArrayList<CrawledLink>(hits.length);
            for (String hit : hits) {
                String file = hit;
                file = file.trim();
                /* cut of any unwanted chars */
                while (file.length() > 0 && file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.length() > 0 && file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }
                file = file.trim();
                chits.add(new CrawledLink(new CryptedLink(file)));
            }
            for (CrawledLink decryptThis : chits) {
                /*
                 * forward important data to new ones
                 */
                forwardCrawledLinkInfos(source, decryptThis, modifier, null, null);
            }
            return chits;
        }
        return null;
    }

    protected void processHostPlugin(int generation, LazyHostPlugin pHost, CrawledLink possibleCryptedLink) {
        final CrawledLinkModifier parentLinkModifier = possibleCryptedLink.getCustomCrawledLinkModifier();
        possibleCryptedLink.setCustomCrawledLinkModifier(null);
        possibleCryptedLink.setBrokenCrawlerHandler(null);
        if (pHost == null || possibleCryptedLink.getURL() == null || this.isCrawledLinkFiltered(possibleCryptedLink)) {
            return;
        }
        if (checkStartNotify(generation)) {
            try {
                final String[] sourceURLs = getAndClearSourceURLs(possibleCryptedLink);
                PluginForHost wplg = null;
                /*
                 * use a new PluginClassLoader here
                 */
                wplg = pHost.newInstance(getPluginClassLoaderChild());
                if (wplg != null) {
                    /* now we run the plugin and let it find some links */
                    LinkCrawlerThread lct = null;
                    if (Thread.currentThread() instanceof LinkCrawlerThread) {
                        lct = (LinkCrawlerThread) Thread.currentThread();
                    }
                    Object owner = null;
                    LinkCrawler previousCrawler = null;
                    boolean oldDebug = false;
                    boolean oldVerbose = false;
                    Logger oldLogger = null;
                    try {
                        LogSource logger = LogController.getFastPluginLogger(wplg.getHost());
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
                        wplg.setBrowser(new Browser());
                        wplg.setLogger(logger);
                        String url = possibleCryptedLink.getURL();
                        FilePackage sourcePackage = null;
                        if (possibleCryptedLink.getDownloadLink() != null) {
                            sourcePackage = possibleCryptedLink.getDownloadLink().getFilePackage();
                            if (FilePackage.isDefaultFilePackage(sourcePackage)) {
                                /* we don't want the various filePackage getting used */
                                sourcePackage = null;
                            }
                        }
                        final long startTime = System.currentTimeMillis();
                        final List<CrawledLink> crawledLinks = new ArrayList<CrawledLink>();
                        try {
                            wplg.setCurrentLink(possibleCryptedLink);
                            final List<DownloadLink> hosterLinks = wplg.getDownloadLinks(url, sourcePackage);
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
                                    crawledLinks.add(wplg.convert(hosterLink));
                                }
                            }
                            /* in case the function returned without exceptions, we can clear log */
                            logger.clear();
                        } finally {
                            wplg.setCurrentLink(null);
                            final long endTime = System.currentTimeMillis() - startTime;
                            wplg.getLazyP().updateParseRuntime(endTime);
                            /* close the logger */
                            logger.close();
                        }
                        if (crawledLinks.size() > 0) {
                            final boolean singleDest = crawledLinks.size() == 1;
                            for (final CrawledLink crawledLink : crawledLinks) {
                                /*
                                 * forward important data to new ones
                                 */
                                forwardCrawledLinkInfos(possibleCryptedLink, crawledLink, parentLinkModifier, sourceURLs, singleDest);
                                handleFinalCrawledLink(crawledLink);
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
                } else {
                    LogController.CL().info("Hoster Plugin not available:" + pHost.getDisplayName());
                }
            } catch (Throwable e) {
                LogController.CL().log(e);
            } finally {
                /* restore old ClassLoader for current Thread */
                checkFinishNotify();
            }
        }
    }

    public String[] getAndClearSourceURLs(final CrawledLink link) {
        final ArrayList<String> sources = new ArrayList<String>();
        CrawledLink next = link;
        while (next != null) {
            final CrawledLink current = next;
            next = current.getSourceLink();
            final String currentURL = cleanURL(current.getURL());
            if (currentURL != null) {
                if (sources.size() == 0) {
                    sources.add(currentURL);
                } else if (!StringUtils.equals(currentURL, sources.get(sources.size() - 1))) {
                    sources.add(currentURL);
                }
            }
        }
        link.setSourceUrls(null);
        final String customSourceUrl = getReferrerUrl(link);
        if (customSourceUrl != null) {
            sources.add(customSourceUrl);
        }
        return sources.toArray(new String[] {});
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
        if (unsafeName != null) {
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
        }
        return ret;
    }

    private CryptedLink getLatestCryptedLink(CrawledLink link) {
        final CryptedLink ret = link.getCryptedLink();
        if (ret == null && link.getSourceLink() != null) {
            return link.getSourceLink().getCryptedLink();
        }
        return ret;
    }

    private void forwardCryptedLinkInfos(final CrawledLink sourceCrawledLink, final CryptedLink destCryptedLink) {
        if (sourceCrawledLink != null && destCryptedLink != null) {
            String pw = null;
            final CrawledLink sourceCrawledLink2 = sourceCrawledLink.getSourceLink();
            if (sourceCrawledLink2 != null && sourceCrawledLink2.getDownloadLink() != null) {
                pw = sourceCrawledLink2.getDownloadLink().getDownloadPassword();
            }
            final CryptedLink sourceCryptedLink = getLatestCryptedLink(sourceCrawledLink);
            if (sourceCryptedLink != null) {
                if (pw == null) {
                    pw = sourceCryptedLink.getDecrypterPassword();
                }
            }
            if (pw == null && LinkCrawler.this instanceof JobLinkCrawler && ((JobLinkCrawler) LinkCrawler.this).getJob() != null) {
                pw = ((JobLinkCrawler) LinkCrawler.this).getJob().getCrawlerPassword();
            }
            destCryptedLink.setDecrypterPassword(pw);
        }
    }

    protected void forwardCrawledLinkInfos(final CrawledLink sourceCrawledLink, final CrawledLink destCrawledLink, final CrawledLinkModifier sourceLinkModifier, final String sourceURLs[], final Boolean singleDestCrawledLink) {
        if (sourceCrawledLink != null && destCrawledLink != null && sourceCrawledLink != destCrawledLink) {
            destCrawledLink.setSourceLink(sourceCrawledLink);
            destCrawledLink.setOrigin(sourceCrawledLink.getOrigin());
            destCrawledLink.setSourceUrls(sourceURLs);
            destCrawledLink.setMatchingFilter(sourceCrawledLink.getMatchingFilter());
            forwardCryptedLinkInfos(sourceCrawledLink, destCrawledLink.getCryptedLink());
            forwardDownloadLinkInfos(getLatestDownloadLink(sourceCrawledLink), destCrawledLink.getDownloadLink(), singleDestCrawledLink);
            final CrawledLinkModifier destCustomModifier = destCrawledLink.getCustomCrawledLinkModifier();
            if (destCustomModifier == null) {
                destCrawledLink.setCustomCrawledLinkModifier(sourceLinkModifier);
            } else if (sourceLinkModifier != null) {
                destCrawledLink.setCustomCrawledLinkModifier(new CrawledLinkModifier() {

                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        if (sourceLinkModifier != null) {
                            sourceLinkModifier.modifyCrawledLink(link);
                        }
                        destCustomModifier.modifyCrawledLink(link);
                    }
                });
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
    }

    private PackageInfo convertFilePackageInfos(CrawledLink link) {
        if (link.getDownloadLink() != null) {
            final FilePackage fp = link.getDownloadLink().getFilePackage();
            if (!FilePackage.isDefaultFilePackage(fp)) {
                fp.remove(link.getDownloadLink());
                PackageInfo fpi = null;
                if (fp.getDownloadDirectory() != null && !fp.getDownloadDirectory().equals(defaultDownloadFolder)) {
                    // do not set downloadfolder if it is the defaultfolder
                    if (link.getDesiredPackageInfo() == null) {
                        fpi = new PackageInfo();
                    } else {
                        fpi = link.getDesiredPackageInfo();
                    }
                    fpi.setDestinationFolder(fp.getDownloadDirectory());
                }

                final String name;
                if (Boolean.FALSE.equals(fp.getBooleanProperty(PACKAGE_CLEANUP_NAME, true))) {
                    name = fp.getName();
                } else {
                    name = LinknameCleaner.cleanFileName(fp.getName(), false, true, LinknameCleaner.EXTENSION_SETTINGS.REMOVE_KNOWN, true);
                }
                if (StringUtils.isNotEmpty(name)) {
                    if (fpi == null) {
                        if (link.getDesiredPackageInfo() == null) {
                            fpi = new PackageInfo();
                        } else {
                            fpi = link.getDesiredPackageInfo();
                        }
                    }
                    fpi.setName(name);
                }

                if (fp.hasProperty(PACKAGE_ALLOW_MERGE)) {
                    if (Boolean.FALSE.equals(fp.getBooleanProperty(PACKAGE_ALLOW_MERGE, false))) {
                        if (fpi == null) {
                            if (link.getDesiredPackageInfo() == null) {
                                fpi = new PackageInfo();
                            } else {
                                fpi = link.getDesiredPackageInfo();
                            }
                        }
                        fpi.setUniqueId(fp.getUniqueID());
                    } else {
                        if (fpi == null) {
                            fpi = link.getDesiredPackageInfo();
                        }
                        if (fpi != null) {
                            fpi.setUniqueId(null);
                        }
                    }
                }
                if (fp.hasProperty(PACKAGE_IGNORE_VARIOUS)) {
                    if (fpi == null) {
                        if (link.getDesiredPackageInfo() == null) {
                            fpi = new PackageInfo();
                        } else {
                            fpi = link.getDesiredPackageInfo();
                        }
                    }
                    if (Boolean.TRUE.equals(fp.getBooleanProperty(PACKAGE_IGNORE_VARIOUS, false))) {
                        fpi.setIgnoreVarious(true);
                    } else {
                        fpi.setIgnoreVarious(false);
                    }
                }
                if (fpi != null) {
                    link.setDesiredPackageInfo(fpi);
                }
                return fpi;
            }
        }
        return null;
    }

    private void permanentOffline(CrawledLink link) {
        final DownloadLink dl = link.getDownloadLink();
        try {
            if (dl != null && dl.getDefaultPlugin().getLazyP().getClassName().endsWith("r.Offline")) {
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
        if (sourceDownloadLink != null && destDownloadLink != null && sourceDownloadLink != destDownloadLink) {
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
    }

    public boolean isCrawlingAllowed() {
        return this.allowCrawling.get();
    }

    public void setCrawlingAllowed(boolean b) {
        this.allowCrawling.set(b);
    }

    public void stopCrawling() {
        crawlerGeneration.incrementAndGet();
    }

    public boolean waitForCrawling() {
        while (isRunning()) {
            synchronized (LinkCrawler.this) {
                if (isRunning()) {
                    try {
                        LinkCrawler.this.wait(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
        return isRunning() == false;
    }

    public boolean isRunning() {
        return runningState.get();
    }

    public static boolean isCrawling() {
        return CRAWLER.get() > 0;
    }

    protected void container(int generation, PluginsC oplg, final CrawledLink cryptedLink) {
        final CrawledLinkModifier parentLinkModifier = cryptedLink.getCustomCrawledLinkModifier();
        cryptedLink.setCustomCrawledLinkModifier(null);
        cryptedLink.setBrokenCrawlerHandler(null);
        if (oplg == null || cryptedLink.getURL() == null || duplicateFinderContainer.putIfAbsent(cryptedLink.getURL(), this) != null || this.isCrawledLinkFiltered(cryptedLink)) {
            return;
        }
        if (checkStartNotify(generation)) {
            final String[] sourceURLs = getAndClearSourceURLs(cryptedLink);
            try {
                processedLinksCounter.incrementAndGet();
                /* set new PluginClassLoaderChild because ContainerPlugin maybe uses Hoster/Crawler */
                PluginsC plg = null;
                try {
                    plg = oplg.getClass().newInstance();
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                    return;
                }
                /* now we run the plugin and let it find some links */
                LinkCrawlerThread lct = null;
                if (Thread.currentThread() instanceof LinkCrawlerThread) {
                    lct = (LinkCrawlerThread) Thread.currentThread();
                }
                Object owner = null;
                LinkCrawler previousCrawler = null;
                boolean oldDebug = false;
                boolean oldVerbose = false;
                Logger oldLogger = null;
                try {
                    LogSource logger = LogController.getFastPluginLogger(plg.getName());
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
                        logger.clear();
                        if (decryptedPossibleLinks != null && decryptedPossibleLinks.size() > 0) {
                            /* we found some links, distribute them */
                            final boolean singleDest = decryptedPossibleLinks.size() == 1;
                            for (CrawledLink decryptedPossibleLink : decryptedPossibleLinks) {
                                forwardCrawledLinkInfos(cryptedLink, decryptedPossibleLink, parentLinkModifier, sourceURLs, singleDest);
                            }
                            if (insideCrawlerPlugin()) {
                                /*
                                 * direct decrypt this link because we are already inside a LinkCrawlerThread and this avoids deadlocks on
                                 * plugin waiting for linkcrawler results
                                 */
                                if (generation != this.getCrawlerGeneration(false) || !isCrawlingAllowed()) {
                                    /* LinkCrawler got aborted! */
                                    return;
                                }
                                distribute(generation, decryptedPossibleLinks);
                            } else {
                                if (checkStartNotify(generation)) {
                                    /* enqueue distributing of the links */
                                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                        @Override
                                        public long getAverageRuntime() {
                                            final Long ret = getDefaultAverageRuntime();
                                            if (ret != null) {
                                                return ret;
                                            }
                                            return super.getAverageRuntime();
                                        }

                                        @Override
                                        void crawling() {
                                            LinkCrawler.this.distribute(getGeneration(), decryptedPossibleLinks);
                                        }
                                    });
                                }
                            }
                        }
                    } finally {
                        /* close the logger */
                        logger.close();
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
                checkFinishNotify();
            }
        }
    }

    protected void crawl(final int generation, LazyCrawlerPlugin lazyC, final CrawledLink cryptedLink) {
        final CrawledLinkModifier parentLinkModifier = cryptedLink.getCustomCrawledLinkModifier();
        cryptedLink.setCustomCrawledLinkModifier(null);
        final BrokenCrawlerHandler brokenCrawler = cryptedLink.getBrokenCrawlerHandler();
        cryptedLink.setBrokenCrawlerHandler(null);
        if (lazyC == null || cryptedLink.getCryptedLink() == null || duplicateFinderCrawler.putIfAbsent(cryptedLink.getURL(), this) != null || this.isCrawledLinkFiltered(cryptedLink)) {
            return;
        }
        if (checkStartNotify(generation)) {
            try {
                final String[] sourceURLs = getAndClearSourceURLs(cryptedLink);
                processedLinksCounter.incrementAndGet();
                final PluginForDecrypt wplg;
                /*
                 * we want a fresh pluginClassLoader here
                 */
                try {
                    wplg = lazyC.newInstance(getPluginClassLoaderChild());
                } catch (UpdateRequiredClassNotFoundException e1) {
                    LogController.CL().log(e1);
                    return;
                }
                wplg.setBrowser(new Browser());
                LogSource logger = null;
                Logger oldLogger = null;
                boolean oldVerbose = false;
                boolean oldDebug = false;
                logger = LogController.getFastPluginLogger(wplg.getHost());
                logger.info("Crawling: " + cryptedLink.getURL());
                wplg.setLogger(logger);
                /* now we run the plugin and let it find some links */
                LinkCrawlerThread lct = null;
                if (Thread.currentThread() instanceof LinkCrawlerThread) {
                    lct = (LinkCrawlerThread) Thread.currentThread();
                }
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
                                    }
                                    linksToDistribute = new ArrayList<CrawledLink>(distributedLinks);
                                    distributedLinks.clear();
                                }
                                final List<CrawledLink> linksToDistributeFinal = linksToDistribute;
                                if (checkStartNotify(generation)) {
                                    /* enqueue distributing of the links */
                                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                        @Override
                                        public long getAverageRuntime() {
                                            final Long ret = getDefaultAverageRuntime();
                                            if (ret != null) {
                                                return ret;
                                            }
                                            return super.getAverageRuntime();
                                        }

                                        @Override
                                        void crawling() {
                                            final List<CrawledLink> distributeThis = new ArrayList<CrawledLink>(linksToDistributeFinal);
                                            LinkCrawler.this.distribute(getGeneration(), distributeThis);
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
                            if (links == null || links.length == 0) {
                                return;
                            }

                            for (DownloadLink link : links) {
                                if (link.getPluginPatternMatcher() != null && !fastDuplicateDetector.contains(link)) {
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
                            } else {
                                /* we do not delay the distribute */
                                if (checkStartNotify(generation)) {
                                    /* enqueue distributing of the links */
                                    threadPool.execute(new LinkCrawlerRunnable(LinkCrawler.this, generation) {
                                        @Override
                                        public long getAverageRuntime() {
                                            final Long ret = getDefaultAverageRuntime();
                                            if (ret != null) {
                                                return ret;
                                            }
                                            return super.getAverageRuntime();
                                        }

                                        @Override
                                        void crawling() {
                                            LinkCrawler.this.distribute(getGeneration(), possibleCryptedLinks);
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
                    final long startTime = System.currentTimeMillis();
                    try {
                        wplg.setCrawler(this);
                        wplg.setLinkCrawlerAbort(new LinkCrawlerAbort(generation, this));
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
                        logger.clear();
                    } finally {
                        /* close the logger */
                        wplg.setLinkCrawlerAbort(null);
                        wplg.setCurrentLink(null);
                        final long endTime = System.currentTimeMillis() - startTime;
                        lazyC.updateCrawlRuntime(endTime);
                        logger.close();
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
                checkFinishNotify();
            }
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
        if (downloadLink != null && plugin != null) {
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
                            if (hits.length == 1 && hits[0] != null && !StringUtils.equals(pluginURL, hits[0])) {
                                return hits[0];
                            }
                            return null;
                        }
                    }
                    if (next.getDownloadLink() != null) {
                        continue;
                    }
                }
                break;

            }
        }
        return null;
    }

    private String getOriginURL(final CrawledLink link) {
        final DownloadLink downloadLink = link.getDownloadLink();
        if (downloadLink != null) {
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
        return null;
    }

    protected void postprocessFinalCrawledLink(CrawledLink link) {
        final DownloadLink downloadLink = link.getDownloadLink();
        if (downloadLink != null) {
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
    }

    protected void preprocessFinalCrawledLink(CrawledLink link) {
    }

    public static boolean isTempDecryptedURL(String url) {
        if (url != null) {
            final String host = Browser.getHost(url, true);
            return StringUtils.containsIgnoreCase(host, "decrypted") || StringUtils.containsIgnoreCase(host, "yt.not.allowed");
        }
        return false;
    }

    public static String cleanURL(String cUrl) {
        final String protocol = HTMLParser.getProtocol(cUrl);
        if (protocol != null) {
            final String host = Browser.getHost(cUrl, true);
            if (protocol != null && !StringUtils.containsIgnoreCase(host, "decrypted") && !StringUtils.containsIgnoreCase(host, "dummycnl.jdownloader.org") && !StringUtils.containsIgnoreCase(host, "yt.not.allowed")) {
                if (cUrl.startsWith("http://") || cUrl.startsWith("https://") || cUrl.startsWith("ftp://") || cUrl.startsWith("file:/")) {
                    return cUrl;
                } else if (cUrl.startsWith("directhttp://")) {
                    return cUrl.substring("directhttp://".length());
                } else if (cUrl.startsWith("httpviajd://")) {
                    return "http://".concat(cUrl.substring("httpviajd://".length()));
                } else if (cUrl.startsWith("httpsviajd://")) {
                    return "https://".concat(cUrl.substring("httpsviajd://".length()));
                }
            }
        }
        return null;
    }

    protected void handleFinalCrawledLink(CrawledLink link) {
        if (link != null) {
            final CrawledLink origin = link.getOriginLink();
            if (link.getCreated() == -1) {
                link.setCreated(getCreated());
                preprocessFinalCrawledLink(link);
                final CrawledLinkModifier customModifier = link.getCustomCrawledLinkModifier();
                link.setCustomCrawledLinkModifier(null);
                if (customModifier != null) {
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
                if (duplicateFinderFinal.putIfAbsent(link.getLinkID(), this) != null && !specialHandling) {
                    return;
                }
            }
            if (isCrawledLinkFiltered(link) == false) {
                /* link is not filtered, so we can process it normally */
                crawledLinksCounter.incrementAndGet();
                getHandler().handleFinalLink(link);
            }
        }
    }

    protected boolean isCrawledLinkFiltered(CrawledLink link) {
        if (parentCrawler != null && getFilter() != parentCrawler.getFilter()) {
            if (parentCrawler.isCrawledLinkFiltered(link)) {
                return true;
            }
        }
        if (getFilter().dropByUrl(link)) {
            filteredLinksCounter.incrementAndGet();
            getHandler().handleFilteredLink(link);
            return true;
        }
        return false;
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
            public List<CrawledLink> deepInspect(LinkCrawler lc, Browser br, URLConnectionAdapter urlConnection, CrawledLink link) throws Exception {
                final int limit = Math.max(1 * 1024 * 1024, CONFIG.getDeepDecryptLoadLimit());
                if (br != null) {
                    br.setLoadLimit(limit);
                }

                final String contentType = urlConnection.getContentType();
                if (urlConnection.getRequest().getLocation() == null && (urlConnection.isContentDisposition() || !StringUtils.containsIgnoreCase(contentType, "text") || urlConnection.getCompleteContentLength() > limit)) {
                    try {
                        urlConnection.disconnect();
                    } catch (Throwable e) {
                    }
                    return find("directhttp://" + urlConnection.getRequest().getUrl(), null, false);
                } else {
                    br.followConnection();
                    return null;
                }
            }
        };
    }

    public LinkCrawlerHandler defaulHandlerFactory() {
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

    public void setFilter(LinkCrawlerFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter is null");
        }
        this.filter = filter;
    }

    public void setHandler(LinkCrawlerHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        this.handler = handler;
    }

    public void setDeepInspector(LinkCrawlerDeepInspector deepInspector) {
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
