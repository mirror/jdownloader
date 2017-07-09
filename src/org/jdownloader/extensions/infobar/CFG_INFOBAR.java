package org.jdownloader.extensions.infobar;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_INFOBAR {

    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(InfoBarConfig.class, "Application.getResource(\"cfg/\" + " + InfoBarExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.infobar.InfoBarConfig
    public static final InfoBarConfig                 CFG                        = JsonConfig.create(Application.getResource("cfg/" + InfoBarExtension.class.getName()), InfoBarConfig.class);
    public static final StorageHandler<InfoBarConfig> SH                         = (StorageHandler<InfoBarConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final BooleanKeyHandler             WINDOW_VISIBLE             = SH.getKeyHandler("WindowVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             DRAG_AND_DROP_ENABLED      = SH.getKeyHandler("DragAndDropEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             FRESH_INSTALL              = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);

    /**
     * Java 1.7 or higher required!
     **/
    public static final IntegerKeyHandler             TRANSPARENCY               = SH.getKeyHandler("Transparency", IntegerKeyHandler.class);

    public static final BooleanKeyHandler             ENABLED                    = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             DRAGNDROP_ICON_DISPLAYED     = SH.getKeyHandler("DragNDropIconDisplayed", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             LINKGRABBER_BUTTON_DISPLAYED = SH.getKeyHandler("LinkgrabberButtonDisplayed", BooleanKeyHandler.class);

}