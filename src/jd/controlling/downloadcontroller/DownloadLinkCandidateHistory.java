package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;

import org.appwork.exceptions.WTFException;
import org.jdownloader.plugins.SkipReason;

public class DownloadLinkCandidateHistory {

    public static interface DownloadLinkCandidateHistorySelector {
        boolean select(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result);
    }

    private final LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> history = new LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>();

    protected DownloadLinkCandidateHistory() {
    }

    public boolean attach(DownloadLinkCandidate candidate) {
        if (history.containsKey(candidate)) {
            return false;
        }
        history.put(candidate, null);
        return true;
    }

    public boolean dettach(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        if (history.containsKey(candidate) && history.get(candidate) == null) {
            history.put(candidate, result);
            return true;
        }
        return false;
    }

    protected Map<DownloadLinkCandidate, DownloadLinkCandidateResult> getHistory() {
        return history;
    }

    public List<DownloadLinkCandidateResult> getResults(final CachedAccount cachedAccount) {
        return selectResults(new DownloadLinkCandidateHistorySelector() {

            @Override
            public boolean select(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
                return candidate != null && cachedAccount.equals(candidate.getCachedAccount());
            };
        });
    }

    public List<DownloadLinkCandidateResult> selectResults(DownloadLinkCandidateHistorySelector selector) {
        return selectResults(this, selector);
    }

    public static List<DownloadLinkCandidateResult> selectResults(Collection<DownloadLinkCandidateHistory> histories, DownloadLinkCandidateHistorySelector selector) {
        final List<DownloadLinkCandidateResult> ret = new ArrayList<DownloadLinkCandidateResult>();
        for (DownloadLinkCandidateHistory history : histories) {
            ret.addAll(selectResults(history, selector));
        }
        return ret;
    }

    public static List<DownloadLinkCandidateResult> selectResults(DownloadLinkCandidateHistory history, DownloadLinkCandidateHistorySelector selector) {
        final List<DownloadLinkCandidateResult> ret = new ArrayList<DownloadLinkCandidateResult>();
        final Iterator<Entry<DownloadLinkCandidate, DownloadLinkCandidateResult>> it = history.getHistory().entrySet().iterator();
        while (it.hasNext()) {
            final Entry<DownloadLinkCandidate, DownloadLinkCandidateResult> next = it.next();
            final DownloadLinkCandidate candidate = next.getKey();
            final DownloadLinkCandidateResult result = next.getValue();
            if (candidate != null && result != null) {
                if (selector == null || selector.select(candidate, result)) {
                    ret.add(result);
                }
            }
        }
        return ret;
    }

    public int size() {
        return history.size();
    }

    public DownloadLinkCandidateResult getBlockingHistory(DownloadLinkCandidateSelector selector, DownloadLinkCandidate candidate) {
        if (selector != null && candidate != null) {
            final List<DownloadLinkCandidateResult> ret = getResults(candidate.getCachedAccount());
            final Iterator<DownloadLinkCandidateResult> it = ret.iterator();
            while (it.hasNext()) {
                final DownloadLinkCandidateResult next = it.next();
                switch (next.getResult()) {
                case IP_BLOCKED:
                case HOSTER_UNAVAILABLE:
                    /* evaluated in onDetach and handled by proxyInfoHistory */
                    break;
                case CONDITIONAL_SKIPPED:
                case ACCOUNT_INVALID:
                case ACCOUNT_ERROR:
                case ACCOUNT_UNAVAILABLE:
                    /* already handled in onDetach */
                    break;
                case FINISHED:
                case FINISHED_EXISTS:
                case SKIPPED:
                case STOPPED:
                case FAILED:
                case FAILED_EXISTS:
                case OFFLINE_TRUSTED:
                    /* these results(above) should have ended in removal of DownloadLinkHistory */
                case PROXY_UNAVAILABLE:
                case CONNECTION_TEMP_UNAVAILABLE:
                    /* if we end up here(results above) -> find the bug :) */
                    throw new WTFException("This should not happen! " + next.getResult() + " should already be handled in onDetach!");
                case FILE_UNAVAILABLE:
                case CONNECTION_ISSUES:
                    /* these results(above) can TEMP. block */
                    if (next.getRemainingTime() <= 0) {
                        break;
                    }
                case PLUGIN_DEFECT:
                case ACCOUNT_REQUIRED:
                case FATAL_ERROR:
                    return next;
                case CAPTCHA:
                case FAILED_INCOMPLETE:
                case RETRY:
                    /* these results(above) do NEVER block */
                    break;
                }
            }
            final int maxNumberOfDownloadLinkCandidates = selector.getMaxNumberOfDownloadLinkCandidatesResults(candidate);
            if (maxNumberOfDownloadLinkCandidates > 0 && size() > maxNumberOfDownloadLinkCandidates) {
                return new DownloadLinkCandidateResult(SkipReason.TOO_MANY_RETRIES, null, null);
            }
        }
        return null;
    }
}
