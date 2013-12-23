package jd.plugins;

public class AccountProperty {
    public static enum Property {
        ENABLED,
        ERROR,
        PASSWORD,
        USERNAME;
    }

    private final Object  value;
    private final Account acc;
    private Property      property;

    public AccountProperty(Account acc, Property property, Object value) {
        this.value = value;
        this.acc = acc;
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public Account getAccount() {
        return acc;
    }

    @Override
    public String toString() {
        return acc + ":" + property + "=" + value;
    }

    public Property getProperty() {
        return property;
    }
}
