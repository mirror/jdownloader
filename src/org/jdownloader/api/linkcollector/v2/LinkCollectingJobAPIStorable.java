package org.jdownloader.api.linkcollector.v2;

import jd.controlling.linkcollector.LinkCollectingJob;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.LinkCollectingJobStorable;

public class LinkCollectingJobAPIStorable extends LinkCollectingJobStorable implements Storable {
    public LinkCollectingJobAPIStorable(/* Storable */) {
    }

    public LinkCollectingJobAPIStorable(LinkCollectingJob job) {
        this.setId(job.getUniqueAlltimeID().getID());
    }
}
