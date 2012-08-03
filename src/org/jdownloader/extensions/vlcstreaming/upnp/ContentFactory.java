package org.jdownloader.extensions.vlcstreaming.upnp;

import org.jdownloader.extensions.vlcstreaming.upnp.content.BasicContentProvider;
import org.jdownloader.extensions.vlcstreaming.upnp.content.ContentProvider;

public class ContentFactory {

    public static ContentProvider create() {
        return new BasicContentProvider();
    }

}
