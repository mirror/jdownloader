package org.jdownloader.captcha.blacklist;

import jd.plugins.DownloadLink;

import org.jdownloader.captcha.v2.Challenge;

public class BlockDownloadCaptchasByLink implements SessionBlackListEntry<Object> {

    private DownloadLink downloadLink;

    public BlockDownloadCaptchasByLink(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;

    }

    @Override
    public boolean canCleanUp() {
        return false;
    }

    @Override
    public boolean matches(Challenge<Object> c) {
        DownloadLink link = Challenge.getDownloadLink(c);
        return downloadLink == link;
    }

}
