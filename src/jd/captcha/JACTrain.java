
package jd.captcha;



import java.io.File;
import java.util.logging.Logger;

import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

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

        String methodsPath=UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods"});
        
        String hoster="datenklo.net2";

        JAntiCaptcha jac= new JAntiCaptcha(methodsPath,hoster);
        //jac.runTestMode(new File("1186941165349_captcha.jpg"));
    // jac.displayLibrary();
       // jac.setShowDebugGui(true);
       jac.showPreparedCaptcha(new File("/home/dwd/.jd_home/captchas/datenklo.net/08.01.2008_18.08.52.gif"));
       // jac.trainAllCaptchas("/home/dwd/.jd_home/captchas/datenklo.net");
       // jac.saveMTHFile();
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