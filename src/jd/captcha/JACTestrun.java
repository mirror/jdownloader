
package jd.captcha;



import java.io.File;
import java.util.logging.Logger;

import jd.JDUtilities;
import jd.captcha.utils.UTILITIES;

/**
 * Jac Training
 *

 * 
 * @author coalado
 */
public class JACTestrun {
    @SuppressWarnings("unused")
    private Logger logger = UTILITIES.getLogger();
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACTestrun main = new JACTestrun();
        main.go();
    }
    private void go(){

        @SuppressWarnings("unused")
        String methodsPath=UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectory().getAbsolutePath(), "jd", "captcha", "methods"});
        JAntiCaptcha.testMethod(new File("C:/Users/coalado/.jd_home/jd/captcha/methods/serienjunkies.safehost.be/"));

//       File[] methods= JAntiCaptcha.getMethods(methodsPath);
//      logger.info("Found "+methods.length+" Methods");
//       JAntiCaptcha.testMethods(methods);

    }
}