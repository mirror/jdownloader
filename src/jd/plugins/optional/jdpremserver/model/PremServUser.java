package jd.plugins.optional.jdpremserver.model;

import java.util.ArrayList;

public class PremServUser {

    private String username;
    private String password;
    private boolean enabled;
    private ArrayList<String> hosters;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ArrayList<String> getHosters() {
        return hosters;
    }

    public void setHosters(ArrayList<String> hosters) {
        this.hosters = hosters;
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

    public PremServUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

}
