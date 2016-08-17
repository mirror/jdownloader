package org.jdownloader.api.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.utils.StringUtils;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.extraction.ArchiveStatusStorable.ArchiveFileStatus;
import org.jdownloader.api.extraction.ArchiveStatusStorable.ControllerStatus;
import org.jdownloader.api.utils.PackageControllerUtils;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveController;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ArchiveSettings;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ExtractionInterface;

public class ExtractionAPIImpl implements ExtractionAPI {
    private final PackageControllerUtils<FilePackage, DownloadLink>   packageControllerDownloadList;
    private final PackageControllerUtils<CrawledPackage, CrawledLink> packageControllerLinkCollector;

    public ExtractionAPIImpl() {
        RemoteAPIController.validateInterfaces(ExtractionAPI.class, ExtractionInterface.class);
        packageControllerDownloadList = new PackageControllerUtils<FilePackage, DownloadLink>(DownloadController.getInstance());
        packageControllerLinkCollector = new PackageControllerUtils<CrawledPackage, CrawledLink>(LinkCollector.getInstance());
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
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null) {
            final List<Archive> archives = getArchives(linkIds, packageIds);
            if (archives.size() > 0) {
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

    private List<Archive> getArchives(final long[] linkIds, final long[] packageIds) {
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        final ArrayList<Archive> ret = new ArrayList<Archive>();
        if (extension != null) {
            final SelectionInfo<FilePackage, DownloadLink> downloadListSelection = packageControllerDownloadList.getSelectionInfo(linkIds, packageIds);
            if (downloadListSelection != null && !downloadListSelection.isEmpty()) {
                final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(downloadListSelection.getChildren());
                if (archives != null) {
                    ret.addAll(archives);
                }
            }
            final SelectionInfo<CrawledPackage, CrawledLink> linkCollectorSelection = packageControllerLinkCollector.getSelectionInfo(linkIds, packageIds);
            if (linkCollectorSelection != null && !linkCollectorSelection.isEmpty()) {
                final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(linkCollectorSelection.getChildren());
                if (archives != null) {
                    ret.addAll(archives);
                }
            }
        }
        return ret;
    }

    public List<ArchiveStatusStorable> getArchiveInfo(final long[] linkIds, final long[] packageIds) {
        final List<ArchiveStatusStorable> ret = new ArrayList<ArchiveStatusStorable>();
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null) {
            final List<Archive> archives = getArchives(linkIds, packageIds);
            if (archives.size() > 0) {
                final List<ExtractionController> jobs = extension.getJobQueue().getJobs();
                for (final Archive archive : archives) {
                    final ArchiveStatusStorable archiveStatus = new ArchiveStatusStorable(archive, getArchiveFileStatusMap(archive));
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
    public boolean cancelExtraction(final long controllerID) {
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

    @Override
    public List<ArchiveSettingsAPIStorable> getArchiveSettings(String[] archiveIds) throws BadParameterException {
        List<ArchiveSettingsAPIStorable> ret = new ArrayList<ArchiveSettingsAPIStorable>();
        final ExtractionExtension extension = ArchiveValidator.EXTENSION;
        if (extension != null && archiveIds != null) {
            for (String archiveId : archiveIds) {
                ArchiveSettings settings = ArchiveController.getInstance().getArchiveSettings(archiveId, null);
                ret.add(createStorable(archiveId, settings));
            }
        }
        return ret;
    }

    @Override
    public boolean setArchiveSettings(final String archiveId, final ArchiveSettingsAPIStorable remoteSettings) throws BadParameterException {
        if (remoteSettings == null) {
            throw new BadParameterException("settings == null");
        }
        if (StringUtils.isEmpty(archiveId)) {
            throw new BadParameterException("invalid archive id");
        }
        final ArchiveSettings localSettings = ArchiveController.getInstance().getArchiveSettings(archiveId, null);
        if (localSettings != null) {
            if (remoteSettings.getAutoExtract() != null) {
                localSettings.setAutoExtract(BooleanStatus.convert(remoteSettings.getAutoExtract()));
            }
            if (remoteSettings.getRemoveDownloadLinksAfterExtraction() != null) {
                localSettings.setRemoveDownloadLinksAfterExtraction(BooleanStatus.convert(remoteSettings.getRemoveDownloadLinksAfterExtraction()));
            }
            if (remoteSettings.getRemoveFilesAfterExtraction() != null) {
                localSettings.setRemoveFilesAfterExtraction(BooleanStatus.convert(remoteSettings.getRemoveFilesAfterExtraction()));
            }
            if (!StringUtils.isEmpty(remoteSettings.getExtractPath())) {
                localSettings.setExtractPath(remoteSettings.getExtractPath());
            }
            if (!StringUtils.isEmpty(remoteSettings.getFinalPassword())) {
                localSettings.setFinalPassword(remoteSettings.getFinalPassword());
            }
            if (remoteSettings.getPasswords() != null) {
                localSettings.setPasswords(remoteSettings.getPasswords());
            }
            return true;
        } else {
            throw new BadParameterException("unknown archive id");
        }
    }

    private ArchiveSettingsAPIStorable createStorable(final String archiveId, final ArchiveSettings settings) {
        ArchiveSettingsAPIStorable storable = new ArchiveSettingsAPIStorable();
        storable.setArchiveId(archiveId);
        storable.setAutoExtract(BooleanStatus.convert(settings.getAutoExtract()));
        storable.setExtractPath(settings.getExtractPath());
        storable.setFinalPassword(settings.getFinalPassword());
        storable.setPasswords(settings.getPasswords());
        storable.setRemoveDownloadLinksAfterExtraction(BooleanStatus.convert(settings.getRemoveDownloadLinksAfterExtraction()));
        storable.setRemoveFilesAfterExtraction(BooleanStatus.convert(settings.getRemoveFilesAfterExtraction()));
        return storable;
    }
}
