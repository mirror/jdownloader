package jd.controlling.reconnect;

public class IP_INVALID extends IP {

    public static IP_INVALID IP_INVALID = new IP_INVALID();

    public IP_INVALID() {
        super("Invalid");
    }

    public boolean equals(Object c) {
        if (c != null && c instanceof IP_INVALID) return true;
        return false;
    }

    public int hashCode() {
        return "Invalid".hashCode();
    }
    
    public String toString() {
        return "Invalid" + (IPV4 != null ? " " + IPV4 : "");
    }

}
