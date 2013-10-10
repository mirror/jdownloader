package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
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

    private static final FileAccessManager                     FILE_ACCESS_MANAGER           = new FileAccessManager();
    private final long                                         created;
    private final HashMap<UniqueAlltimeID, IfFileExistsAction> fileExistsActions             = new HashMap<UniqueAlltimeID, IfFileExistsAction>();
    private final FileAccessManager                            fileAccessManager;

    private final NullsafeAtomicReference<Object>              stopMark                      = new NullsafeAtomicReference<Object>(STOPMARK.NONE);
    private final AtomicInteger                                downloadsStarted              = new AtomicInteger(0);
    private final AtomicInteger                                skipCounter                   = new AtomicInteger(0);
    private final AtomicBoolean                                activateForcedOnly            = new AtomicBoolean(false);
    private final AtomicBoolean                                avoidCaptchas                 = new AtomicBoolean(false);
    private final AtomicBoolean                                mirrorManagement              = new AtomicBoolean(true);
    private final AtomicBoolean                                refreshCandidates             = new AtomicBoolean(false);
    private final AtomicInteger                                maxConcurrentDownloadsPerHost = new AtomicInteger(Integer.MAX_VALUE);

    private final NullsafeAtomicReference<SessionState>        sessionState                  = new NullsafeAtomicReference<SessionState>(SessionState.NORMAL);

    public AtomicBoolean getRefreshCandidates() {
        return refreshCandidates;
    }

    public AtomicBoolean getMirrorManagement() {
        return mirrorManagement;
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

    public AtomicBoolean getAvoidCaptchas() {
        return avoidCaptchas;
    }

    private final HashMap<String, AccountCache>                       accountCache     = new HashMap<String, AccountCache>();
    private final HashMap<DownloadLink, DownloadLinkCandidateHistory> candidateHistory = new HashMap<DownloadLink, DownloadLinkCandidateHistory>();
    private final ProxyInfoHistory                                    proxyInfoHistory;                                                             ;

    public AtomicBoolean getActivateForcedOnly() {
        return activateForcedOnly;
    }

    public AtomicInteger getSkipCounter() {
        return skipCounter;
    }

    public NullsafeAtomicReference<Integer> getSpeedLimitBeforePause() {
        return speedLimitBeforePause;
    }

    public NullsafeAtomicReference<Boolean> getSpeedLimitedBeforePause() {
        return speedLimitedBeforePause;
    }

    private final NullsafeAtomicReference<Integer> speedLimitBeforePause   = new NullsafeAtomicReference<Integer>(null);
    private final NullsafeAtomicReference<Boolean> speedLimitedBeforePause = new NullsafeAtomicReference<Boolean>(null);

    private CopyOnWriteArrayList<DownloadLink>     forcedLinks             = new CopyOnWriteArrayList<DownloadLink>();
    private CopyOnWriteArrayList<DownloadLink>     activationRequests      = new CopyOnWriteArrayList<DownloadLink>();

    public void setActivationRequests(CopyOnWriteArrayList<DownloadLink> activationRequests) {
        if (getRefreshCandidates().get() == false) {
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
        if (getRefreshCandidates().get() == false) {
            if (!forcedLinks.equals(this.forcedLinks)) {
                refreshCandidates();
            }
        }
        this.forcedLinks = forcedLinks;
    }

    private final HashMap<String, PluginForHost> activationPluginCache = new HashMap<String, PluginForHost>();

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
                } else if (con.getAccount() != null && con.getAccount().getHoster().equals(host)) {
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

    public CopyOnWriteArrayList<DownloadLink> getForcedLinks() {
        return forcedLinks;
    }

    public void toggleStopMark(Object entry) {
        if (entry == null || stopMark.get() == entry || entry == STOPMARK.NONE) {
            /* no stopmark OR toggle current set stopmark */
            stopMark.set(STOPMARK.NONE);
        } else {
            /* set new stopmark */
            stopMark.set(entry);
            DownloadsTableModel.getInstance().setStopSignColumnVisible(true);
        }
    }

    public boolean forcedLinksWaiting() {
        return forcedLinks.size() > 0;
    }

    public boolean activationRequestsWaiting() {
        if (activateForcedOnly.get()) {
            return forcedLinks.size() > 0;
        } else {
            return forcedLinks.size() > 0 || activationRequests.size() > 0;
        }
    }

    public long getCreated() {
        return created;
    }

    protected DownloadSession() {
        this(null);
    }

    protected DownloadSession(DownloadSession previousSession) {
        created = System.currentTimeMillis();
        fileAccessManager = FILE_ACCESS_MANAGER;
        if (previousSession == null) {
            proxyInfoHistory = new ProxyInfoHistory();
        } else {
            proxyInfoHistory = previousSession.getProxyInfoHistory();
            if (previousSession.isStopMarkSet() && previousSession.isStopMarkReached() == false) {
                setStopMark(previousSession.getStopMark());
            }
            setMaxConcurrentDownloadsPerHost(previousSession.getMaxConcurrentDownloadsPerHost());
        }
    }

    public DownloadLinkCandidateHistory getHistory(DownloadLink downloadLink) {
        synchronized (candidateHistory) {
            return candidateHistory.get(downloadLink);
        }
    }

    public DownloadLinkCandidateHistory buildHistory(DownloadLink downloadLink) {
        synchronized (candidateHistory) {
            DownloadLinkCandidateHistory ret = candidateHistory.get(downloadLink);
            if (ret == null) {
                ret = new DownloadLinkCandidateHistory();
                candidateHistory.put(downloadLink, ret);
            }
            return ret;
        }
    }

    public DownloadLinkCandidateHistory removeHistory(DownloadLink downloadLink) {
        synchronized (candidateHistory) {
            if (downloadLink == null) {
                candidateHistory.clear();
                return null;
            } else {
                return candidateHistory.remove(downloadLink);
            }
        }
    }

    public void removeAccountCache(String host) {
        refreshCandidates.set(true);
        synchronized (accountCache) {
            if (StringUtils.isEmpty(host)) {
                accountCache.clear();
            } else {
                accountCache.remove(host.toLowerCase(Locale.ENGLISH));
            }
        }
    }

    public AccountCache getAccountCache(String host) {
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
        ret = new AccountCache(newCache);
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
        synchronized (fileExistsActions) {
            return fileExistsActions.get(filePackage.getUniqueID().toString());
        }
    }

    public void setOnFileExistsAction(FilePackage filePackage, IfFileExistsAction doAction) {
        synchronized (fileExistsActions) {
            if (doAction == null) {
                fileExistsActions.remove(filePackage.getUniqueID());
            } else {
                fileExistsActions.put(filePackage.getUniqueID(), doAction);
            }
        }
    }

    public FileAccessManager getFileAccessManager() {
        return fileAccessManager;
    }

    public Object getStopMark() {
        return stopMark.get();
    }

    /**
     * @return the downloadsStarted
     */
    public AtomicInteger getDownloadsStarted() {
        return downloadsStarted;
    }

    /**
     * @return the controllers
     */
    public CopyOnWriteArrayList<SingleDownloadController> getControllers() {
        return controllers;
    }

    /**
     * @return the activationLinks
     */
    public CopyOnWriteArrayList<DownloadLink> getActivationRequests() {
        return activationRequests;
    }

    /**
     * @return the sessionState
     */
    public NullsafeAtomicReference<SessionState> getSessionState() {
        return sessionState;
    }

}
