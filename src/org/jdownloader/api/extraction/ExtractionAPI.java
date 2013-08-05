package org.jdownloader.api.extraction;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("extraction")
public interface ExtractionAPI extends RemoteAPIInterface {
    public void addArchivePassword(String password);
}
