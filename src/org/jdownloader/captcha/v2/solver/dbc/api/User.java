/*
 * Source: http://deathbycaptcha.eu/user/api
 * Slightly modified to work without json and base64 dependencies
 */
package org.jdownloader.captcha.v2.solver.dbc.api;

/**
 * Death by Captcha user details.
 * 
 */
public class User {
    private int    id      = 0;
    private double balance = 0.0;

    public int getId() {
        return id;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isBanned() {
        return isBanned;
    }

    private boolean isBanned = false;
    private double  rate     = 0.0;

    public double getRate() {
        return rate;
    }

    public User() {
    }

    public User(DataObject src) {
        this();
        this.id = Math.max(0, src.optInt("user", 0));
        if (0 < this.id) {
            this.balance = src.optDouble("balance", 0.0);
            this.rate = src.optDouble("rate", 0.0);
            this.isBanned = src.optBoolean("is_banned", false);
        }
    }
}
