package jd.plugins;

public class DownloadLinkProperty {
    public static enum Property {
        RESET,
        NAME,
        PRIORITY,
        ENABLED,
        AVAILABILITY,
        FINAL_STATE,
        SKIPPED,
        PLUGIN_PROGRESS,
        CONDITIONAL_SKIPPED,
        ARCHIVE,
        EXTRACTION_STATUS;
    }

    private final Object       value;
    private final DownloadLink link;
    private Property           property;

    public DownloadLinkProperty(DownloadLink link, Property property, Object value) {
        this.value = value;
        this.link = link;
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public DownloadLink getDownloadLink() {
        return link;
    }

    @Override
    public String toString() {
        return link + ":" + property + "=" + value;
    }

    public Property getProperty() {
        return property;
    }
}
