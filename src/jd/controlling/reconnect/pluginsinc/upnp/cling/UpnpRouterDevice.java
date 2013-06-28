package jd.controlling.reconnect.pluginsinc.upnp.cling;

import java.net.MalformedURLException;
import java.net.URL;

import org.appwork.storage.Storable;

public class UpnpRouterDevice implements Storable {

    private String wanservice;
    private String modelname;
    private String controlURL;
    private String serviceType;
    private String manufactor;
    private String friendlyname;

    public void setWanservice(String wanservice) {
        this.wanservice = wanservice;
    }

    public void setModelname(String modelname) {
        this.modelname = modelname;
    }

    public UpnpRouterDevice(/* storable */) {

    }

    public String getWanservice() {
        return wanservice;
    }

    public String getModelname() {
        return modelname;
    }

    public void setControlURL(String string) {
        controlURL = string;
    }

    public String getControlURL() {
        return controlURL;
    }

    public void setServiceType(String string) {
        serviceType = string;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getHost() {
        try {
            return new URL(getControlURL()).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public String getManufactor() {
        return manufactor;
    }

    public String getFriendlyname() {
        return friendlyname;
    }

    public void setManufactor(String manufactor) {
        this.manufactor = manufactor;
    }

    public void setFriendlyname(String friendlyname) {
        this.friendlyname = friendlyname;
    }
}
