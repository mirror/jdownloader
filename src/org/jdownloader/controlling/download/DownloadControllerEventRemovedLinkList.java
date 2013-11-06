package org.jdownloader.controlling.download;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;

public class DownloadControllerEventRemovedLinkList extends DownloadControllerEvent {

    public DownloadControllerEventRemovedLinkList(ArrayList<DownloadLink> arrayList) {
        super(DownloadControllerEvent.TYPE.REMOVE_CONTENT, arrayList);
    }

    @Override
    public void sendToListener(DownloadControllerListener listener) {
        listener.onDownloadControllerRemovedLinklist((List<DownloadLink>) getParameter());
    }

}
