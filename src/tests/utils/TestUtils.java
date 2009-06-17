package tests.utils;

//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

import javax.swing.JFrame;

import jd.DecryptPluginWrapper;
import jd.JDInit;
import jd.OptionalPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.userio.SimpleUserIO;
import jd.nutils.OSDetector;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.MacOSController;

public abstract class TestUtils {

    private static JFrame FRAME;

    private static JDInit jdi;

    private static JDController jdc;

    /**
     * Returns a stored property or asks to enter it
     * 
     * @param string
     * @return
     */
    public static String getStringProperty(String string) {
        SubConfiguration cfg = SubConfiguration.getConfig("UNITTEST");
        String ret = cfg.getStringProperty(string);

        ret = SimpleUserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, "PLease enter String", string, ret, null, null, null);
        ;
        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    public static int getIntegerProperty(String string) {
        SubConfiguration cfg = SubConfiguration.getConfig("UNITTEST");
        int ret = cfg.getIntegerProperty(string);

        ret = Integer.parseInt(SimpleUserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, "Please enter Integer", string, ret + "", null, null, null));
        ;

        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    public static boolean ask(String question) {
        return SimpleUserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, "We need to know if..?", question, null, null, null) == UserIO.RETURN_OK;

    }

    public static void initJD() {
        mainInit();
        initAllPlugins();
        initGUI();
        initControllers();
        finishInit();
    }

    public static void log(String msg) {
        System.out.println(new Exception().getStackTrace()[1].toString() + " : " + msg);
    }

    public static void mainInit() {
        if (JDUtilities.getController() != null) return;

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                FRAME = new JFrame();
                FRAME.setVisible(false);
                return null;
            }
        }.waitForEDT();

        // frame.setAlwaysOnTop(true);
        System.setProperty("file.encoding", "UTF-8");
        // Mac specific //
        if (OSDetector.isMac()) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            new MacOSController();
        }

        Interaction.initTriggers();

        JDTheme.setTheme("default");

        jdi = new JDInit();
        jdi.init();

        if (jdi.loadConfiguration() == null) {
            UserIO.getInstance().requestMessageDialog("JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }

        jdc = JDController.getInstance();
    }

    public static void initDecrypter() {
        if (DecryptPluginWrapper.getDecryptWrapper().size() > 0) return;

        jdi.loadPluginForDecrypt();
    }

    public static void initHosts() {
        if (JDUtilities.getPluginsForHost().size() > 0) return;

        jdi.loadPluginForHost();
    }

    public static void initOptionalPlugins() {
        if (OptionalPluginWrapper.getOptionalWrapper().size() > 0) return;

        jdi.loadPluginOptional();
    }

    public static void initAllPlugins() {
        initDecrypter();
        initHosts();
        initOptionalPlugins();
    }

    public static void initGUI() {
        if (SimpleGUI.CURRENTGUI != null) return;

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                jdi.initGUI(jdc);
                return null;
            }
        }.waitForEDT();

        SimpleGUI.CURRENTGUI.setVisible(false);
    }

    public static void initControllers() {
        jdi.initControllers();
    }

    public static void finishInit() {
        jdc.setInitStatus(JDController.INIT_STATUS_COMPLETE);
        JDUtilities.getController().fireControlEvent(new ControlEvent(new Object(), ControlEvent.CONTROL_INIT_COMPLETE, null));
    }
}