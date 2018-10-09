package org.jdownloader.controlling.packagizer;

import java.util.EventListener;

import jd.controlling.linkcrawler.CrawledLink;

public interface PackagizerControllerListener extends EventListener {
    public static enum STATE {
        BEFORE,
        AFTER
    }

    void onPackagizerUpdate();

    void onPackagizerRunBeforeLinkcheck(CrawledLink link, STATE state);

    void onPackagizerRunAfterLinkcheck(CrawledLink link, STATE state);
}