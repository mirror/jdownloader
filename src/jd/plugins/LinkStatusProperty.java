package jd.plugins;

public class LinkStatusProperty {
    public static enum Property {
        STATUSTEXT,
        STATUS,
        PROGRESS,
        ACTIVE
    }

    private final Object     value;
    private final LinkStatus status;
    private Property         property;

    public LinkStatusProperty(LinkStatus status, Property property, Object value) {
        this.value = value;
        this.status = status;
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public LinkStatus getLinkStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status + ":" + property + "=" + value;
    }

    public Property getProperty() {
        return property;
    }
}
