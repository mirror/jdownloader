package org.jdownloader.extensions.extraction;

public class DummyArchiveFile {

    private String  name;
    private boolean exists = true;

    public boolean isExists() {
        return exists;
    }

    public DummyArchiveFile setExists(boolean exists) {
        this.exists = exists;
        return this;
    }

    public DummyArchiveFile(String miss) {
        name = miss;
        setExists(false);
    }

    public String toString() {
        return name;
    }

    public DummyArchiveFile(ArchiveFile af) {
        name = af.getName();
    }

    public String getName() {
        return name;
    }

}
