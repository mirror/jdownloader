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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.locale.JDL;

public class Linkgrabber extends ConfigPanel {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.gui.Linkgrabber.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "gui.linkgrabber.title", "Linkgrabber");
    }

    public static String getIconKey() {
        return "gui.images.taskpanes.linkgrabber";
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public Linkgrabber() {
        super();

        init(true);
    }

    @Override
    protected ConfigContainer setupContainer() {
        SubConfiguration config = SubConfiguration.getConfig(LinkGrabberController.CONFIG);
        ConfigContainer container = new ConfigContainer();

        ConfigEntry ce;

        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.linggrabber", "General Linkgrabber Settings"), "gui.images.taskpanes.linkgrabber"));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_ONLINECHECK, JDL.L("gui.config.linkgrabber.onlincheck", "Check linkinfo and onlinestatus")));
        ce.setDefaultValue(true);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_REPLACECHARS, JDL.L("gui.config.linkgrabber.replacechars", "(Autopackager)Replace dots and _ with spaces?")));
        ce.setDefaultValue(false);
        String[] options = new String[] { JDL.L(JDL_PREFIX + "newpackages.expanded", "Expanded"), JDL.L(JDL_PREFIX + "newpackages.automatic", "Automatic"), JDL.L(JDL_PREFIX + "newpackages.collapsed", "Collapsed") };
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, config, LinkGrabberController.PARAM_NEWPACKAGES, options, JDL.L(JDL_PREFIX + "newpackages", "Open new packages by default")));
        ce.setDefaultValue(2);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_CONTROLPOSITION, JDL.L("gui.config.linkgrabber.controlposition", "Put Linkgrabberbuttons above table")));
        ce.setDefaultValue(true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_INFOPANEL_ONLINKGRAB, JDL.L("gui.config.linkgrabber.infopanel.onlinkgrab", "Show infopanel on linkgrab")));
        ce.setDefaultValue(false);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_USE_CNL2, JDL.L("gui.config.linkgrabber.cnl2", "Enable Click'n'Load Support")));
        ce.setDefaultValue(true);

        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.linggrabber.ignorelist", "Linkfilter"), "gui.images.filter"));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, config, LinkGrabberController.IGNORE_LIST, JDL.L("gui.config.linkgrabber.iognorelist", "The linkfilter is used to filter links based on regular expressions.")));
        ce.setDefaultValue("#Ignorefiletype 'olo':\r\n\r\n.+?\\.olo\r\n\r\n#Ignore hoster 'examplehost.com':\r\n\r\n.*?examplehost\\.com.*?");

        return container;
    }

}
