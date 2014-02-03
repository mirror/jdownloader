package org.jdownloader.statistics.interfaces;

import java.util.ArrayList;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSessionRequired;
import org.jdownloader.statistics.LogEntryWrapper;

@ApiNamespace("plugins")
@ApiSessionRequired()
public interface PluginStatsInterface extends RemoteAPIInterface {
public static enum Response{
    OK,
}

    Response push(ArrayList<LogEntryWrapper> sendTo, RemoteAPIRequest request);

}
