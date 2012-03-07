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

import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.Spinner;

import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.AutoDownloadStartOption;
import org.jdownloader.settings.CleanAfterDownloadAction;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.translate._JDT;

public class DownloadControll extends AbstractConfigPanel implements ChangeListener {

    private static final long                  serialVersionUID = 1L;
    private Spinner                            maxSimPerHost;
    private ComboBox<CleanAfterDownloadAction> remove;
    private ComboBox<IfFileExistsAction>       ifFileExists;
    private Spinner                            maxSim;
    private GeneralSettings                    config;
    private Spinner                            maxchunks;
    private ComboBox<AutoDownloadStartOption>  startDownloadsAfterAppStart;

    public String getTitle() {
        return _JDT._.gui_settings_downloadcontroll_title();
    }

    @SuppressWarnings("unchecked")
    public DownloadControll() {
        super();

        this.addHeader(_JDT._.gui_settings_downloadcontroll_title(), NewTheme.I().getIcon("downloadmanagment", 32));
        this.addDescription(_JDT._.gui_settings_downloadcontroll_description());

        maxSimPerHost = new Spinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST));

        maxSim = new Spinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS));

        maxchunks = new Spinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_CHUNKS_PER_FILE));

        String[] removeDownloads = new String[] { _GUI._.gui_config_general_toDoWithDownloads_immediate(), _GUI._.gui_config_general_toDoWithDownloads_atstart(), _GUI._.gui_config_general_toDoWithDownloads_packageready(), _GUI._.gui_config_general_toDoWithDownloads_never() };

        remove = new ComboBox<CleanAfterDownloadAction>(CleanAfterDownloadAction.values(), removeDownloads);
        startDownloadsAfterAppStart = new ComboBox<AutoDownloadStartOption>(CFG_GENERAL.SH.getKeyHandler("AutoStartDownloadOption", KeyHandler.class), AutoDownloadStartOption.values(), new String[] { _GUI._.gui_config_general_AutoDownloadStartOption_always(), _GUI._.gui_config_general_AutoDownloadStartOption_only_if_closed_running(), _GUI._.gui_config_general_AutoDownloadStartOption_never() });
        String[] fileExists = new String[] { _GUI._.system_download_triggerfileexists_overwrite(), _GUI._.system_download_triggerfileexists_skip(), _GUI._.system_download_triggerfileexists_rename(), _GUI._.system_download_triggerfileexists_ask(), _GUI._.system_download_triggerfileexists_ask() };
        ifFileExists = new ComboBox<IfFileExistsAction>(IfFileExistsAction.values(), fileExists);
        this.addPair(_GUI._.gui_config_download_simultan_downloads(), null, maxSim);
        this.addPair(_GUI._.gui_config_download_simultan_downloads_per_host(), org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED, maxSimPerHost);
        this.addPair(_GUI._.gui_config_download_max_chunks(), null, maxchunks);

        this.addPair(_GUI._.gui_config_general_todowithdownloads(), null, remove);
        this.addPair(_GUI._.system_download_triggerfileexists(), null, ifFileExists);
        addPair(_GUI._.system_download_autostart(), null, startDownloadsAfterAppStart);
        config = org.jdownloader.settings.staticreferences.CFG_GENERAL.CFG;

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("downloadmanagment", 32);
    }

    @Override
    public void save() {

        config.setCleanupAfterDownloadAction(remove.getValue());
        config.setIfFileExistsAction(this.ifFileExists.getValue());

    }

    @Override
    public void updateContents() {

        this.remove.setValue(config.getCleanupAfterDownloadAction());
        this.ifFileExists.setValue(config.getIfFileExistsAction());

    }

    public void stateChanged(ChangeEvent e) {
        save();
    }
}