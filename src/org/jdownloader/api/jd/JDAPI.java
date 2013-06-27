package org.jdownloader.api.jd;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("jd")
public interface JDAPI extends RemoteAPIInterface {

    public void doSomethingCool();

    public long uptime();

    public long version();

    public Integer getCoreRevision();

    public boolean refreshPlugins();

    public int sum(int a, int b);
}
