package jd.controlling.proxy;

import java.net.URL;
import java.util.List;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public interface SelectProxyByURLHook {

    void onProxyChoosen(URL url, List<HTTPProxy> ret);

}
