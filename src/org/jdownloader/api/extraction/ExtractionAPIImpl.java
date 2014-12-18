package org.jdownloader.api.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl;
import org.jdownloader.api.extraction.ArchiveStatusStorable.ArchiveFileStatus;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ExtractionInterface;

public class ExtractionAPIImpl implements ExtractionAPI {

    public ExtractionAPIImpl() {
        RemoteAPIController.validateInterfaces(ExtractionAPI.class, ExtractionInterface.class);
    }

    @Override
    public void addArchivePassword(String password) {
        ExtractionExtension.getInstance().addPassword(password);
    }

    @Override
    public HashMap<String, Boolean> startExtractionNow(final long[] linkIds, final long[] packageIds) {
        SelectionInfo<FilePackage, DownloadLink> selection = DownloadsAPIV2Impl.getSelectionInfo(linkIds, packageIds);
        final HashMap<String, Boolean> ret = new HashMap<String, Boolean>();
        if (selection != null && !selection.isEmpty()) {
            final ExtractionExtension extension = ExtractionExtension.getInstance();
            List<Archive> archives = ArchiveValidator.validate(selection);
            if (archives != null && !archives.isEmpty() && extension != null) {
                for (Archive archive : archives) {
                    final String archiveId = archive.getFactory().getID();
                    try {
                        final DummyArchive da = extension.createDummyArchive(archive);
                        if (da.isComplete()) {
                            extension.addToQueue(archive, true);
                            ret.put(archiveId, true);
                        } else {
                            ret.put(archiveId, false);
                        }
                    } catch (CheckException e) {
                        ret.put(archiveId, false);
                    }
                }
            }
        }
        return ret;
    }

    public List<ArchiveStatusStorable> getArchiveInfo(final long[] linkIds, final long[] packageIds) {
        SelectionInfo<FilePackage, DownloadLink> selection = DownloadsAPIV2Impl.getSelectionInfo(linkIds, packageIds);
        final List<ArchiveStatusStorable> ret = new ArrayList<ArchiveStatusStorable>();
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null && selection != null && !selection.isEmpty()) {
            List<Archive> archives = ArchiveValidator.validate(selection);
            if (archives != null && !archives.isEmpty() && extension != null) {
                for (Archive archive : archives) {
                    final String archiveId = archive.getFactory().getID();
                    final String archiveName = extension.getArchiveName(archive.getFactory());
                    final HashMap<String, ArchiveFileStatus> extractionStates = new HashMap<String, ArchiveFileStatus>();
                    for (ArchiveFile file : archive.getArchiveFiles()) {
                        DummyArchiveFile da = new DummyArchiveFile(file);
                        if (da.isIncomplete()) {
                            if (da.isMissing()) {
                                extractionStates.put(file.getName(), ArchiveFileStatus.MISSING);
                            } else {
                                extractionStates.put(file.getName(), ArchiveFileStatus.INCOMPLETE);
                            }
                        } else {
                            extractionStates.put(file.getName(), ArchiveFileStatus.COMPLETE);
                        }
                    }
                    ArchiveStatusStorable archiveStatus = new ArchiveStatusStorable(archiveId, archiveName, extractionStates);
                    ret.add(archiveStatus);
                }
            }
        }
        return ret;
    }

    @Override
    public Boolean cancelExtraction(long archiveId) {
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        boolean ret = false;
        if (extension != null) {
            List<ExtractionController> jobs = extension.getJobQueue().getJobs();
            if (jobs != null && !jobs.isEmpty()) {
                for (ExtractionController controller : jobs) {
                    if (("" + archiveId).equals(controller.getArchiv().getFactory().getID())) {
                        return extension.cancel(controller);
                    }
                }
            }
        }
        return ret;
    }
}
