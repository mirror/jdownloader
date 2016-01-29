package jd.controlling.proxy;

import java.net.URI;

import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public interface ConnectionBan {

    boolean isSelectorBannedByPlugin(Plugin plugin, boolean ignoreConnectBans);

    boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URI url, Plugin pluginFromThread, boolean ignoreConnectBans);

    boolean isExpired();

    boolean canSwallow(ConnectionBan ban);

}
