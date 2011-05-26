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

import jd.controlling.DownloadController;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class ConfigPanelGeneral extends AbstractConfigPanel {

    public String getTitle() {
        return _JDT._.gui_settings_general_title();
    }

    public String getIconKey() {
        return "settings";
    }

    private static final long serialVersionUID = 3383448498625377495L;

    private FolderChooser     downloadFolder;

    private Checkbox          subfolder;

    private Checkbox          autoCRC;

    private Checkbox          simpleContainer;

    public ConfigPanelGeneral() {
        super();

        downloadFolder = new FolderChooser("downloadfolder");
        subfolder = new Checkbox();
        simpleContainer = new Checkbox();

        this.addHeader(_GUI._.gui_config_general_downloaddirectory(), NewTheme.I().getIcon("downloadpath", 32));
        this.addDescription(_JDT._.gui_settings_downloadpath_description());
        this.add(downloadFolder);
        this.addPair(_GUI._.gui_config_general_createsubfolders(), subfolder);

        /* File Writing */
        autoCRC = new Checkbox();
        this.addHeader(_GUI._.gui_config_download_write(), NewTheme.I().getIcon("hashsum", 32));
        this.addDescription(_JDT._.gui_settings_filewriting_description());
        this.addPair(_GUI._.gui_config_download_crc(), autoCRC);

        this.addHeader(_GUI._.gui_config_various(), NewTheme.I().getIcon("settings", 32));
        this.addPair(_GUI._.gui_config_simple_container(), simpleContainer);
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("home", 32);
    }

    @Override
    public void save() {
        GeneralSettings st = JsonConfig.create(GeneralSettings.class);
        st.setDefaultDownloadFolder(downloadFolder.getText());
        st.setCreatePackageNameSubFolderEnabled(subfolder.isSelected());
        st.setHashCheckEnabled(autoCRC.isSelected());
        st.setAutoOpenContainerAfterDownload(simpleContainer.isSelected());
    }

    @Override
    public void updateContents() {
        GeneralSettings st = JsonConfig.create(GeneralSettings.class);
        downloadFolder.setText(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
        subfolder.setSelected(st.isCreatePackageNameSubFolderEnabled());
        autoCRC.setSelected(st.isHashCheckEnabled());
        this.simpleContainer.setSelected(st.isAutoOpenContainerAfterDownload());
    }
}