package jd.controlling.reconnect.plugins.upnp;

import java.util.HashMap;

public class UpnpRouterDevice extends HashMap<String, String> {
    /**
     * These constancs are not only storage keys, but are used by the Parser in
     * UPNPRouterPlugin. So <b>DO NEVER CHANGE THEM WITHOUT KNOWING 100% what
     * you are doing</b>
     */
    public static final String LOCATION         = "location";
    public static final String URLBASE          = "urlbase";
    public static final String FRIENDLYNAME     = "friendlyname";
    public static final String HOST             = "host";
    public static final String CONTROLURL       = "controlurl";
    public static final String WANSERVICE       = "wanservice";
    public static final String SERVICETYPE      = "servicetype";
    /**
     * 
     */
    private static final long  serialVersionUID = 1L;

    public String getControlURL() {
        return this.get(UpnpRouterDevice.CONTROLURL);
    }

    public String getFriendlyname() {
        return this.get(UpnpRouterDevice.FRIENDLYNAME);
    }

    public String getHost() {
        return this.get(UpnpRouterDevice.HOST);
    }

    public String getLocation() {
        return this.get(UpnpRouterDevice.LOCATION);
    }

    public String getServiceType() {
        return this.get(UpnpRouterDevice.SERVICETYPE);
    }

    public String getUrlBase() {
        return this.get(UpnpRouterDevice.URLBASE);
    }

    public String getWanservice() {
        return this.get(UpnpRouterDevice.WANSERVICE);
    }

    public void setControlURL(final String controlURL) {
        this.put(UpnpRouterDevice.CONTROLURL, controlURL);
    }

    public void setFriendlyname(final String friendlyname) {
        this.put(UpnpRouterDevice.FRIENDLYNAME, friendlyname);
    }

    public void setHost(final String host) {
        this.put(UpnpRouterDevice.HOST, host);
    }

    public void setLocation(final String location) {
        this.put(UpnpRouterDevice.LOCATION, location);
    }

    public void setServiceType(final String servicyType) {
        this.put(UpnpRouterDevice.SERVICETYPE, servicyType);
    }

    public void setUrlBase(final String urlBase) {
        this.put(UpnpRouterDevice.URLBASE, urlBase);
    }

    public void setWanservice(final String wanservice) {
        this.put(UpnpRouterDevice.WANSERVICE, wanservice);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != UpnpRouterDevice.class) return false;
        UpnpRouterDevice other = ((UpnpRouterDevice) o);
        String c1 = this.get(LOCATION);
        String c2 = other.get(LOCATION);
        if (c1 != null && c2 != null && !c1.equalsIgnoreCase(c2)) return false;
        String c3 = this.get(CONTROLURL);
        String c4 = other.get(CONTROLURL);
        /* only equal if location and controlurl are equal */
        if (c3 != null && c4 != null && !c3.equalsIgnoreCase(c4)) return false;
        if (c1 != null && c2 != null && c3 != null && c4 != null) return true;
        return false;
    }
    
    @Override
    public int hashCode(){
    return (this.get(LOCATION)+""+this.get(CONTROLURL)).hashCode();    
    }

}
