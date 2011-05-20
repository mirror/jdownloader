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

package jd.gui.swing.jdgui.views.settings.panels.passwords;

import javax.swing.Icon;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.PasswordListController;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;

import org.jdownloader.gui.translate._GUI;

public class PasswordList extends ConfigPanel {

    @Override
    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_settings_panels_PasswordList_general_title();
    }

    public static String getIconKey() {
        return "password";
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II(getIconKey(), ConfigPanel.ICON_SIZE, ConfigPanel.ICON_SIZE);
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public PasswordList() {
        super();

        init(true);
    }

    @Override
    protected ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(getTitle(), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, PasswordListController.getInstance(), _GUI._.plugins_optional_jdunrar_config_passwordlist()));

        return container;
    }

}