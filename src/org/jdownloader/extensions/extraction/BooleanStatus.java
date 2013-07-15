package org.jdownloader.extensions.extraction;

public enum BooleanStatus {
    UNSET,
    TRUE,
    FALSE;

    public Boolean getBoolean() {
        switch (this) {
        case FALSE:
            return Boolean.FALSE;
        case TRUE:
            return Boolean.TRUE;
        default:
            return null;
        }
    }
}