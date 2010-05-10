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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.locale.JDL;

public class ConfigPanelCaptcha extends ConfigPanel {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelCaptcha.";
    private static final long serialVersionUID = 3383448498625377495L;

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "captcha.title", "JAntiCaptcha");
    }

    public static String getIconKey() {
        return "gui.images.config.ocr";
    }

    public ConfigPanelCaptcha() {
        super();
        initPanel();
        load();
    }

    private ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();
        ConfigEntry ce1;
        ConfigEntry ce2;

        container.setGroup(new ConfigGroup(JDL.L("gui.config.captcha.settings", "Captcha settings"), "gui.images.config.ocr"));
        container.addEntry(ce1 = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("JAC"), Configuration.PARAM_CAPTCHA_JAC_DISABLE, JDL.L("gui.config.captcha.jac_disable", "Disable automatic CAPTCHA")).setDefaultValue(false));
        container.addEntry(ce2 = new ConfigEntry(ConfigContainer.TYPE_SPINNER, SubConfiguration.getConfig("JAC"), Configuration.JAC_SHOW_TIMEOUT, JDL.L("gui.config.captcha.train.show_timeout", "Countdown for CAPTCHA window"), 0, 600).setDefaultValue(20));
        ce2.setEnabledCondidtion(ce1, false);
        container.addEntry(ce2 = new ConfigEntry(ConfigContainer.TYPE_SPINNER, SubConfiguration.getConfig("JAC"), Configuration.AUTOTRAIN_ERROR_LEVEL, JDL.L("gui.config.captcha.train.level", "Display Threshold"), 0, 100).setDefaultValue(95));
        ce2.setEnabledCondidtion(ce1, false);
        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.barrierfree", "Barrier-Free"), "gui.images.barrierfree"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, SubConfiguration.getConfig("JAC"), Configuration.PARAM_CAPTCHA_SIZE, JDL.L(JDL_PREFIX + "captchaSize", "Size of Captcha in percent:"), 100, 200).setStep(5).setDefaultValue(100));

        return container;
    }

    @Override
    public void initPanel() {
        add(createPanel(setupContainer()));
    }
}