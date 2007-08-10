package jd;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import jd.captcha.Captcha;
import jd.captcha.JAntiCaptcha;
import jd.captcha.UTILITIES;
import jd.gui.MainWindow;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
/**
 * Start der Applikation
 *
 * TODO Astaldo : Speedometer
 * TODO Astaldo : Serialisierung
 * TODO Astaldo : GUI
 * TODO Astaldo : Konfiguration speichern
 * TODO Wulfskin: Reconnect Paket
 * TODO Coalado : AntiCaptcha
 * 
 * @author astaldo
 */
public class Main {
    public static void main(String args[]){
    	/*coalado testcode
    	 * 
    	 *
    	 */
    	JAntiCaptcha jac = new JAntiCaptcha("rapidshare.com");
    	Captcha captcha= jac.createCaptcha(UTILITIES.loadImage(new File("rscaptcha.jpg")));
    	String captchaCode=jac.checkCaptcha(captcha);
    	UTILITIES.trace("Code: "+captchaCode);
    	/*
    	 * ctest ende
    	 */
        Main main = new Main();
        main.go();
    }
    private void go(){
//        PluginForHost p = new Rapidshare();
//        try {
//            URL url = new URL("http://javadl.sun.com/webapps/download/AutoDL?BundleId=11281");
//            DownloadLink dLink = new DownloadLink(p,"test","test","",true);
//            p.download(dLink, url.openConnection());
//        }
//        catch (MalformedURLException e1) {
//        }
//        catch (IOException e1) {
//        }
//        
//        if (true) return;
        try {
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        }
        catch (UnsupportedLookAndFeelException e) {}

        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
    }
}
