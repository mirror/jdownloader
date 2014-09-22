package org.jdownloader.controlling;

import java.util.ArrayList;

import jd.plugins.DownloadLink;

public enum UrlProtection {
    /* internal urls not visible to the user, because the origin is a link container */
    PROTECTED_CONTAINER,
    /* internal urls not visible to the user, because the origin is a link decrypter */
    PROTECTED_DECRYPTER,
    UNSET;

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