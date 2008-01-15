package jd.captcha;

import java.util.logging.Logger;

import jd.captcha.utils.UTILITIES;
import jd.update.WebUpdater;









/**
 * JAC Updater

 * 
 * @author JD-Team
 */
public class JACUpdater {
    private Logger logger = UTILITIES.getLogger();
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACUpdater main = new JACUpdater();
        main.go();
    }
    private void go(){ 
        
//        JAntiCaptcha.updateMethods();
        WebUpdater web= new WebUpdater("http://lagcity.de/~JDownloaderFiles/autoUpdate");
        logger.info("New files: "+web.getUpdateNum());

   
    }
}