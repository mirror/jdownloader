package jd.controlling.proxy;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class ProxyDataList extends ArrayList<ProxyData> implements Storable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ProxyDataList() {
        super();
    }

    private int defaultProxy = 0;

    /**
     * @return the defaultProxy
     */
    protected int getDefaultProxy() {
        return defaultProxy;
    }

    /**
     * @param defaultProxy
     *            the defaultProxy to set
     */
    protected void setDefaultProxy(int defaultProxy) {
        this.defaultProxy = defaultProxy;
    }

}
