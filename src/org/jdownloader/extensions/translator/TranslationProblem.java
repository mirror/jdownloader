package org.jdownloader.extensions.translator;

/**
 * Defines various Problems in a translation entry.
 * 
 * @see org.jdownloader.extensions.translator.TranslateEntry.validate()
 * @author thomas
 * 
 */
public class TranslationProblem {
    public static enum Type {
        ERROR, WARNING
    }

    private String message;
    private Type   type;

    /**
     * Returns the Problem Message
     * 
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * 
     * @return Problem Type
     */
    public Type getType() {
        return type;
    }

    public TranslationProblem(Type type, String string) {
        this.message = string;
        this.type = type;
    }

    public String toString() {
        return "[" + type + "]" + message;
    }

}
