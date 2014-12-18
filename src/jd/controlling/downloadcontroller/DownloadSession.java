package jd.controlling.downloadcontroller;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEvent;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.download.raf.FileBytesCache;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;

public class DownloadSession extends Property {

    public static enum STOPMARK {
        /* no stopmark is set */
        NONE,
        /*
         * stopmark is set but no longer visible, eg link/package removed from list
         */
        HIDDEN,
        /* to set a random stopmark */
        RANDOM;
    }

    public static enum SessionState {
        NORMAL,
        RECONNECT_REQUESTED,
        RECONNECT_RUNNING;
    }

    /* non shared between DownloadSessions */
    private static final FileAccessManager FILE_ACCESS_MANAGER = new FileAccessManager();
    private static final DiskSpaceManager  DISK_SPACE_MANAGER  = new DiskSpaceManager();

    public DiskSpaceManager getDiskSpaceManager() {
        return DISK_SPACE_MANAGER;
    }

    private final NullsafeAtomicReference<SessionState>                   sessionState            = new NullsafeAtomicReference<SessionState>(SessionState.NORMAL);
    private final HashMap<String, AccountCache>                           accountCache            = new HashMap<String, AccountCache>();
    private final WeakHashMap<DownloadLink, DownloadLinkCandidateHistory> candidateHistory        = new WeakHashMap<DownloadLink, DownloadLinkCandidateHistory>();
    private final WeakHashMap<UniqueAlltimeID, IfFileExistsAction>        fileExistsActions       = new WeakHashMap<UniqueAlltimeID, IfFileExistsAction>();
    private final AtomicInteger                                           downloadsStarted        = new AtomicInteger(0);
    private final AtomicInteger                                           skipCounter             = new AtomicInteger(0);
    private final NullsafeAtomicReference<Integer>                        speedLimitBeforePause   = new NullsafeAtomicReference<Integer>(null);
    private final NullsafeAtomicReference<Boolean>                        speedLimitedBeforePause = new NullsafeAtomicReference<Boolean>(null);
    private volatile List<DownloadLink>                                   forcedLinks             = new CopyOnWriteArrayList<DownloadLink>();
    private volatile List<DownloadLink>                                   activationRequests      = new CopyOnWriteArrayList<DownloadLink>();
    private final WeakHashMap<PluginForHost, PluginClassLoaderChild>      activationPluginCache   = new WeakHashMap<PluginForHost, PluginClassLoaderChild>();
    private final AtomicBoolean                                           refreshCandidates       = new AtomicBoolean(false);
    private final AtomicBoolean                                           activateForcedOnly      = new AtomicBoolean(false);
    private AtomicLong                                                    activatorRebuildRequest = new AtomicLong(1);
    private NullsafeAtomicReference<CaptchaSettings.MODE>                 captchaMode             = new NullsafeAtomicReference<CaptchaSettings.MODE>(CaptchaSettings.MODE.NORMAL);

    public CaptchaSettings.MODE getCaptchaMode() {
        return captchaMode.get();
    }

    protected void setCaptchaMode(CaptchaSettings.MODE captchaMode) {
        if (captchaMode == null) {
            captchaMode = CaptchaSettings.MODE.NORMAL;
        }
        this.captchaMode.set(captchaMode);
    }

    public long getActivatorRebuildRequest() {
        return activatorRebuildRequest.get();
    }

    protected void incrementActivatorRebuildRequest() {
        activatorRebuildRequest.incrementAndGet();
    }

    /* shared between DownloadSessions */
    private final ProxyInfoHistory                proxyInfoHistory;
    private final AtomicInteger                   maxConcurrentDownloadsPerHost = new AtomicInteger(Integer.MAX_VALUE);
    private final NullsafeAtomicReference<Object> stopMark                      = new NullsafeAtomicReference<Object>(STOPMARK.NONE);
    private final AtomicBoolean                   useAccounts                   = new AtomicBoolean(true);
    private final AtomicBoolean                   mirrorManagement              = new AtomicBoolean(true);

