package org.jdownloader.captcha.v2.solver.captchasolutions;

public class CaptchaSolutionsAccount {
    private int    tokens;

    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getTokens() {
        return tokens;
    }

    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setTokens(int balance) {
        this.tokens = balance;
    }

    public boolean isValid() {
        return error == null;
    }

}
