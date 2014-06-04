package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;

public interface BuyPremiumInterface extends RemoteCallInterface {
    //
    public String put(String domain, String pattern, String url);

    public String remove(String domain);

    public void redirect(String domainOrUrl, String src);

    public String list();

    public String listCache();

    public boolean clearCache();

}
