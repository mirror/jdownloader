package org.jdownloader.extensions.extraction;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
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
            }

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                save();
            }
        });
    }

    protected void save() {
        synchronized (map) {

            for (Entry<String, ArchiveSettings> e : map.entrySet()) {
                try {
                    File path = getPathByID(e.getKey());
                    logger.info("Save " + path);
                    IO.secureWrite(path, JSonStorage.toString(e.getValue()).getBytes("UTF-8"));
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
        ArchiveSettings ret = map.get(id);
        if (ret != null) return ret;
        synchronized (this) {
            ret = map.get(id);
            if (ret != null) return ret;
            ret = createSettingsObject(id);
            if (ret.getAutoExtract() == null || BooleanStatus.UNSET.equals(ret.getAutoExtract())) {
                /* only set AutoExtract value when it is UNSET */
                ret.setAutoExtract(defaultAutoExtract);
            }
            map.put(id, ret);
        }
        return ret;
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
}