    public boolean isCandidatesRefreshRequired() {
        return refreshCandidates.get();
    }

    public boolean isUseAccountsEnabled() {
        return useAccounts.get();
    }

    protected void setUseAccountsEnabled(boolean b) {
        useAccounts.set(b);
    }

    public boolean isMirrorManagementEnabled() {
        return mirrorManagement.get();
    }

    protected void setMirrorManagementEnabled(boolean b) {
        mirrorManagement.set(b);
    }

    public int getMaxConcurrentDownloadsPerHost() {
        return maxConcurrentDownloadsPerHost.get();
    }

    protected void setMaxConcurrentDownloadsPerHost(int max) {
        if (max <= 0) {
            maxConcurrentDownloadsPerHost.set(Integer.MAX_VALUE);
        } else {
            maxConcurrentDownloadsPerHost.set(max);
        }
    }

    public boolean isForcedOnlyModeEnabled() {
        return activateForcedOnly.get();
    }

    public int getSkipCounter() {
        return skipCounter.get();
    }

    public int getSpeedLimitBeforePause() {
        Integer ret = speedLimitBeforePause.get();
        if (ret == null) {
            return -1;
        }
        return Math.max(-1, ret);
    }

    public Boolean isSpeedWasLimitedBeforePauseEnabled() {
        return speedLimitedBeforePause.get();
    }

    protected void setActivationRequests(List<DownloadLink> activationRequests) {
        if (isCandidatesRefreshRequired() == false) {
            if (!activationRequests.equals(this.activationRequests)) {
                refreshCandidates();
            }
        }
        this.activationRequests = activationRequests;
    }

    public void refreshCandidates() {
        refreshCandidates.set(true);
    }

    protected void setForcedLinks(CopyOnWriteArrayList<DownloadLink> forcedLinks) {
        if (isCandidatesRefreshRequired() == false) {
            if (!forcedLinks.equals(this.forcedLinks)) {
                refreshCandidates();
            }
        }
        this.forcedLinks = forcedLinks;
    }

    protected boolean removeActivationRequest(DownloadLink link) {
        if (link == null) {
            return false;
        }
        boolean removed = activationRequests.remove(link);
        removed = forcedLinks.remove(link) || removed;
        if (removed) {
            refreshCandidates();
        }
        return removed;
    }

    protected boolean removeActivationRequests(List<DownloadLink> links) {
        if (links == null) {
            return false;
        }
        boolean removed = activationRequests.remove(links);
        removed = forcedLinks.remove(links) || removed;
        if (removed) {
            refreshCandidates();
        }
        return removed;
    }

    private Map<PluginForHost, PluginClassLoaderChild> getActivationPluginCache() {
        return activationPluginCache;
    }

    private final CopyOnWriteArraySet<SingleDownloadController> controllers        = new CopyOnWriteArraySet<SingleDownloadController>() {
                                                                                       /**
         * 
         */
                                                                                       private static final long serialVersionUID = -3897088297641777499L;

                                                                                       public boolean add(SingleDownloadController e) {
                                                                                           downloadsStarted.incrementAndGet();
                                                                                           e.getDownloadLinkCandidate().getLink().setDownloadLinkController(e);
                                                                                           return super.add(e);
                                                                                       };

                                                                                       @Override
                                                                                       public boolean remove(Object e) {
                                                                                           boolean ret = super.remove(e);
                                                                                           if (ret) {
                                                                                               try {
                                                                                                   getDiskSpaceManager().freeAllReservationsBy(e);
                                                                                               } catch (final Throwable ignore) {
                                                                                               }
                                                                                               try {
                                                                                                   getFileAccessManager().unlockAllHeldby(e);
                                                                                               } finally {
                                                                                                   if (e instanceof SingleDownloadController) {
                                                                                                       ((SingleDownloadController) e).getDownloadLinkCandidate().getLink().setDownloadLinkController(null);
                                                                                                   }
                                                                                               }
                                                                                           }
                                                                                           return ret;
                                                                                       };
                                                                                   };
    private long                                                createTime;
    private static volatile WeakReference<FileBytesCache>       downloadWriteCache = null;

