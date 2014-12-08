package org.jdownloader.controlling.packagizer;

import java.util.EventListener;

import jd.controlling.linkcrawler.CrawledLink;

public interface PackagizerControllerListener extends EventListener {

    void onPackagizerUpdate();

    void onPackagizerRunBeforeLinkcheck(CrawledLink link);

    void onPackagizerRunAfterLinkcheck(CrawledLink link);

}