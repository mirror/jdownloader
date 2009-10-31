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

package tests.singletests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.JDInit;
import jd.OptionalPluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ByteBufferController;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.PasswordListController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.MacOSController;
import jd.gui.swing.SwingGui;
import jd.nutils.OSDetector;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.junit.Test;

public class StartUp {
    private static JDInit jdi;

    @Test
    public void setEncoding() {
        System.setProperty("file.encoding", "UTF-8");

        assertEquals(System.getProperty("file.encoding"), "UTF-8");
    }

    @Test
    public void Mac() {
        if (OSDetector.isMac()) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            assertEquals(System.getProperty("com.apple.mrj.application.apple.menu.about.name"), "jDownloader");

            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            assertEquals(System.getProperty("com.apple.mrj.application.growbox.intrudes"), "false");

            System.setProperty("apple.laf.useScreenMenuBar", "true");
            assertEquals(System.getProperty("apple.laf.useScreenMenuBar"), "true");

            new MacOSController();
        }
    }

    @Test
    public void setTheme() {
        JDTheme.setTheme("default");

        assertEquals(JDTheme.getTheme(), "default");
    }

    @Test
    public static void JDInit() {
        jdi = new JDInit();
        assertTrue(jdi != null);
        jdi.init();
    }

    @Test
    public void Configuration() {
        assertTrue(jdi.loadConfiguration() != null);
    }

    @Test
    public void Controller() {
        assertTrue(JDController.getInstance() != null);
    }

    @Test
    public void Decrypter() {
        assertTrue(DecryptPluginWrapper.getDecryptWrapper().size() > 0);
    }

    @Test
    public void Host() {
        assertTrue(HostPluginWrapper.getHostWrapper().size() > 0);
    }

    @Test
    public void OptionalPlugins() {
        jdi.loadPluginOptional();

        assertTrue(OptionalPluginWrapper.getOptionalWrapper().size() > 0);
    }

    @Test
    public void GUI() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                jdi.initGUI(JDUtilities.getController());
                return null;
            }
        }.waitForEDT();

        SwingGui.getInstance().getMainFrame().setVisible(false);

        assertTrue(SwingGui.getInstance().getMainFrame() != null);
    }

    @Test
    public void Controllers() {
        jdi.initControllers();

        assertTrue(DownloadController.getInstance() != null);
        assertTrue(PasswordListController.getInstance() != null);
        assertTrue(AccountController.getInstance() != null);
        assertTrue(ByteBufferController.getInstance() != null);
    }
}