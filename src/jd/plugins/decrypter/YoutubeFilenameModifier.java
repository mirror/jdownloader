package jd.plugins.decrypter;

import jd.plugins.DownloadLink;

public interface YoutubeFilenameModifier {

    String run(String formattedFilename, DownloadLink link);

}