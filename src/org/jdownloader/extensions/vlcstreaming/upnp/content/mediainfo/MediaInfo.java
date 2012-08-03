package org.jdownloader.extensions.vlcstreaming.upnp.content.mediainfo;

import org.seamless.util.MimeType;

public interface MediaInfo {

    public MimeType getMimeType();

    public Long getContentLength();

    public String getTitle();
}
