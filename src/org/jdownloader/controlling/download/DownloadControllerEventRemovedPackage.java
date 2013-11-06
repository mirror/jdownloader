package org.jdownloader.controlling.download;

import jd.plugins.FilePackage;

public class DownloadControllerEventRemovedPackage extends DownloadControllerEvent {

    public DownloadControllerEventRemovedPackage(FilePackage pkg) {
        super(DownloadControllerEvent.TYPE.REMOVE_CONTENT, pkg);
    }

    @Override
    public void sendToListener(DownloadControllerListener listener) {
        listener.onDownloadControllerRemovedPackage((FilePackage) getParameter());
    }

}
