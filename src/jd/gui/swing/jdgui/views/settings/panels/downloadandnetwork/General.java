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

import javax.swing.Icon;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.JSonWrapper;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class General extends ConfigPanel {

    @Override
    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_settings_panels_downloadandnetwork_general_download_title();
    }

    @Override
    public Icon getIcon() {
        return NewTheme.I().getIcon("network-idle", ConfigPanel.ICON_SIZE);
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public General() {
        super();

        init(true);
    }

    @Override
    protected ConfigContainer setupContainer() {
        Property config = JSonWrapper.get("DOWNLOAD");

        ConfigContainer container = new ConfigContainer();
        ConfigEntry ce, cond;

        /* Download Directory */
        container.setGroup(new ConfigGroup(_GUI._.gui_config_general_downloaddirectory(), "home"));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, ""));
        ce.setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, _GUI._.gui_config_general_createsubfolders()));
        ce.setDefaultValue(false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_CREATE_SUBFOLDER_BEFORE_DOWNLOAD, _GUI._.gui_config_general_createsubfoldersbefore()));
        ce.setDefaultValue(false);
        ce.setEnabledCondidtion(cond, true);

        /* Download Control */
        container.setGroup(new ConfigGroup(_GUI._.gui_config_download_download_tab(), "downloadpath"));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, _GUI._.gui_config_download_simultan_downloads_per_host(), 0, 20, 1).setDefaultValue(0));

        String[] removeDownloads = new String[] { _GUI._.gui_config_general_toDoWithDownloads_immediate(), _GUI._.gui_config_general_toDoWithDownloads_atstart(), _GUI._.gui_config_general_toDoWithDownloads_packageready(), _GUI._.gui_config_general_toDoWithDownloads_never() };
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, removeDownloads, _GUI._.gui_config_general_todowithdownloads()).setDefaultValue(3));

        String[] fileExists = new String[] { _GUI._.system_download_triggerfileexists_overwrite(), _GUI._.system_download_triggerfileexists_skip(), _GUI._.system_download_triggerfileexists_rename(), _GUI._.system_download_triggerfileexists_askpackage(), _GUI._.system_download_triggerfileexists_ask() };
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, config, Configuration.PARAM_FILE_EXISTS, fileExists, _GUI._.system_download_triggerfileexists()).setDefaultValue(1));

        // container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // GUIUtils.getConfig(),
        // JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START,
        // JDL.L("gui.config.download.startdownloadsonstartUp",
        // "Start Downloads on Startup")).setDefaultValue(false));

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JSonWrapper.get("DOWNLOAD"), Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, _GUI._.gui_config_download_pausespeed(), 10, 500, 10).setDefaultValue(10));

        /* File Writing */
        container.setGroup(new ConfigGroup(_GUI._.gui_config_download_write(), "save"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_DO_CRC, _GUI._.gui_config_download_crc()).setDefaultValue(true));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, "MAXBUFFERSIZE", _GUI._.gui_config_download_buffersize2(), 500, 2000, 100).setDefaultValue(500));

        return container;
    }

}