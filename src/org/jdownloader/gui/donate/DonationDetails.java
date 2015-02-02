package org.jdownloader.gui.donate;

import java.util.ArrayList;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

public class DonationDetails implements Storable {
    public static final TypeRef<DonationDetails> TYPEREF = new TypeRef<DonationDetails>() {
                                                         };

    public DonationDetails(/* Storable */) {
    }

    private boolean recurringEnabled = false;

    public boolean isRecurringEnabled() {
        return recurringEnabled;
    }

    public PaymentProvider[] paymentProvider;

    public PaymentProvider[] getPaymentProvider() {
        return paymentProvider;
    }

    private int defaultProvider = -1;

    public int getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(int defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public void setPaymentProvider(PaymentProvider[] paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public void setRecurringEnabled(boolean recurringEnabled) {
        this.recurringEnabled = recurringEnabled;
    }

    private boolean enabled;
    private int     defaultAmount = 20;

    public int getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(int defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private ArrayList<CategoryPriority> categories;

    public ArrayList<CategoryPriority> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<CategoryPriority> categories) {
        this.categories = categories;
    }

}