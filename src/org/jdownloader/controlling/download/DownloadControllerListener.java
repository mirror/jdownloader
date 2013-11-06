package org.jdownloader.controlling.download;

import java.util.EventListener;
import java.util.List;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatusProperty;

public interface DownloadControllerListener extends EventListener {

    void onDownloadControllerAddedPackage(FilePackage pkg);

    void onDownloadControllerStructureRefresh(FilePackage pkg);

    void onDownloadControllerStructureRefresh();

    void onDownloadControllerStructureRefresh(AbstractNode node, Object param);

    void onDownloadControllerRemovedPackage(FilePackage pkg);

    void onDownloadControllerRemovedLinklist(List<DownloadLink> list);

    void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property);

    void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property);

    void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property);

    void onDownloadControllerUpdatedData(DownloadLink downloadlink);

    void onDownloadControllerUpdatedData(FilePackage pkg);

}