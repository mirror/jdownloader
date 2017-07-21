package org.jdownloader.auth;

import java.util.Locale;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;

public class AuthenticationInfo implements Storable {
    public AuthenticationInfo() {
        // empty const. required for Storable interface
    }

    private final UniqueAlltimeID id = new UniqueAlltimeID();

    public void setId(long id) {
        this.id.setID(id);
    }

    public long getId() {
        return id.getID();
    }

    public static enum Type {
        FTP,
        HTTP
    }

    private long created = System.currentTimeMillis();

    public long getCreated() {
        return created;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj instanceof AuthenticationInfo)) {
            return false;
        } else {
            final AuthenticationInfo o = (AuthenticationInfo) obj;
            return getType() == o.getType() && StringUtils.equals(getUsername(), o.getUsername()) && StringUtils.equals(getPassword(), o.getPassword()) && StringUtils.equals(getRealm(), o.getRealm()) && StringUtils.equals(getHostmask(), o.getHostmask());
        }
    }

    @Override
    public int hashCode() {
        return AuthenticationInfo.class.hashCode();
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getLastValidated() {
        return lastValidated;
    }

    public void setLastValidated(long lastValidated) {
        this.lastValidated = lastValidated;
    }

    private long    lastValidated = -1;
    private boolean enabled       = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getHostmask() {
        return hostmask;
    }

    public void setHostmask(String hostmask) {
        if (hostmask == null) {
            this.hostmask = null;
        } else {
            this.hostmask = hostmask.toLowerCase(Locale.ENGLISH);
        }
    }

    private String username;
    private String password;
    private Type   type = Type.HTTP;
    private String hostmask;
    private String realm;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
