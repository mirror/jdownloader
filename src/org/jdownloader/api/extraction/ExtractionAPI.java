package org.jdownloader.api.extraction;

import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("extraction")
public interface ExtractionAPI extends RemoteAPIInterface {

    public void addArchivePassword(String password);

    public HashMap<String, Boolean> startExtractionNow(final long[] linkIds, final long[] packageIds);

    public List<ArchiveStatusStorable> getArchiveInfo(final long[] linkIds, final long[] packageIds);

    public Boolean cancelExtraction(long archiveId);

}
