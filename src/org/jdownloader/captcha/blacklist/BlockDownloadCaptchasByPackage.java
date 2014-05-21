package org.jdownloader.captcha.blacklist;

import java.lang.ref.WeakReference;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.captcha.v2.Challenge;

public class BlockDownloadCaptchasByPackage implements SessionBlackListEntry<Object> {

    private final WeakReference<FilePackage> blockedPackage;

    public BlockDownloadCaptchasByPackage(FilePackage parentNode) {
        blockedPackage = new WeakReference<FilePackage>(parentNode);
    }

    @Override
    public boolean canCleanUp() {
        return getFilePackage() == null;
    }

    public FilePackage getFilePackage() {
        return blockedPackage.get();
    }

    @Override
    public boolean matches(Challenge<Object> c) {
        FilePackage filePackage = getFilePackage();
        if (filePackage != null) {
            DownloadLink link = Challenge.getDownloadLink(c);
            if (link == null) {
                return false;
            }
            FilePackage parent = link.getParentNode();
            return filePackage == parent;
        }
        return false;
    }

}
