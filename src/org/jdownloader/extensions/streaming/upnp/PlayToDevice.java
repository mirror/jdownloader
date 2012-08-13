package org.jdownloader.extensions.streaming.upnp;

import jd.plugins.DownloadLink;

public interface PlayToDevice {

    String getDisplayName();

    void play(DownloadLink link, String id);

}
