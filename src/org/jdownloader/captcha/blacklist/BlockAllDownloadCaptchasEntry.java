package org.jdownloader.captcha.blacklist;

import org.jdownloader.captcha.v2.Challenge;

public class BlockAllDownloadCaptchasEntry implements SessionBlackListEntry {

    public BlockAllDownloadCaptchasEntry() {

    }

    @Override
    public boolean canCleanUp() {
        return false;
    }

    @Override
    public boolean matches(Challenge c) {
        return true;
    }

}
