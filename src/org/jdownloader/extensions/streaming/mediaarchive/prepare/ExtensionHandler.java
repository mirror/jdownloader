package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public abstract class ExtensionHandler<ItemType extends MediaItem> {

    public abstract ItemType handle(StreamingExtension extension, DownloadLink dl);
}
