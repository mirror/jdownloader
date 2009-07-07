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

package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;

public class LinkGrabberTableRenderer extends DefaultTableRenderer {

    private static final long serialVersionUID = -3912572910439565199L;

    private Component co;

    private DownloadLink dLink;

    private LinkGrabberFilePackage fp;

    private LinkGrabberTable table;

    private ImageIcon icon_fp_closed;

    private ImageIcon icon_fp_open;

    private StringBuilder sb = new StringBuilder();
    private Border leftGap;

    private String strOnline;
    private String strOffline;
    private String strUnchecked;

    private ImageIcon imgFinished;

    private ImageIcon imgFailed;

    private ImageIcon imgFileFailed;

    private ImageIcon imgPriority1;

    private ImageIcon imgPriority2;

    private ImageIcon imgPriority3;

    private ImageIcon icon_fp_open_error;

    private ImageIcon icon_fp_closed_error;

    private ImageIcon imgPriorityS;

    private ImageIcon imgUnknown;

    private String strUnCheckable;

    public LinkGrabberTableRenderer(LinkGrabberTable linkgrabberTreeTable) {

        table = linkgrabberTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        initLocale();
        initIcons();
    }

    private void initLocale() {
        strOnline = JDL.L("linkgrabber.onlinestatus.online", "online");
        strOffline = JDL.L("linkgrabber.onlinestatus.offline", "offline");
        strUnchecked = JDL.L("linkgrabber.onlinestatus.unchecked", "not checked");
        strUnCheckable = JDL.L("linkgrabber.onlinestatus.uncheckable", "temp. uncheckable");
    }

    private void initIcons() {
        icon_fp_open = JDTheme.II("gui.images.package_opened_tree", 16, 16);
        icon_fp_open_error = JDTheme.II("gui.images.package_open_error_tree", 16, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_closed_tree", 16, 16);
        icon_fp_closed_error = JDTheme.II("gui.images.package_closed_error_tree", 16, 16);
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
        imgFileFailed = JDTheme.II("gui.images.offlinefile", 16, 16);
        imgPriorityS = JDTheme.II("gui.images.priority-1", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        imgUnknown = JDTheme.II("gui.images.help", 16, 16);
    }

    // @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getColumn(column).getModelIndex();
        if (value instanceof LinkGrabberFilePackage) {
            return getFilePackageCell(table, value, isSelected, hasFocus, row, column);
        } else if (value instanceof DownloadLink) {
            return getDownloadLinkCell(table, value, isSelected, hasFocus, row, column);
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

    }

    private Component getDownloadLinkCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        dLink = (DownloadLink) value;
        switch (column) {
        case LinkGrabberJTableModel.COL_PACK_FILE:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (dLink.isAvailabilityStatusChecked() && !dLink.isAvailable()) {
                ((JRendererLabel) co).setIcon(this.imgFileFailed);
            } else {
                ((JRendererLabel) co).setIcon(dLink.getIcon());
            }

            ((JRendererLabel) co).setText(dLink.getName());
            ((JRendererLabel) co).setBorder(leftGap);
            return co;
        case LinkGrabberJTableModel.COL_SIZE:
            value = dLink.getDownloadSize() > 0 ? Formatter.formatReadable(dLink.getDownloadSize()) : "~";
            break;
        case LinkGrabberJTableModel.COL_HOSTER:

            if (dLink.getPlugin().hasHosterIcon()) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                clearSB();
                sb.append(dLink.getPlugin().getHost());
                sb.append(dLink.getPlugin().getSessionInfo());
                ((JRendererLabel) co).setText(dLink.getPlugin().getSessionInfo());
                ((JRendererLabel) co).setIcon(dLink.getPlugin().getHosterIcon());
                ((JComponent) co).setToolTipText(sb.toString());

                ((JRendererLabel) co).setBorder(null);
                return co;

            } else {

                value = dLink.getPlugin().getHost();
            }

            break;
        case LinkGrabberJTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
            return co;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private Component getFilePackageCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        fp = (LinkGrabberFilePackage) value;
        switch (column) {
        case LinkGrabberJTableModel.COL_PACK_FILE:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName() + " [" + fp.size() + "]");
            if (fp.countFailedLinks(false) > 0) {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false) ? icon_fp_closed_error : icon_fp_open_error);
            } else {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
            }
            ((JRendererLabel) co).setBorder(null);
            return co;
        case LinkGrabberJTableModel.COL_SIZE:
            value = fp.getDownloadSize(false) > 0 ? Formatter.formatReadable(fp.getDownloadSize(false)) : "~";
            break;
        case LinkGrabberJTableModel.COL_HOSTER:
            value = fp.getHoster();

            break;
        case LinkGrabberJTableModel.COL_STATUS:
            int failedCount = fp.countFailedLinks(false);
            int size = fp.getDownloadLinks().size();
            if (failedCount > 0) {
                value = JDL.LF("gui.linkgrabber.packageofflinepercent", "%s offline", JDUtilities.getPercent(failedCount, size));
            } else {
                value = "";
            }
            break;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity());
    }

}
