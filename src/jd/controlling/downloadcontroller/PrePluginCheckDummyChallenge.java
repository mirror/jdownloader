package jd.controlling.downloadcontroller;

import jd.controlling.captcha.SkipRequest;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;

import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;

public class PrePluginCheckDummyChallenge extends Challenge<Object> {

    private DownloadLink link;

    @Override
    public Plugin getPlugin() {

        return this.getLink().getDefaultPlugin();

    }

    public String getHost() {
        return getLink().getHost();
    }

    @Override
    public DomainInfo getDomainInfo() {
        return this.getLink().getDomainInfo();
    }

    public PrePluginCheckDummyChallenge(DownloadLink link) {
        super("", "");
        this.link = link;
    }

    public DownloadLink getLink() {
        return link;
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
