//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.downloads;

import jd.controlling.DownloadController;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.downloads.columns.DateAddedColumn;
import jd.gui.swing.jdgui.views.downloads.columns.DateFinishedColumn;
import jd.gui.swing.jdgui.views.downloads.columns.FileColumn;
import jd.gui.swing.jdgui.views.downloads.columns.HosterColumn;
import jd.gui.swing.jdgui.views.downloads.columns.LoadedColumn;
import jd.gui.swing.jdgui.views.downloads.columns.ProgressColumn;
import jd.gui.swing.jdgui.views.downloads.columns.ProxyColumn;
import jd.gui.swing.jdgui.views.downloads.columns.RemainingColumn;
import jd.gui.swing.jdgui.views.downloads.columns.SizeColumn;
import jd.gui.swing.jdgui.views.downloads.columns.StatusColumn;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;

public class DownloadJTableModel extends JDTableModel {

    private static final long serialVersionUID = 1L;

    public DownloadJTableModel(String configname) {
        super(configname);
    }

    protected void initColumns() {
        this.addColumn(new FileColumn(_GUI._.gui_treetable_name(), this));
        this.addColumn(new SizeColumn(_GUI._.gui_treetable_size(), this));
        this.addColumn(new LoadedColumn(_GUI._.gui_treetable_loaded(), this));
        this.addColumn(new RemainingColumn(_GUI._.gui_treetable_remaining(), this));
        this.addColumn(new HosterColumn(_GUI._.gui_treetable_hoster(), this));
        this.addColumn(new DateAddedColumn(_GUI._.gui_treetable_added(), this));
        this.addColumn(new DateFinishedColumn(_GUI._.gui_treetable_finished(), this));
        this.addColumn(new StatusColumn(_GUI._.gui_treetable_status(), this));
        this.addColumn(new ProgressColumn(_GUI._.gui_treetable_progress(), this));
        this.addColumn(new ProxyColumn(_GUI._.gui_treetable_proxy(), this));
    }

    public void refreshModel() {
        synchronized (DownloadController.ACCESSLOCK) {
            list.clear();
            for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                list.add(fp);
                if (fp.isExpanded()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        list.add(dl);
                    }
                }
            }
        }
    }
}