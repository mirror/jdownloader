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
        EXTRACTION_STATUS,
        LINKSTATUS,
        ARCHIVE_ID,

        CHUNKS,
        SPEED_LIMIT,
        DOWNLOAD_PASSWORD,
        MD5,
        RESUMABLE,
        SHA1,
        URL_CONTENT,
        URL_PROTECTION,
        VARIANT,
        VARIANTS,
        VARIANTS_ENABLED,
        DOWNLOADSIZE_VERIFIED,
        DOWNLOADSIZE,
        COMMENT,
        URL_CONTAINER,
        URL_ORIGIN,
        URL_REFERRER,
        URL_CUSTOM;
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
