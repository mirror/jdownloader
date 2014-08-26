package org.jdownloader.controlling;

import java.util.ArrayList;

import jd.plugins.DownloadLink;

public enum UrlProtection {
    /* internal urls not visible to the user, because the origin is a link container */
    PROTECTED_CONTAINER,
    /* internal urls not visible to the user, because the origin is a link decrypter */
    PROTECTED_DECRYPTER,
    UNSET,
    /*
     * internal urls not visible to the user, because it is not a real url, and/or for intermaö plugin usage only. show browser url if
     * available
     */
    PROTECTED_INTERNAL_URL;

    public void setTo(ArrayList<DownloadLink> ret) {
        if (ret != null) {
            for (DownloadLink dl : ret) {
                dl.setUrlProtection(this);
            }
        }
    }

    public void setTo(DownloadLink ret) {
        if (ret != null) {
            ret.setUrlProtection(this);

        }
    }
}