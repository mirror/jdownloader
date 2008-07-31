package jd.plugins;

import jd.config.Property;

public class Account extends Property {
    /**
     * 
     */
    private static final long serialVersionUID = -7578649066389032068L;
    private String user;
    private String pass;
    private int id;

    public Account(String user, String pass) {
        this.user=user;
        this.pass=pass;
    }

    public String getUser() {

        return user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public void setUser(String user) {
        this.user = user;
    }



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
