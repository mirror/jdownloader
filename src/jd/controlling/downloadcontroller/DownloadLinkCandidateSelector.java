package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.downloadcontroller.AccountCache.ACCOUNTTYPE;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.ProxyController;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.domainrules.CompiledDomainRule;
import org.jdownloader.controlling.domainrules.DomainRuleController;
import org.jdownloader.controlling.domainrules.DomainRuleSet;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DownloadLinkCandidateSelector {

    private static class CandidateResultHolder {
        private final DownloadLinkCandidateResult result;

        public DownloadLinkCandidateResult getResult() {
            return result;
        }

        public DownloadLinkCandidate getCandidate() {
            return candidate;
        }

        private final DownloadLinkCandidate candidate;

        private CandidateResultHolder(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
            this.result = result;
            this.candidate = candidate;
        }
    }

    public static enum DownloadLinkCandidatePermission {
        OK,
        OK_FORCED,
        CONCURRENCY_LIMIT,
        CONCURRENCY_FORBIDDEN,
        OK_SPEED_EXTENSION
    }

    public static enum CachedAccountPermission {
        OK,
        DISABLED,
        TEMP_DISABLED,
        IMPOSSIBLE
    }

    public static enum ProxyBalanceMode {
        DISABLED,
        RANDOM,
        CYCLE,
        BALANCE
    }

    private final Comparator<CandidateResultHolder>                                                        RESULT_SORTER = new Comparator<CandidateResultHolder>() {
                                                                                                                             private final DownloadLinkCandidateResult.RESULT[] FINAL_RESULT_SORT_ORDER = new RESULT[] { DownloadLinkCandidateResult.RESULT.SKIPPED, DownloadLinkCandidateResult.RESULT.ACCOUNT_REQUIRED, DownloadLinkCandidateResult.RESULT.PLUGIN_DEFECT, DownloadLinkCandidateResult.RESULT.FATAL_ERROR };

                                                                                                                             private int indexOf(RESULT o1) {
                                                                                                                                 for (int index = 0; index < FINAL_RESULT_SORT_ORDER.length; index++) {
                                                                                                                                     if (FINAL_RESULT_SORT_ORDER[index] == o1) {
                                                                                                                                         return index;
                                                                                                                                     }
                                                                                                                                 }
                                                                                                                                 return -1;
                                                                                                                             }

                                                                                                                             private int compare(long x, long y) {
                                                                                                                                 return (x < y) ? -1 : ((x == y) ? 0 : 1);
                                                                                                                             }

                                                                                                                             @Override
                                                                                                                             public int compare(CandidateResultHolder o1, CandidateResultHolder o2) {
                                                                                                                                 long i1 = indexOf(o1.getResult().getResult());
                                                                                                                                 long i2 = indexOf(o2.getResult().getResult());
                                                                                                                                 if (i1 >= 0 && i2 < 0) {
                                                                                                                                     return -1;
                                                                                                                                 } else if (i2 >= 0 && i1 < 0) {
                                                                                                                                     return 1;
                                                                                                                                 } else if (i1 >= 0 && i2 >= 0) {
                                                                                                                                     return compare(i1, i2);
                                                                                                                                 } else {
                                                                                                                                     i1 = o1.getResult().getRemainingTime();
                                                                                                                                     i2 = o2.getResult().getRemainingTime();
                                                                                                                                     return -compare(i1, i2);
                                                                                                                                 }
                                                                                                                             };
                                                                                                                         };

    private final DownloadSession                                                                          session;

    private LinkedHashMap<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>> roundResults  = new LinkedHashMap<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>>();

    private final ProxyBalanceMode                                                                         freeProxyBalanceMode;

    public DownloadSession getSession() {
        return session;
    }

    public DownloadLinkCandidateSelector(DownloadSession session) {
        this.session = session;
        this.freeProxyBalanceMode = JsonConfig.create(GeneralSettings.class).getFreeProxyBalanceMode();
    }

    public DownloadLinkCandidateResult getBlockingDownloadLinkCandidateResult(DownloadLinkCandidate candidate, List<DownloadLinkCandidateResult> candidateResults, DownloadLinkCandidateHistory history) {
        if (candidateResults.size() > 1) {
            final HashMap<RESULT, AtomicInteger> resultCounterMap = new HashMap<RESULT, AtomicInteger>();
            for (final DownloadLinkCandidateResult candidateResult : candidateResults) {
                final RESULT result = candidateResult.getResult();
                switch (result) {
                case FILE_UNAVAILABLE:
                case CONNECTION_ISSUES:
                case HOSTER_UNAVAILABLE:
                    resultCounterMap.clear();
                    break;
                case FAILED_INCOMPLETE:
                case RETRY:
                    AtomicInteger resultCounter = resultCounterMap.get(result);
                    if (resultCounter == null) {
                        resultCounter = new AtomicInteger(1);
                        resultCounterMap.put(result, resultCounter);
                    } else {
                        resultCounter.incrementAndGet();
                    }
                    if (resultCounter.get() > 5) {
                        final DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.FILE_UNAVAILABLE, null, candidate.getCachedAccount().getHost(), false);
                        ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getDownloadTempUnavailableRetryWaittime());
                        return ret;
                    }
                }
            }
        }
        // final int maxNumberOfDownloadLinkCandidates = -1;// disabled for now
        // if (maxNumberOfDownloadLinkCandidates > 0 && history.size() > maxNumberOfDownloadLinkCandidates) {
        // return new DownloadLinkCandidateResult(SkipReason.TOO_MANY_RETRIES, null, null);
        // }
        return null;
    }

    private final static AtomicInteger CYCLE = new AtomicInteger(0);

    public final List<AbstractProxySelectorImpl> getProxies(final DownloadLinkCandidate candidate, final boolean ignoreConnectBans, final boolean ignoreAllBans) {
        List<AbstractProxySelectorImpl> ret = ProxyController.getInstance().getProxySelectors(candidate, ignoreConnectBans, ignoreAllBans);
        if (candidate.getCachedAccount().getAccount() == null) {
            try {
                switch (freeProxyBalanceMode) {
                case BALANCE:
                    Collections.sort(ret, new DownloadLinkCandidateLoadBalancer(candidate));
                    break;
                case RANDOM:
                    Collections.shuffle(ret);
                    break;
                case CYCLE:
                    int cyleIndex = CYCLE.getAndIncrement();
                    if (cyleIndex >= ret.size()) {
                        CYCLE.set(0);
                        cyleIndex = 0;
                    }
                    final ArrayList<AbstractProxySelectorImpl> cycle = new ArrayList<AbstractProxySelectorImpl>();
                    for (; cyleIndex < ret.size(); cyleIndex++) {
                        cycle.add(ret.get(cyleIndex));
                    }
                    if (cyleIndex > 0) {
                        cycle.addAll(ret.subList(0, cyleIndex));
                    }
                    ret = cycle;
                    break;
                default:
                case DISABLED:
                    break;
                }
            } catch (final Throwable e) {
                LogController.CL(true).log(e);
            }
        }
        return ret;
    }

    public final CachedAccountPermission getCachedAccountPermission(final CachedAccount cachedAccount) {
        if (cachedAccount != null) {
            if (session.isUseAccountsEnabled() == false && !ACCOUNTTYPE.NONE.equals(cachedAccount.getType())) {
                return CachedAccountPermission.DISABLED;
            } else {
                final Account canidateAccount = cachedAccount.getAccount();
                if (canidateAccount != null) {
                    if (!canidateAccount.isEnabled()) {
                        return CachedAccountPermission.DISABLED;
                    }
                    if (canidateAccount.isTempDisabled()) {
                        return CachedAccountPermission.TEMP_DISABLED;
                    }
                }
                return CachedAccountPermission.OK;
            }
        }
        return CachedAccountPermission.IMPOSSIBLE;
    }

    protected final DomainRuleSet getDomainRuleSet(DownloadLinkCandidate candidate) {
        final String downloadDomain = candidate.getLink().getDomainInfo().getTld();
        final CachedAccount cachedAccount = candidate.getCachedAccount();
        final String pluginDomain = cachedAccount.getHost();
        final String fileName = candidate.getLink().getName();
        return DomainRuleController.getInstance().createRuleSet(cachedAccount.getAccount(), downloadDomain, pluginDomain, fileName);
    }

    public final DownloadLinkCandidatePermission getDownloadLinkCandidatePermission(final DownloadLinkCandidate candidate) {
        final int maxDownloads = Math.max(1, CFG_GENERAL.CFG.getMaxSimultaneDownloads());
        DomainRuleSet domainRuleSet = null;
        if (!candidate.isForced() && session.getControllers().size() >= maxDownloads && (domainRuleSet = getDomainRuleSet(candidate)).size() == 0) {
            /**
             * not a forced candidate and no special rules and already reached max concurrent downloads
             */
            if (!DownloadWatchDog.getInstance().checkForAdditionalDownloadSlots(session)) {
                return DownloadLinkCandidatePermission.CONCURRENCY_LIMIT;
            }
        }
        final DownloadLink candidateLink = candidate.getLink();
        final CachedAccount cachedAccount = candidate.getCachedAccount();
        final Account candidateAccount = cachedAccount.getAccount();
        int maxPluginConcurrentAccount = cachedAccount.getPlugin().getMaxSimultanDownload(null, candidateAccount, candidate.getProxySelector());
        if (maxPluginConcurrentAccount == 0) {
            final String plugin = cachedAccount.getPlugin().getLazyP().getClassName();
            if (candidateAccount == null) {
                throw new WTFException(plugin + ".getMaxSimultanDownload(null,null) returned 0!");
            } else {
                throw new WTFException(plugin + ".getMaxSimultanDownload(null," + candidateAccount.getUser() + ") returned 0!");
            }
        }
        int maxPluginConcurrentHost = cachedAccount.getPlugin().getMaxSimultanDownload(candidateLink, candidateAccount, candidate.getProxySelector());
        if (maxPluginConcurrentHost <= 0) {
            return DownloadLinkCandidatePermission.CONCURRENCY_LIMIT;
        }
        int maxConcurrentHost = session.getMaxConcurrentDownloadsPerHost();
        final String candidatePlugin = cachedAccount.getPlugin().getHost();
        int forcedDownloads = 0;
        for (final SingleDownloadController download : session.getControllers()) {
            final Account downloadAccount = download.getAccount();
            final DownloadLinkCandidate downloadCandidate = download.getDownloadLinkCandidate();
            final DownloadLink downloadLink = download.getDownloadLink();
            if (downloadAccount != null) {
                if (candidateAccount != null) {
                    final boolean sameAccountHost = StringUtils.equals(downloadAccount.getHoster(), candidateAccount.getHoster());
                    if (sameAccountHost && downloadAccount != candidateAccount && candidateAccount.isConcurrentUsePossible() == false) {
                        return DownloadLinkCandidatePermission.CONCURRENCY_FORBIDDEN;
                    }
                } else {
                    final boolean sameAccountHost = StringUtils.equals(downloadAccount.getHoster(), candidatePlugin);
                    if (sameAccountHost && downloadAccount.isConcurrentUsePossible() == false) {
                        return DownloadLinkCandidatePermission.CONCURRENCY_FORBIDDEN;
                    }
                }
            } else if (candidateAccount != null) {
                final boolean sameAccountHost = StringUtils.equals(candidateAccount.getHoster(), downloadLink.getHost());
                if (sameAccountHost && candidateAccount.isConcurrentUsePossible() == false) {
                    return DownloadLinkCandidatePermission.CONCURRENCY_FORBIDDEN;
                }
            }
            if (downloadCandidate.isForced()) {
                forcedDownloads++;
            }
            if (downloadLink.getDomainInfo().compareTo(candidateLink.getDomainInfo()) == 0) {
                /**
                 * use DomainInfo here because we want to count concurrent downloads from same domain and not same plugin
                 */
                --maxConcurrentHost;
            }
            if (candidatePlugin.equals(downloadCandidate.getCachedAccount().getPlugin().getHost())) {
                /**
                 * same plugin is in use
                 */
                if (cachedAccount.getPlugin().isSameAccount(downloadAccount, download.getProxySelector(), candidateAccount, candidate.getProxySelector())) {
                    /**
                     * same account is in use
                     */
                    if (--maxPluginConcurrentAccount <= 0) {
                        /**
                         * the plugin does not allow more downloads with candidateAccount
                         */
                        return DownloadLinkCandidatePermission.CONCURRENCY_LIMIT;
                    }
                    if (candidateLink.getHost().equals(downloadLink.getHost())) {
                        /**
                         * count concurrent downloads from same plugin
                         */
                        if (--maxPluginConcurrentHost <= 0) {
                            /**
                             * the plugin does not allow more downloads with candidateAccount
                             */
                            return DownloadLinkCandidatePermission.CONCURRENCY_LIMIT;
                        }
                    }
                }
            }
            final String downloadDomain = downloadLink.getDomainInfo().getTld();
            final String pluginDomain = downloadCandidate.getCachedAccount().getHost();
            final String fileName = downloadLink.getName();
            if (domainRuleSet == null) {
                domainRuleSet = getDomainRuleSet(candidate);
            }
            for (final Entry<CompiledDomainRule, AtomicInteger> s : domainRuleSet.getMap().entrySet()) {
                if (s.getKey().matches(downloadCandidate.getCachedAccount().getAccount(), downloadDomain, pluginDomain, fileName)) {
                    s.getValue().incrementAndGet();
                }
            }
        }
        if (candidate.isForced() && forcedDownloads < Math.max(0, CFG_GENERAL.MAX_FORCED_DOWNLOADS.getValue())) {
            /**
             * we can still start forced downloads
             */
            return DownloadLinkCandidatePermission.OK_FORCED;
        }

        if (maxConcurrentHost <= 0 || session.getControllers().size() >= maxDownloads) {
            /**
             * max concurrent downloads or max concurrent downloads per host reached
             */
            if (domainRuleSet == null) {
                domainRuleSet = getDomainRuleSet(candidate);
            }
            for (final Entry<CompiledDomainRule, AtomicInteger> s : domainRuleSet.getMap().entrySet()) {
                if (s.getKey().isAllowToExceedTheGlobalLimit()) {
                    if (s.getValue().get() < s.getKey().getMaxSimultanDownloads()) {
                        return DownloadLinkCandidatePermission.OK;
                    }
                }
            }
            if (DownloadWatchDog.getInstance().checkForAdditionalDownloadSlots(session)) {
                return DownloadLinkCandidatePermission.OK_SPEED_EXTENSION;
            }
            return DownloadLinkCandidatePermission.CONCURRENCY_LIMIT;
        }

        if (domainRuleSet == null) {
            domainRuleSet = getDomainRuleSet(candidate);
        }
        for (final Entry<CompiledDomainRule, AtomicInteger> s : domainRuleSet.getMap().entrySet()) {
            if (s.getValue().get() >= s.getKey().getMaxSimultanDownloads()) {
                return DownloadLinkCandidatePermission.CONCURRENCY_LIMIT;
            }
        }
        return DownloadLinkCandidatePermission.OK;
    }

    public boolean isMirrorManagement() {
        return session.isMirrorManagementEnabled();
    }

    public boolean isForcedOnly() {
        return forcedOnly;
    }

    public void setForcedOnly(boolean forcedOnly) {
        this.forcedOnly = forcedOnly;
    }

    public boolean isExcluded(DownloadLink link) {
        return roundResults.containsKey(link);
    }

    public void addExcluded(DownloadLink link) {
        if (roundResults.containsKey(link)) {
            return;
        }
        roundResults.put(link, null);
    }

    public boolean validateDownloadLinkCandidate(DownloadLinkCandidate possibleCandidate) {
        DownloadLinkCandidateResult linkResult = null;
        DownloadLinkCandidateResult proxyResult = null;
        DownloadLinkCandidateHistory linkHistory = getSession().getHistory(possibleCandidate.getLink());
        if (linkHistory != null) {
            linkResult = linkHistory.getBlockingHistory(this, possibleCandidate);
        }
        ProxyInfoHistory proxyHistory = getSession().getProxyInfoHistory();
        proxyResult = proxyHistory.getBlockingHistory(possibleCandidate);
        if (linkResult != null && proxyResult == null) {
            addExcluded(possibleCandidate, linkResult);
            return false;
        } else if (proxyResult != null && linkResult == null) {
            addExcluded(possibleCandidate, proxyResult);
            return false;
        } else if (proxyResult != null && linkResult != null) {
            switch (linkResult.getResult()) {
            case PLUGIN_DEFECT:
            case ACCOUNT_REQUIRED:
            case FATAL_ERROR:
            case SKIPPED:
                addExcluded(possibleCandidate, linkResult);
                break;
            case FILE_UNAVAILABLE:
            case CONNECTION_ISSUES:
                if (proxyResult.getRemainingTime() >= linkResult.getRemainingTime()) {
                    addExcluded(possibleCandidate, proxyResult);
                } else {
                    addExcluded(possibleCandidate, linkResult);
                }
                break;
            default:
                System.out.println("FIXME " + linkResult.getResult());
                break;
            }
            return false;
        }
        return true;
    }

    public void setExcluded(DownloadLink link) {
        roundResults.put(link, null);
    }

    public boolean isStopMarkReached = false;

    public boolean isStopMarkReached() {
        return isStopMarkReached;
    }

    public void addExcluded(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result == null");
        }
        LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> map = roundResults.get(candidate.getLink());
        if (map == null) {
            map = new LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>();
            roundResults.put(candidate.getLink(), map);
        }
        map.put(candidate, result);
        if (candidate.getLink() == getSession().getStopMark()) {
            switch (result.getResult()) {
            case PLUGIN_DEFECT:
            case ACCOUNT_REQUIRED:
            case FATAL_ERROR:
            case SKIPPED:
                isStopMarkReached = true;
                break;
            }
        }
    }

    public LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> finalizeDownloadLinkCandidatesResults() {
        LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> ret = new LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>();
        Iterator<Entry<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>>> it = roundResults.entrySet().iterator();
        linkLoop: while (it.hasNext()) {
            Entry<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>> next = it.next();
            Map<DownloadLinkCandidate, DownloadLinkCandidateResult> map = next.getValue();
            if (map == null || map.size() == 0) {
                continue;
            }
            List<CandidateResultHolder> results = new ArrayList<DownloadLinkCandidateSelector.CandidateResultHolder>();
            Iterator<Entry<DownloadLinkCandidate, DownloadLinkCandidateResult>> it2 = map.entrySet().iterator();
            while (it2.hasNext()) {
                Entry<DownloadLinkCandidate, DownloadLinkCandidateResult> next2 = it2.next();
                DownloadLinkCandidateResult candidateResult = next2.getValue();
                switch (candidateResult.getResult()) {
                case CONNECTION_TEMP_UNAVAILABLE:
                    continue linkLoop;
                case PLUGIN_DEFECT:
                case ACCOUNT_REQUIRED:
                case FATAL_ERROR:
                case SKIPPED:
                case PROXY_UNAVAILABLE:
                case FILE_UNAVAILABLE:
                case CONNECTION_ISSUES:
                case CONDITIONAL_SKIPPED:
                    results.add(new CandidateResultHolder(next2.getKey(), candidateResult));
                    break;
                default:
                    throw new WTFException("This should not happen " + candidateResult.getResult());
                }
                try {
                    Collections.sort(results, RESULT_SORTER);
                } catch (final Throwable e) {
                    LogController.CL(true).log(e);
                }
                CandidateResultHolder mostImportantResult = results.get(0);
                ret.put(mostImportantResult.getCandidate(), mostImportantResult.getResult());
            }
        }
        return ret;
    }

    private boolean forcedOnly = false;

}
