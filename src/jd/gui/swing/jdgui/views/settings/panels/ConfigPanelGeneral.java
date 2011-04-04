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
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.jdownloader.extensions.AbstractConfigPanel;
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

        this.addHeader(JDL.L("gui.config.general.downloaddirectory", "Download directory"), JDTheme.II("gui.images.userhome", 32, 32));
        this.addDescription(JDT._.gui_settings_downloadpath_description());
        this.add(downloadFolder);
        this.addPair(JDL.L("gui.config.general.createsubfolders", "Create Subfolder with packagename if possible"), subfolder);

        /* File Writing */
        autoCRC = new Checkbox();
        this.addHeader(JDL.L("gui.config.download.write", "File writing"), JDTheme.II("gui.images.save", 32, 32));
        this.addDescription(JDT._.gui_settings_filewriting_description());
        this.addPair(JDL.L("gui.config.download.crc", "SFV/CRC check when possible"), autoCRC);

    }

    @Override
    public ImageIcon getIcon() {
        return JDTheme.II(getIconKey(), 32, 32);
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }
}
