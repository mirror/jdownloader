package org.jdownloader.controlling.download;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatusProperty;

import org.appwork.exceptions.WTFException;

public class DownloadControllerEventDataUpdate extends DownloadControllerEvent {

    public DownloadControllerEventDataUpdate(AbstractNode source, Object param) {
        super(DownloadControllerEvent.TYPE.REFRESH_CONTENT, source, param);
    }

    @Override
    public void sendToListener(DownloadControllerListener listener) {
        if (getParameter(0) instanceof DownloadLink && getParameter(1) instanceof DownloadLinkProperty) {
            listener.onDownloadControllerUpdatedData((DownloadLink) getParameter(0), (DownloadLinkProperty) getParameter(1));
        } else if (getParameter(0) instanceof FilePackage && getParameter(1) instanceof FilePackageProperty) {
            listener.onDownloadControllerUpdatedData((FilePackage) getParameter(0), (FilePackageProperty) getParameter(1));
        } else if (getParameter(0) instanceof DownloadLink && getParameter(1) instanceof LinkStatusProperty) {
            listener.onDownloadControllerUpdatedData((DownloadLink) getParameter(0), (LinkStatusProperty) getParameter(1));
        } else {
            throw new WTFException();
        }
    }

}
