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

package jd.gui.swing.jdgui.views.settings.panels.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.nutils.JDFlags;
import jd.nutils.OSDetector;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class Browser extends ConfigPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.gui.Browser.";

    @Override
    public String getBreadcrumb() {
        return JDL.L(JDL_PREFIX + "breadcrum", "Basics - User Interface - Browser");
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "gui.browser.title", "Browser");
    }

    private SubConfiguration subConfig;

    public Browser() {
        super();
        subConfig = GUIUtils.getConfig();
        initPanel();
        load();
    }

    private ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();

        ConfigEntry ce;

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L("gui.config.gui.testcontainer.message", "JDownloader now tries to open http://jdownloader.org in your container.")), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    try {
                        save();
                        JLink.openURL("http://jdownloader.org");
                    } catch (Exception e) {
                        JDLogger.exception(e);
                        UserIO.getInstance().requestMessageDialog(JDL.LF("gui.config.gui.testcontainer.error", "Browser launcher failed: %s", e.getLocalizedMessage()));
                    }
                }

            }
        }, JDL.L("gui.config.gui.testcontainer.short", "Start browser"), JDL.L("gui.config.gui.testcontainer.long", "Test starting your browser"), JDTheme.II("gui.images.config.host", 16, 16)));

        ConfigEntry conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDGuiConstants.PARAM_CUSTOM_BROWSER_USE, JDL.L("gui.config.gui.use_custom_browser", "Use custom browser"));
        conditionEntry.setDefaultValue(false);

        LocalBrowser[] lb = LocalBrowser.getBrowserList();
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, JDGuiConstants.PARAM_BROWSER, lb, JDL.L("gui.config.gui.Browser", "Browser")).setDefaultValue(lb[0]));
        ce.setEnabledCondidtion(conditionEntry, false);

        container.addEntry(conditionEntry);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, JDGuiConstants.PARAM_CUSTOM_BROWSER, JDL.L("gui.config.gui.custom_browser", "Browserpath")));

        String parameter = null;
        String path = null;
        if (OSDetector.isWindows()) {

            if (new File("C:\\Program Files\\Mozilla Firefox\\firefox.exe").exists()) {
                parameter = "-new-tab\r\n%url";
                path = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Programme\\Mozilla Firefox\\firefox.exe").exists()) {
                parameter = "-new-tab\r\n%url";
                path = "C:\\Programme\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Program Files\\Internet Explorer\\iexplore.exe").exists()) {
                parameter = "%url";
                path = "C:\\Program Files\\Internet Explorer\\iexplore.exe";
            } else {
                parameter = "%url";
                path = "C:\\Programme\\Internet Explorer\\iexplore.exe";
            }

        } else if (OSDetector.isMac()) {

            if (new File("/Applications/Firefox.app").exists()) {
                parameter = "/Applications/Firefox.app\r\n-new-tab\r\n%url";
                path = "open";
            } else {
                parameter = "/Applications/Safari.app\r\n-new-tab\r\n%url";
                path = "open";
            }

        } else if (OSDetector.isLinux()) {

            // TODO: das ganze für linux

        }

        ce.setDefaultValue(path);
        ce.setEnabledCondidtion(conditionEntry, true);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, JDGuiConstants.PARAM_CUSTOM_BROWSER_PARAM, JDL.L("gui.config.gui.custom_browser_param", "Parameter %url (one parameter per line)")));
        ce.setDefaultValue(parameter);
        ce.setEnabledCondidtion(conditionEntry, true);

        return container;
    }

    @Override
    public void initPanel() {
        add(createTabbedPane(setupContainer()));
    }

}
