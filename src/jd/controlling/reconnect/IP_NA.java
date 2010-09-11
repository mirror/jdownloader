package jd.controlling.reconnect;

public class IP_NA extends IP {

    public static int   IP_NA_FAILED        = 1;
    public static int   IP_NA_SEQ_FAILED    = 2;
    public static IP_NA IP_NA               = new IP_NA(null, 0);
    public static IP_NA IPCHECK_UNSUPPORTED = new IP_NA("Unsupported", 0);
    private String      reason              = null;
    private int         reasonID            = -1;

    public IP_NA(String reason, int reasonID) {
        super("NotAvailable");
        this.reason = reason;
        this.reasonID = reasonID;
    }

    public IP_NA(String reason) {
        super("NotAvailable");
        this.reason = reason;
        this.reasonID = -1;
    }

    public String getReason() {
        return reason;
    }

    public int getReasonID() {
        return reasonID;
    }

    public boolean equals(Object c) {
        if (c != null && c instanceof IP_NA) return true;
        return false;
    }

    public String toString() {
        return "NotAvailable" + (reason != null ? " " + reason : "");
    }
}
