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

import javax.swing.Icon;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;

import org.jdownloader.gui.translate._GUI;

public class Linkgrabber extends ConfigPanel {

    @Override
    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_gui_linkgrabber_title();
    }

    public static String getIconKey() {
        return "linkgrabber";
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II(getIconKey(), ConfigPanel.ICON_SIZE, ConfigPanel.ICON_SIZE);
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

        container.setGroup(new ConfigGroup(_GUI._.gui_config_gui_linggrabber(), "linkgrabber"));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_ONLINECHECK, _GUI._.gui_config_linkgrabber_onlincheck()));
        ce.setDefaultValue(true);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_REPLACECHARS, _GUI._.gui_config_linkgrabber_replacechars()));
        ce.setDefaultValue(false);
        String[] options = new String[] { _GUI._.jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages_expanded(), _GUI._.jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages_automatic(), _GUI._.jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages_collapsed() };
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, config, LinkGrabberController.PARAM_NEWPACKAGES, options, _GUI._.jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages()));
        ce.setDefaultValue(2);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_CONTROLPOSITION, _GUI._.gui_config_linkgrabber_controlposition()));
        ce.setDefaultValue(true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_INFOPANEL_ONLINKGRAB, _GUI._.gui_config_linkgrabber_infopanel_onlinkgrab()));
        ce.setDefaultValue(false);
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, LinkGrabberController.PARAM_USE_CNL2, _GUI._.gui_config_linkgrabber_cnl2()));
        ce.setDefaultValue(true);

        container.setGroup(new ConfigGroup(_GUI._.gui_config_gui_linggrabber_ignorelist(), "filter"));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, config, LinkGrabberController.IGNORE_LIST, _GUI._.gui_config_linkgrabber_ignorelist()));
        ce.setDefaultValue("#Ignorefiletype 'olo':\r\n\r\n.+?\\.olo\r\n\r\n#Ignore hoster 'examplehost.com':\r\n\r\n.*?examplehost\\.com.*?");

        return container;
    }

}