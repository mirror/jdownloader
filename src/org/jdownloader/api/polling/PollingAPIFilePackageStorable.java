package org.jdownloader.api.polling;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;

import org.appwork.storage.Storable;

public class PollingAPIFilePackageStorable implements Storable {

    private FilePackage                          fp;
    private Long                                 speed;
    private List<PollingAPIDownloadLinkStorable> downloadLinks = new ArrayList<PollingAPIDownloadLinkStorable>();
    private FilePackageView                      packageView;

    public PollingAPIFilePackageStorable(/* Storable */) {
        this.fp = null;
    }

    public PollingAPIFilePackageStorable(FilePackage link) {
        this.fp = link;
        packageView = new FilePackageView(fp);
        packageView.update();
    }

    public Long getUUID() {
        return fp.getUniqueID().getID();
    }

    public Long getDone() {
        return packageView.getDone();
    }

    public Boolean getFinished() {
        return packageView.isFinished();
    }

    public Long getSize() {
        return packageView.getSize();
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public Long getSpeed() {
        return speed;
    }

    public Long getETA() {
        return packageView.getETA();
    }

    public List<PollingAPIDownloadLinkStorable> getLinks() {
        return downloadLinks;
    }

    public void setLinks(List<PollingAPIDownloadLinkStorable> downloadLinks) {
        this.downloadLinks = downloadLinks;
    }
}
