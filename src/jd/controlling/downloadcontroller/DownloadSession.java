package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.AccountCache.ACCOUNTTYPE;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.settings.IfFileExistsAction;

public class DownloadSession {

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
    private static final FileAccessManager                            FILE_ACCESS_MANAGER     = new FileAccessManager();
    private final NullsafeAtomicReference<SessionState>               sessionState            = new NullsafeAtomicReference<SessionState>(SessionState.NORMAL);
    private final HashMap<String, AccountCache>                       accountCache            = new HashMap<String, AccountCache>();
    private final HashMap<DownloadLink, DownloadLinkCandidateHistory> candidateHistory        = new HashMap<DownloadLink, DownloadLinkCandidateHistory>();
    private final HashMap<UniqueAlltimeID, IfFileExistsAction>        fileExistsActions       = new HashMap<UniqueAlltimeID, IfFileExistsAction>();
    private final AtomicInteger                                       downloadsStarted        = new AtomicInteger(0);
    private final AtomicInteger                                       skipCounter             = new AtomicInteger(0);
    private final NullsafeAtomicReference<Integer>                    speedLimitBeforePause   = new NullsafeAtomicReference<Integer>(null);
    private final NullsafeAtomicReference<Boolean>                    speedLimitedBeforePause = new NullsafeAtomicReference<Boolean>(null);
    private volatile List<DownloadLink>                               forcedLinks             = new CopyOnWriteArrayList<DownloadLink>();
    private volatile List<DownloadLink>                               activationRequests      = new CopyOnWriteArrayList<DownloadLink>();
    private final HashMap<String, PluginForHost>                      activationPluginCache   = new HashMap<String, PluginForHost>();
    private final AtomicBoolean                                       refreshCandidates       = new AtomicBoolean(false);
    private final AtomicBoolean                                       activateForcedOnly      = new AtomicBoolean(false);
    private AtomicLong                                                activatorRebuildRequest = new AtomicLong(1);

    public long getActivatorRebuildRequest() {
        return activatorRebuildRequest.get();
    }

