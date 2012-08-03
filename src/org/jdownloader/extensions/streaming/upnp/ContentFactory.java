package org.jdownloader.extensions.streaming.upnp;

import org.jdownloader.extensions.streaming.upnp.content.BasicContentProvider;
import org.jdownloader.extensions.streaming.upnp.content.ContentProvider;

public class ContentFactory {

    public static ContentProvider create() {
        return new BasicContentProvider();
    }

}
