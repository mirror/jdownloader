package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.controlling.filter.LinkFilterSettings;

public class CFG_LINKFILTER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(LinkFilterSettings.class);
    }

    // Static Mappings for interface org.jdownloader.controlling.filter.LinkFilterSettings
    public static final LinkFilterSettings                 CFG                                        = JsonConfig.create(LinkFilterSettings.class);
    public static final StorageHandler<LinkFilterSettings> SH                                         = (StorageHandler<LinkFilterSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler                  LINK_FILTER_ENABLED                        = SH.getKeyHandler("LinkFilterEnabled", BooleanKeyHandler.class);
    // true
    /**
     * true to show filterexceptions as quickfilters in linkgrabber sidebar
     **/
    public static final BooleanKeyHandler                  EXCEPTION_AS_QUICKFILTER_ENABLED           = SH.getKeyHandler("ExceptionAsQuickfilterEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                   FILTER_LIST                                = SH.getKeyHandler("FilterList", ObjectKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED = SH.getKeyHandler("LinkgrabberExceptionsQuickfilterEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED   = SH.getKeyHandler("LinkgrabberFiletypeQuickfilterEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                  LINKGRABBER_HOSTER_QUICKFILTER_ENABLED     = SH.getKeyHandler("LinkgrabberHosterQuickfilterEnabled", BooleanKeyHandler.class);
}