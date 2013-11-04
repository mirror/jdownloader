package org.jdownloader.captcha.blacklist;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.Challenge;

public class BlockDownloadCaptchasByHost implements SessionBlackListEntry<Object> {

    private String blockedHost;

    public BlockDownloadCaptchasByHost(String host) {
        this.blockedHost = host;
    }

    @Override
    public boolean canCleanUp() {
        return false;
    }

    @Override
    public boolean matches(Challenge<Object> c) {

        return StringUtils.equals(Challenge.getHost(c), blockedHost);
    }

}
