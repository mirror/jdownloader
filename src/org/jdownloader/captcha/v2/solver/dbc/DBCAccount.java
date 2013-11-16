package org.jdownloader.captcha.v2.solver.dbc;

public class DBCAccount {
    private double balance;

    public void setId(int id) {
        this.id = id;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    private int id;

    public int getId() {
        return id;
    }

    private double  rate;
    private boolean banned;

    public double getRate() {
        return rate;
    }

    public boolean isBanned() {
        return banned;
    }

    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isValid() {
        return error == null;
    }
}
