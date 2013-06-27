package org.jdownloader.api.linkcollector;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.Storable;

public class CrawledLinkAPIStorable implements Storable {

    private CrawledLink      link;
    private QueryResponseMap infoMap;

    public CrawledLinkAPIStorable(/* Storable */) {

    }

    public CrawledLinkAPIStorable(CrawledLink link) {
        this.link = link;
    }

    public Long getUuid() {
        return link.getUniqueID().getID();
    }
    
    //to be compatbile with older API version
    public Long getUniqueID() {
        return link.getUniqueID().getID();
    }

    public void setName(String name) {
        this.link.setName(name);
    }

    public String getName() {
        return this.link.getName();
    }

    public QueryResponseMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(QueryResponseMap infoMap) {
        this.infoMap = infoMap;
    }
}
