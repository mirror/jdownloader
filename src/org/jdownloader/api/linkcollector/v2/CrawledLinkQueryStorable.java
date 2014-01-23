package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.linkgrabber.CrawledLinkQuery;

public class CrawledLinkQueryStorable extends CrawledLinkQuery implements Storable {
    public CrawledLinkQueryStorable() {
        super(/* Storable */);
    }

}