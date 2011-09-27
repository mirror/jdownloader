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

package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import javax.swing.ImageIcon;

import jd.controlling.IOEQ;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class Packagizer extends AbstractConfigPanel {

    private static final long serialVersionUID = 1L;

    private PackagizerFilter  packagizer;

    public String getTitle() {
        return _GUI._.gui_config_linkgrabber_packagizer();
    }

    public Packagizer() {
        super();

        this.addHeader(getTitle(), NewTheme.I().getIcon("packagizer", 32));
        this.addDescriptionPlain(_JDT._.gui_settings_linkgrabber_packagizer_description());
        packagizer = new PackagizerFilter();
        add(packagizer);
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("packagizer", 32);
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {
        GeneralSettings st = JsonConfig.create(GeneralSettings.class);

        IOEQ.add(new Runnable() {

            public void run() {
                packagizer.getTable().getExtTableModel()._fireTableStructureChanged(PackagizerController.getInstance().list(), false);
            }

        }, true);

    }
}