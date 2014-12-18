package jd.plugins;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;

public class CountryIPBlockException extends PluginException implements CandidateResultProvider {
    private String message;

    public CountryIPBlockException(String msg) {
        super(-1);
        this.message = msg;

    }

    public CountryIPBlockException() {
        this(null);

    }

    @Override
    public DownloadLinkCandidateResult createCandidateResult(DownloadLinkCandidate candidate, String host) {
        DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.COUNTRY_BLOCKED, this, host);

        return ret;
    }
}
