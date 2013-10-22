package org.jdownloader.settings.advanced;

import java.util.ArrayList;
import java.util.HashMap;

import jd.controlling.linkchecker.LinkCheckerConfig;
import jd.controlling.linkcrawler.LinkCrawlerConfig;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.synthetica.SyntheticaSettings;
import org.appwork.utils.logging2.LogConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.shortcuts.ShortcutSettings;
import org.jdownloader.jdserv.stats.StatsManagerConfig;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.AccountSettings;
import org.jdownloader.settings.RtmpdumpSettings;
import org.jdownloader.settings.SoundSettings;
import org.jdownloader.settings.staticreferences.CFG_API;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR;
import org.jdownloader.settings.staticreferences.CFG_LINKFILTER;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.settings.staticreferences.CFG_MYJD;
import org.jdownloader.settings.staticreferences.CFG_PACKAGIZER;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.updatev2.InternetConnectionSettings;
import org.jdownloader.updatev2.UpdateSettings;
import org.jdownloader.updatev2.gui.LAFOptions;

public class AdvancedConfigManager {
    private static final AdvancedConfigManager INSTANCE = new AdvancedConfigManager();

    public static AdvancedConfigManager getInstance() {
        return AdvancedConfigManager.INSTANCE;
    }

    private java.util.List<AdvancedConfigEntry> configInterfaces;
    private AdvancedConfigEventSender           eventSender;
    private LogSource                           logger;

    private AdvancedConfigManager() {
        logger = LogController.getInstance().getLogger(AdvancedConfigManager.class.getName());
        configInterfaces = new ArrayList<AdvancedConfigEntry>();
        eventSender = new AdvancedConfigEventSender();
        // REFERENCE via static CFG_* classes if possible. this way, we get error messages if there are error in the static refs
        this.register(CFG_GENERAL.CFG);
        register(CFG_LINKFILTER.CFG);
        register(JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class));
        register(CFG_MYJD.CFG);
        register(JsonConfig.create(AccountSettings.class));
        register(CFG_GUI.CFG);

        register(JsonConfig.create(LinkCheckerConfig.class));
        register(JsonConfig.create(LinkCrawlerConfig.class));
        register(CFG_LINKGRABBER.CFG);
        register(CFG_LINKCOLLECTOR.CFG);
        register(CFG_RECONNECT.CFG);
        register(CFG_API.CFG);
        register(CFG_PACKAGIZER.CFG);

        register(JsonConfig.create(StatsManagerConfig.class));
        register(JsonConfig.create(LogConfig.class));
        register(JsonConfig.create(ShortcutSettings.class));
        register(JsonConfig.create(RtmpdumpSettings.class));
        register(JsonConfig.create(UpdateSettings.class));
        register(JsonConfig.create(SyntheticaSettings.class));
        register(JsonConfig.create(SoundSettings.class));
        register(CFG_BUBBLE.CFG);
        register(CFG_CAPTCHA.CFG);
        register(CFG_SILENTMODE.CFG);
        try {
            register(LAFOptions.getInstance().getCfg());
        } catch (Exception e) {
            // we need to take care that LookAndFeelController has been initialized before we init the advancedconfigManager!
            logger.log(e);
        }
    }

    public AdvancedConfigEventSender getEventSender() {
        return eventSender;
    }

    public void register(ConfigInterface cf) {
        logger.info("Register " + cf._getStorageHandler().getConfigInterface());
        HashMap<KeyHandler, Boolean> map = new HashMap<KeyHandler, Boolean>();

        for (KeyHandler m : cf._getStorageHandler().getMap().values()) {

            if (map.containsKey(m)) continue;

            if (m.getAnnotation(AboutConfig.class) != null) {
                if (m.getSetter() == null) {
                    throw new RuntimeException("Setter for " + m.getGetter().getMethod() + " missing");
                } else if (m.getGetter() == null) {
                    throw new RuntimeException("Getter for " + m.getSetter().getMethod() + " missing");
                } else {
                    synchronized (configInterfaces) {
                        configInterfaces.add(new AdvancedConfigEntry(cf, m));
                    }
                    map.put(m, true);
                }
            }

        }

        eventSender.fireEvent(new AdvancedConfigEvent(this, AdvancedConfigEvent.Types.UPDATED, cf));
    }

    public java.util.List<AdvancedConfigEntry> list() {
        synchronized (configInterfaces) {
            return new ArrayList<AdvancedConfigEntry>(configInterfaces);
        }
    }

}
