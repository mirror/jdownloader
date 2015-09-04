package org.jdownloader.api.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.extraction.ArchiveStatusStorable.ArchiveFileStatus;
import org.jdownloader.api.extraction.ArchiveStatusStorable.ControllerStatus;
import org.jdownloader.api.utils.PackageControllerUtils;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ExtractionInterface;

public class ExtractionAPIImpl implements ExtractionAPI {

    private final PackageControllerUtils<FilePackage, DownloadLink> packageControllerUtils;

    public ExtractionAPIImpl() {
        RemoteAPIController.validateInterfaces(ExtractionAPI.class, ExtractionInterface.class);
        packageControllerUtils = new PackageControllerUtils<FilePackage, DownloadLink>(DownloadController.getInstance());
    }

    @Override
    public void addArchivePassword(String password) {
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null) {
            extension.addPassword(password);
        }
    }

    @Override
    public HashMap<String, Boolean> startExtractionNow(final long[] linkIds, final long[] packageIds) {
        final HashMap<String, Boolean> ret = new HashMap<String, Boolean>();
        final ExtractionExtension extension = ExtractionExtension.getInstance();
        if (extension != null) {
            final SelectionInfo<FilePackage, DownloadLink> selection = packageControllerUtils.getSelectionInfo(linkIds, packageIds);
            if (selection != null && !selection.isEmpty()) {
                final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(selection.getChildren());
                for (final Archive archive : archives) {
                    try {
                        final DummyArchive da = extension.createDummyArchive(archive);
                        if (da.isComplete()) {
                            final ExtractionController controller = extension.addToQueue(archive, true);
                            ret.put(controller.getUniqueID().toString(), true);
                        }
                    } catch (CheckException e) {
                    }
                }
            }
        }
        return ret;
    }

    public List<ArchiveStatusStorable> getArchiveInfo(final long[] linkIds, final long[] packageIds) {
        final List<ArchiveStatusStorable> ret = new ArrayList<ArchiveStatusStorable>();
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null) {
            final SelectionInfo<FilePackage, DownloadLink> selection = packageControllerUtils.getSelectionInfo(linkIds, packageIds);
            if (selection != null && !selection.isEmpty()) {
                final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(selection.getChildren());
                if (archives.size() > 0) {
                    final List<ExtractionController> jobs = extension.getJobQueue().getJobs();
                    for (final Archive archive : archives) {
                        final ArchiveStatusStorable archiveStatus = new ArchiveStatusStorable(archive.getArchiveID(), archive.getName(), getArchiveFileStatusMap(archive));
                        for (final ExtractionController controller : jobs) {
                            if (StringUtils.equals(controller.getArchive().getArchiveID(), archive.getArchiveID())) {
                                archiveStatus.setControllerId(controller.getUniqueID().getID());
                                if (controller.gotStarted()) {
                                    archiveStatus.setControllerStatus(ControllerStatus.RUNNING);
                                } else {
                                    archiveStatus.setControllerStatus(ControllerStatus.QUEUED);
                                }
                                break;
                            }
                        }
                        ret.add(archiveStatus);
                    }
                }
            }
        }
        return ret;
    }

    private HashMap<String, ArchiveFileStatus> getArchiveFileStatusMap(final Archive archive) {
        final HashMap<String, ArchiveFileStatus> extractionStates = new HashMap<String, ArchiveFileStatus>();
        for (final ArchiveFile file : archive.getArchiveFiles()) {
            if (file instanceof MissingArchiveFile) {
                extractionStates.put(file.getName(), ArchiveFileStatus.MISSING);
            } else {
                if (Boolean.TRUE.equals(file.isComplete())) {
                    extractionStates.put(file.getName(), ArchiveFileStatus.COMPLETE);
                } else {
                    extractionStates.put(file.getName(), ArchiveFileStatus.INCOMPLETE);
                }
            }
        }
        return extractionStates;
    }

    @Override
    public Boolean cancelExtraction(final long controllerID) {
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null) {
            final List<ExtractionController> jobs = extension.getJobQueue().getJobs();
            if (jobs != null) {
                for (final ExtractionController controller : jobs) {
                    if (controller.getUniqueID().getID() == controllerID) {
                        return extension.cancel(controller);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<ArchiveStatusStorable> getQueue() {
        final List<ArchiveStatusStorable> ret = new ArrayList<ArchiveStatusStorable>();
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null) {
            final List<ExtractionController> jobs = extension.getJobQueue().getJobs();
            for (final ExtractionController controller : jobs) {
                final Archive archive = controller.getArchive();
                final ArchiveStatusStorable archiveStatus = new ArchiveStatusStorable(archive.getArchiveID(), archive.getName(), getArchiveFileStatusMap(archive));
                archiveStatus.setControllerId(controller.getUniqueID().getID());
                if (controller.gotStarted()) {
                    archiveStatus.setControllerStatus(ControllerStatus.RUNNING);
                } else {
                    archiveStatus.setControllerStatus(ControllerStatus.QUEUED);
                }
                ret.add(archiveStatus);
            }
        }
        return ret;
    }
}
