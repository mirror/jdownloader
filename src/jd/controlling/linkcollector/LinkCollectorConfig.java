package jd.controlling.linkcollector;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;

public interface LinkCollectorConfig extends ConfigInterface {

    public static final LinkCollectorConfig                 CFG         = JsonConfig.create(LinkCollectorConfig.class);
    public static final StorageHandler<LinkCollectorConfig> SH          = (StorageHandler<LinkCollectorConfig>) CFG.getStorageHandler();

    public static final BooleanKeyHandler                   DOLINKCHECK = SH.getKeyHandler("DoLinkCheck", BooleanKeyHandler.class);
    public static final BooleanKeyHandler                   DOMERGETOP  = SH.getKeyHandler("DoMergeTopBottom", BooleanKeyHandler.class);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @Description("check links for on/offline status")
    boolean getDoLinkCheck();

    void setDoLinkCheck(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @Description("use top(true) or bottom(false) position for merge")
    boolean getDoMergeTopBottom();

    void setDoMergeTopBottom(boolean b);
}