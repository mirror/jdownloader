package jd.plugins;

import java.io.Serializable;

public class TransferStatus implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5529970643122096722L;
    private boolean supportsresume = false;
    private boolean supportspremium = false;
    private boolean usespremium = false;

    public boolean supportsResume() {
        return supportsresume;
    }

    public boolean usesPremium() {
        return usespremium;
    }

    public boolean supportsPremium() {
        return supportspremium;
    }

    public void setResumeSupport(boolean b) {
        supportsresume = b;
    }

    public void setPremiumSupport(boolean b) {
        supportspremium = b;
    }

    public void usePremium(boolean b) {
        usespremium = b;
    }

}
