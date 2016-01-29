package jd.controlling.proxy;

import java.net.URI;
import java.util.List;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public interface SelectProxyByURIHook {

    void onProxyChoosen(URI uri, List<HTTPProxy> ret);

}
