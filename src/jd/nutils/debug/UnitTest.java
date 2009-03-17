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

package jd.nutils.debug;

import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.JDInit;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.MacOSController;

public abstract class UnitTest {

    private static ArrayList<Class<?>> tests;
    private StringBuffer log;
    private JFrame frame = null;

    private static void init() {
        tests = new ArrayList<Class<?>>();
        tests.add(Browser.Test.class);
        // tests.add(LinkGrabber.Test.class);

    }

    public String getStringProperty(String string) {
        SubConfiguration cfg = JDUtilities.getSubConfig("UNITTEST");
        String ret = cfg.getStringProperty(string);

        ret = JOptionPane.showInputDialog(frame, "Enter " + string, ret);
        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    public int getIntegerProperty(String string) {
        SubConfiguration cfg = JDUtilities.getSubConfig("UNITTEST");
        int ret = cfg.getIntegerProperty(string);

        ret = Integer.parseInt(JOptionPane.showInputDialog(frame, "Enter " + string, ret + ""));
        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    public boolean ask(String string) {
        return JOptionPane.showConfirmDialog(frame, string) == JOptionPane.OK_OPTION;

    }

    public void initJD() {
        frame = new JFrame();
        frame.setVisible(true);
        // frame.setAlwaysOnTop(true);
        System.setProperty("file.encoding", "UTF-8");
        // Mac specific //
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            new MacOSController();
        }

        Interaction.initTriggers();

        JDTheme.setTheme("default");
        JDSounds.setSoundTheme("default");

        final JDInit init = new JDInit(null);
        init.init();

        if (init.loadConfiguration() == null) {

            JOptionPane.showMessageDialog(null, "JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }

        final JDController controller = init.initController();
        JDUtilities.getConfiguration();

        init.initPlugins();

        init.initGUI(controller);
        SimpleGUI.CURRENTGUI.getFrame().setVisible(false);
        init.loadDownloadQueue();

        controller.setInitStatus(JDController.INIT_STATUS_COMPLETE);

        // init.createQueueBackup();

        // init.checkUpdate();

        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_INIT_COMPLETE, null));

        // JDFileReg.registerFileExts();

    }

    public static UnitTest newInstance() {
        return null;
    }

    public static void main(String args[]) throws Exception {
        // UnitTest.run("jd\\.http.*");
        UnitTest.run("jd\\.gui.*");
    }

    private static void run(String pattern) {
        init();
        UnitTest testInstance;
        for (Class<?> test : tests) {
            String name = test.getName();
            if (!name.matches(pattern)) continue;
            try {
                System.out.println("-----------Run Test: " + name + "----------");
                Method f = test.getMethod("newInstance", new Class<?>[] {});
                testInstance = (UnitTest) f.invoke(null, new Object[] {});
                if (testInstance == null) {
                    System.out.println("FAILED: forgot to override public static UnitTest newInstance");
                } else {

                    try {
                        testInstance.run();
                        System.out.println("Successfull");
                        // System.out.println(testInstance.getLog());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("FAILED");
                        // System.err.println(testInstance.getLog());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void log(String msg) {
        if (this.log == null) log = new StringBuffer();
        // System.err.println(ee.getStackTrace()[1].getClassName() + "." +
        // ee.getStackTrace()[1].getMethodName() + "[" +
        // ee.getStackTrace()[1].getLineNumber() + "] " + msg);
        System.out.println(new Exception().getStackTrace()[1].toString() + " : " + msg);
        log.append(new Exception().getStackTrace()[1].toString() + " : " + msg + "\r\n");
    }

    public String getLog() {
        return log.toString();
    }

    public abstract void run() throws Exception;

}
