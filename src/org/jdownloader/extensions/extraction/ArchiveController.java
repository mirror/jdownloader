package org.jdownloader.extensions.extraction;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkProperty;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.logging.LogController;

public class ArchiveController implements LinkCollectorListener {
    private static final ArchiveController INSTANCE = new ArchiveController();

    /**
     * get the only existing instance of ArchiveController. This is a singleton
     * 
     * @return
     */
    public static ArchiveController getInstance() {
        return ArchiveController.INSTANCE;
    }

    private HashMap<String, ArchiveSettings> map;
    private TypeRef<ArchiveSettings>         typeRef;
    private LogSource                        logger;

    /**
     * Create a new instance of ArchiveController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ArchiveController() {
        map = new HashMap<String, ArchiveSettings>();
        typeRef = new TypeRef<ArchiveSettings>() {

        };
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
        });
        // DownloadController.getInstance().addListener(this);
        LinkCollector.getInstance().getEventsender().addListener(this);
    }

    protected void save() {
        synchronized (map) {
            for (Entry<String, ArchiveSettings> e : map.entrySet()) {
                try {
                    if (e.getValue().needsSaving()) {
                        File path = getPathByID(e.getKey());
                        logger.info("Save " + path);
                        IO.secureWrite(path, JSonStorage.serializeToJson(e.getValue()).getBytes("UTF-8"));
                    }
                } catch (Exception e1) {
                    logger.log(e1);
                }
            }
        }
    }

    protected File getPathByID(String id) {
        return Application.getResource("cfg/archives/v2_" + id + ".json");
    }

    public ArchiveSettings getArchiveSettings(ArchiveFactory archiveFactory) {
        return getArchiveSettings(archiveFactory.getID(), archiveFactory.getDefaultAutoExtract());
    }

    private ArchiveSettings getArchiveSettings(String id, BooleanStatus defaultAutoExtract) {
        synchronized (this) {
            ArchiveSettings ret = map.get(id);
            if (ret != null) return ret;
            ret = createSettingsObject(id);
            if (ret.getAutoExtract() == null || BooleanStatus.UNSET.equals(ret.getAutoExtract())) {
                /* only set AutoExtract value when it is UNSET */
                ret.setAutoExtract(defaultAutoExtract);
            }
            map.put(id, ret);
            return ret;
        }
    }

    private ArchiveSettings createSettingsObject(String id) {
        try {
            File path = getPathByID(id);
            if (path.exists()) {
                ArchiveSettings instance = JSonStorage.restoreFromString(IO.readFileToString(path), typeRef);
                instance.assignController(this);
                return instance;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArchiveSettings instance = new ArchiveSettings();
        instance.assignController(this);
        return instance;
    }

    public void update(ArchiveSettings archiveSettings) {
        // we could start some kind of asynch saver delayer here.

    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {

    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {

    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {

    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        try {
            if (event.getType() == LinkCollectorEvent.TYPE.REFRESH_DATA) {
                if (event.getParameters().length == 2) {
                    if (event.getParameter(1) instanceof CrawledLinkProperty) {
                        CrawledLinkProperty property = (CrawledLinkProperty) event.getParameter(1);
                        if (event.getParameter(0) instanceof CrawledLink) {
                            CrawledLink cLink = (CrawledLink) event.getParameter(0);
                            if (cLink.getArchiveID() != null) {
                                switch (property.getProperty()) {
                                case NAME:
                                    cloneSettings(cLink);
                                    break;
                                }
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            // god knows what can happen in this CAST Armageddon
            logger.log(e);
        }
    }

    private void cloneSettings(CrawledLink cLink) {
        String newID = DownloadLinkArchiveFactory.createUniqueAlltimeID() + "";
        String oldID = cLink.getArchiveID();
        File path = getPathByID(oldID);
        File newPath = getPathByID(newID);
        // if the oldfile does not exist, we do not have to clone
        boolean doClone = path.exists();
        ArchiveSettings clonedData = null;
        // clone only if we already have settings
        if (!doClone) {
            synchronized (map) {
                clonedData = map.get(oldID);
                // if we alread have data in cache, we have to clone
                doClone = clonedData != null;
            }
        }

        if (doClone) {
            boolean weNeedToCopyFile = false;
            if (clonedData != null) {

                ArchiveSettings clone = clonedData.createClone();
                if (!clone.needsSaving()) {
                    weNeedToCopyFile = true;
                }
                synchronized (map) {
                    map.put(newID, clone);
                }

            }
            if (weNeedToCopyFile && path.exists()) {
                // we need to copy the file. clone has no write flag, and the oldfile exists;
                FileCreationManager.getInstance().delete(newPath);
                try {
                    IO.copyFile(path, newPath);
                } catch (IOException e) {
                    logger.log(e);
                }
            }
            cLink.setArchiveID(newID);

        }
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {

    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {

    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {

    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {

    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {

    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
    }
}
