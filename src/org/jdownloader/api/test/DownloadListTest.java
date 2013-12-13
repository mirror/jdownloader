package org.jdownloader.api.test;

import java.util.List;

import org.appwork.storage.Storage;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.bindings.DownloadLinkStorable;
import org.jdownloader.myjdownloader.client.bindings.LinkQuery;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadsListInterface;

public class DownloadListTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        DownloadsListInterface link = api.link(DownloadsListInterface.class, chooseDevice(api));
        List<DownloadLinkStorable> smallList = link.queryLinks(new LinkQuery());
        LinkQuery query = new LinkQuery();
        query.setBytesLoaded(true);
        query.setBytesTotal(true);
        query.setEnabled(true);
        query.setEta(true);
        query.setExtractionStatus(true);
        query.setFinished(true);
        query.setHost(true);
        query.setRunning(true);
        query.setSkipped(true);
        query.setSpeed(true);
        query.setUrl(true);
        List<DownloadLinkStorable> bigList = link.queryLinks(query);

        System.out.println(1);
    }
}
