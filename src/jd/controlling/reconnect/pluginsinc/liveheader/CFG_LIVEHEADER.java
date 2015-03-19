package jd.controlling.reconnect.pluginsinc.liveheader;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;

public class CFG_LIVEHEADER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(LiveHeaderReconnectSettings.class);
    }

    // Static Mappings for interface jd.controlling.reconnect.pluginsinc.liveheader.LiveHeaderReconnectSettings
    public static final LiveHeaderReconnectSettings                 CFG                                   = JsonConfig.create(LiveHeaderReconnectSettings.class);
    public static final StorageHandler<LiveHeaderReconnectSettings> SH                                    = (StorageHandler<LiveHeaderReconnectSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * If False, we already tried to send this script to the colect server. Will be resetted each time we change reconnect settings.
     **/
    public static final BooleanKeyHandler                           ALREADY_SEND_TO_COLLECT_SERVER3       = SH.getKeyHandler("AlreadySendToCollectServer3", BooleanKeyHandler.class);

    public static final StringKeyHandler                            ROUTER_IP                             = SH.getKeyHandler("RouterIP", StringKeyHandler.class);

    public static final StringKeyHandler                            PASSWORD                              = SH.getKeyHandler("Password", StringKeyHandler.class);

    public static final StringKeyHandler                            SCRIPT                                = SH.getKeyHandler("Script", StringKeyHandler.class);

    public static final StringKeyHandler                            USER_NAME                             = SH.getKeyHandler("UserName", StringKeyHandler.class);

    public static final ObjectKeyHandler                            ROUTER_DATA                           = SH.getKeyHandler("RouterData", ObjectKeyHandler.class);

    public static final BooleanKeyHandler                           AUTO_SEARCH_BEST_MATCH_FILTER_ENABLED = SH.getKeyHandler("AutoSearchBestMatchFilterEnabled", BooleanKeyHandler.class);
}