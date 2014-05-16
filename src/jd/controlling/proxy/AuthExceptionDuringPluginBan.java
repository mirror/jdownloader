package jd.controlling.proxy;

import java.net.URL;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class AuthExceptionDuringPluginBan extends AbstractBasicBan {

    private DownloadLinkCandidate candidate;

    private URL                   url;

    public AuthExceptionDuringPluginBan(DownloadLinkCandidate candidate, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy, URL url) {
        super(candidate.getCachedAccount().getPlugin(), proxySelector, proxy);
        this.candidate = candidate;

        this.url = url;
    }

}
