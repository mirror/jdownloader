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

package jd.gui.swing.jdgui.views.linkgrabber.columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;

import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.gui.translate._GUI;

public class HosterColumn extends JDTableColumn {

    private static final long      serialVersionUID = 2228210790952050305L;
    private DownloadLink           dLink;
    private LinkGrabberFilePackage fp;
    private JRendererLabel         jlr;
    private String                 strLoadingFrom;

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        strLoadingFrom = _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_loadingFrom() + " ";
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof LinkGrabberFilePackage) {
            fp = (LinkGrabberFilePackage) value;
            jlr.setText(fp.getHoster());
            jlr.setIcon(null);
            jlr.setToolTipText(strLoadingFrom + fp.getHoster());
        } else {
            dLink = (DownloadLink) value;
            jlr.setToolTipText(dLink.getDownloadLinkInfo().getLoadingFrom());
            jlr.setText(dLink.getDefaultPlugin().getSessionInfo());
            jlr.setIcon(dLink.getDefaultPlugin().getHosterIconScaled());
        }
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        /*
         * LinkGrabber hat nur null(Header) oder ne
         * ArrayList(LinkGrabberFilePackage)
         */
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<LinkGrabberFilePackage> packages = null;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                packages = LinkGrabberController.getInstance().getPackages();
                if (obj == null && packages.size() > 1) {
                    /* header, sortiere die packages nach namen */
                    Collections.sort(packages, new Comparator<LinkGrabberFilePackage>() {
                        public int compare(LinkGrabberFilePackage a, LinkGrabberFilePackage b) {
                            LinkGrabberFilePackage aa = a;
                            LinkGrabberFilePackage bb = b;
                            if (sortingToggle) {
                                aa = b;
                                bb = a;
                            }
                            return aa.getHoster().compareToIgnoreCase(bb.getHoster());
                        }
                    });
                } else {
                    /*
                     * in obj stecken alle selektierten packages, sortiere die
                     * links nach namen
                     */
                    if (obj != null) packages = (ArrayList<LinkGrabberFilePackage>) obj;
                    for (LinkGrabberFilePackage fp : packages) {
                        Collections.sort(fp.getDownloadLinks(), new Comparator<DownloadLink>() {
                            public int compare(DownloadLink a, DownloadLink b) {
                                DownloadLink aa = b;
                                DownloadLink bb = a;
                                if (sortingToggle) {
                                    aa = a;
                                    bb = b;
                                }
                                return aa.getHost().compareToIgnoreCase(bb.getHost());
                            }
                        });
                    }
                }
            }
        }
        /* inform LinkGrabberController that structure changed */
        LinkGrabberController.getInstance().throwRefresh();
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof LinkGrabberFilePackage) return ((LinkGrabberFilePackage) obj).countEnabledLinks(false) > 0;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        return true;
    }

}