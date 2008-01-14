package jd;

import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JWindow;

import jd.captcha.JACController;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.ExternExecute;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.JDExit;
import jd.controlling.interaction.ResetLink;
import jd.event.UIEvent;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * @author astaldo/coalado
 */

public class Main {

    private static Logger logger = JDUtilities.getLogger();

    public static void main(String args[]) {
        JDLocale.setLocale("german");
        JDTheme.setTheme("default");
        boolean stop = false;
        for (int i = 0; i < args.length; i++) {
            String string = args[i];
            if(string.equals("--help") || string.equals("-h"))
            {
                String [][] help = new String[][] {
                        {JDUtilities.getJDTitle(), "Coalado::Astaldo::DwD::Botzi GPL"},
                        {"http://jdownloader.ath.cx/","http://www.the-lounge.org/viewforum.php?f=217"+System.getProperty("line.separator")},
                        {"-h, --help","Print help for jDownloader"},
                        {"-s --show", "Open a menu to show a JAC prepared captcha"},
                        {"-t --train", "Open a menu to train a JAC method"}};
                for (int j = 0; j < help.length; j++) {
                    System.out.println(help[j][0]+"\t"+help[j][1]);
                }
                System.exit(0);
            }
            else if(string.equals("--show") || string.equals("-s"))
            {
                JACController.showDialog(false);
                stop=true;
                break;
            }
            else if(string.equals("--train") || string.equals("-t"))
            {
                JACController.showDialog(true);
                stop=true;
                break;
            }
            
        }
        logger.info(System.getProperty("java.class.path"));
        // rausgenommen verlängert nur den startvorgang
        // if (SingleInstanceController.isApplicationRunning()) {
        // JOptionPane.showMessageDialog(null,
        // JDLocale.L("sys.warning.multiple_instance"),
        // JDLocale.L("sys.header.jdownloader","JDownloader runs already"),
        // JOptionPane.WARNING_MESSAGE);
        // System.exit(0);
        // return;
        // }
        // SingleInstanceController.bindRMIObject(new
        // SingleInstanceController());
        if(!stop)
        {
        Main main = new Main();
        main.go();
        }
    }

    private void go() {
     

        JDInit init = new JDInit();
        init.init();
        init.loadImages();
        JWindow window = new JWindow() {
            public void paint(Graphics g) {
                Image splashImage = JDUtilities.getImage("jd_logo_large");
                g.drawImage(splashImage, 0, 0, this);
            }
        };
    
        window.setSize(450, 100);
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        logger.info("OS: " + System.getProperty("os.name") + ", " + System.getProperty("os.arch") + ", " + System.getProperty("os.version"));
        logger.info("Registriere Plugins");
       init.loadConfiguration();

        final JDController controller = init.initController();
        if (init.installerWasVisible()) {
            init.doWebupdate();

        }
        else {
            init.initGUI(controller);

            JDUtilities.initFileLogger();

            init.initPlugins();
            init.loadDownloadQueue();
            init.loadModules();
            init.checkUpdate();
if(JDUtilities.getRunType()==JDUtilities.RUNTYPE_LOCAL_JARED){
   
            init.doWebupdate();
}
        }
     
       
        /*
         * Das ist ein kurzeitiger Übergangsfix. der teil löscht Interactionen
         * aus der confog die nicht emhr benötigt werden
         */
        Vector<Interaction> interactions = JDUtilities.getConfiguration().getInteractions();

        int s = interactions.size();
        for (int i = interactions.size() - 1; i >= 0; i--) {
            if (interactions.get(i) instanceof ResetLink || interactions.get(i) instanceof JDExit || interactions.get(i) instanceof ExternExecute) {

            }
            else {
                interactions.remove(i);
            }
        }
        if (interactions.size() < s) {
            JDUtilities.getConfiguration().setInteractions(interactions);
            JDUtilities.saveConfig();
        }
        /*
         * Ende des Interactionfixes
         */
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_START_DOWNLOADS_AFTER_START, false)) {
            controller.getUiInterface().fireUIEvent(new UIEvent(controller.getUiInterface(), UIEvent.UI_START_DOWNLOADS));
        }

        window.dispose();

    }

}
