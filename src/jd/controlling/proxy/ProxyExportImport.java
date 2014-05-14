package jd.controlling.proxy;

import java.util.ArrayList;

import org.appwork.storage.Storable;
import org.jdownloader.updatev2.ProxyData;

public class ProxyExportImport implements Storable {
    private ArrayList<ProxyData> customProxyList;

    public ProxyExportImport(/* storable */) {

    }

    public void setCustomProxyList(ArrayList<ProxyData> ret) {
        customProxyList = ret;
    }

    public ArrayList<ProxyData> getCustomProxyList() {
        return customProxyList;
    }

}
