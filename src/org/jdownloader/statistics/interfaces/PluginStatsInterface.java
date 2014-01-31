package org.jdownloader.statistics.interfaces;

import java.util.ArrayList;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.jdownloader.statistics.LogEntryWrapper;

public interface PluginStatsInterface extends RemoteAPIInterface {

    void push(ArrayList<LogEntryWrapper> sendTo);

}
