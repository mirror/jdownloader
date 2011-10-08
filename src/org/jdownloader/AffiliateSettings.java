package org.jdownloader;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.gui.translate._GUI;

public interface AffiliateSettings extends ConfigInterface {
    public static enum PremiumFeature {
        UNLIMITED_SPEED(_GUI._.PremiumFeature_speed_(), "speed"), NO_WAITTIME(_GUI._.PremiumFeature_noWaittime_(), "wait"), PARALLEL_DOWNLOADS(_GUI._.PremiumFeature_parallel_(), "paralell"), RESUME(_GUI._.PremiumFeature_resume_(), "resume"), CHUNKS(_GUI._.PremiumFeature_chunkload_(), "chunks"), UNLIMITED_BANDWIDTH(_GUI._.PremiumFeature_bandwidth_(), "bandwidth");

        private String translation;

        public String getTranslation() {
            return translation;
        }

        public String getIconKey() {
            return iconKey;
        }

        private String iconKey;

        private PremiumFeature(String translation, String iconKey) {
            this.translation = translation;
            this.iconKey = iconKey;
        }
    }

    @DefaultBooleanValue(false)
    boolean isCaptchaDialog();

    void setCaptchaDialog(boolean b);

    String getName();

    void setName(String name);

    PremiumFeature[] getPremiumfeatures();

    String getBuyPremiumLink();

}
