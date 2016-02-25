package org.jdownloader.captcha.v2.solver.dbc;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

public class DBCGetUserResponse implements Storable {
    public static final TypeRef<DBCGetUserResponse> TYPE = new TypeRef<DBCGetUserResponse>() {
    };

    public DBCGetUserResponse(/* Storable */) {
    }

    private boolean is_banned;

    public boolean isIs_banned() {
        return is_banned;
    }

    public void setIs_banned(boolean is_banned) {
        this.is_banned = is_banned;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    private int    status;
    private double rate;
    private double balance;
    private int    user;
}
