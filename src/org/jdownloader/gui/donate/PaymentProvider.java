package org.jdownloader.gui.donate;

import org.appwork.storage.Storable;

public class PaymentProvider implements Storable {
    private String  id;
    private String  cCode;
    private String  cSymbol;
    private boolean recurring = false;

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getcCode() {
        return cCode;
    }

    public void setcCode(String cCode) {
        this.cCode = cCode;
    }

    public String getcSymbol() {
        return cSymbol;
    }

    public void setcSymbol(String cSymbol) {
        this.cSymbol = cSymbol;
    }

    public double getAmt() {
        return amt;
    }

    public void setAmt(double amt) {
        this.amt = amt;
    }

    public double[] getAmtSuggest() {
        return amtSuggest;
    }

    public void setAmtSuggest(double[] amtSuggest) {
        this.amtSuggest = amtSuggest;
    }

    private double   amt;
    private double[] amtSuggest;
    private double   amtMin;

    public double getAmtMin() {
        return amtMin;
    }

    public void setAmtMin(double amtMin) {
        this.amtMin = amtMin;
    }

    public double getAmtMax() {
        return amtMax;
    }

    public void setAmtMax(double amtMax) {
        this.amtMax = amtMax;
    }

    private double amtMax;

    public PaymentProvider(/* Storable */) {
    }

    public PaymentProvider(String providerID, String currency, String currencySymbol, double amount, double[] suggestions, double min, double max) {
        this.id = providerID;
        this.cCode = currency;
        this.cSymbol = currencySymbol;
        this.amt = amount;
        this.amtSuggest = suggestions;
        this.amtMin = min;
        this.amtMax = max;
    }

}
