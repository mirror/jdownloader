package org.jdownloader.captcha.blacklist;

import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;

public class BlockAllDownloadCaptchasEntry implements SessionBlackListEntry {

    public BlockAllDownloadCaptchasEntry() {
    }

    @Override
    public boolean canCleanUp() {
        return false;
    }

    @Override
    public String toString() {
        return "BlockAllDownloadCaptchasEntry";
    }

    @Override
    public boolean matches(Challenge c) {
        final Plugin plugin = c.getPlugin();
        if (plugin instanceof PluginForDecrypt) {
            return false;
        } else {
            return true;
        }
    }

}
