package org.jdownloader.captcha.v2.solver.antiCaptchaCom;

public class AntiCaptchaComAccount {
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

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isValid() {
        return error == null;
    }
}
