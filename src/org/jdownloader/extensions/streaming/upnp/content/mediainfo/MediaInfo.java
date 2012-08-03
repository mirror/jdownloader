package org.jdownloader.extensions.streaming.upnp.content.mediainfo;

import org.seamless.util.MimeType;

public interface MediaInfo {

    public MimeType getMimeType();

    public Long getContentLength();

    public String getTitle();
}