    public static synchronized FileBytesCache getDownloadWriteCache() {
        FileBytesCache ret = null;
        if (downloadWriteCache != null && (ret = downloadWriteCache.get()) != null) {
            return ret;
        }
        ret = new FileBytesCache(JsonConfig.create(GeneralSettings.class).getMaxBufferSize() * 1024, JsonConfig.create(GeneralSettings.class).getFlushBufferTimeout());
        downloadWriteCache = new WeakReference<FileBytesCache>(ret);
        return ret;
    }

    public ProxyInfoHistory getProxyInfoHistory() {
        return proxyInfoHistory;
    }

    public synchronized PluginForHost getPlugin(String host) {
        if (!StringUtils.isEmpty(host)) {
            final Iterator<Entry<PluginForHost, PluginClassLoaderChild>> it = getActivationPluginCache().entrySet().iterator();
            PluginForHost plugin = null;
            while (it.hasNext()) {
                final Entry<PluginForHost, PluginClassLoaderChild> next = it.next();
                plugin = next.getKey();
                if (StringUtils.equalsIgnoreCase(host, plugin.getHost())) {
                    return plugin;
                }
            }
            plugin = JDUtilities.getPluginForHost(host);
            getActivationPluginCache().put(plugin, PluginClassLoader.getSharedChild(plugin));
            return plugin;
        }
        return null;
    }

    public synchronized PluginClassLoaderChild getPluginClassLoaderChild(final PluginForHost plugin) {
        if (plugin != null) {
            final Iterator<Entry<PluginForHost, PluginClassLoaderChild>> it = getActivationPluginCache().entrySet().iterator();
            while (it.hasNext()) {
                final Entry<PluginForHost, PluginClassLoaderChild> next = it.next();
                final PluginForHost entry = next.getKey();
                if (entry == plugin) {
                    return next.getValue();
                }
            }
        }
        return null;
    }

    public List<DownloadLink> getForcedLinks() {
        return forcedLinks;
    }

    public void toggleStopMark(Object entry) {
        if (entry == STOPMARK.RANDOM) {
            if (getStopMark() != STOPMARK.NONE) {
                entry = null;
            }
        }
        if (entry == null || stopMark.get() == entry || entry == STOPMARK.NONE) {
            /* no stopmark OR toggle current set stopmark */
            setStopMark(STOPMARK.NONE);
        } else {
            /* set new stopmark */
            setStopMark(entry);
        }
    }

    public boolean isForcedLinksWaiting() {
        return forcedLinks.size() > 0;
    }

    public boolean isActivationRequestsWaiting() {
        if (activateForcedOnly.get()) {
            return forcedLinks.size() > 0;
        } else {
            return forcedLinks.size() > 0 || activationRequests.size() > 0;
        }
    }

    public DownloadSession() {
        this(null);
    }

    protected DownloadSession(DownloadSession previousSession) {
        createTime = System.currentTimeMillis();
        if (previousSession == null) {
            proxyInfoHistory = new ProxyInfoHistory();
        } else {
            if (previousSession.getControllers().size() > 0) {
                throw new IllegalArgumentException("previousSession contains active controllers!");
            }
            proxyInfoHistory = previousSession.getProxyInfoHistory();
            if (previousSession.isStopMarkSet() && previousSession.isStopMarkReached() == false) {
                setStopMark(previousSession.getStopMark());
            }
            setCaptchaMode(previousSession.getCaptchaMode());
            setMaxConcurrentDownloadsPerHost(previousSession.getMaxConcurrentDownloadsPerHost());
            setUseAccountsEnabled(previousSession.isUseAccountsEnabled());
            setMirrorManagementEnabled(previousSession.isMirrorManagementEnabled());
        }
    }

