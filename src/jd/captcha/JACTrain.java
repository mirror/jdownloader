
package jd.captcha;



import java.util.logging.Logger;

import jd.JDUtilities;
import jd.captcha.utils.UTILITIES;

/**
 * Jac Training
 *

 * 
 * @author coalado
 */
public class JACTrain {
    private Logger logger = UTILITIES.getLogger();
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACTrain main = new JACTrain();
        main.go();
    }
    private void go(){

        String methodsPath=UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectory().getAbsolutePath(), "jd", "captcha", "methods"});
        
        String hoster="sj";

        JAntiCaptcha jac= new JAntiCaptcha(methodsPath,hoster);
        //jac.runTestMode(new File("1186941165349_captcha.jpg"));
     jac.displayLibrary();
       // jac.setShowDebugGui(true);
       // jac.showPreparedCaptcha(new File("captcha\\methods\\rapidshare.com\\captchas\\rapidsharecom138040807171852.jpg"));
        jac.trainAllCaptchas(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"\\jd\\captcha\\methods\\"+hoster+"\\captchas\\");
  
        logger.info("Training Ende");
        //jac.addLetterMap();
           // jac.saveMTHFile();
    }
//    private static class FilterJAR implements FileFilter{
//        public boolean accept(File f) {
//            if(f.getName().endsWith(".jar"))
//                return true;
//            else
//                return false;
//        }
//    }
}