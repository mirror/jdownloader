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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.reconnect.ReconnectMethod;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class Advanced extends ConfigPanel {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.reconnect.Advanced.";
    private static final long serialVersionUID = 3383448498625377495L;

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "reconnect.advanced.title", "Advanced");
    }

    public static String getIconKey() {
        return "gui.images.reconnect_settings";
    }

    public Advanced() {
        super();

        setContainer(setupContainer());
    }

    private ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();
        ConfigGroup group = new ConfigGroup(JDL.L("gui.config.reconnect.shared", "General Reconnect Settings"), "gui.images.reconnect_settings");
        container.setGroup(group);
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_IPCHECKWAITTIME, JDL.L("reconnect.waittimetofirstipcheck", "First IP check wait time (sec)"), 5, 600, 5).setDefaultValue(5));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_RETRIES, JDL.L("reconnect.retries", "Max repeats (-1 = no limit)"), -1, 20, 1).setDefaultValue(5));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), ReconnectMethod.PARAM_WAITFORIPCHANGE, JDL.L("reconnect.waitforip", "Timeout for ip change [sec]"), 30, 600, 10).setDefaultValue(30));

        return container;
    }

}
