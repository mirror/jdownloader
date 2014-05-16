package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.Account;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class AuthExceptionDuringAccountCheckBan extends AbstractBasicBan {

    private URL url;

    public AuthExceptionDuringAccountCheckBan(Account account, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy, URL url) {
        super(account.getPlugin(), proxySelector, proxy);

        this.url = url;
    }

}
