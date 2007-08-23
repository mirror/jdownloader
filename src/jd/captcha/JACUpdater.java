package jd.captcha;









/**
 * JAC Updater

 * 
 * @author coalado
 */
public class JACUpdater {
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACUpdater main = new JACUpdater();
        main.go();
    }
    private void go(){ 
        
        JAntiCaptcha.updateMethods();
        

   
    }
}