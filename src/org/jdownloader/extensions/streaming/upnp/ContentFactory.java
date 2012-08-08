package org.jdownloader.extensions.streaming.upnp;

import org.jdownloader.extensions.streaming.upnp.content.ContentDirectory;
import org.jdownloader.extensions.streaming.upnp.content.ContentProvider;
import org.jdownloader.extensions.streaming.upnp.content.ListContentProvider;

public class ContentFactory {

    public static ContentProvider create(ContentDirectory contentDirectory, MediaServer mediaServer) {
        return new ListContentProvider(contentDirectory, mediaServer);
    }

}
