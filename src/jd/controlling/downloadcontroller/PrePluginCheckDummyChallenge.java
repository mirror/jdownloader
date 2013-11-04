package jd.controlling.downloadcontroller;

import jd.controlling.captcha.SkipRequest;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;

public class PrePluginCheckDummyChallenge extends Challenge<Object> {

    private DownloadLinkCandidate candidate;

    public PrePluginCheckDummyChallenge(DownloadLinkCandidate candidate) {
        super("", "");
        this.candidate = candidate;
    }

    public DownloadLinkCandidate getCandidate() {
        return candidate;
    }

    @Override
    public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
        return false;
    }

    @Override
    public boolean isSolved() {
        return false;
    }

}