    public long getCreateTime() {
        return createTime;
    }

    protected DownloadLinkCandidateHistory getHistory(DownloadLink downloadLink) {
        return candidateHistory.get(downloadLink);
    }

    protected Collection<DownloadLinkCandidateHistory> getHistories() {
        return candidateHistory.values();
    }

    protected DownloadLinkCandidateHistory buildHistory(DownloadLink downloadLink) {
        DownloadLinkCandidateHistory ret = candidateHistory.get(downloadLink);
        if (ret == null) {
            ret = new DownloadLinkCandidateHistory();
            candidateHistory.put(downloadLink, ret);
        }
        return ret;
    }

    protected DownloadLinkCandidateHistory removeHistory(DownloadLink downloadLink) {
        if (downloadLink == null) {
            candidateHistory.clear();
            return null;
        } else {
            return candidateHistory.remove(downloadLink);
        }
    }

    protected void removeAccountCache(String host) {
        refreshCandidates.set(true);
        if (StringUtils.isEmpty(host)) {
            accountCache.clear();
        } else {
            accountCache.remove(host.toLowerCase(Locale.ENGLISH));
        }
    }

    public AccountCache getAccountCache(final DownloadLink link) {
        String host = link.getHost();
        if (StringUtils.isEmpty(host)) {
            return AccountCache.NA;
        }
        host = host.toLowerCase(Locale.ENGLISH);
        AccountCache ret = null;
        synchronized (accountCache) {
            if (accountCache.containsKey(host)) {
                ret = accountCache.get(host);
                if (ret == null) {
                    return AccountCache.NA;
                }
                return ret;
            }
        }
        ret = HosterRuleController.getInstance().getAccountCache(host, this);
        if (ret == null) {
            ArrayList<CachedAccount> newCache = new ArrayList<CachedAccount>();
            for (Account acc : AccountController.getInstance().list(host)) {
                newCache.add(new CachedAccount(host, acc, getPlugin(host)));
            }
            List<Account> multiHosts = AccountController.getInstance().getMultiHostAccounts(host);
            if (multiHosts != null) {
                for (Account acc : multiHosts) {
                    newCache.add(new CachedAccount(host, acc, getPlugin(acc.getHoster())));
                }
            }
            newCache.add(new CachedAccount(host, null, getPlugin(host)));
            try {
                Collections.sort(newCache, new Comparator<CachedAccount>() {

                    private int compare(boolean x, boolean y) {
                        return (x == y) ? 0 : (x ? 1 : -1);
                    }

                    @Override
                    public int compare(CachedAccount o1, CachedAccount o2) {
                        /* 1ST SORT: ORIGINAL;MULTI;NONE */
                        int ret = o1.getType().compareTo(o2.getType());
                        if (ret == 0) {
                            /* 2ND SORT: NO CAPTCHA;CAPTCHA */
                            ret = compare(o1.hasCaptcha(link), o2.hasCaptcha(link));
                        }
                        return ret;
                    }
                });
            } catch (final Throwable e) {
                LogController.CL(true).log(e);
            }
            ret = new AccountCache(newCache);
        }
        synchronized (accountCache) {
            if (!accountCache.containsKey(host)) {
                accountCache.put(host, ret);
                return ret;
            } else {
                ret = accountCache.get(host);
                if (ret == null) {
                    return AccountCache.NA;
                }
                return ret;
            }
        }
    }

    public boolean isStopMark(final Object item) {
        Object stopMark = this.stopMark.get();
        if (stopMark instanceof DownloadLink) {
            //
            return stopMark == item || item == ((DownloadLink) stopMark).getFilePackage();
        }
        return stopMark == item;
    }

