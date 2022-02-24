package org.jdownloader.api.downloads;

import org.appwork.storage.Storable;
import org.appwork.testframework.IgnoreInAWTest;
import org.jdownloader.myjdownloader.client.json.DownloadListDiff;

@IgnoreInAWTest
public class DownloadListDiffStorable extends DownloadListDiff implements Storable {
    public DownloadListDiffStorable(/* Storable */) {
    }
}
