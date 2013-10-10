package org.jdownloader.plugins;

public interface ValidatableConditionalSkipReason {

    public boolean isValid();

    public void invalidate();
}
