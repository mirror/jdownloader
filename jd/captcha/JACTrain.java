package jd.captcha;



import java.util.logging.Logger;

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

        JAntiCaptcha jac= new JAntiCaptcha("filefactory.com");
        //jac.runTestMode(new File("1186941165349_captcha.jpg"));
     
       // jac.showPreparedCaptcha(new File("captcha\\methods\\rapidshare.com\\captchas\\rapidsharecom138040807171852.jpg"));
        jac.trainAllCaptchas();
  
        logger.info("Training Ende");
    }
}