    public boolean isStopMarkSet() {
        return stopMark.get() != STOPMARK.NONE;
    }

    protected boolean isStopMarkReached() {
        Object stop = stopMark.get();
        if (stop == STOPMARK.NONE) {
            return false;
        }
        if (stop == STOPMARK.HIDDEN) {
            return true;
        }
        if (stop instanceof DownloadLink) {
            DownloadLink link = (DownloadLink) stop;
            return (!link.isEnabled() || link.isSkipped() || link.getFinalLinkState() != null || getHistory(link) != null);
        }
        if (stop instanceof FilePackage) {
            FilePackage fp = (FilePackage) stop;
            boolean readL = fp.getModifyLock().readLock();
            try {
                for (final DownloadLink link : fp.getChildren()) {
                    if ((!link.isEnabled() || link.isSkipped() || link.getFinalLinkState() != null || getHistory(link) != null)) {
                        continue;
                    }
                    return false;
                }
            } finally {
                fp.getModifyLock().readUnlock(readL);
            }
            return true;
        }
        return false;
    }

    public void setStopMark(final Object stopEntry) {
        Object entry = stopEntry;
        if (entry == null || entry == STOPMARK.NONE) {
            entry = STOPMARK.NONE;
        }
        if (entry == STOPMARK.RANDOM) {
            /* user wants to set a random stopmark */
            Iterator<SingleDownloadController> it = controllers.iterator();
            if (it.hasNext()) {
                entry = it.next().getDownloadLink();
            } else {
                entry = STOPMARK.HIDDEN;
            }
        }
        stopMark.set(entry);
        DownloadWatchDog.getInstance().getEventSender().fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.PROPERTY_CHANGE, new DownloadWatchDogProperty(DownloadWatchDogProperty.Property.STOPSIGN, entry)));
    }

    public IfFileExistsAction getOnFileExistsAction(FilePackage filePackage) {
        return fileExistsActions.get(filePackage.getUniqueID());
    }

    public void setOnFileExistsAction(FilePackage filePackage, IfFileExistsAction doAction) {
        if (filePackage == null) {
            fileExistsActions.clear();
        } else {
            if (doAction == null) {
                fileExistsActions.remove(filePackage.getUniqueID());
            } else {
                fileExistsActions.put(filePackage.getUniqueID(), doAction);
            }
        }
    }

    public FileAccessManager getFileAccessManager() {
        return FILE_ACCESS_MANAGER;
    }

    public Object getStopMark() {
        return stopMark.get();
    }

    /**
     * @return the downloadsStarted
     */
    public int getDownloadsStarted() {
        return downloadsStarted.get();
    }

    /**
     * @return the controllers
     */
    protected Set<SingleDownloadController> getControllers() {
        return controllers;
    }

    /**
     * @return the activationLinks
     */
    protected List<DownloadLink> getActivationRequests() {
        return activationRequests;
    }

    /**
     * @return the sessionState
     */
    public SessionState getSessionState() {
        return sessionState.get();
    }

    public void setForcedOnlyModeEnabled(boolean b) {
        activateForcedOnly.set(b);
    }

    public boolean setCandidatesRefreshRequired(boolean b) {
        return refreshCandidates.getAndSet(b);
    }

    public void setSpeedLimitBeforePause(int downloadSpeedLimit) {
        speedLimitBeforePause.set(downloadSpeedLimit);
    }

    public void setSpeedWasLimitedBeforePauseEnabled(boolean b) {
        speedLimitedBeforePause.set(b);
    }

    public boolean compareAndSetSessionState(SessionState expect, SessionState update) {
        return sessionState.compareAndSet(expect, update);
    }

    protected void setSessionState(SessionState state) {
        sessionState.set(state);
    }

    protected void setSkipCounter(int i) {
        skipCounter.set(i);
    }

    public synchronized void clearPluginCache() {
        getActivationPluginCache().clear();
    }

}
