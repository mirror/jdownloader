package org.jdownloader.extensions.streaming.upnp;

import org.jdownloader.extensions.streaming.upnp.content.ContentProvider;
import org.jdownloader.extensions.streaming.upnp.content.ListContentProvider;

public class ContentFactory {

    public static ContentProvider create() {
        return new ListContentProvider();
    }

}
