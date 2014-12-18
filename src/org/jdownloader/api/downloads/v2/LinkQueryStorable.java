package org.jdownloader.api.downloads.v2;

import org.appwork.storage.Storable;
import org.appwork.storage.jackson.JacksonMapper;
import org.jdownloader.myjdownloader.client.bindings.downloadlist.DownloadLinkQuery;

public class LinkQueryStorable extends DownloadLinkQuery implements Storable {
    public static void main(String[] args) {

        System.out.println(LinkQueryStorable.class.getSimpleName() + "= ");
        System.out.println(new JacksonMapper().objectToString(new LinkQueryStorable()));
    }

    public static final LinkQueryStorable FULL = new LinkQueryStorable();
    static {
        FULL.setBytesLoaded(true);
        FULL.setBytesTotal(true);
        FULL.setComment(true);
        FULL.setEnabled(true);
        FULL.setEta(true);
        FULL.setExtractionStatus(true);
        FULL.setFinished(true);
        FULL.setHost(true);
        FULL.setPriority(true);
        FULL.setRunning(true);
        FULL.setSkipped(true);
        FULL.setSpeed(true);
        FULL.setStatus(true);
        FULL.setUrl(true);
        ;

    }

    public LinkQueryStorable() {
        super(/* Storable */);
    }

}