package jd.controlling.proxy;

import org.appwork.storage.Storable;

/*this class is for */
public class ProxyData implements Storable {

    public ProxyData() {
        // required by Storable
    }

    public static enum StatusID {
        OK, OFFLINE, INVALIDAUTH
    }

    public static enum Type {
        SOCKS5, HTTP
    }

    private String host = null;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user
     *            the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * @param pass
     *            the pass to set
     */
    public void setPass(String pass) {
        this.pass = pass;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the status
     */
    public StatusID getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(StatusID status) {
        this.status = status;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    private String   user                 = null;
    private String   pass                 = null;
    private int      port                 = 0;
    private StatusID status               = StatusID.OK;
    private Type     type                 = Type.HTTP;
    private boolean  proxyRotationEnabled = true;
    private boolean  defaultProxy;

    public boolean isDefaultProxy() {
        return defaultProxy;
    }

    public void setDefaultProxy(boolean defaultProxy) {
        this.defaultProxy = defaultProxy;
    }

    /**
     * @return the enabled
     */
    public boolean isProxyRotationEnabled() {
        return proxyRotationEnabled;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setProxyRotationEnabled(boolean enabled) {
        this.proxyRotationEnabled = enabled;
    }

}
