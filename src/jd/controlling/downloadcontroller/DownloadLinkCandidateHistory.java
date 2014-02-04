package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;

import org.appwork.exceptions.WTFException;
import org.jdownloader.plugins.SkipReason;

public class DownloadLinkCandidateHistory {

    private final LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> history = new LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>();

    protected DownloadLinkCandidateHistory() {
    }

    public boolean attach(DownloadLinkCandidate candidate) {
        if (history.containsKey(candidate)) return false;
        history.put(candidate, null);
        return true;
    }

    public boolean dettach(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        if (history.containsKey(candidate)) {
            history.put(candidate, result);
            return true;
        }
        return false;
    }

    public List<DownloadLinkCandidateResult> getResults(DownloadLinkCandidate candidate) {
        List<DownloadLinkCandidateResult> ret = new ArrayList<DownloadLinkCandidateResult>();
        Iterator<Entry<DownloadLinkCandidate, DownloadLinkCandidateResult>> it = history.entrySet().iterator();
        while (it.hasNext()) {
            Entry<DownloadLinkCandidate, DownloadLinkCandidateResult> next = it.next();
            DownloadLinkCandidate historyCandidate = next.getKey();
            CachedAccount historyAccount = historyCandidate.getCachedAccount();
            if (candidate.getCachedAccount().equals(historyAccount)) ret.add(next.getValue());
        }
        return ret;
    }

    public int size() {
        return history.size();
    }

    public DownloadLinkCandidateResult getBlockingHistory(DownloadLinkCandidateSelector selector, DownloadLinkCandidate candidate) {
        List<DownloadLinkCandidateResult> ret = getResults(candidate);
        Iterator<DownloadLinkCandidateResult> it = ret.iterator();
        while (it.hasNext()) {
            DownloadLinkCandidateResult next = it.next();
            switch (next.getResult()) {
            case IP_BLOCKED:
            case HOSTER_UNAVAILABLE:
                /* evaluated in onDetach and handled by proxyInfoHistory */
                break;
            case CONDITIONAL_SKIPPED:
            case ACCOUNT_INVALID:
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
            case CONNECTION_UNAVAILABLE:
                /* if we end up here(results above) -> find the bug :) */
                throw new WTFException("This should not happen! " + next.getResult() + " should already be handled in onDetach!");
            case FILE_UNAVAILABLE:
            case CONNECTION_ISSUES:
                /* these results(above) can TEMP. block */
                if (next.getRemainingTime() <= 0) {
                    break;
                }
            case PLUGIN_DEFECT:
            case OFFLINE_UNTRUSTED:
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
        int maxNumberOfDownloadLinkCandidates = selector.getMaxNumberOfDownloadLinkCandidatesResults(candidate);
        if (maxNumberOfDownloadLinkCandidates > 0 && size() > maxNumberOfDownloadLinkCandidates) { return new DownloadLinkCandidateResult(SkipReason.TOO_MANY_RETRIES, null); }
        return null;
    }
}
