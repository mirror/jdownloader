package jd.plugins;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;

public interface CandidateResultProvider {

    DownloadLinkCandidateResult createCandidateResult(DownloadLinkCandidate candidate, String host);

}
