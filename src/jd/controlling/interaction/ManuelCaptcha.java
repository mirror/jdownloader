package jd.controlling.interaction;

import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

/**
 * Manuelle captchaeingabe
 * 
 * @author coalado
 */
public class ManuelCaptcha extends Interaction implements Serializable{



    /**
     * 
     */
    private static final long serialVersionUID = 4732389782312830473L;
    private static final String NAME             = "Captcha: Manuelle Eingabe";
 

    public ManuelCaptcha(){
     
    }
   
    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Manuell captcha");
        DownloadLink dink=(DownloadLink)arg;
        String captchaText = JDUtilities.getController().getCaptchaCodeFromUser(dink.getPlugin(), dink.getLatestCaptchaFile());
        setProperty("captchaCode",captchaText);        
        if(captchaText!=null && captchaText.length()>0)return true;
        return false;
    }

    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {

    }

    public String toString() {
        return "Captcha: Manuelle Eingabe und Kontrolle (Bestätigung)";
    }

    @Override
    public String getInteractionName() {

        return NAME;
    }

    @Override
    public void initConfig() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_MANUAL_CAPTCHA_USE_JAC, "jAntiCaptcha Werte anzeigen").setDefaultValue(true));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER,  JDUtilities.getConfiguration(), Configuration.PARAM_MANUAL_CAPTCHA_WAIT_FOR_JAC, "Bei jAntiCaptcha Ergebnis [x] Millisekunden warten und dann fortfahren",0,30000).setDefaultValue(10000).setStep(1000));
        
        
    }


  
}
