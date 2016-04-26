package jd.plugins;

import jd.http.Cookie;

import org.appwork.storage.Storable;

public class CookieStorable implements Storable {

    private String path = null;

    public String getPath() {
        if (this.cookie != null) {
            return this.cookie.getPath();
        }
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHost() {
        if (this.cookie != null) {
            return this.cookie.getHost();
        }
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getValue() {
        if (this.cookie != null) {
            return this.cookie.getValue();
        }
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        if (this.cookie != null) {
            return this.cookie.getKey();
        }
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDomain() {
        if (this.cookie != null) {
            return this.cookie.getDomain();
        }
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public long getHostTime() {
        if (this.cookie != null) {
            return this.cookie.getHostTime();
        }
        return this.hostTime;
    }

    public void setHostTime(long hostTime) {
        this.hostTime = hostTime;
    }

    public long getCreationTime() {
        if (this.cookie != null) {
            return this.cookie.getCreationTime();
        }
        return this.creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getExpireTime() {
        if (this.cookie != null) {
            return this.cookie.getExpireDate();
        }
        return this.expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    private String  host   = null;
    private String  value  = null;
    private String  key    = null;
    private String  domain = null;
    private Boolean secure = null;

    public Boolean getSecure() {
        if (this.cookie != null) {
            return this.cookie.isSecure();
        }
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    private long         hostTime     = -1;
    private long         creationTime = -1;
    private long         expireTime   = -1;

    private final Cookie cookie;

    public CookieStorable(/* Storable */) {
        this.cookie = null;
    }

    public CookieStorable(Cookie cookie) {
        this.cookie = cookie;
    }

    public Cookie _restore() {
        final Cookie ret = new Cookie();
        ret.setHost(this.getHost());
        ret.setValue(this.getValue());
        ret.setKey(this.getKey());
        ret.setDomain(this.getDomain());
        ret.setHostTime(this.getHostTime());
        ret.setCreationTime(this.getCreationTime());
        ret.setExpireDate(this.getExpireTime());
        ret.setSecure(getSecure());
        return ret;
    }
}