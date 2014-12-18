package org.jdownloader.api.downloads.v2;

import org.appwork.storage.Storable;
import org.appwork.storage.jackson.JacksonMapper;
import org.jdownloader.myjdownloader.client.bindings.downloadlist.DownloadPackageQuery;

public class PackageQueryStorable extends DownloadPackageQuery implements Storable {
    public static void main(String[] args) {

        System.out.println(DownloadPackageQuery.class.getSimpleName() + "= ");
        System.out.println(new JacksonMapper().objectToString(new DownloadPackageQuery()));
    }

    public static final PackageQueryStorable FULL = new PackageQueryStorable();
    static {
        FULL.setBytesLoaded(true);
        FULL.setBytesTotal(true);
        FULL.setComment(true);
        FULL.setEnabled(true);
        FULL.setEta(true);

        FULL.setFinished(true);

        FULL.setRunning(true);

        FULL.setSpeed(true);
        FULL.setStatus(true);
        FULL.setChildCount(true);
        FULL.setHosts(true);
        FULL.setSaveTo(true);
    }

    public PackageQueryStorable(/* Storable */) {

    }

}