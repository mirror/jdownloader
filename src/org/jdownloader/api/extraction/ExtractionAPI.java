package org.jdownloader.api.extraction;

import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;

@ApiNamespace("extraction")
public interface ExtractionAPI extends RemoteAPIInterface {
    @APIParameterNames({ "password" })
    public void addArchivePassword(String password);

    @APIParameterNames({ "linkIds", "packageIds" })
    public HashMap<String, Boolean> startExtractionNow(final long[] linkIds, final long[] packageIds);

    @APIParameterNames({ "linkIds", "packageIds" })
    public List<ArchiveStatusStorable> getArchiveInfo(final long[] linkIds, final long[] packageIds);

    public List<ArchiveStatusStorable> getQueue();

    @APIParameterNames({ "controllerID" })
    public boolean cancelExtraction(long controllerID);

    @APIParameterNames({ "archiveIds" })
    public List<ArchiveSettingsAPIStorable> getArchiveSettings(String[] archiveIds) throws BadParameterException;

    @APIParameterNames({ "archiveId", "archiveSettings" })
    public boolean setArchiveSettings(String archiveId, ArchiveSettingsAPIStorable archiveSettings) throws BadParameterException;
}
