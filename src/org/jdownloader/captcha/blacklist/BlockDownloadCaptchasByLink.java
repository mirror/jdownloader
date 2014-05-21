package org.jdownloader.captcha.blacklist;

import java.lang.ref.WeakReference;

import jd.plugins.DownloadLink;

import org.jdownloader.captcha.v2.Challenge;

public class BlockDownloadCaptchasByLink implements SessionBlackListEntry<Object> {

    private final WeakReference<DownloadLink> downloadLink;

    public BlockDownloadCaptchasByLink(DownloadLink downloadLink) {
        this.downloadLink = new WeakReference<DownloadLink>(downloadLink);

    }

    @Override
    public boolean canCleanUp() {
        DownloadLink link = getDownloadLink();
        return link == null || CaptchaBlackList.getInstance().isWhitelisted(link);
    }

    public DownloadLink getDownloadLink() {
        return downloadLink.get();
    }

    @Override
    public boolean matches(Challenge<Object> c) {
        DownloadLink link = getDownloadLink();
        if (link != null) {
            return link == Challenge.getDownloadLink(c);
        }
        return false;
    }

}
