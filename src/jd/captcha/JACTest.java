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
      

  
       JAntiCaptcha jac= new JAntiCaptcha(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods","rapidshare.com");
     //sharegullicom47210807182105.gif
     jac.showPreparedCaptcha(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg"));
      
     //UTILITIES.getLogger().info(JAntiCaptcha.getCaptchaCode(UTILITIES.loadImage(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")), null, "rapidshare.com"));
     //jac.removeBadLetters();
      //jac.addLetterMap();
      //jac.saveMTHFile();

      
   
    }
}