//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package tests.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.JFrame;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.JDInit;
import jd.Main;
import jd.OptionalPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.MacOSController;
import jd.gui.swing.SwingGui;
import jd.http.Browser;
import jd.nutils.OSDetector;
import jd.parser.html.Form;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;

public abstract class TestUtils {
    /**
     * Normal usual downloadlink
     */
    public static final String HOSTER_LINKTYPE_NORMAL         = "NORMAL_DOWNLOADLINK_";
    /**
     * File not found downloadlink. invalid URL. NOT ABUSED!
     */
    public static final String HOSTER_LINKTYPE_FNF            = "FNF_DOWNLOADLINK_";
    /**
     * Very very tiny download. just a few kb or less
     */
    public static final String HOSTER_LINKTYPE_TINY           = "TINY_DOWNLOADLINK_";
    /**
     * Bigger than normaldownloadlinks
     */
    public static final String HOSTER_LINKTYPE_OVERSIZE       = "OVERSIZE_DOWNLOADLINK_";
    /**
     * Link abused
     */
    public static final String HOSTER_LINKTYPE_ABUSED         = "ABUSED_DOWNLOADLINK_";
    /**
     * Serverside error
     */
    public static final String HOSTER_LINKTYPE_ERROR_HARDWARE = "HARDWARE_ERROR_DOWNLOADLINK_";
    /**
     * Tempora. unavailable
     */
    public static final String HOSTER_LINKTYPE_ERROR_TEMP     = "TEM_ERROR_DOWNLOADLINK_";

    private static JFrame      FRAME;

    private static JDInit      jdi;

    private static String      WIKI_USER;

    private static String      WIKI_PASS;

