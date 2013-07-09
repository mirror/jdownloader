package org.jdownloader.settings.staticreferences;

import jd.controlling.linkcollector.LinkCollectorConfig;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;

public class CFG_LINKCOLLECTOR {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(LinkCollectorConfig.class);
    }

    // Static Mappings for interface
    // jd.controlling.linkcollector.LinkCollectorConfig
    public static final LinkCollectorConfig                 CFG                 = JsonConfig.create(LinkCollectorConfig.class);
    public static final StorageHandler<LinkCollectorConfig> SH                  = (StorageHandler<LinkCollectorConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // Keyhandler interface
    // jd.controlling.linkcollector.LinkCollectorConfig.dolinkcheck = true
    /**
     * check links for on/offline status
     **/
    public static final BooleanKeyHandler                   DO_LINK_CHECK       = SH.getKeyHandler("DoLinkCheck", BooleanKeyHandler.class);
    // Keyhandler interface
    // jd.controlling.linkcollector.LinkCollectorConfig.domergetopbottom = true
    /**
     * use top(true) or bottom(false) position for merge
     **/
    public static final BooleanKeyHandler                   DO_MERGE_TOP_BOTTOM = SH.getKeyHandler("DoMergeTopBottom", BooleanKeyHandler.class);
}
