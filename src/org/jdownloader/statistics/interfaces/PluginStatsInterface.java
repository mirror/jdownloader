package org.jdownloader.statistics.interfaces;

import java.util.ArrayList;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.statistics.ErrorDetails;
import org.jdownloader.statistics.LogDetails;
import org.jdownloader.statistics.LogEntryWrapper;
import org.jdownloader.statistics.StatsManager.Response;
import org.jdownloader.statistics.TimeWrapper;

@ApiNamespace("plugins")
public interface PluginStatsInterface extends RemoteAPIInterface {

    Response push2(TimeWrapper sendTo, RemoteAPIRequest request) throws BadParameterException;

    void sendError(ErrorDetails error);

    void sendLog(LogDetails error);

    // old
    Response push(ArrayList<LogEntryWrapper> timeWrapper, RemoteAPIRequest request) throws BadParameterException;
}
