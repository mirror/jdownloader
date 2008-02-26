package jd.captcha;



import java.io.File;

import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;



/**
 * JAC Tester

 * 
 * @author JD-Team
 */
public class JACTest {
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACTest main = new JACTest();
        main.go();
    }
    private void go(){
      String methodsPath=UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods"});
      String hoster="rapidshare.com";

       JAntiCaptcha jac= new JAntiCaptcha(methodsPath,hoster);
     //sharegullicom47210807182105.gif
      jac.setShowDebugGui(true);
//  jac.exportDB();
      UTILITIES.getLogger().info("has method: "+JAntiCaptcha.hasMethod(methodsPath, hoster));
// jac.importDB();
//LetterComperator.CREATEINTERSECTIONLETTER=true;
      //jac.displayLibrary();
  
    jac.getJas().set("preScanFilter", 50);
//       jac.trainCaptcha(new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"+"securedin1730080724541.jpg"), 4);
     jac.showPreparedCaptcha(new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()+"/captchas"+"/"+hoster+"/"+"26.02.2008_20.00.02_DOWNLOAD+VIA+GLOBALCROSSING+#2_9OR4_GOOD.jpg"));
      
     //UTILITIES.getLogger().info(JAntiCaptcha.getCaptchaCode(UTILITIES.loadImage(new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")), null, "rapidshare.com"));
     //jac.removeBadLetters();
      //jac.addLetterMap();
      //jac.saveMTHFile();

      
   
    }
}