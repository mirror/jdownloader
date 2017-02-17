package org.jdownloader.settings.staticreferences;

import java.io.File;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.ListHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings;
import org.jdownloader.logging.LogController;

public class CFG_MYJD {

    // Static Mappings for interface org.jdownloader.api.myjdownloader.MyJDownloaderSettings
    public static final MyJDownloaderSettings                 CFG = initializeMyJDownloaderSettings();                                ;
    public static final StorageHandler<MyJDownloaderSettings> SH  = (StorageHandler<MyJDownloaderSettings>) CFG._getStorageHandler();

    private final static MyJDownloaderSettings initializeMyJDownloaderSettings() {
        final MyJDownloaderSettings ret = JsonConfig.create(MyJDownloaderSettings.class);
        try {
            if (StringUtils.isEmpty(ret.getEmail()) || StringUtils.isEmpty(ret.getPassword())) {
                final StorageHandler<MyJDownloaderSettings> sh = (StorageHandler<MyJDownloaderSettings>) ret._getStorageHandler();
                final File file = new File(sh.getPath().getAbsolutePath() + ".json");
                if (file.exists()) {
                    final String fileContent = IO.readFileToString(file).trim();
                    final String password = new Regex(fileContent, "\"password\"\\s*:\\s*\"(.*?)(\"(,\\s*\"|\\s*\\}$))").getMatch(0);
                    String email = new Regex(fileContent, "\"email\"\\s*:\\s*\"(.*?)(\"(,\\s*\"|\\s*\\}$))").getMatch(0);
                    if (email == null) {
                        email = new Regex(fileContent, "\"username\"\\s*:\\s*\"(.*?)(\"(,\\s*\"|\\s*\\}$))").getMatch(0);
                    }
                    if (StringUtils.isAllNotEmpty(password, email)) {
                        LogController.CL().info("Rescued 'MyJDownloader logins' from broken-/false escaped-json!");
                        ret.setEmail(email);
                        ret.setPassword(password);
                    }
                }
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return ret;
    }

    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // NONE
    public static final EnumKeyHandler     LATEST_ERROR         = SH.getKeyHandler("LatestError", EnumKeyHandler.class);

    // null

    public static final StringKeyHandler   UNIQUE_DEVICE_ID     = SH.getKeyHandler("UniqueDeviceID", StringKeyHandler.class);
    // api.jdownloader.org
    public static final StringKeyHandler   SERVER_HOST          = SH.getKeyHandler("ServerHost", StringKeyHandler.class);
    // true
    public static final BooleanKeyHandler  AUTO_CONNECT_ENABLED = SH.getKeyHandler("AutoConnectEnabledV2", BooleanKeyHandler.class);
    // null
    public static final StringKeyHandler   PASSWORD             = SH.getKeyHandler("Password", StringKeyHandler.class);
    // [I@7df36af6
    public static final ListHandler<int[]> DEVICE_CONNECT_PORTS = SH.getKeyHandler("DeviceConnectPorts", ListHandler.class);
    // JDownloader
    public static final StringKeyHandler   DEVICE_NAME          = SH.getKeyHandler("DeviceName", StringKeyHandler.class);
    // null
    public static final StringKeyHandler   EMAIL                = SH.getKeyHandler("Email", StringKeyHandler.class);
}