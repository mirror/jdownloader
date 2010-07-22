package jd.plugins.optional.jdpremserver.model;

import java.util.ArrayList;

public class UserData {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public PremServUser getPremServUser() {
        // TODO Auto-generated method stub
        PremServUser ret = new PremServUser(username, password);
        ret.setHosters(hosters);
        ret.setEnabled(enabled);
        return ret;
    }

    public static UserData create(PremServUser u) {
        UserData ret = new UserData();
        ret.password = u.getPassword();
        ret.username = u.getUsername();
        ret.hosters = u.getHosters();
        ret.enabled = u.isEnabled();
        return ret;
    }

}
