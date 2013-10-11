package org.jdownloader.api.linkcrawler;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("linkcrawler")
public interface LinkCrawlerAPI extends RemoteAPIInterface {
    boolean isCrawling();
}
