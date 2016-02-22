package org.jdownloader.api.ui;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.exceptions.FileNotFound404Exception;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.content.v2.MyJDMenuItem;
import org.jdownloader.api.myjdownloader.remotemenu.MenuManagerMYJDDownloadTableContext;
import org.jdownloader.api.utils.PackageControllerUtils;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.myjdownloader.client.bindings.interfaces.UIInterface;
import org.jdownloader.myjdownloader.client.bindings.interfaces.UIInterface.Context;

public class UIAPIImpl implements UIAPI {

    private final PackageControllerUtils<FilePackage, DownloadLink> downloadList;

    // private final PackageControllerUtils<CrawledPackage, CrawledLink> linkgrabber;

    public UIAPIImpl() {
        RemoteAPIController.validateInterfaces(UIAPI.class, UIInterface.class);
        downloadList = new PackageControllerUtils<FilePackage, DownloadLink>(DownloadController.getInstance());
        // linkgrabber = new PackageControllerUtils<CrawledPackage, CrawledLink>(LinkCollector.getInstance());
    }

    @Override
    public MyJDMenuItem getMenu(RemoteAPIRequest request, Context context) throws InternalApiException, FileNotFound404Exception {
        switch (context) {
        case DLC:
            return MenuManagerMYJDDownloadTableContext.getInstance().getMenuStructure();
        default:
            throw new FileNotFound404Exception();
        }
    }

    @Override
    public Object invokeAction(RemoteAPIRequest request, Context context, String id, long[] linkIds, long[] packageIds) throws InternalApiException, FileNotFound404Exception {
        switch (context) {
        case DLC:
            SelectionInfo<FilePackage, DownloadLink> selection = downloadList.getSelectionInfo(linkIds, packageIds);
            return MenuManagerMYJDDownloadTableContext.getInstance().invoke(id, selection, context);
        }
        return null;
    }
}
