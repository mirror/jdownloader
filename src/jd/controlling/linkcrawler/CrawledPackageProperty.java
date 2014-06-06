package jd.controlling.linkcrawler;

public class CrawledPackageProperty {
    public static enum Property {
        NAME,
        FOLDER,
        PRIORITY
    }

    private final Object         value;
    private final CrawledPackage pkg;
    private Property             property;

    public CrawledPackageProperty(CrawledPackage pkg, Property property, Object value) {
        this.value = value;
        this.pkg = pkg;
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public CrawledPackage getCrawledPackage() {
        return pkg;
    }

    @Override
    public String toString() {
        return pkg + ":" + property + "=" + value;
    }

    public Property getProperty() {
        return property;
    }
}
