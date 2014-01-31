package jd.plugins.components;

import jd.plugins.DownloadLink;

public interface YoutubeFilenameModifier {

    String run(String formattedFilename, DownloadLink link);

}