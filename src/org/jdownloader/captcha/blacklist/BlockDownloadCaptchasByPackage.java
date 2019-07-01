package org.jdownloader.captcha.blacklist;

import java.lang.ref.WeakReference;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.captcha.v2.Challenge;

public class BlockDownloadCaptchasByPackage implements SessionBlackListEntry<Object> {
    private final WeakReference<FilePackage> blockedPackage;

    public BlockDownloadCaptchasByPackage(FilePackage parentNode) {
        if (FilePackage.isDefaultFilePackage(parentNode)) {
            /* we cannot blacklist the default filePackage */
            blockedPackage = new WeakReference<FilePackage>(null);
        } else {
            blockedPackage = new WeakReference<FilePackage>(parentNode);
        }
    }

    @Override
    public boolean canCleanUp() {
        final FilePackage filePackage = getFilePackage();
        return filePackage == null || filePackage.getControlledBy() == null;
    }

    public FilePackage getFilePackage() {
        return blockedPackage.get();
    }

    @Override
    public String toString() {
        final FilePackage filePackage = getFilePackage();
        if (filePackage != null) {
            return "BlockDownloadCaptchasByPackage:" + filePackage.getUniqueID();
        } else {
            return "BlockDownloadCaptchasByPackage";
        }
    }

    @Override
    public boolean matches(Challenge<Object> c) {
        final FilePackage filePackage = getFilePackage();
        if (filePackage != null) {
            final DownloadLink link = c.getDownloadLink();
            if (link == null) {
                return false;
            }
            final FilePackage parent = link.getParentNode();
            return filePackage == parent;
        }
        return false;
    }
}
