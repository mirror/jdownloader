package jd.controlling.proxy;

import java.net.URL;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class GenericConnectExceptionBan extends AuthExceptionGenericBan {

    public GenericConnectExceptionBan(AbstractProxySelectorImpl selector, HTTPProxy orgReference, URL url) {
        super(selector, orgReference, url);
        created = System.currentTimeMillis();

    }

    @Override
    public String toString() {
        return _JDT._.ConnectExceptionInPluginBan(proxy.toString());
    }

    private long created;

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - created > 15 * 60 * 1000l;
    }

}
