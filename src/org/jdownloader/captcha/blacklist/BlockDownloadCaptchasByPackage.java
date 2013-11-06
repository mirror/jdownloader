package org.jdownloader.captcha.blacklist;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.captcha.v2.Challenge;

public class BlockDownloadCaptchasByPackage implements SessionBlackListEntry<Object> {

    private FilePackage blockedPackage;

    public BlockDownloadCaptchasByPackage(FilePackage parentNode) {
        blockedPackage = parentNode;
    }

    @Override
    public boolean canCleanUp() {
        return false;
    }

    @Override
    public boolean matches(Challenge<Object> c) {
        DownloadLink link = Challenge.getDownloadLink(c);
        if (link == null) return false;
        FilePackage parent = link.getParentNode();
        return blockedPackage == parent;

    }

}
