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

package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;
import javax.swing.JTable;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class StatusColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private LinkGrabberFilePackage fp;
    private String strOnline;
    private String strOffline;
    private String strUnchecked;
    private String strUnCheckable;
    private StringBuilder sb = new StringBuilder();
    private ImageIcon imgFinished;
    private ImageIcon imgFailed;
    private ImageIcon imgPriorityS;
    private ImageIcon imgPriority1;
    private ImageIcon imgPriority2;
    private ImageIcon imgPriority3;
    private ImageIcon imgUnknown;

    public StatusColumn(String name, JDTableModel table) {
        super(name, table);
        strOnline = JDL.L("linkgrabber.onlinestatus.online", "online");
        strOffline = JDL.L("linkgrabber.onlinestatus.offline", "offline");
        strUnchecked = JDL.L("linkgrabber.onlinestatus.unchecked", "not checked");
        strUnCheckable = JDL.L("linkgrabber.onlinestatus.uncheckable", "temp. uncheckable");
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
        imgPriorityS = JDTheme.II("gui.images.priority-1", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        imgUnknown = JDTheme.II("gui.images.help", 16, 16);
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof LinkGrabberFilePackage) {
            fp = (LinkGrabberFilePackage) value;
            int failedCount = fp.countFailedLinks(false);
            int size = fp.getDownloadLinks().size();
            if (failedCount > 0) {
                value = JDL.LF("gui.linkgrabber.packageofflinepercent", "%s offline", JDUtilities.getPercent(failedCount, size));
            } else {
                value = "";
            }
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setBorder(null);
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            clearSB();
            if (!dLink.isAvailabilityStatusChecked()) {
                ((JRendererLabel) co).setIcon(null);
                sb.append(strUnchecked);
            } else {
                if (dLink.isAvailable()) {
                    sb.append(strOnline);
                    if (dLink.getPriority() != 0) {
                        switch (dLink.getPriority()) {
                        case 0:
                        default:
                            break;
                        case -1:
                            ((JRendererLabel) co).setIcon(imgPriorityS);
                            break;
                        case 1:
                            ((JRendererLabel) co).setIcon(imgPriority1);
                            break;
                        case 2:
                            ((JRendererLabel) co).setIcon(imgPriority2);
                            break;
                        case 3:
                            ((JRendererLabel) co).setIcon(imgPriority3);
                            break;
                        }
                    } else {
                        switch (dLink.getAvailableStatus()) {
                        case UNCHECKABLE:
                            ((JRendererLabel) co).setIcon(this.imgUnknown);
                            clearSB();
                            sb.append(strUnCheckable);
                            break;
                        default:
                            ((JRendererLabel) co).setIcon(imgFinished);
                        }
                    }
                } else {
                    ((JRendererLabel) co).setIcon(imgFailed);
                    sb.append(strOffline);
                }
            }
            if (dLink.getLinkStatus().getErrorMessage() != null && dLink.getLinkStatus().getErrorMessage().trim().length() > 0) {
                sb.append(">" + dLink.getLinkStatus().getErrorMessage());
            } else if (dLink.getLinkStatus().getStatusString() != null && dLink.getLinkStatus().getStatusString().trim().length() > 0) {
                sb.append(">" + dLink.getLinkStatus().getStatusString());
            }
            ((JRendererLabel) co).setText(sb.toString());
            ((JRendererLabel) co).setBorder(null);
        }
        return co;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    public Object getCellEditorValue() {
        return null;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity());
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
                            if (aa.countFailedLinks(false) == bb.countFailedLinks(false)) return 0;
                            return aa.countFailedLinks(false) < bb.countFailedLinks(false) ? -1 : 1;
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
                                DownloadLink bb = b;
                                if (sortingToggle) {
                                    aa = b;
                                    bb = a;
                                }
                                if (aa.isAvailabilityStatusChecked() && aa.isAvailable() && bb.isAvailabilityStatusChecked() && bb.isAvailable()) return 0;
                                if (aa.isAvailabilityStatusChecked() && aa.isAvailable()) return 1;
                                return -1;
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
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        return true;
    }

}
