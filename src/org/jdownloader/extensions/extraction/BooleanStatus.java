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

    public static BooleanStatus convert(Boolean status) {
        return status == null ? UNSET : (status ? TRUE : FALSE);
    }

    public static BooleanStatus get(BooleanStatus status) {
        return status == null ? UNSET : status;
    }

    public static Boolean convert(BooleanStatus status) {
        return status == null ? null : (status == TRUE ? Boolean.TRUE : Boolean.FALSE);
    }
}