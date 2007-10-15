package jd.captcha;



import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;



/**
 * JAC Tester

 * 
 * @author coalado
 */
public class JACLoad {
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACLoad main = new JACLoad();
        main.go();
    }
    private void go(){
      String methodsPath=UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectory().getAbsolutePath(), "jd", "captcha", "methods"});
      String hoster="SerienJunkies.dl.am";

   
 loadSerienJunkies(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"), 6000);
     
      
   
    }
    private void loadSerienJunkies(File file, int i) {
        UTILITIES.useCookies=true;
        long stamp= UTILITIES.getTimer();
        while(i>0){
        i--;
        UTILITIES.cookie=null;
       String html= UTILITIES.getPagewithScanner("http://85.17.177.195/sjsafe/f-e5750571accc91e7/rc_las-vegas-s04e01-dvdrip.html");
       html = html.replaceAll("(?s)<!--.*?-->", "");
       html = html.replaceAll("(?s)<.*?c8d1ae64a5be11b14c8140d243b198d9.gif.*?>", "");
       String path="http://85.17.177.195"+UTILITIES.getMatches(html,"<TD><IMG SRC=\"°\" ALT=\"\" BORDER=\"0\"")[0];
       URL url=null;
       try {
         url= new URL(path);
    }
    catch (MalformedURLException e) {
        e.printStackTrace();
    }
    int c=0;
    File dest=new File(file,"captcha_"+c+"_"+url.getPath().substring(url.getPath().lastIndexOf("/")+1));

    while(dest.exists()){
        c++;
        UTILITIES.getLogger().info(i+"DOPPELT!! "+path);
       dest=new File(file,"captcha_"+c+"_"+url.getPath().substring(url.getPath().lastIndexOf("/")+1));
    }
        
    
       UTILITIES.downloadBinary(dest.getAbsolutePath(), path);
       UTILITIES.getLogger().info(i+"new captcha: : "+path);
    
        }
        UTILITIES.useCookies=false;
        
    }
}