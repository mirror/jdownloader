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

package jd.gui.skins.jdgui.settings.panels.gui;

import javax.swing.JTabbedPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.gui.skins.jdgui.settings.ConfigPanel;
import jd.gui.skins.jdgui.settings.GUIConfigEntry;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class Linkgrabber extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.skins.jdgui.settings.panels.gui.Linkgrabber.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "gui.linkgrabber.title", "Linkgrabber");
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public Linkgrabber(Configuration configuration) {
        super();
        initPanel();
        load();
    }

    private ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();

        ConfigEntry ce;

        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.linggrabber", "General Linkgrabber Settings"), JDTheme.II("gui.images.taskpanes.linkgrabber", 32, 32)));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig(LinkGrabberController.CONFIG), LinkGrabberController.PARAM_ONLINECHECK, JDL.L("gui.config.linkgrabber.onlincheck", "Check linkinfo and onlinestatus")));
        ce.setDefaultValue(true);

        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.linggrabber.ignorelist", "Linkfilter"), JDTheme.II("gui.images.filter", 32, 32)));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, SubConfiguration.getConfig(LinkGrabberController.CONFIG), LinkGrabberController.IGNORE_LIST, JDL.L("gui.config.linkgrabber.iognorelist", "The linkfilter is used to filter links based on regular expressions.")));
        ce.setDefaultValue("#Ignorefiletype 'olo':\r\n\r\n.+?\\.olo\r\n\r\n#Ignore hoster 'examplehost.com':\r\n\r\n.*?examplehost\\.com.*?");

        return container;
    }

    @Override
    public void initPanel() {
        ConfigContainer container = setupContainer();

        for (ConfigEntry cfgEntry : container.getEntries()) {
            GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
            if (ce != null) addGUIConfigEntry(ce);
        }

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }
}
