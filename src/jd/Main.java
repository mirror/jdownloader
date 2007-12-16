package jd;

import java.awt.Graphics;
import java.awt.Image;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JWindow;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author astaldo/coalado
 */

public class Main {

    private static Logger logger = JDUtilities.getLogger();

    public static void main(String args[]) {
        JDLocale.setLocale("german");
        //rausgenommen verlängert nur den startvorgang
//        if (SingleInstanceController.isApplicationRunning()) {
//            JOptionPane.showMessageDialog(null, JDLocale.L("sys.warning.multiple_instance"), JDLocale.L("sys.header.jdownloader","JDownloader runs already"), JOptionPane.WARNING_MESSAGE);
//            System.exit(0);
//            return;
//        }
        SingleInstanceController.bindRMIObject(new SingleInstanceController());
        Main main = new Main();
        main.go();
    }

    private void go() {
       

          
                JWindow window = new JWindow() {
                    public void paint(Graphics g) {
                        Image splashImage = getToolkit().getImage("img/jd_logo_large.png");
                        g.drawImage(splashImage, 0, 0, this);
                    }
                };
                window.setSize(450, 100);
                window.setLocationRelativeTo(null);
                window.setVisible(true);


        JDInit init = new JDInit();
        init.init();
        init.loadImages();
        logger.info("OS: " + System.getProperty("os.name") + ", " + System.getProperty("os.arch") + ", " + System.getProperty("os.version"));
        logger.info("Registriere Plugins");
        final Configuration configuration = init.loadConfiguration();
        final JDController controller = init.initController();
        final UIInterface uiInterface = init.initGUI(controller);

        JDUtilities.initFileLogger();

        init.initPlugins();
        init.loadDownloadQueue();
        init.loadModules();
        init.checkUpdate();

        init.doWebupdate();

        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_START_DOWNLOADS_AFTER_START, false)) {
            controller.getUiInterface().fireUIEvent(new UIEvent(controller.getUiInterface(), UIEvent.UI_START_DOWNLOADS));
        }
        
       
    
        window.dispose();
     
        

    }

}
