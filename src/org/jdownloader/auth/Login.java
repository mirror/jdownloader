package org.jdownloader.auth;


public class Login {
    private String username;

    public Login(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Login() {
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

    private String password;

    public String toBasicAuth() {
        return new BasicAuth(getUsername(), getPassword()).toAuthString();
    }

}
