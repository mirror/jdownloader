package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.bindings.CrawledLinkQuery;
import org.jdownloader.myjdownloader.client.bindings.CrawledLinkStorable;
import org.jdownloader.myjdownloader.client.bindings.LinkVariantStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.LinkgrabberInterface;

public class LinkgrabberTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        LinkgrabberInterface link = api.link(LinkgrabberInterface.class, chooseDevice(api));
        CrawledLinkStorable[] smallList = link.queryLinks(new CrawledLinkQuery());
        // CrawledLinkQuery query = new CrawledLinkQuery();
        //
        // query.setBytesTotal(true);
        // query.setEnabled(true);
        //
        // query.setHost(true);
        //
        // query.setUrl(true);
        // CrawledLinkStorable[] bigList = link.queryLinks(query);
        // CrawledPackageQuery pq = new CrawledPackageQuery();
        // pq.setHosts(true);
        // link.queryPackages(pq);
        CrawledLinkStorable cl = smallList[0];
        LinkVariantStorable[] variants = link.getVariants(cl.getUuid());
        link.setVariant(cl.getUuid(), variants[0].getId());
        System.out.println(1);
    }
}
