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
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.laf.LookAndFeelController;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;

import org.appwork.utils.swing.dialog.AbstractDialog;

public class General extends ConfigPanel {

    private static final long   serialVersionUID = 3383448498625377495L;

    private static final String JDL_PREFIX       = "jd.gui.swing.jdgui.settings.panels.gui.General.";

    public static String getIconKey() {
        return "gui.images.config.gui";
    }

    public static String getTitle() {
        return JDL.L(General.JDL_PREFIX + "gui.title", "User Interface");
    }

    private final SubConfiguration subConfig;

    public General() {
        super();

        this.subConfig = GUIUtils.getConfig();

        this.init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        ConfigEntry ce;

        final ConfigContainer look = new ConfigContainer();

        look.setGroup(new ConfigGroup(JDL.L("gui.config.gui.view", "Look"), "gui.images.config.gui"));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, SubConfiguration.getConfig(JDL.CONFIG), JDL.LOCALE_PARAM_ID, JDL.getLocaleIDs().toArray(new JDLocale[] {}), JDL.L("gui.config.gui.language", "Language")));
        ce.setDefaultValue(JDL.DEFAULT_LOCALE);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);

        final ArrayList<String> themeIDs = JDTheme.getThemeIDs();
        if (themeIDs.size() == 0) {
            this.logger.info("You have to update your resources dir! No Themefiles (*.icl) found!");
        } else if (themeIDs.size() == 1) {
            this.subConfig.setProperty(JDGuiConstants.PARAM_THEME, themeIDs.get(0));
            this.subConfig.save();
        } else {
            look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, this.subConfig, JDGuiConstants.PARAM_THEME, themeIDs.toArray(new String[] {}), JDL.L("gui.config.gui.theme", "Theme")));
            ce.setDefaultValue("default");
            ce.setPropertyType(PropertyType.NEEDS_RESTART);
        }

        if (LookAndFeelController.getSupportedLookAndFeels().length > 1) {
            look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, this.subConfig, LookAndFeelController.PARAM_PLAF, LookAndFeelController.getSupportedLookAndFeels(), JDL.L("gui.config.gui.plaf", "Style (Restart required)")));
            ce.setDefaultValue(LookAndFeelController.getPlaf());
            ce.setPropertyType(PropertyType.NEEDS_RESTART);
        }

        look.setGroup(new ConfigGroup(JDL.L("gui.config.gui.feel", "Feel"), "gui.images.configuration"));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this.subConfig, JDGuiConstants.PARAM_INPUTTIMEOUT, JDL.L("gui.config.gui.inputtimeout", "Timeout for InputWindows"), 0, 600, 5));
        ce.setDefaultValue(20);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.subConfig, JDGuiConstants.PARAM_SHOW_SPLASH, JDL.L("gui.config.gui.showsplash", "Show splash screen on program startup")));
        ce.setDefaultValue(true);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.subConfig, JDGuiConstants.PARAM_SHOW_BALLOON, JDL.L("gui.config.gui.showBalloon", "Show Balloon infos")));
        ce.setDefaultValue(false);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                AbstractDialog.resetDialogInformations();
                UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.gui.resetdialogs.message", "Dialog Information has been reseted."));
            }

        }, JDL.L("gui.config.gui.resetdialogs.short", "Reset"), JDL.L("gui.config.gui.resetdialogs2", "Reset Dialog Information"), JDTheme.II("gui.images.restart", 16, 16)));

        look.setGroup(new ConfigGroup(JDL.L("gui.config.gui.performance", "Performance"), "gui.images.performance"));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.subConfig, JDGuiConstants.DECORATION_ENABLED, JDL.L("gui.config.gui.decoration", "Enable Windowdecoration")));
        ce.setDefaultValue(true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);

        look.setGroup(new ConfigGroup(JDL.L("gui.config.gui.barrierfree", "Barrier-Free"), "gui.images.barrierfree"));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, this.subConfig, JDGuiConstants.PARAM_GENERAL_FONT_SIZE, JDL.L("gui.config.gui.font size", "Font Size [%]"), 10, 200, 10).setDefaultValue(100));
        ce.setPropertyType(PropertyType.NEEDS_RESTART);

        return look;
    }
}
