package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("InstallerInterface")
public interface InstallerInterface extends RemoteAPIInterface {
    String getLatestInstallerUrl(String id, String version, String os, boolean win64);

}
