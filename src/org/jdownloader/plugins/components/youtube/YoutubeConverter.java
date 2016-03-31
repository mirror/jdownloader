package org.jdownloader.plugins.components.youtube;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public interface YoutubeConverter {

    void run(DownloadLink downloadLink, PluginForHost plugin) throws Exception;

}