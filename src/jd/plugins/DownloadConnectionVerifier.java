package jd.plugins;

import jd.http.URLConnectionAdapter;

public interface DownloadConnectionVerifier {
    public Boolean verifyDownloadableContent(final URLConnectionAdapter urlConnection);
}
