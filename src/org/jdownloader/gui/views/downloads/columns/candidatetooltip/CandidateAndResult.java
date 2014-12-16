package org.jdownloader.gui.views.downloads.columns.candidatetooltip;

import java.util.Date;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;

public class CandidateAndResult implements Comparable<CandidateAndResult> {

    private DownloadLinkCandidate candidate;

    public DownloadLinkCandidate getCandidate() {
        return candidate;
    }

    public DownloadLinkCandidateResult getResult() {
        return result;
    }

    private DownloadLinkCandidateResult result;

    public CandidateAndResult(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        this.candidate = candidate;
        this.result = result;

    }

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    @Override
    public int compareTo(CandidateAndResult o) {
        long t1 = result == null ? Long.MAX_VALUE : result.getStartTime();
        long t2 = o.result == null ? Long.MAX_VALUE : o.result.getStartTime();
        return compare(t1, t2);
    }

    public Date getDate() {
        if (result != null) {
            return new Date(result.getStartTime());
        }
        return new Date();
    }

}
