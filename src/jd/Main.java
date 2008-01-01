package jd;

import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JWindow;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.ExternExecute;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.JDExit;
import jd.controlling.interaction.ResetLink;
import jd.event.UIEvent;
import jd.gui.UIInterface;
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
        Main main = new Main();
        main.go();
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
        final Configuration configuration = init.loadConfiguration();

        final JDController controller = init.initController();
        if (init.installerWasVisible()) {
            init.doWebupdate();

        }
        else {
            final UIInterface uiInterface = init.initGUI(controller);

            JDUtilities.initFileLogger();

            init.initPlugins();
            init.loadDownloadQueue();
            init.loadModules();
            init.checkUpdate();

            init.doWebupdate();
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
