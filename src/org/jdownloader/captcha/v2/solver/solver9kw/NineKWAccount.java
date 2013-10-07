package org.jdownloader.captcha.v2.solver.solver9kw;

import org.appwork.utils.StringUtils;

public class NineKWAccount {
    private int solved;

    public int getSolved() {
        return solved;
    }

    public void setSolved(int solved) {
        this.solved = solved;
    }

    public int getAnswered() {
        return answered;
    }

    public void setAnswered(int answered) {
        this.answered = answered;
    }

    private int    answered;
    private int    creditBalance;
    private String error;

    public String getError() {
        return error;
    }

    public void setCreditBalance(int creditBalance) {
        this.creditBalance = creditBalance;
    }

    public boolean isValid() {
        return StringUtils.isEmpty(error);
    }

    public int getCreditBalance() {
        return creditBalance;
    }

    public void setError(String credits) {
        error = credits;
    }

}
