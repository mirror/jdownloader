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

package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.JSonWrapper;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class Reconnect extends ConfigPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    public static String getIconKey() {
        return "reconnect";
    }

    @Override
    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_settings_panels_reconnect_Advanced_reconnect_advanced_title();
    }

    @Override
    public Icon getIcon() {
        return NewTheme.I().getIcon(getIconKey(), ConfigPanel.ICON_SIZE);
    }

    public Reconnect() {
        super();

        this.init(true);
    }

    @Override
    protected ConfigContainer setupContainer() {
        final Property config = JSonWrapper.get("DOWNLOAD");

        ConfigEntry ce, cond;
        final ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(_GUI._.gui_config_reconnect_shared(), "reconnect"));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_IPCHECKWAITTIME, _GUI._.reconnect_waittimetofirstipcheck(), 5, 600, 5).setDefaultValue(5));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_RETRIES, _GUI._.reconnect_retries(), -1, 20, 1).setDefaultValue(5));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_WAITFORIPCHANGE, _GUI._.reconnect_waitforip(), 30, 600, 10).setDefaultValue(30));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", _GUI._.gui_config_download_autoresume()).setDefaultValue(true));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_PREFER_RECONNECT", _GUI._.gui_config_download_preferreconnect()).setDefaultValue(true));

        container.setGroup(new ConfigGroup(_GUI._.gui_config_download_ipcheck(), "network-idle"));

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_DISABLE, _GUI._.gui_config_download_ipcheck_disable()) {
            private static final long serialVersionUID = 1L;
            /**
             * assures that the user sees the warning only once
             */
            private boolean           warned           = true;

            /**
             * This method gets called when the user clicks the checkbox. It
             * gets also invoked at startup not only on user IO.
             */
            @Override
            public void valueChanged(final Object newValue) {
                // get Current Databasevalue
                super.valueChanged(newValue);
                // Only show the warning if the newValue differs from the
                // database stored one
                if (newValue == Boolean.TRUE && !this.warned) {
                    this.warned = true;
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, _GUI._.jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_title(), _GUI._.jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_message());
                        }

                    });
                } else if (newValue == Boolean.FALSE) {
                    this.warned = false;
                }
            }

        });
        cond.setDefaultValue(false);

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_BALANCE, _GUI._.gui_config_download_ipcheck_balance()));
        cond.setDefaultValue(true);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, _GUI._.gui_config_download_ipcheck_website()));
        ce.setDefaultValue(_GUI._.gui_config_download_ipcheck_website_default());
        ce.setEnabledCondidtion(cond, false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_PATTERN, _GUI._.gui_config_download_ipcheck_regex()));
        ce.setDefaultValue(_GUI._.gui_config_download_ipcheck_regex_default());
        ce.setEnabledCondidtion(cond, false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_MASK, _GUI._.gui_config_download_ipcheck_mask()));
        ce.setDefaultValue("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, "EXTERNAL_IP_CHECK_INTERVAL2", _GUI._.gui_config_download_ipcheck_externalinterval2(), 10, 240, 10));
        ce.setDefaultValue(10);

        return container;
    }

}