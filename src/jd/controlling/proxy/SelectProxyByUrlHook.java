package jd.controlling.proxy;

import java.util.List;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public interface SelectProxyByUrlHook {

    void onProxyChoosen(String urlOrDomain, List<HTTPProxy> ret);

}
