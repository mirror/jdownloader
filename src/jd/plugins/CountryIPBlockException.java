package jd.plugins;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;

import org.appwork.utils.StringUtils;
import org.jdownloader.translate._JDT;

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
        if (StringUtils.isNotEmpty(message)) {
            ret.setMessage(message);
        } else {
            ret.setMessage(_JDT._.CountryIPBlockException_createCandidateResult());
        }
        return ret;
    }
}
