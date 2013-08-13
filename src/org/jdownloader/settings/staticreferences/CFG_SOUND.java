package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.settings.SoundSettings;

public class CFG_SOUND {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(SoundSettings.class);
    }

    // Static Mappings for interface org.jdownloader.settings.SoundSettings
    public static final SoundSettings                 CFG                   = JsonConfig.create(SoundSettings.class);
    public static final StorageHandler<SoundSettings> SH                    = (StorageHandler<SoundSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // 100
    public static final IntegerKeyHandler             CAPTCHA_SOUND_VOLUME  = SH.getKeyHandler("CaptchaSoundVolume", IntegerKeyHandler.class);
    // true
    /**
     * Play a Sound when a Captchadialog opens
     **/
    public static final BooleanKeyHandler             CAPTCHA_SOUND_ENABLED = SH.getKeyHandler("CaptchaSoundEnabled", BooleanKeyHandler.class);
}