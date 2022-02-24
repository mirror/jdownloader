package org.jdownloader.api.linkcollector;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;

import jd.controlling.linkcrawler.CrawledLink;

public class CrawledLinkAPIStorable implements Storable {
    private CrawledLink                                       link;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap;

    public CrawledLinkAPIStorable(/* Storable */) {
    }

    public CrawledLinkAPIStorable(CrawledLink link) {
        this.link = link;
    }

    @StorableValidatorIgnoresMissingSetter
    public Long getUuid() {
        CrawledLink llink = link;
        if (llink != null) {
            return llink.getUniqueID().getID();
        }
        return 0l;
    }

    // to be compatbile with older API version
    @StorableValidatorIgnoresMissingSetter
    public Long getUniqueID() {
        CrawledLink llink = link;
        if (llink != null) {
            return llink.getUniqueID().getID();
        }
        return 0l;
    }

    public void setName(String name) {
        this.link.setName(name);
    }

    public String getName() {
        CrawledLink llink = link;
        if (llink != null) {
            return llink.getName();
        }
        return null;
    }

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }
}
