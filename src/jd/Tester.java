package jd;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.dialog.AbstractDialog;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.JDImage;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Using the scaled icon in a JLabel works without problems! But when using the
 * scaled icon as a IconImage of a JDialog, it freezes!
 */
public class Tester {

    private static final Logger logger = JDLogger.getLogger();

    public static void main(String[] args) throws Exception {
        JDTheme.setTheme("default");
        JDL.setLocale(JDL.getConfigLocale());
        UserIO.setInstance(UserIOGui.getInstance());

        File[] icons = JDUtilities.getResourceFile("jd/img/favicons/").listFiles();
        testIcon(icons[1]);
    }

    private static void testIcon(File host) {
        logger.info("Testing " + host.getName() + " ...");

        final ImageIcon icon = JDImage.getImageIcon(host);
        final ImageIcon scaledIcon = JDImage.getScaledImageIcon(icon, 80, 80);

        AbstractDialog dialog = new AbstractDialog(UserIO.NO_ICON | UserIO.NO_COUNTDOWN, "Test", null, null, null) {

            private static final long serialVersionUID = -4149362553408821100L;

            @Override
            public JComponent contentInit() {
                /*
                 * Works fine!
                 */
                return new JLabel(scaledIcon);
            }

            @Override
            protected void packed() {
                /*
                 * Here's the problem!
                 */
                setIconImage(scaledIcon.getImage());
            }

        };
        dialog.init();
        logger.info(dialog.getReturnValue() + "");
    }
}