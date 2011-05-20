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

import javax.swing.Icon;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JSonWrapper;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class General extends ConfigPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    public static String getIconKey() {
        return "gui";
    }

    @Override
    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_settings_panels_gui_General_gui_title();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II(getIconKey(), ConfigPanel.ICON_SIZE, ConfigPanel.ICON_SIZE);
    }

    private JSonWrapper subConfig;

    public General() {
        super();

        // this.subConfig = GUIUtils.getConfig();

        this.init(true);
    }

    @Override
    protected ConfigContainer setupContainer() {
        ConfigEntry ce;

        final ConfigContainer look = new ConfigContainer();

        look.setGroup(new ConfigGroup(_GUI._.gui_config_gui_view(), "gui"));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, SubConfiguration.getConfig(JDL.CONFIG), JDL.LOCALE_PARAM_ID, JDL.getLocaleIDs().toArray(new JDLocale[] {}), _GUI._.gui_config_gui_language()));
        ce.setDefaultValue(JDL.DEFAULT_LOCALE);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);

        // if
        // (LookAndFeelController.getInstance().getSupportedLookAndFeels().length
        // > 1) {
        // look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX,
        // this.subConfig, LookAndFeelController.PARAM_PLAF,
        // LookAndFeelController.getInstance().getSupportedLookAndFeels(),
        // T._.gui_config_gui_plaf()));
        // ce.setDefaultValue(LookAndFeelController.getInstance().getPlaf());
        // ce.setPropertyType(PropertyType.NEEDS_RESTART);
        // }

        look.setGroup(new ConfigGroup(_GUI._.gui_config_gui_feel(), "settings"));
        // look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER,
        // this.subConfig, JDGuiConstants.PARAM_INPUTTIMEOUT,
        // JDL.L("gui.config.gui.inputtimeout", "Timeout for InputWindows"), 0,
        // 600, 5));
        // ce.setDefaultValue(20);
        // ce.setPropertyType(PropertyType.NEEDS_RESTART);
        // look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // this.subConfig, JDGuiConstants.PARAM_SHOW_BALLOON,
        // JDL.L("gui.config.gui.showBalloon", "Show Balloon infos")));
        // ce.setDefaultValue(false);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                AbstractDialog.resetDialogInformations();
                UserIO.getInstance().requestMessageDialog(_GUI._.gui_config_gui_resetdialogs_message());
            }

        }, _GUI._.gui_config_gui_resetdialogs_short(), _GUI._.gui_config_gui_resetdialogs2(), NewTheme.I().getIcon("restart", 16)));

        look.setGroup(new ConfigGroup(_GUI._.gui_config_gui_performance(), "guiperformance"));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.subConfig, JDGuiConstants.DECORATION_ENABLED, _GUI._.gui_config_gui_decoration()));
        ce.setDefaultValue(true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);

        look.setGroup(new ConfigGroup(_GUI._.gui_config_gui_barrierfree(), "barrierfreesettings"));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this.subConfig, JDGuiConstants.PARAM_GENERAL_FONT_SIZE, _GUI._.gui_config_gui_font_size(), 50, 200, 10).setDefaultValue(100));
        ce.setPropertyType(PropertyType.NEEDS_RESTART);

        return look;
    }
}