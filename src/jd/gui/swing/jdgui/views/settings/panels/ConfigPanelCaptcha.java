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

package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.Icon;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;

import org.jdownloader.gui.translate._GUI;

public class ConfigPanelCaptcha extends ConfigPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_settings_panels_ConfigPanelCaptcha_captcha_title();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II(getIconKey(), ConfigPanel.ICON_SIZE, ConfigPanel.ICON_SIZE);
    }

    public static String getIconKey() {
        return "ocr";
    }

    public ConfigPanelCaptcha() {
        super();

        init(true);
    }

    @Override
    protected ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();
        ConfigEntry ce1;

        container.setGroup(new ConfigGroup(_GUI._.gui_config_captcha_settings(), "ocr"));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, SubConfiguration.getConfig("JAC"), Configuration.JAC_SHOW_TIMEOUT, _GUI._.gui_config_captcha_train_show_timeout(), 0, 600, 5).setDefaultValue(20));
        container.addEntry(ce1 = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("JAC"), Configuration.PARAM_CAPTCHA_JAC_DISABLE, _GUI._.gui_config_captcha_jac_disable()).setDefaultValue(false));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, SubConfiguration.getConfig("JAC"), Configuration.AUTOTRAIN_ERROR_LEVEL, _GUI._.gui_config_captcha_train_level(), 0, 100, 5).setDefaultValue(95).setEnabledCondidtion(ce1, false));

        return container;
    }
}