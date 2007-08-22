package jd.captcha;





import jd.JDUtilities;



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
        
        WebUpdater wu= new WebUpdater("http://lagcity.de/~JDownloaderFiles/autoUpdate");
        wu.setDestPath(JDUtilities.getJDHomeDirectory());
      wu.run();
        

   
    }
}