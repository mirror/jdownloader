package org.jdownloader.extensions.streaming.upnp;

import jd.plugins.DownloadLink;

public interface PlayToDevice {

    String getDisplayName();

    String getUniqueDeviceID();

    void play(DownloadLink link, String id, String subpath);

}
