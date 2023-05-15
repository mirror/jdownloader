package jd.plugins;

import java.util.Set;

import jd.http.URLConnectionAdapter;

import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public interface DownloadConnectionVerifier {
    public Boolean verifyDownloadableContent(Set<LazyHostPlugin> plugins, final URLConnectionAdapter urlConnection);
}
