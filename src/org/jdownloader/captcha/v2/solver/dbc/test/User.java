package org.jdownloader.captcha.v2.solver.dbc.test;

/**
 * Death by Captcha user details.
 *
 */
public class User {
    public int     id       = 0;
    public double  balance  = 0.0;
    public boolean isBanned = false;

    public User() {
    }

    public User(DataObject src) {
        this();
        this.id = Math.max(0, src.optInt("user", 0));
        if (0 < this.id) {
            this.balance = src.optDouble("balance", 0.0);
            this.isBanned = src.optBoolean("is_banned", false);
        }
    }
}
