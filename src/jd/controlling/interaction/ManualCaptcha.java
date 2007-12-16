package jd.controlling.interaction;

import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Manuelle captchaeingabe
 * 
 * @author coalado
 */
public class ManualCaptcha extends Interaction implements Serializable {
    /**
     * serialVersionUID
     */
    private static final long   serialVersionUID = 4732389782312830473L;
    private static final String NAME             = JDLocale.L("interaction.manualCaptcha.name","Captcha: Manuelle Eingabe");
    public ManualCaptcha() {}
    @Override
    public boolean doInteraction(Object arg) {
        logger.info("start");
        ProgressController progress = new ProgressController(JDLocale.L("interaction.manualCaptcha.progress.0_title","Captcha Input"),2);
     
        progress.setStatusText(JDLocale.L("interaction.manualCaptcha.progress.1_title","Manual Captcha (JAC supported)"));
        logger.info("Starting Manuell captcha");
        DownloadLink dink = (DownloadLink) arg;
        progress.increase(1);
        String captchaText = JDUtilities.getController().getCaptchaCodeFromUser(dink.getPlugin(), dink.getLatestCaptchaFile());
        progress.increase(1);
        setProperty("captchaCode", captchaText);
        logger.info("ende");
        progress.finalize();
        if (captchaText != null && captchaText.length() > 0) return true;
        return false;
    }
    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {}
    public String toString() {
        return JDLocale.L("interaction.manualCaptcha.toString","Captcha: Manuelle Eingabe und Kontrolle (Bestätigung)");
    }
    @Override
    public String getInteractionName() {
        return NAME;
    }
    @Override
    public void initConfig() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_MANUAL_CAPTCHA_USE_JAC, JDLocale.L("interaction.manualCaptcha.display","jAntiCaptcha Werte anzeigen")).setDefaultValue(true));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_MANUAL_CAPTCHA_WAIT_FOR_JAC, JDLocale.L("interaction.manualCaptcha.waitTime","Bei jAntiCaptcha Ergebnis [x] Millisekunden warten und dann fortfahren"), 0, 30000).setDefaultValue(3000).setStep(1000));
    }
    @Override
    public void resetInteraction() {
    }
}
