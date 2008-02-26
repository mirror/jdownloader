package jd.controlling.interaction;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import jd.captcha.LetterComperator;
import jd.config.Configuration;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class CaptchaLetterUploader extends Interaction implements Serializable {

    private static final String NAME              = JDLocale.L("interaction.captchaletteruploaded.name", "Captcha Autotrain");

    public static String        PROPERTY_QUESTION = "INTERACTION_" + NAME + "_QUESTION";

    public CaptchaLetterUploader() {}

    @Override
    public boolean doInteraction(Object arg) {
        Plugin plg = (Plugin) arg;
        LetterComperator[] lcs = plg.getLastCaptcha().getLetterComperators();
       String captchaTxt = plg.getLastCaptcha().getCorrectCaptchaCode();
        if(lcs.length==captchaTxt.length()){
          for( int i=0; i<captchaTxt.length();i++){
         
             if(!captchaTxt.substring(i,i+1).equalsIgnoreCase(lcs[i].getDecodedValue())){           
               String userChar=captchaTxt.substring(i,i+1);
                 logger.severe("Unknown letter: "+i+": JAC:"+lcs[i].getDecodedValue()+"("+lcs[i].getValityPercent()+") USER: "+captchaTxt.substring(i,i+1));
                 //Pixelstring.   getB() ist immer der neue letter
                 //http://ns2.km32221.keymachine.de/jdownloader/update/captchaexchange.php?character=a&pixelstring=11001|1001&host=rapidshare.com&hash=43ghjg&
                 try {
                    RequestInfo ri = Plugin.postRequest(new URL("http://ns2.km32221.keymachine.de/jdownloader/update/captchaexchange.php"), null, null, null, "character="+userChar+"&pixelstring="+lcs[i].getB().getPixelString()+"&host="+plg.getHost()+"&hash="+JDUtilities.getLocalHash(JDUtilities.getResourceFile("jd/captcha/methods/"+plg.getHost()+"/letters.mth")), false);
               logger.info("Posted: "+ "character="+userChar+"&pixelstring="+lcs[i].getB().getPixelString()+"&host="+plg.getHost()+"&hash="+JDUtilities.getLocalHash(JDUtilities.getResourceFile("jd/captcha/methods/"+plg.getHost()+"/letters.mth")));
               logger.info(ri.getHtmlCode());
                 }
                catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
               
                 
                 
             }
         }
        }
        new CaptchaMethodLoader().interact(plg.getHost());
        return true;
    }

    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {}

    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {}
}
