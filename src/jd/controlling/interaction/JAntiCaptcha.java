package jd.controlling.interaction;

import java.io.Serializable;

import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Captach erkennung über JAC
 * 
 * @author coalado
 */
public class JAntiCaptcha extends Interaction implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = -4390257509319544642L;
    /**
     * serialVersionUID
     */

    private static final String NAME             = JDLocale.L("interaction.jac.name","Captcha Erkennung: JAntiCaptcha");

 
   
    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting JAC");
        DownloadLink dink=(DownloadLink)arg;
        String captchaText = JDUtilities.getCaptcha(JDUtilities.getController(), dink.getPlugin(), dink.getLatestCaptchaFile());
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
        return JDLocale.L("interaction.jac.toString","Captcha Erkennung: JAntiCaptcha");
    }

    @Override
    public String getInteractionName() {

        return NAME;
    }

    @Override
    public void initConfig() {
    }

    @Override
    public void resetInteraction() {
        // TODO Auto-generated method stub
        
    }


  
}
