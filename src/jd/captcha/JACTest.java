package jd.captcha;



import java.io.File;

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

//        JAntiCaptcha jac= new JAntiCaptcha("rapidshare.com");
//
//        jac.showPreparedCaptcha(new File("captcha\\methods\\rapidshare.com\\captchas\\rapidsharecom12140807171439.jpg"));
          
        JAntiCaptcha jac= new JAntiCaptcha("share.gulli.com");
       //jac.mergeGif(new File("test.gif"));
        jac.showPreparedCaptcha(new File("captcha\\methods\\share.gulli.com\\captchas\\sharegullicom581100807165738.gif"));
        
   
    }
}