package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
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
    private final TypeRef<ArchiveSettings>         typeRef;
    private final LogSource                        logger;

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

            @Override
            public String toString() {
                return "ShutdownEvent: Save ArchiveController";
            }
        });
    }

    protected void save() {
        synchronized (map) {
            for (Entry<String, ArchiveSettings> e : map.entrySet()) {
                try {
                    if (e.getValue().needsSaving()) {
                        final File path = getPathByID(e.getKey());
                        logger.info("Save " + path);
                        IO.secureWrite(path, JSonStorage.serializeToJson(e.getValue()).getBytes("UTF-8"));
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

    public ArchiveSettings getArchiveSettings(final String id, final ArchiveFactory archiveFactory) {
        if (id != null) {
            synchronized (map) {
                final String internalID = Hash.getSHA256(id);
                ArchiveSettings ret = map.get(internalID);
                if (ret != null) {
                    return ret;
                }
                ret = createSettingsObject(internalID, archiveFactory != null && archiveFactory.isDeepExtraction());
                if (archiveFactory != null) {
                    final BooleanStatus defaultAuto = BooleanStatus.get(archiveFactory.getDefaultAutoExtract());
                    if (BooleanStatus.UNSET.equals(ret.getAutoExtract()) && !ret.getAutoExtract().equals(defaultAuto)) {
                        /* only set AutoExtract value when it is UNSET */
                        ret.setAutoExtract(defaultAuto);
                    }
                }
                map.put(internalID, ret);
                return ret;
            }
        }
        return null;
    }

    private ArchiveSettings createSettingsObject(final String id, final boolean isDeepExtract) {
        if (isDeepExtract) {
            final ArchiveSettings instance = new ArchiveSettings() {
                @Override
                public boolean needsSaving() {
                    return false;
                }
            };
            instance.assignController(this);
            return instance;
        } else {
            try {
                final File path = getPathByID(id);
                if (path.exists()) {
                    final ArchiveSettings instance = JSonStorage.restoreFromString(IO.readFileToString(path), typeRef);
                    instance.assignController(this);
                    return instance;
                }
            } catch (Throwable e) {
                logger.log(e);
            }
            final ArchiveSettings instance = new ArchiveSettings();
            instance.assignController(this);
            return instance;
        }
    }

    public void update(ArchiveSettings archiveSettings) {
        // we could start some kind of asynch saver delayer here.
    }

}
