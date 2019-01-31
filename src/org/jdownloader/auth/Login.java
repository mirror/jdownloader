package org.jdownloader.auth;

import org.jdownloader.auth.AuthenticationInfo.Type;

public class Login {
    protected final Type    type;
    protected final String  host;
    protected final String  realm;
    protected final String  username;
    protected final String  password;
    protected final boolean alwaysFlag;

    public String getRealm() {
        return realm;
    }

    public Login(Type type, String host, String realm, String username, String password, boolean alwaysFlag) {
        this.type = type;
        this.host = host;
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.alwaysFlag = alwaysFlag;
    }

    public boolean isAlwaysFlag() {
        return alwaysFlag;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Type getType() {
        return type;
    }

    public void validate() {
    }

    public boolean isRememberSelected() {
        return true;
    }

    public String getHost() {
        return host;
    }
}