    public void incrementActivatorRebuildRequest() {
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

    public void setUseAccountsEnabled(boolean b) {
        useAccounts.set(b);
    }

    public boolean isMirrorManagementEnabled() {
        return mirrorManagement.get();
    }

    public void setMirrorManagementEnabled(boolean b) {
        mirrorManagement.set(b);
    }

    public int getMaxConcurrentDownloadsPerHost() {
        return maxConcurrentDownloadsPerHost.get();
    }

    public void setMaxConcurrentDownloadsPerHost(int max) {
        if (max < 0) {
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
        if (ret == null) return -1;
        return Math.max(-1, ret);
    }

    public Boolean isSpeedWasLimitedBeforePauseEnabled() {
        return speedLimitedBeforePause.get();
    }

    public void setActivationRequests(List<DownloadLink> activationRequests) {
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

    public void setForcedLinks(CopyOnWriteArrayList<DownloadLink> forcedLinks) {
        if (isCandidatesRefreshRequired() == false) {
            if (!forcedLinks.equals(this.forcedLinks)) {
                refreshCandidates();
            }
        }
        this.forcedLinks = forcedLinks;
    }

    public HashMap<String, PluginForHost> getActivationPluginCache() {
        return activationPluginCache;
    }

    private final CopyOnWriteArrayList<SingleDownloadController> controllers = new CopyOnWriteArrayList<SingleDownloadController>() {
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

    public int getActiveDownloadsFromHost(String host) {
        if (host == null) return 0;
        int ret = 0;
        for (SingleDownloadController con : controllers) {
            if (con.isActive()) {
                if (con.getDownloadLink().getHost().equals(host)) {
                    ret++;
                }
            }
        }
        return ret;
    }

    public ProxyInfoHistory getProxyInfoHistory() {
        return proxyInfoHistory;
    }

    public PluginForHost getPlugin(String host) {
        if (StringUtils.isEmpty(host)) return null;
        host = host.toLowerCase(Locale.ENGLISH);
        PluginForHost plugin = getActivationPluginCache().get(host);
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost(host);
            getActivationPluginCache().put(host, plugin);
        }
        return plugin;
    }

    public List<DownloadLink> getForcedLinks() {
        return forcedLinks;
    }

    public void toggleStopMark(Object entry) {
        if (entry == null || stopMark.get() == entry || entry == STOPMARK.NONE) {
            /* no stopmark OR toggle current set stopmark */
            stopMark.set(STOPMARK.NONE);
        } else {
            /* set new stopmark */
            stopMark.set(entry);
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

    protected DownloadSession() {
        this(null);
    }

    protected DownloadSession(DownloadSession previousSession) {
        if (previousSession == null) {
            proxyInfoHistory = new ProxyInfoHistory();
        } else {
            if (previousSession.getControllers().size() > 0) throw new IllegalArgumentException("previousSession contains active controllers!");
            proxyInfoHistory = previousSession.getProxyInfoHistory();
            if (previousSession.isStopMarkSet() && previousSession.isStopMarkReached() == false) {
                setStopMark(previousSession.getStopMark());
            }
            setMaxConcurrentDownloadsPerHost(previousSession.getMaxConcurrentDownloadsPerHost());
            setUseAccountsEnabled(previousSession.isUseAccountsEnabled());
            setMirrorManagementEnabled(previousSession.isMirrorManagementEnabled());

        }
    }

    public DownloadLinkCandidateHistory getHistory(DownloadLink downloadLink) {
        return candidateHistory.get(downloadLink);
    }

    public DownloadLinkCandidateHistory buildHistory(DownloadLink downloadLink) {
        DownloadLinkCandidateHistory ret = candidateHistory.get(downloadLink);
        if (ret == null) {
            ret = new DownloadLinkCandidateHistory();
            candidateHistory.put(downloadLink, ret);
        }
        return ret;
    }

    public DownloadLinkCandidateHistory removeHistory(DownloadLink downloadLink) {
        if (downloadLink == null) {
            candidateHistory.clear();
            return null;
        } else {
            return candidateHistory.remove(downloadLink);
        }
    }

    public void removeAccountCache(String host) {
        refreshCandidates.set(true);
        if (StringUtils.isEmpty(host)) {
            accountCache.clear();
        } else {
            accountCache.remove(host.toLowerCase(Locale.ENGLISH));
        }
    }

    public AccountCache getAccountCache(final DownloadLink link) {
        String host = link.getHost();
        if (StringUtils.isEmpty(host)) return AccountCache.NA;
        host = host.toLowerCase(Locale.ENGLISH);
        AccountCache ret = null;
        synchronized (accountCache) {
            if (accountCache.containsKey(host)) {
                ret = accountCache.get(host);
                if (ret == null) return AccountCache.NA;
                return ret;
            }
        }
        ret = HosterRuleController.getInstance().getAccountCache(host, this);
        if (ret == null) {
            ArrayList<CachedAccount> newCache = new ArrayList<CachedAccount>();
            for (Account acc : AccountController.getInstance().list(host)) {
                newCache.add(new CachedAccount(host, acc, ACCOUNTTYPE.ORIGINAL, getPlugin(host)));
            }
            List<Account> multiHosts = AccountController.getInstance().getMultiHostAccounts(host);
            if (multiHosts != null) {
                for (Account acc : multiHosts) {
                    newCache.add(new CachedAccount(host, acc, ACCOUNTTYPE.MULTI, getPlugin(acc.getHoster())));
                }
            }
            newCache.add(new CachedAccount(host, null, ACCOUNTTYPE.NONE, getPlugin(host)));
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
            ret = new AccountCache(newCache);
        }
        synchronized (accountCache) {
            if (!accountCache.containsKey(host)) {
                accountCache.put(host, ret);
                return ret;
            } else {
                ret = accountCache.get(host);
                if (ret == null) return AccountCache.NA;
                return ret;
            }
        }
    }

    public boolean isStopMark(final Object item) {
        return stopMark.get() == item;
    }

    public boolean isStopMarkSet() {
        return stopMark.get() != STOPMARK.NONE;
    }

    protected boolean isStopMarkReached() {
        Object stop = stopMark.get();
        if (stop == STOPMARK.NONE) return false;
        if (stop == STOPMARK.HIDDEN) { return true; }
        if (stop instanceof DownloadLink) {
            DownloadLink link = (DownloadLink) stop;
            return (!link.isEnabled() || link.isSkipped() || link.getFinalLinkState() != null || getHistory(link) != null);
        }
        if (stop instanceof FilePackage) {
            FilePackage fp = (FilePackage) stop;
            boolean readL = fp.getModifyLock().readLock();
            try {
                for (final DownloadLink link : fp.getChildren()) {
                    if ((!link.isEnabled() || link.isSkipped() || link.getFinalLinkState() != null || getHistory(link) != null)) continue;
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
                entry = it.next();
            } else {
                entry = STOPMARK.NONE;
            }
        }
        stopMark.set(entry);
    }

    public IfFileExistsAction getOnFileExistsAction(FilePackage filePackage) {
        return fileExistsActions.get(filePackage.getUniqueID());
    }

    public void setOnFileExistsAction(FilePackage filePackage, IfFileExistsAction doAction) {
        if (doAction == null) {
            fileExistsActions.remove(filePackage.getUniqueID());
        } else {
            fileExistsActions.put(filePackage.getUniqueID(), doAction);
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
    public List<SingleDownloadController> getControllers() {
        return controllers;
    }

    /**
     * @return the activationLinks
     */
    public List<DownloadLink> getActivationRequests() {
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

    public void setSessionState(SessionState state) {
        sessionState.set(state);
    }

    public void setSkipCounter(int i) {
        skipCounter.set(i);
    }

}
