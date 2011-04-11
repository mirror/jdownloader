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

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;

import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class ConfigPanelGeneral extends AbstractConfigPanel {

    public String getTitle() {
        return JDT._.gui_settings_general_title();
    }

    public String getIconKey() {
        return "gui.images.configuration";
    }

    private static final long serialVersionUID = 3383448498625377495L;

    private FolderChooser     downloadFolder;

    private Checkbox          subfolder;

    private Checkbox          autoCRC;

    public ConfigPanelGeneral() {
        super();

        downloadFolder = new FolderChooser("downloadfolder");
        subfolder = new Checkbox();

        this.addHeader(T._.gui_config_general_downloaddirectory(), Theme.getIcon("settings/downloadpath", 32));
        this.addDescription(JDT._.gui_settings_downloadpath_description());
        this.add(downloadFolder);
        this.addPair(T._.gui_config_general_createsubfolders(), subfolder);

        /* File Writing */
        autoCRC = new Checkbox();
        this.addHeader(T._.gui_config_download_write(), Theme.getIcon("settings/hashsum", 32));
        this.addDescription(JDT._.gui_settings_filewriting_description());
        this.addPair(T._.gui_config_download_crc(), autoCRC);

    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("settings/home", 32);
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }
}