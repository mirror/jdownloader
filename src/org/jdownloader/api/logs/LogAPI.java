package org.jdownloader.api.logs;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.LogInterface.NAMESPACE)
public interface LogAPI extends RemoteAPIInterface {
    public List<LogFolderStorable> getAvailableLogs();

    @APIParameterNames({ "logFolders" })
    public String sendLogFile(LogFolderStorable[] logFolders) throws BadParameterException;
}
