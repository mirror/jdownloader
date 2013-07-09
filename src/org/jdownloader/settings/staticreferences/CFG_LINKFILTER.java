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

    // Static Mappings for interface
    // org.jdownloader.controlling.filter.LinkFilterSettings

    public static final LinkFilterSettings                 CFG                                        = JsonConfig.create(LinkFilterSettings.class);

    public static final StorageHandler<LinkFilterSettings> SH                                         = (StorageHandler<LinkFilterSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkgrabberaddattop
    // = false
    public static final BooleanKeyHandler                  LINKGRABBER_ADD_AT_TOP                     = SH.getKeyHandler("LinkgrabberAddAtTop", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkgrabberquicksettingsvisible
    // = true
    public static final BooleanKeyHandler                  LINKGRABBER_QUICK_SETTINGS_VISIBLE         = SH.getKeyHandler("LinkgrabberQuickSettingsVisible", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.exceptionasquickfilterenabled
    // = true
    /**
     * true to show filterexceptions as quickfilters in linkgrabber sidebar
     **/
    public static final BooleanKeyHandler                  EXCEPTION_AS_QUICKFILTER_ENABLED           = SH.getKeyHandler("ExceptionAsQuickfilterEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkfilterenabled =
    // true
    public static final BooleanKeyHandler                  LINK_FILTER_ENABLED                        = SH.getKeyHandler("LinkFilterEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkgrabberfiletypequickfilterenabled
    // = true
    public static final BooleanKeyHandler                  LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED   = SH.getKeyHandler("LinkgrabberFiletypeQuickfilterEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkgrabberautostartenabled
    // = true
    public static final BooleanKeyHandler                  LINKGRABBER_AUTO_START_ENABLED             = SH.getKeyHandler("LinkgrabberAutoStartEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkgrabberhosterquickfilterenabled
    // = true
    public static final BooleanKeyHandler                  LINKGRABBER_HOSTER_QUICKFILTER_ENABLED     = SH.getKeyHandler("LinkgrabberHosterQuickfilterEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.filterlist = [File
    // isn't online]
    public static final ObjectKeyHandler                   FILTER_LIST                                = SH.getKeyHandler("FilterList", ObjectKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkgrabberautoconfirmenabled
    // = false
    public static final BooleanKeyHandler                  LINKGRABBER_AUTO_CONFIRM_ENABLED           = SH.getKeyHandler("LinkgrabberAutoConfirmEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.filter.LinkFilterSettings.linkgrabberexceptionsquickfilterenabled
    // = true
    public static final BooleanKeyHandler                  LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED = SH.getKeyHandler("LinkgrabberExceptionsQuickfilterEnabled", BooleanKeyHandler.class);
}
