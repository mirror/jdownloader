package org.jdownloader.extensions.schedulerV2.actions;


public class AccountActionConfig implements IScheduleActionConfig {
    public AccountActionConfig(/* Storable */) {
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHoster() {
        return hoster;
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    private String hoster = "";

    private String user   = "";

}
