package org.jdownloader.extensions.vlcstreaming.upnp;

public class ContentFactory {

    public static ContentProvider create() {
        return new BasicContentProvider();
    }

}
