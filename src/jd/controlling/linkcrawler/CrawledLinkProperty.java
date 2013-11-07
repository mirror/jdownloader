package jd.controlling.linkcrawler;

public class CrawledLinkProperty {
    public static enum Property {
        NAME,
        PRIORITY,
        ENABLED,
        AVAILABILITY,
        ARCHIVE
    }

    private final Object      value;
    private final CrawledLink link;
    private Property          property;

    public CrawledLinkProperty(CrawledLink link, Property property, Object value) {
        this.value = value;
        this.link = link;
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public CrawledLink getCrawledLink() {
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
