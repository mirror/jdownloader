package org.jdownloader.captcha.v2.solver.imagetyperz;

public class ImageTyperzAccount {
    private double balance;

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

    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isValid() {
        return error == null;
    }

}
