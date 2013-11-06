package org.jdownloader.controlling.download;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;

public class DownloadControllerEventStructureRefresh extends DownloadControllerEvent {

    public DownloadControllerEventStructureRefresh() {
        super(DownloadControllerEvent.TYPE.REFRESH_STRUCTURE);
    }

    public DownloadControllerEventStructureRefresh(AbstractNode source, Object param) {
        super(DownloadControllerEvent.TYPE.REFRESH_STRUCTURE, source, param);
    }

    public DownloadControllerEventStructureRefresh(FilePackage pkg) {
        super(DownloadControllerEvent.TYPE.REFRESH_STRUCTURE, pkg);
    }

    @Override
    public void sendToListener(DownloadControllerListener listener) {
        if (getParameters().length == 1) {
            if (getParameter() instanceof FilePackage) {
                listener.onDownloadControllerStructureRefresh((FilePackage) getParameter());
                return;
            } else {
                throw new WTFException();
            }
        } else if (getParameters().length == 0) {
            listener.onDownloadControllerStructureRefresh();
        } else {
            listener.onDownloadControllerStructureRefresh((AbstractNode) getParameter(0), getParameter(1));
        }

    }

}
