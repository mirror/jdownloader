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
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class Advanced extends ConfigPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.gui.advanced.";

    private SubConfiguration subConfig;

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "gui.advanced.title", "Advanced");
    }

    public Advanced() {
        super();
        subConfig = GUIUtils.getConfig();
        initPanel();
        load();
    }

    private ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();

        ConfigEntry ce;

        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.container", "Container (RSDF,DLC,CCF,..)"), "gui.images.container"));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_RELOADCONTAINER, JDL.L("gui.config.reloadcontainer", "Reload Download Container")));
        ce.setDefaultValue(true);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("GUI"), Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, JDL.L("gui.config.showContainerOnLoadInfo", "Show detailed container information on load")));
        ce.setDefaultValue(false);

        /* Speedmeter */
        ConfigGroup speedmeter = new ConfigGroup(JDL.L("gui.config.gui.speedmeter", "Speedmeter"), "gui.images.download");

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDGuiConstants.PARAM_SHOW_SPEEDMETER, JDL.L("gui.config.gui.show_speed_graph", "Display speedmeter graph")).setGroup(speedmeter));
        ce.setDefaultValue(true);
        ConfigEntry cond = ce;

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE, JDL.L("gui.config.gui.show_speed_graph_window", "Speedmeter Time period (sec)"), 10, 60 * 60 * 12).setGroup(speedmeter));
        ce.setDefaultValue(60);
        ce.setEnabledCondidtion(cond, true);

        container.setGroup(null);

        return container;
    }

    @Override
    public void initPanel() {
        add(createTabbedPane(setupContainer()));
    }

}
