package org.jdownloader.extensions.translator;


public class TranslationError {
    public static enum Type {
        ERROR, WARNING
    }

    private String message;
    private Type   type;

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }

    public TranslationError(Type type, String string) {
        this.message = string;
        this.type = type;
    }

    public String toString() {
        return "[" + type + "]" + message;
    }

}
