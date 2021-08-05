package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.logging.LogController;

public class ArchiveController {
    private static final ArchiveController INSTANCE = new ArchiveController();

    /**
     * get the only existing instance of ArchiveController. This is a singleton
     *
     * @return
     */
    public static ArchiveController getInstance() {
        return ArchiveController.INSTANCE;
    }

    private final HashMap<String, ArchiveSettings> map;
    private final LogSource                        logger;

    /**
     * Create a new instance of ArchiveController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ArchiveController() {
        map = new HashMap<String, ArchiveSettings>();
        logger = LogController.getInstance().getLogger(ArchiveController.class.getName());
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            {
                setHookPriority(Integer.MIN_VALUE - 100);
                setMaxDuration(3 * 60000);
            }

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                save();
            }

            @Override
            public String toString() {
                return "ShutdownEvent: Save ArchiveController";
            }
        });
    }

    protected void save() {
        synchronized (map) {
            for (final Entry<String, ArchiveSettings> e : map.entrySet()) {
                try {
                    final File path = getPathByID(e.getKey());
                    final ArchiveSettings settings = e.getValue();
                    Boolean exists = settings._exists();
                    if (exists == null) {
                        // TODO: check if archive still exists in list
                    }
                    if (Boolean.FALSE.equals(exists)) {
                        if (path.isFile()) {
                            logger.info("Archive (" + settings._getArchiveID() + "/" + path + ") no longer exists: removed:" + path.delete());
                        } else {
                            logger.info("Archive (" + settings._getArchiveID() + "/" + path + ") no longer exists");
                        }
                    } else if (settings._needsSaving()) {
                        logger.info("Archive (" + settings._getArchiveID() + "/" + path + ") " + (exists == null ? "maybe" : "still") + " exits and changes must be saved");
                        IO.secureWrite(path, JSonStorage.serializeToJson(e.getValue()).getBytes("UTF-8"));
                    } else {
                        logger.info("Archive (" + settings._getArchiveID() + "/" + path + ") " + (exists == null ? "maybe" : "still") + " exits but no changes");
                    }
                } catch (Throwable e1) {
                    logger.log(e1);
                }
            }
        }
    }

    protected File getPathByID(String internalID) {
        return Application.getResource("cfg/archives/v2_" + internalID + ".json");
    }

    public ArchiveSettings getArchiveSettings(final String archiveID, final ArchiveFactory archiveFactory) {
        return getArchiveSettings(archiveID, null, archiveFactory);
    }

    public ArchiveSettings getArchiveSettings(final String archiveID, final Archive archive, final ArchiveFactory archiveFactory) {
        if (archiveID != null) {
            synchronized (map) {
                final String internalID = Hash.getSHA256(archiveID);
                ArchiveSettings ret = map.get(internalID);
                if (ret != null) {
                    return ret;
                }
                ret = createSettingsObject(archiveID, internalID, archiveFactory != null && archiveFactory.isDeepExtraction());
                if (archiveFactory != null) {
                    final BooleanStatus defaultAuto = BooleanStatus.get(archiveFactory.getDefaultAutoExtract());
                    if (BooleanStatus.UNSET.equals(ret.getAutoExtract()) && !ret.getAutoExtract().equals(defaultAuto)) {
                        /* only set AutoExtract value when it is UNSET */
                        ret.setAutoExtract(defaultAuto);
                    }
                }
                if (archive != null) {
                    // make sure assignedLinks field is initialized
                    ret._getAssignedLinks().size();
                    for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                        if (archiveFile instanceof DownloadLinkArchiveFile) {
                            for (final DownloadLink downloadLink : ((DownloadLinkArchiveFile) archiveFile).getDownloadLinks()) {
                                ret._getAssignedLinks().put(downloadLink, null);
                            }
                        } else if (archiveFile instanceof CrawledLinkArchiveFile) {
                            for (final CrawledLink crawledLink : ((CrawledLinkArchiveFile) archiveFile).getLinks()) {
                                final DownloadLink downloadLink = crawledLink.getDownloadLink();
                                if (downloadLink != null) {
                                    ret._getAssignedLinks().put(downloadLink, null);
                                }
                                ret._getAssignedLinks().put(crawledLink, null);
                            }
                        }
                    }
                }
                map.put(internalID, ret);
                return ret;
            }
        }
        return null;
    }

    private ArchiveSettings createSettingsObject(final String archiveID, final String internalID, final boolean isDeepExtract) {
        if (isDeepExtract) {
            final ArchiveSettings instance = new ArchiveSettings() {
                @Override
                public boolean _needsSaving() {
                    return false;
                }
            };
            instance.assignController(this, archiveID, internalID);
            return instance;
        } else {
            try {
                final File path = getPathByID(internalID);
                if (path.exists()) {
                    final ArchiveSettings instance = JSonStorage.restoreFromString(IO.readFileToString(path), ArchiveSettings.TYPE_REF);
                    instance.assignController(this, archiveID, internalID);
                    return instance;
                }
            } catch (Throwable e) {
                logger.log(e);
            }
            final ArchiveSettings instance = new ArchiveSettings();
            instance.assignController(this, archiveID, internalID);
            return instance;
        }
    }

    public void update(ArchiveSettings archiveSettings) {
        // we could start some kind of asynch saver delayer here.
    }
}
