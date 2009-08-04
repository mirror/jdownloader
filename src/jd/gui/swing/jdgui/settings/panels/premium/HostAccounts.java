package jd.gui.swing.jdgui.settings.panels.premium;

import jd.config.Property;

public class HostAccounts extends Property {
    /**
     * 
     */
    private static final long serialVersionUID = 1332558916724384633L;
    private String host = null;
    private long traffic = -1;
    private boolean enabled = true;
    private boolean gotaccountinfos = false;

    public HostAccounts(String host) {
        this.host = host;
    }

    public boolean gotAccountInfos() {
        return gotaccountinfos;
    }

    public void hasAccountInfos(boolean b) {
        gotaccountinfos = b;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean b) {
        enabled = b;
    }

    public String getHost() {
        return host;
    }

    public long getTraffic() {
        return traffic;
    }

    /*-1 for unlimited*/
    public void setTraffic(long t) {
        traffic = t;
    }

}
