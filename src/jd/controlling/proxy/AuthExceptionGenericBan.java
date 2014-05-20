package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class AuthExceptionGenericBan extends AbstractBan {

    protected HTTPProxy proxy;
    protected URL       url;

    public AuthExceptionGenericBan(AbstractProxySelectorImpl proxySelector, HTTPProxy proxy, URL url) {

        super(proxySelector);
        this.proxy = proxy;
        this.url = url;
    }

    // _JDT._.plugins_errors_proxy_connection()
    //
    // @Override
    // public boolean validate(AbstractProxySelectorImpl selector, HTTPProxy orgReference, URL url) {
    // return false;
    // }

    @Override
    public String toString() {
        return _JDT._.AuthExceptionGenericBan_toString(proxy.toString());
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate) {
        return true;
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread) {
        return proxy.equals(orgReference);
    }

    // @Override
    // public boolean isSelectorBannedByUrl(URL url) {
    //
    // return true;
    // }

    @Override
    public boolean isExpired() {
        return false;
    }

}
