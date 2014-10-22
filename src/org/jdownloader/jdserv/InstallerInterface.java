package org.jdownloader.jdserv;

import java.util.HashMap;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("InstallerInterface")
public interface InstallerInterface extends RemoteAPIInterface {
    String getLatestInstallerUrl(String id, String version, String os, boolean win64);

    HashMap<String, String> get09to2Updater(String os, boolean is64bit, String osVersion) throws InternalApiException;
}
