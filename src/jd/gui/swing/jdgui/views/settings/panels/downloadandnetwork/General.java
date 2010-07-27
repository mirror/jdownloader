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

package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ByteBufferController;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class General extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.downloadandnetwork.general.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "download.title", "Download & Network");
    }

    public static String getIconKey() {
        return "gui.images.config.network_local";
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public General() {
        super();

        init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        SubConfiguration config = SubConfiguration.getConfig("DOWNLOAD");

        ConfigContainer container = new ConfigContainer();
        ConfigEntry ce, cond;

        /* Download Directory */
        container.setGroup(new ConfigGroup(JDL.L("gui.config.general.downloaddirectory", "Download directory"), "gui.images.userhome"));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, ""));
        ce.setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, JDL.L("gui.config.general.createsubfolders", "Create Subfolder with packagename if possible")));
        ce.setDefaultValue(false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_CREATE_SUBFOLDER_BEFORE_DOWNLOAD, JDL.L("gui.config.general.createsubfoldersbefore", "Create sub-folders after adding links")));
        ce.setDefaultValue(false);
        ce.setEnabledCondidtion(cond, true);

        /* Download Control */
        container.setGroup(new ConfigGroup(JDL.L("gui.config.download.download.tab", "Download Control"), "gui.images.downloadorder"));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, JDL.L("gui.config.download.simultan_downloads_per_host", "Maximum of simultaneous downloads per host (0 = no limit)"), 0, 20, 1).setDefaultValue(0));

        String[] removeDownloads = new String[] { JDL.L("gui.config.general.toDoWithDownloads.immediate", "immediately"), JDL.L("gui.config.general.toDoWithDownloads.atstart", "at startup"), JDL.L("gui.config.general.toDoWithDownloads.packageready", "when package is ready"), JDL.L("gui.config.general.toDoWithDownloads.never", "never") };
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, removeDownloads, JDL.L("gui.config.general.todowithdownloads", "Remove finished downloads ...")).setDefaultValue(3));

        String[] fileExists = new String[] { JDL.L("system.download.triggerfileexists.overwrite", "Overwrite"), JDL.L("system.download.triggerfileexists.skip", "Skip Link"), JDL.L("system.download.triggerfileexists.rename", "Auto rename"), JDL.L("system.download.triggerfileexists.askpackage", "Ask for each package"), JDL.L("system.download.triggerfileexists.ask", "Ask for each file") };
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, config, Configuration.PARAM_FILE_EXISTS, fileExists, JDL.L("system.download.triggerfileexists", "If the file already exists:")).setDefaultValue(1));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, GUIUtils.getConfig(), JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, JDL.L("gui.config.download.startdownloadsonstartUp", "Start Downloads on Startup")).setDefaultValue(false));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("DOWNLOAD"), "PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", JDL.L("gui.config.download.autoresume", "Let Reconnects interrupt resumeable downloads")).setDefaultValue(true));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("DOWNLOAD"), "PARAM_DOWNLOAD_PREFER_RECONNECT", JDL.L("gui.config.download.preferreconnect", "Do not start new links if reconnect requested")).setDefaultValue(true));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, SubConfiguration.getConfig("DOWNLOAD"), Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, JDL.L("gui.config.download.pausespeed", "Speed of pause in kb/s"), 10, 500, 10).setDefaultValue(10));

        /* File Writing */
        container.setGroup(new ConfigGroup(JDL.L("gui.config.download.write", "File writing"), "gui.images.save"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_DO_CRC, JDL.L("gui.config.download.crc", "SFV/CRC check when possible")).setDefaultValue(true));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, ByteBufferController.MAXBUFFERSIZE, JDL.L("gui.config.download.buffersize2", "Max. Buffersize[KB]"), 500, 2000, 100).setDefaultValue(500));

        return container;
    }

}