    public static boolean ask(final String question) {
        return UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, "We need to know if..?", question, null, null, null) == UserIO.RETURN_OK;

    }

    public static void finishInit() {

        try {
            Main.loadDynamics();
        } catch (final Exception e1) {
            JDLogger.exception(Level.FINEST, e1);
        }
        WebUpdate.doUpdateCheck(false);
        JDUtilities.getController().fireControlEvent(new ControlEvent(new Object(), ControlEvent.CONTROL_INIT_COMPLETE, null));
    }

    /**
     * 
     * 
     * 
     NORMAL_DECRYPTERLINK_1
     * 
     * 
     * DLC_DECRYPTER_LINK_1
     * 
     * 
     * PASSWORD_PROTECTED_1:12345
     * 
     * PASSWORD_PROTECTED_2:12345
     * 
     * 
     * CAPTCHA_DECRYPTER_1
     * 
     * FOLDER_DECRYPTER_1
     * 
     * 
     * @param string
     * @return
     */
    public static HashMap<String, String> getDecrypterLinks(final String string) {
        final HashMap<String, String> ret = new HashMap<String, String>();
        try {

            final Browser br = new Browser();
            br.setFollowRedirects(true);
            br.setDebug(true);
            br.getPage("http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/decrypter/" + string + "?lng=en");
            final String login = br.getRegex("(http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/decrypter/" + string + "\\?do=login\\&amp\\;sectok=.*?)\"").getMatch(0);
            br.getPage(login);

            final Form form = br.getForm(2);
            if (TestUtils.WIKI_USER == null) {
                TestUtils.WIKI_USER = TestUtils.getStringProperty("JD_WIKI_USER");
                TestUtils.WIKI_PASS = TestUtils.getStringProperty("JD_WIKI_PASS");
            }
            form.put("u", TestUtils.WIKI_USER);
            form.put("p", TestUtils.WIKI_PASS);
            br.submitForm(form);
            final String[][] matches = br.getRegex("<div class=\"li\"> <a href=\"(.*?)\" class=\"urlextern\" target=\"_blank\" title=\".*?\"  rel=\"nofollow\">(.*?)</a>").getMatches();
            if (matches == null) { return ret; }
            for (final String[] m : matches) {
                if (!m[0].trim().equalsIgnoreCase("http://decryptlink")) {
                    ret.put(m[1].trim(), m[0].trim());
                }
            }

        } catch (final Exception e) {
            e.printStackTrace();

        }
        return ret;

    }

    /**
     * Returns a hashmap of examplelinks. See
     * http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/
     * 
     * Musthave: NORMAL_DOWNLOADLINK_1 FNF_DOWNLOADLINK_1
     * 
     * @param string
     * @return
     */
    public static HashMap<String, String> getHosterLinks(final String string) {
        final HashMap<String, String> ret = new HashMap<String, String>();
        try {

            final Browser br = new Browser();
            br.setFollowRedirects(true);
            br.setDebug(true);
            br.getPage("http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/hoster/" + string + "?lng=en");
            final String login = br.getRegex("(http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/hoster/" + string + "\\?do=login\\&amp\\;sectok=.*?)\"").getMatch(0);
            br.getPage(login);

            final Form form = br.getForm(2);
            if (TestUtils.WIKI_USER == null) {
                TestUtils.WIKI_USER = TestUtils.getStringProperty("JD_WIKI_USER");
                TestUtils.WIKI_PASS = TestUtils.getStringProperty("JD_WIKI_PASS");
            }
            form.put("u", TestUtils.WIKI_USER);
            form.put("p", TestUtils.WIKI_PASS);
            br.submitForm(form);
            final String[][] matches = br.getRegex("<div class=\"li\"> <a href=\"(.*?)\" class=\"urlextern\" target=\"_blank\" title=\".*?\"  rel=\"nofollow\">(.*?)</a>").getMatches();
            if (matches == null) { return ret; }
            for (final String[] m : matches) {
                if (!m[0].trim().equalsIgnoreCase("http://downloadlink")) {
                    ret.put(m[1].trim(), m[0].trim());
                }
            }

        } catch (final Exception e) {
            e.printStackTrace();

        }
        return ret;

    }

    public static int getIntegerProperty(final String string) {
        final SubConfiguration cfg = SubConfiguration.getConfig("UNITTEST");
        int ret = cfg.getIntegerProperty(string);

        ret = Integer.parseInt(UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, "Please enter Integer", string, ret + "", null, null, null));

        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    /**
     * Returns a stored property or asks to enter it
     * 
     * @param string
     * @return
     */
    public static String getStringProperty(final String string) {
        final SubConfiguration cfg = SubConfiguration.getConfig("UNITTEST");
        String ret = cfg.getStringProperty(string);
        if (ret != null) {
            UserIO.setCountdownTime(5);
        }
        ret = UserIO.getInstance().requestInputDialog(0, "Please enter String", string, ret, null, null, null);
        UserIO.setCountdownTime(20);
        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    public static void initAllPlugins() {
        TestUtils.initDecrypter();
        TestUtils.initHosts();
        TestUtils.initOptionalPlugins();
    }

    public static void initContainer() {
        TestUtils.jdi.loadCPlugins();

    }

    public static void initControllers() {
        TestUtils.jdi.initControllers();
    }

    public static void initDecrypter() {
        if (DecryptPluginWrapper.getDecryptWrapper().size() > 0) { return; }

    }

    public static void initGUI() {
        if (SwingGui.getInstance() != null) { return; }

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {

                TestUtils.jdi.initGUI(JDUtilities.getController());
                return null;
            }
        }.waitForEDT();

        // SwingGui.getInstance().setVisible(false);
    }

    public static void initHosts() {
        if (HostPluginWrapper.getHostWrapper().size() > 0) { return; }

    }

    public static void initJD() {
        TestUtils.mainInit();
        TestUtils.initGUI();
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();
        // JDLogger.getLogger().setLevel(Level.ALL);
    }

    public static void initOptionalPlugins() {
        if (OptionalPluginWrapper.getOptionalWrapper().size() > 0) { return; }

        TestUtils.jdi.loadPluginOptional();
    }

    public static String log(final String msg) {
        System.out.println(new Exception().getStackTrace()[1].toString() + " : " + msg);
        return new Exception().getStackTrace()[1].toString() + " : " + msg;
    }

    public static void mainInit() {
        // if (JDUtilities.getController() != null) return;

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                TestUtils.FRAME = new JFrame();
                TestUtils.FRAME.setVisible(false);
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

        JDTheme.setTheme("default");

        TestUtils.jdi = new JDInit();
        TestUtils.jdi.init();
        if (TestUtils.jdi.loadConfiguration() == null) {
            UserIO.getInstance().requestMessageDialog("JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }

    }

    /**
     * logs in the browser to jd wiki
     * 
     * @param br
     */
    public static void wikiLogin(final Browser br) {
        try {
            br.setFollowRedirects(true);
            br.setDebug(true);
            br.getPage("http://jdownloader.net:8081/home/index");
            final String login = br.getRegex("(http://jdownloader.net:8081/home/index\\?do=login\\&amp\\;sectok=.*?)\"").getMatch(0);

            br.getPage(login);

            final Form form = br.getForm(2);

            TestUtils.WIKI_USER = TestUtils.getStringProperty("JD_WIKI_USER");
            TestUtils.WIKI_PASS = TestUtils.getStringProperty("JD_WIKI_PASS");

            form.put("u", TestUtils.WIKI_USER);
            form.put("p", TestUtils.WIKI_PASS);
            br.submitForm(form);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}