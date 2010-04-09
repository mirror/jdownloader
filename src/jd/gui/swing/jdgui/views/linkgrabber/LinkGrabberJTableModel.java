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

package jd.gui.swing.jdgui.views.linkgrabber;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.linkgrabber.columns.FileColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.HosterColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.PartColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.RequestTimeColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.SizeColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.StatusColumn;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class LinkGrabberJTableModel extends JDTableModel {

    private static final long serialVersionUID = 896882146491584908L;

    public LinkGrabberJTableModel(String configname) {
        super(configname);
    }

    protected void initColumns() {
        this.addColumn(new FileColumn(JDL.L("gui.linkgrabber.header.packagesfiles", "Pakete/Dateien"), this));
        this.addColumn(new PartColumn(JDL.L("gui.treetable.header.part", "Part"), this));
        this.addColumn(new SizeColumn(JDL.L("gui.treetable.header.size", "Größe"), this));
        this.addColumn(new HosterColumn(JDL.L("gui.treetable.hoster", "Anbieter"), this));
        this.addColumn(new StatusColumn(JDL.L("gui.treetable.status", "Status"), this));
        this.addColumn(new RequestTimeColumn(JDL.L("gui.treetable.requesttime", "RequestTime"), this));
    }

    public void refreshModel() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                synchronized (list) {
                    list.clear();
                    for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                        list.add(fp);
                        if (fp.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinks()) {
                                list.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }

}
