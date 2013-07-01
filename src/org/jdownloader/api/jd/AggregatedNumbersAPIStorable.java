package org.jdownloader.api.jd;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.AggregatedNumbers;

public class AggregatedNumbersAPIStorable implements Storable {

    public AggregatedNumbersAPIStorable(/* Storable */) {

    }

    private AggregatedNumbers aggregated;

    public AggregatedNumbersAPIStorable(AggregatedNumbers aggregated) {
        // SelectionInfo<FilePackage, DownloadLink> sel = new SelectionInfo<FilePackage, DownloadLink>(dc.getAllDownloadLinks());
        this.aggregated = aggregated;
    }

    public Integer getPackageCount() {
        return aggregated.getPackageCount();
    }

    public Integer getLinksCount() {
        return aggregated.getLinkCount();
    }

    public Long getTotalBytes() {
        return aggregated.getTotalBytes();
    }

    public Long getDownloadSpeed() {
        return aggregated.getDownloadSpeed();
    }

    public Long getLoadedBytes() {
        return aggregated.getLoadedBytes();
    }

    public Long getETA() {
        return aggregated.getEta();
    }

    public Long getRunning() {
        return aggregated.getRunning();
    }

    public Long getConnections() {
        return aggregated.getConnections();
    }
}
