package org.jdownloader.extensions.extraction;

public enum BooleanStatus {
    UNSET,
    TRUE,
    FALSE;
    public final Boolean getBoolean() {
        switch (this) {
        case FALSE:
            return Boolean.FALSE;
        case TRUE:
            return Boolean.TRUE;
        default:
            return null;
        }
    }

    public static final BooleanStatus convert(final Boolean status) {
        return status == null ? UNSET : (status ? TRUE : FALSE);
    }

    public static final BooleanStatus get(final BooleanStatus status) {
        return status == null ? UNSET : status;
    }

    public static final boolean isSet(final BooleanStatus status) {
        return status != null && status.getBoolean() != null;
    }

    public static final Boolean convert(final BooleanStatus status) {
        if (status != null) {
            return status.getBoolean();
        } else {
            return null;
        }
    }
}