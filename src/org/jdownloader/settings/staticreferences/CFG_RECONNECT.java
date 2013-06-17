package org.jdownloader.settings.staticreferences;

import jd.controlling.reconnect.ReconnectConfig;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;

public class CFG_RECONNECT {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(ReconnectConfig.class);
    }

    // Static Mappings for interface jd.controlling.reconnect.ReconnectConfig
    public static final ReconnectConfig                 CFG                                                = JsonConfig.create(ReconnectConfig.class);
    public static final StorageHandler<ReconnectConfig> SH                                                 = (StorageHandler<ReconnectConfig>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // 10000
    public static final IntegerKeyHandler               IPCHECK_READ_TIMEOUT                               = SH.getKeyHandler("IPCheckReadTimeout", IntegerKeyHandler.class);
    // 2000
    public static final IntegerKeyHandler               IPCHECK_CONNECT_TIMEOUT                            = SH.getKeyHandler("IPCheckConnectTimeout", IntegerKeyHandler.class);
    // true
    /**
     * AutoReconnect enabled?
     **/
    public static final BooleanKeyHandler               AUTO_RECONNECT_ENABLED                             = SH.getKeyHandler("AutoReconnectEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler               IPCHECK_GLOBALLY_DISABLED                          = SH.getKeyHandler("IPCheckGloballyDisabled", BooleanKeyHandler.class);
    // null
    /**
     * Please enter Website for IPCheck here
     **/
    public static final StringKeyHandler                GLOBAL_IPCHECK_URL                                 = SH.getKeyHandler("GlobalIPCheckUrl", StringKeyHandler.class);
    // true
    /**
     * Do not start further downloads if others are waiting for a reconnect/new ip
     **/
    public static final BooleanKeyHandler               DOWNLOAD_CONTROLLER_PREFERS_RECONNECT_ENABLED      = SH.getKeyHandler("DownloadControllerPrefersReconnectEnabled", BooleanKeyHandler.class);
    // 5
    /**
     * Auto Reconnect Wizard performs a few reconnects for each successful script to find the fastest one. The more rounds we use, the
     * better the result will be, but the longer it will take.
     **/
    public static final IntegerKeyHandler               OPTIMIZATION_ROUNDS                                = SH.getKeyHandler("OptimizationRounds", IntegerKeyHandler.class);
    // 0
    public static final IntegerKeyHandler               SUCCESS_COUNTER                                    = SH.getKeyHandler("SuccessCounter", IntegerKeyHandler.class);
    // 5
    public static final IntegerKeyHandler               MAX_RECONNECT_RETRY_NUM                            = SH.getKeyHandler("MaxReconnectRetryNum", IntegerKeyHandler.class);
    // 5
    public static final IntegerKeyHandler               SECONDS_BEFORE_FIRST_IPCHECK                       = SH.getKeyHandler("SecondsBeforeFirstIPCheck", IntegerKeyHandler.class);
    // httpliveheader
    public static final StringKeyHandler                ACTIVE_PLUGIN_ID                                   = SH.getKeyHandler("ActivePluginID", StringKeyHandler.class);
    // 300
    public static final IntegerKeyHandler               SECONDS_TO_WAIT_FOR_IPCHANGE                       = SH.getKeyHandler("SecondsToWaitForIPChange", IntegerKeyHandler.class);
    // \b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b
    /**
     * Please enter Regex for IPCheck here
     **/
    public static final StringKeyHandler                GLOBAL_IPCHECK_PATTERN                             = SH.getKeyHandler("GlobalIPCheckPattern", StringKeyHandler.class);
    // true
    /**
     * If disabled, No Reconnects will be done while Resumable Downloads (Premium Downloads) are running
     **/
    public static final BooleanKeyHandler               RECONNECT_ALLOWED_TO_INTERRUPT_RESUMABLE_DOWNLOADS = SH.getKeyHandler("ReconnectAllowedToInterruptResumableDownloads", BooleanKeyHandler.class);
    // 0
    public static final IntegerKeyHandler               GLOBAL_FAILED_COUNTER                              = SH.getKeyHandler("GlobalFailedCounter", IntegerKeyHandler.class);
    // 60
    public static final IntegerKeyHandler               SECONDS_TO_WAIT_FOR_OFFLINE                        = SH.getKeyHandler("SecondsToWaitForOffline", IntegerKeyHandler.class);
    // false
    public static final BooleanKeyHandler               CUSTOM_IPCHECK_ENABLED                             = SH.getKeyHandler("CustomIPCheckEnabled", BooleanKeyHandler.class);
    // 0
    public static final IntegerKeyHandler               GLOBAL_SUCCESS_COUNTER                             = SH.getKeyHandler("GlobalSuccessCounter", IntegerKeyHandler.class);
    // 0
    public static final IntegerKeyHandler               FAILED_COUNTER                                     = SH.getKeyHandler("FailedCounter", IntegerKeyHandler.class);
}
