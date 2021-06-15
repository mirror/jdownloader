package org.jdownloader.captcha.blacklist;

import jd.controlling.captcha.SkipRequest;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.Challenge;

public class BlockDownloadCaptchasByHost implements SessionBlackListEntry<Object> {
    private final String blockedHost;

    public String getBlockedHost() {
        return blockedHost;
    }

    public BlockDownloadCaptchasByHost(String host) {
        this.blockedHost = host;
    }

    @Override
    public boolean canCleanUp() {
        return false;
    }

    @Override
    public String toString() {
        return "BlockDownloadCaptchasByHost:" + getBlockedHost();
    }

    @Override
    public boolean matches(Challenge<Object> c) {
        return StringUtils.equals(c.getHost(), getBlockedHost());
    }

    @Override
    public SkipRequest getSkipRequest() {
        return SkipRequest.BLOCK_HOSTER;
    }
}
