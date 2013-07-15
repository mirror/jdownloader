package jd.controlling.proxy;

import java.util.ArrayList;

import org.appwork.storage.Storable;
import org.jdownloader.updatev2.ProxyData;

public class ProxyExportImport implements Storable {
    private ArrayList<ProxyData> customProxyList;
    private boolean              noneDefault;
    private boolean              noneRotationEnabled;

    public ProxyExportImport(/* storable */) {

    }

    public void setCustomProxyList(ArrayList<ProxyData> ret) {
        customProxyList = ret;
    }

    public ArrayList<ProxyData> getCustomProxyList() {
        return customProxyList;
    }

    public void setNoneDefault(boolean b) {
        this.noneDefault = b;
    }

    public boolean isNoneDefault() {
        return noneDefault;
    }

    public void setNoneRotationEnabled(boolean proxyRotationEnabled) {
        this.noneRotationEnabled = proxyRotationEnabled;
    }

    public boolean isNoneRotationEnabled() {
        return noneRotationEnabled;
    }

}
