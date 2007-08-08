package jd;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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
