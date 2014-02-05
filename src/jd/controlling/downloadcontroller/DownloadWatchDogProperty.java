package jd.controlling.downloadcontroller;


public class DownloadWatchDogProperty {
    public static enum Property {
        STOPSIGN
    }

    private final Object value;
    private Property     property;

    public DownloadWatchDogProperty(Property property, Object value) {
        this.value = value;
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return property + "=" + value;
    }

    public Property getProperty() {
        return property;
    }
}
