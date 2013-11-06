package org.jdownloader.controlling.download;

import jd.plugins.FilePackage;

public class DownloadControllerEventAddedPackage extends DownloadControllerEvent {

    public DownloadControllerEventAddedPackage(FilePackage pkg) {
        super(DownloadControllerEvent.TYPE.ADD_CONTENT, pkg);
    }

    @Override
    public void sendToListener(DownloadControllerListener listener) {
        listener.onDownloadControllerAddedPackage((FilePackage) getParameter());
    }

}
