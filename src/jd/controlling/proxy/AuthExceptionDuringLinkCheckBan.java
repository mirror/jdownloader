package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.PluginForHost;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class AuthExceptionDuringLinkCheckBan extends AbstractBasicBan {

    private URL url;

    public AuthExceptionDuringLinkCheckBan(PluginForHost plg, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy, URL url) {
        super(plg, proxySelector, proxy);

        this.url = url;
    }

    // _JDT._.plugins_errors_proxy_connection()

}
