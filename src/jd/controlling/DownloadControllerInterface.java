package jd.controlling;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public interface DownloadControllerInterface {

    void addDownloadLinks(FilePackage fp, DownloadLink... dl);

    void removeDownloadLinks(FilePackage fp, DownloadLink... dl);
}
