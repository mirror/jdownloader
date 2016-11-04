package org.jdownloader.plugins.components.google;

import jd.http.Browser;
import jd.plugins.DownloadLink;

public interface GoogleVideoRefresh {
    public String refreshVideoDirectUrl(final DownloadLink dl, final Browser br) throws Exception;
}
