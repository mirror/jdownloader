package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public interface ConnectionBan {

    boolean validate(AbstractProxySelectorImpl selector, HTTPProxy orgReference, URL url);

    // boolean canReplace(ConnectionBan ban);

    boolean validate(ConnectionBan ban);

    boolean isSelectorBannedByPlugin(Plugin plugin);

    boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread);

    boolean isSelectorBannedByUrl(URL url);

    boolean isExpired();

}
