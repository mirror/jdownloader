package jd.plugins;

public class FilePackageProperty {
    public static enum Property {
        NAME,
        FOLDER

    }

    private final Object      value;
    private final FilePackage pkg;
    private Property          property;

    public FilePackageProperty(FilePackage pkg, Property property, Object value) {
        this.value = value;
        this.pkg = pkg;
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public FilePackage getFilePackage() {
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
