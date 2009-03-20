package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.nutils.JDImage;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.renderer.PainterAware;

public class LinkGrabberV2TreeTableRenderer extends DefaultTableRenderer implements PainterAware {

    private static final long serialVersionUID = -3912572910439565199L;

    private Component co;

    private DownloadLink dLink;

    private LinkGrabberV2FilePackage fp;

    private LinkGrabberV2TreeTable table;

    private ImageIcon icon_fp_closed;

    private ImageIcon icon_fp_open;

    private ImageIcon icon_link;

    private StringBuilder sb = new StringBuilder();

    private Border leftGap;

    private Painter painter;

    private String strOnline;
    private String strOffline;
    private String strUnchecked;
    private String strExists;

    public LinkGrabberV2TreeTableRenderer(LinkGrabberV2TreeTable linkgrabberTreeTable) {

        icon_link = new ImageIcon(JDImage.getImage(JDTheme.V("gui.images.link")));

        icon_fp_open = new ImageIcon(JDImage.getImage(JDTheme.V("gui.images.package_closed")));

        icon_fp_closed = new ImageIcon(JDImage.getImage(JDTheme.V("gui.images.package_opened")));

        table = linkgrabberTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        initLocale();
    }

    private void initLocale() {
        strOnline = "online";
        strOffline = "offline";
        strExists = " (already in list)";
        strUnchecked = "unchecked";
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getColumn(column).getModelIndex();
        if (value instanceof LinkGrabberV2FilePackage) {
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
        case LinkGrabberV2TreeTableModel.COL_PACK_FILE:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(icon_link);
            ((JRendererLabel) co).setText(dLink.getName());
            ((JRendererLabel) co).setBorder(leftGap);
            return co;
        case LinkGrabberV2TreeTableModel.COL_SIZE:
            value = dLink.getDownloadSize() > 0 ? JDUtilities.formatBytesToMB(dLink.getDownloadSize()) : "~";
            break;
        case LinkGrabberV2TreeTableModel.COL_HOSTER:
            value = dLink.getPlugin().getHost();
            break;
        case LinkGrabberV2TreeTableModel.COL_STATUS:
            clearSB();
            if (!dLink.isAvailabilityChecked()) {
                sb.append(strUnchecked);
            } else {
                if (dLink.isAvailable()) {
                    sb.append(strOnline);
                } else {
                    sb.append(strOffline);
                }
            }
            if (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
                sb.append(strExists);
            }
            value = sb.toString();
            break;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private Component getFilePackageV2Cell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        fp = (LinkGrabberV2FilePackage) value;
        switch (column) {
        case LinkGrabberV2TreeTableModel.COL_PACK_FILE:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName());
            ((JRendererLabel) co).setIcon(fp.getBooleanProperty(LinkGrabberV2TreeTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
            ((JRendererLabel) co).setBorder(null);
            return co;
        case LinkGrabberV2TreeTableModel.COL_SIZE:
            value = "";
            break;
        case LinkGrabberV2TreeTableModel.COL_HOSTER:
            value = fp.getHoster();
            break;
        case LinkGrabberV2TreeTableModel.COL_STATUS:
            value = "";
            break;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity() - 1);

    }

    public Painter getPainter() {
        // TODO Auto-generated method stub
        return painter;
    }

    public void setPainter(Painter painter) {
        this.painter = painter;

    }

}
