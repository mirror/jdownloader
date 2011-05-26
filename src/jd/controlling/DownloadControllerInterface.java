package jd.controlling;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public interface DownloadControllerInterface {

    void addFilePackage(FilePackage fp);

    void addDownloadLink(FilePackage fp, DownloadLink dl);
}
