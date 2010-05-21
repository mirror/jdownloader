package jd;

import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.CaptchaController;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.dialog.AbstractDialog;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.JDImage;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

/**
 * Using the scaled icon in a JLabel works without problems! But when using the
 * scaled icon as a IconImage of a JDialog, it freezes!
 */
public class Tester {

    private static final Logger logger = JDLogger.getLogger();

    private static boolean SHOW_CAPTCHA_DIALOG = false;
    private static boolean USE_WORKING_METHOD = true;

    public static void main(String[] args) throws Exception {
        JDTheme.setTheme("default");
        JDL.setLocale(JDL.getConfigLocale());
        UserIO.setInstance(UserIOGui.getInstance());

        testIcon("uploaded.to");
        testIcon("uploadfloor.com");
    }

    private static void testIcon(String host) {
        logger.info("Testing " + host + " ...");

        logger.info("Loading Icon ...");
        final ImageIcon icon = JDImage.getImageIcon("favicons/" + host);

        logger.info("Scaling Icon ...");
        final ImageIcon scaledIcon = JDImage.getScaledImageIcon(icon, 80, 80);

        logger.info("Testing Scaled Icon ...");
        if (SHOW_CAPTCHA_DIALOG) {
            CaptchaController cc = new CaptchaController(host, scaledIcon, null, null, null, null);
            logger.info(cc.getCode(0));
        } else {
            new AbstractDialog(UserIO.NO_ICON | UserIO.NO_COUNTDOWN, "Test", null, null, null) {

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
                    if (USE_WORKING_METHOD) {
                        /*
                         * Works fine, too!
                         */
                        setIconImage(icon.getImage());
                    } else {
                        /*
                         * Here's the problem!
                         */
                        setIconImage(scaledIcon.getImage());
                    }
                }

            }.init();
        }
    }
}