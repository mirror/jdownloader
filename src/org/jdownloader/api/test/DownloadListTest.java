package org.jdownloader.api.test;

import java.util.List;

import org.appwork.storage.Storage;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.bindings.DownloadLinkStorable;
import org.jdownloader.myjdownloader.client.bindings.LinkQuery;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadsListInterface;

public class DownloadListTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        DownloadsListInterface link = api.link(DownloadsListInterface.class, chooseDevice(api));
        List<DownloadLinkStorable> smallList = link.queryLinks(new LinkQuery());

        System.out.println(1);
    }
}
