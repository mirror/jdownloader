package org.jdownloader.captcha.v2.solver.jac;

import org.appwork.storage.Storable;

public class AutoTrust implements Storable {
    private int value;

    public AutoTrust(/* Storable */) {
    }

    public AutoTrust(int priority) {
        this.value = priority;
        this.counter = 1;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    private long counter;

    public void add(int priority) {
        value = (int) ((value * counter + priority) / (counter + 1));
        counter++;
    }
}
