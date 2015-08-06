package jd.controlling.downloadcontroller;

import java.lang.ref.WeakReference;

import jd.controlling.captcha.SkipRequest;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;

import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;

public class PrePluginCheckDummyChallenge extends Challenge<Object> {

    private final WeakReference<DownloadLink> link;

    @Override
    public Plugin getPlugin() {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            return link.getDefaultPlugin();
        }
        return null;
    }

    public String getHost() {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            return link.getHost();
        }
        return null;
    }

    @Override
    public DownloadLink getDownloadLink() {
        return link.get();
    }

    @Override
    public DomainInfo getDomainInfo() {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            return link.getDomainInfo();
        }
        return null;
    }

    public PrePluginCheckDummyChallenge(DownloadLink link) {
        super("", "");
        this.link = new WeakReference<DownloadLink>(link);
    }

    @Override
    public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
        return false;
    }

    @Override
    public boolean isSolved() {
        return getDownloadLink() == null;
    }

}
