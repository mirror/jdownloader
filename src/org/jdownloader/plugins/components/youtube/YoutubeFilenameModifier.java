package org.jdownloader.plugins.components.youtube;

import jd.plugins.DownloadLink;

public interface YoutubeFilenameModifier {

    String run(String formattedFilename, DownloadLink link);

}