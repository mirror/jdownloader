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

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.StatusLabel;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class StatusColumn extends JDTableColumn {

    private static final long serialVersionUID = 2228210790952050305L;

    private DownloadLink dLink;
    private LinkGrabberFilePackage fp;
    private String strOnline;
    private String strOffline;
    private String strUnchecked;
    private String strUncheckable;
    private StringBuilder sb = new StringBuilder();
    private ImageIcon imgOnline;
    private ImageIcon imgFailed;
    private ImageIcon imgPriorityS;
    private ImageIcon imgPriority1;
    private ImageIcon imgPriority2;
    private ImageIcon imgPriority3;
    private ImageIcon imgUncheckable;
    private String strPriorityS;
    private String strPriority1;
    private String strPriority2;
    private String strPriority3;
    private StatusLabel statuspanel;
    private int counter = 0;

    public StatusColumn(String name, JDTableModel table) {
        super(name, table);
        strOnline = JDL.L("linkgrabber.onlinestatus.online", "online");
        strOffline = JDL.L("linkgrabber.onlinestatus.offline", "offline");
        strUnchecked = JDL.L("linkgrabber.onlinestatus.unchecked", "not checked");
        strUncheckable = JDL.L("linkgrabber.onlinestatus.uncheckable", "temp. uncheckable");
        imgOnline = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
        imgPriorityS = JDTheme.II("gui.images.priority-1", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        imgUncheckable = JDTheme.II("gui.images.help", 16, 16);
        strPriorityS = JDL.L("gui.treetable.tooltip.priority-1", "Low Priority");
        strPriority1 = JDL.L("gui.treetable.tooltip.priority1", "High Priority");
        strPriority2 = JDL.L("gui.treetable.tooltip.priority2", "Higher Priority");
        strPriority3 = JDL.L("gui.treetable.tooltip.priority3", "Highest Priority");
        statuspanel = new StatusLabel();
        statuspanel.setBorder(null);
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
        counter = 0;
        if (value instanceof LinkGrabberFilePackage) {
            fp = (LinkGrabberFilePackage) value;
            int failedCount = fp.countFailedLinks(false);
            if (fp.countUncheckedLinks() > 0) {
                statuspanel.setText("", null);
            } else {
                int size = fp.getDownloadLinks().size();
                if (failedCount > 0) {
                    if (failedCount == size) {
                        statuspanel.setText(JDL.LF("gui.linkgrabber.packageofflinepercent", "%s offline", JDUtilities.getPercent(failedCount, size)), null);
                    } else {
                        statuspanel.setText(JDL.LF("gui.linkgrabber.packageofflinepercent", "%s offline", JDUtilities.getPercent(failedCount, size)) + "/" + JDL.LF("gui.linkgrabber.packageonlinepercent", "%s online", JDUtilities.getPercent(size - failedCount, size)), null);
                    }
                } else {
                    statuspanel.setText(JDL.L("gui.linkgrabber.packageonlineall", "All online"), null);
                }
            }
            if (fp.hasCustomIcon()) {
                statuspanel.setIcon(-1, fp.getCustomIcon(), null, fp.getCustomIconText());
            }
            statuspanel.clearIcons(counter);
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            clearSB();
            if (dLink.getLinkStatus().getErrorMessage() != null && dLink.getLinkStatus().getErrorMessage().trim().length() > 0) {
                sb.append('>').append(dLink.getLinkStatus().getErrorMessage());
            } else if (dLink.getLinkStatus().getStatusString() != null && dLink.getLinkStatus().getStatusString().trim().length() > 0) {
                sb.append('>').append(dLink.getLinkStatus().getStatusString());
            }
            statuspanel.setText(sb.toString(), null);
            if (!dLink.isAvailabilityStatusChecked()) {
                statuspanel.setText(strUnchecked + sb.toString(), null);
            } else {
                switch (dLink.getAvailableStatus()) {
                case FALSE:
                    statuspanel.setText(strOffline + sb.toString(), null);
                    statuspanel.setIcon(-1, imgFailed, strOffline, null);
                    break;
                case TRUE:
                    statuspanel.setText(strOnline + sb.toString(), null);
                    statuspanel.setIcon(-1, imgOnline, strOnline, null);
                    break;
                case UNCHECKABLE:
                    statuspanel.setText(strUncheckable + sb.toString(), null);
                    statuspanel.setIcon(-1, imgUncheckable, strUncheckable, null);
                    break;
                case UNCHECKED:
                    statuspanel.setText(strUnchecked + sb.toString(), null);
                    break;
                }
            }
            if (dLink.getPriority() != 0) {
                switch (dLink.getPriority()) {
                case -1:
                    statuspanel.setIcon(counter, imgPriorityS, null, strPriorityS);
                    counter++;
                    break;
                case 1:
                    statuspanel.setIcon(counter, imgPriority1, null, strPriority1);
                    counter++;
                    break;
                case 2:
                    statuspanel.setIcon(counter, imgPriority2, null, strPriority2);
                    counter++;
                    break;
                case 3:
                    statuspanel.setIcon(counter, imgPriority3, null, strPriority3);
                    counter++;
                    break;
                }
            }
            if (dLink.hasCustomIcon()) {
                statuspanel.setIcon(counter, dLink.getCustomIcon(), null, dLink.getCustomIconText());
                counter++;
            }
            statuspanel.clearIcons(counter);
        }
        return statuspanel;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
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
