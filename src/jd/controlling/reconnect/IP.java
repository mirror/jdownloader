package jd.controlling.reconnect;

public class IP {

    protected String  IPV4    = null;
    protected String  message = null;
    protected boolean valid   = false;

    protected IP(final String message) {
        this.message = message;
        this.valid = false;
    }

    private IP() {
        valid = true;
    }

    public static IP getIP(String IPv4) {
        if (IPv4 != null) {
            if (IPv4.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                String parts[] = IPv4.split("\\.");
                if (parts.length == 4) {
                    /* filter private networks */
                    int n1 = Integer.parseInt(parts[0]);
                    /* 10.0.0.0-10.255.255.255 */
                    if (n1 == 10) return new IP_INVALID().setIP(IPv4);
                    int n2 = Integer.parseInt(parts[1]);
                    /* 192.168.0.0 - 192.168.255.255 */
                    if (n1 == 192 && n2 == 168) return new IP_INVALID().setIP(IPv4);
                    /* 172.16.0.0 - 172.31.255.255 */
                    if (n1 == 172 && n2 >= 16 && n2 <= 31) return new IP_INVALID().setIP(IPv4);
                    int n3 = Integer.parseInt(parts[2]);
                    int n4 = Integer.parseInt(parts[3]);
                    if (n1 == 0 && n2 == 0 && n3 == 0 && n4 == 0) return new IP_INVALID().setIP(IPv4);
                    if (n1 >= 0 && n1 <= 255 && n2 >= 0 && n2 <= 255 && n3 >= 0 && n3 <= 255 && n4 >= 0 && n4 <= 255) { return new IP().setIP(n1 + "." + n2 + "." + n3 + "." + n4); }
                    return new IP_INVALID().setIP(IPv4);
                }
            }
        }
        return IP_NA.IP_NA;
    }

    protected IP setIP(String IPv4) {
        IPV4 = IPv4;
        return this;
    }

    public boolean equals(Object c) {
        if (c != null && c instanceof IP) {
            IP ip = (IP) c;
            if (ip.valid) {
                if (ip.IPV4 != null && ip.IPV4.equalsIgnoreCase(this.IPV4)) return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return (message + "" + IPV4).hashCode();
    }

    public boolean changed(IP ip) {
        if (this.valid == false && ip.valid == true) return true;
        if (this.valid && ip.valid == true && !this.equals(ip)) return true;
        return false;
    }

    public boolean isValid() {
        return this.valid;
    }

    public String toString() {
        return IPV4;
    }

}
