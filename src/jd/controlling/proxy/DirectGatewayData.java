package jd.controlling.proxy;

import org.appwork.storage.Storable;

public class DirectGatewayData implements Storable {
    public DirectGatewayData() {
        // required by Storable
    }

    private String  ip;
    private boolean proxyRotationEnabled;
    private boolean isDefault;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isProxyRotationEnabled() {
        return proxyRotationEnabled;
    }

    public void setProxyRotationEnabled(boolean proxyRotationEnabled) {
        this.proxyRotationEnabled = proxyRotationEnabled;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

}
