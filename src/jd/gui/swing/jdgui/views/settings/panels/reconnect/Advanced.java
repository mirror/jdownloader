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

import javax.swing.SwingUtilities;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.Property;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class Advanced extends ConfigPanel {

    private static final String JDL_PREFIX       = "jd.gui.swing.jdgui.settings.panels.reconnect.Advanced.";
    private static final long   serialVersionUID = 3383448498625377495L;

    public static String getIconKey() {
        return "gui.images.reconnect_settings";
    }

    public static String getTitle() {
        return JDL.L(Advanced.JDL_PREFIX + "reconnect.advanced.title", "Advanced");
    }

    public Advanced() {
        super();

        this.init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        final Property config = JSonWrapper.get("DOWNLOAD");

        ConfigEntry ce, cond;
        final ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(JDL.L("gui.config.reconnect.shared", "General Reconnect Settings"), "gui.images.reconnect_settings"));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_IPCHECKWAITTIME, JDL.L("reconnect.waittimetofirstipcheck", "First IP check wait time (sec)"), 5, 600, 5).setDefaultValue(5));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_RETRIES, JDL.L("reconnect.retries", "Max repeats (-1 = no limit)"), -1, 20, 1).setDefaultValue(5));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_WAITFORIPCHANGE, JDL.L("reconnect.waitforip", "Timeout for ip change [sec]"), 30, 600, 10).setDefaultValue(30));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", JDL.L("gui.config.download.autoresume", "Let Reconnects interrupt resumeable downloads")).setDefaultValue(true));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_PREFER_RECONNECT", JDL.L("gui.config.download.preferreconnect", "Do not start new links if reconnect requested")).setDefaultValue(true));

        container.setGroup(new ConfigGroup(JDL.L("gui.config.download.ipcheck", "Reconnection IP-Check"), "gui.images.network"));

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_DISABLE, JDL.L("gui.config.download.ipcheck.disable", "Disable IP-Check")) {
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
                            UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.title", "IP-Check disabled!"), JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.message", "You disabled the IP-Check. This will increase the reconnection times dramatically!\r\n\r\nSeveral further modules like Reconnect Recorder are disabled."));
                        }

                    });
                } else if (newValue == Boolean.FALSE) {
                    this.warned = false;
                }
            }

        });
        cond.setDefaultValue(false);

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_BALANCE, JDL.L("gui.config.download.ipcheck.balance", "Use balanced IP-Check")));
        cond.setDefaultValue(true);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, JDL.L("gui.config.download.ipcheck.website", "Check IP online")));
        ce.setDefaultValue(JDL.L("gui.config.download.ipcheck.website.default", "Please enter Website for IPCheck here"));
        ce.setEnabledCondidtion(cond, false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_PATTERN, JDL.L("gui.config.download.ipcheck.regex", "IP Filter RegEx")));
        ce.setDefaultValue(JDL.L("gui.config.download.ipcheck.regex.default", "Please enter Regex for IPCheck here"));
        ce.setEnabledCondidtion(cond, false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_MASK, JDL.L("gui.config.download.ipcheck.mask", "Allowed IPs")));
        ce.setDefaultValue("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, "EXTERNAL_IP_CHECK_INTERVAL2", JDL.L("gui.config.download.ipcheck.externalinterval2", "External IP Check Interval [min]"), 10, 240, 10));
        ce.setDefaultValue(10);

        return container;
    }

}
