
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
//        try {
            // Alle JAR Dateien, die in diesem Verzeichnis liegen, werden dem
            // Classloader hinzugefügt.
//            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//            if(classLoader != null && (classLoader instanceof URLClassLoader)){
//                URLClassLoader urlClassLoader = (URLClassLoader)classLoader;
//                Method         addURL         = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
//                File           files[]        = new File(".").listFiles(new FilterJAR());
//
//                addURL.setAccessible(true);
//                for(int i=0;i<files.length;i++){
//                    logger.info(files[i].getAbsolutePath());
//                    URL jarURL = files[i].toURL();
//                    addURL.invoke(urlClassLoader, new Object[]{jarURL});
//                }
//            }
//        }
//        catch (Exception e) { }

        JAntiCaptcha jac= new JAntiCaptcha(null,"rapidshare.com");
        //jac.runTestMode(new File("1186941165349_captcha.jpg"));
     jac.displayLibrary();
       // jac.setShowDebugGui(true);
       // jac.showPreparedCaptcha(new File("captcha\\methods\\rapidshare.com\\captchas\\rapidsharecom138040807171852.jpg"));
        jac.trainAllCaptchas(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"\\jd\\captcha\\methods\\rapidshare.com\\captchas\\");
  
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