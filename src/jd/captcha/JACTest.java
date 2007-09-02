package jd.captcha;



import java.io.File;

import jd.JDUtilities;



/**
 * JAC Tester

 * 
 * @author coalado
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
      
      String hoster="filefactory.com";

       JAntiCaptcha jac= new JAntiCaptcha(null,hoster);
     //sharegullicom47210807182105.gif
      jac.setShowDebugGui(true);
  jac.exportDB();
    jac.importDB();
       LetterComperator.CREATEINTERSECTIONLETTER=true;
      jac.displayLibrary();
       jac.getJas().set("preScanFilter", 100);
     jac.showPreparedCaptcha(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"+"filefactorycom119020907161320.gif"));
      
     //UTILITIES.getLogger().info(JAntiCaptcha.getCaptchaCode(UTILITIES.loadImage(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")), null, "rapidshare.com"));
     //jac.removeBadLetters();
      //jac.addLetterMap();
      //jac.saveMTHFile();

      
   
    }
}