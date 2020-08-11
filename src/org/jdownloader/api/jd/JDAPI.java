package org.jdownloader.api.jd;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@Deprecated
@ApiNamespace("jd")
public interface JDAPI extends RemoteAPIInterface {
    public long uptime();

    public long version();

    public Integer getCoreRevision();

    public boolean refreshPlugins();
}
