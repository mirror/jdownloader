package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public abstract class AbstractMediaContainer {

    private final Extensions[] extensions;
    private String             name;

    public String getName() {
        return name;
    }

    protected boolean systemStream;

    public boolean isSystemStream() {
        return systemStream;
    }

    public Extensions[] getExtensions() {
        return extensions;
    }

    public void setName(String string) {
        this.name = string;
    }

    public AbstractMediaContainer(Extensions... extensions) {
        this.extensions = extensions;
        name = toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Extensions e : getExtensions()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getExtension());
        }
        return sb.toString();
    }
}
