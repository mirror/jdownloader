package org.jdownloader.api.extraction;

import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;

@ApiNamespace("extraction")
public interface ExtractionAPI extends RemoteAPIInterface {

    public void addArchivePassword(String password);

    public HashMap<String, Boolean> startExtractionNow(final long[] linkIds, final long[] packageIds);

    public List<ArchiveStatusStorable> getArchiveInfo(final long[] linkIds, final long[] packageIds);

    public List<ArchiveStatusStorable> getQueue();

    public Boolean cancelExtraction(long archiveId);

    public List<ArchiveSettingsAPIStorable> getArchiveSettings(String[] archiveIds) throws BadParameterException;

    public Boolean setArchiveSettings(String archiveId, ArchiveSettingsAPIStorable archiveSettings) throws BadParameterException;

}
