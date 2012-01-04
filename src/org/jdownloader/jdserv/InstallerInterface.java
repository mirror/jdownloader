package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;

public interface InstallerInterface extends RemoteCallInterface {
    String getLatestInstallerUrl(String id, String version, String os, boolean win64);

}
