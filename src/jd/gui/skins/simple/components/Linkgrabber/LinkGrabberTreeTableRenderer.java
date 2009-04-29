package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;

public class LinkGrabberTreeTableRenderer extends DefaultTableRenderer {

    private static final long serialVersionUID = -3912572910439565199L;

    private Component co;

    private DownloadLink dLink;

    private LinkGrabberFilePackage fp;

    private LinkGrabberTreeTable table;

    private ImageIcon icon_fp_closed;

    private ImageIcon icon_fp_open;

    private StringBuilder sb = new StringBuilder();
    private Border leftGap;

    private String strOnline;
    private String strOffline;
    private String strUnchecked;

    private ImageIcon imgFinished;

    private ImageIcon imgFailed;

    private ImageIcon icon_fp_error;

    public LinkGrabberTreeTableRenderer(LinkGrabberTreeTable linkgrabberTreeTable) {

        table = linkgrabberTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        initLocale();
        initIcons();
    }

    private void initLocale() {
        strOnline = "online";
        strOffline = "offline";
        strUnchecked = "unchecked";
    }

    private void initIcons() {
        icon_fp_open = JDTheme.II("gui.images.package_closed", 16, 16);
        icon_fp_error = JDTheme.II("gui.images.package_error", 16, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_opened", 16, 16);
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
    }

    //@Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getColumn(column).getModelIndex();
        if (value instanceof LinkGrabberFilePackage) {
            return getFilePackageV2Cell(table, value, isSelected, hasFocus, row, column);
        } else if (value instanceof DownloadLink) {
            return getDownloadLinkCell(table, value, isSelected, hasFocus, row, column);
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

    }

    private Component getDownloadLinkCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        dLink = (DownloadLink) value;
        switch (column) {
        case LinkGrabberTreeTableModel.COL_PACK_FILE:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(dLink.getIcon());
            ((JRendererLabel) co).setText(dLink.getName());
            ((JRendererLabel) co).setBorder(leftGap);
            return co;
        case LinkGrabberTreeTableModel.COL_SIZE:
            value = dLink.getDownloadSize() > 0 ? Formatter.formatReadable(dLink.getDownloadSize()) : "~";
            break;
        case LinkGrabberTreeTableModel.COL_HOSTER:
            value = dLink.getPlugin().getHost();
            
            break;
        case LinkGrabberTreeTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            clearSB();
            if (!dLink.isAvailabilityChecked()) {
                ((JRendererLabel) co).setIcon(null);
                sb.append(strUnchecked);
            } else {
                if (dLink.isAvailable()) {
                    ((JRendererLabel) co).setIcon(imgFinished);
                    sb.append(strOnline);
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

    private Component getFilePackageV2Cell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        fp = (LinkGrabberFilePackage) value;
        switch (column) {
        case LinkGrabberTreeTableModel.COL_PACK_FILE:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName());
            if (fp.countFailedLinks(false) > 0) {
                ((JRendererLabel) co).setIcon(icon_fp_error);
            } else {
                ((JRendererLabel) co).setIcon(fp.getBooleanProperty(LinkGrabberTreeTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
            }
            ((JRendererLabel) co).setBorder(null);
            return co;
        case LinkGrabberTreeTableModel.COL_SIZE:
            value = fp.getDownloadSize(false) > 0 ? Formatter.formatReadable(fp.getDownloadSize(false)) : "~";
            break;
        case LinkGrabberTreeTableModel.COL_HOSTER:
            value = fp.getHoster();
         
            break;
        case LinkGrabberTreeTableModel.COL_STATUS:
            int failedCount = fp.countFailedLinks(false);
            int size = fp.size();
            if (failedCount > 0) {
                value = JDLocale.LF("gui.linkgrabber.packageofflinepercent", "%s offline", JDUtilities.getPercent(failedCount, size));
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
        sb.delete(0, sb.capacity() - 1);

    }

}
