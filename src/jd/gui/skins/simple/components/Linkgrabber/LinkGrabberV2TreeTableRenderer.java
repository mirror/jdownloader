package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
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

    private ImageIcon imgFinished;

    private ImageIcon imgFailed;

    private ImageIcon icon_fp_error;

    public LinkGrabberV2TreeTableRenderer(LinkGrabberV2TreeTable linkgrabberTreeTable) {

        table = linkgrabberTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        initLocale();
        initIcons();
    }

    private void initLocale() {
        strOnline = "online";
        strOffline = "offline";
        strExists = " (already in list)";
        strUnchecked = "unchecked";
    }

    private void initIcons() {
        icon_link = JDTheme.II("gui.images.link", 16, 16);

        icon_fp_open = JDTheme.II("gui.images.package_closed", 16, 16);
icon_fp_error=JDTheme.II("gui.images.package_error", 16, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_opened", 16, 16);
        imgFinished = JDTheme.II("gui.images.selected", 16, 16);
        imgFailed = JDTheme.II("gui.images.unselected", 16, 16);

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
            ((JRendererLabel) co).setIcon(dLink.getIcon());
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
            if (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
                sb.append(strExists);
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
        fp = (LinkGrabberV2FilePackage) value;
        switch (column) {
        case LinkGrabberV2TreeTableModel.COL_PACK_FILE:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName());
            boolean failed=false;
            for (DownloadLink dl : fp.getDownloadLinks()) {
                if (dl.isAvailabilityChecked()) {
                    if (!dl.isAvailable()||dl.getLinkStatus().isFailed()) {
                        failed=true;
                        break;
                    } 
                } 
            }
            if(failed){
                ((JRendererLabel) co).setIcon(icon_fp_error);
                
            }else{
                ((JRendererLabel) co).setIcon(fp.getBooleanProperty(LinkGrabberV2TreeTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
                   
            }
            ((JRendererLabel) co).setBorder(null);
            return co;
        case LinkGrabberV2TreeTableModel.COL_SIZE:
            value = fp.getDownloadSize() > 0 ? JDUtilities.formatBytesToMB(fp.getDownloadSize()) : "~";
            break;
        case LinkGrabberV2TreeTableModel.COL_HOSTER:
            value = fp.getHoster();
            break;
        case LinkGrabberV2TreeTableModel.COL_STATUS:

            int ok = 0;
            int failedCount = 0;
            int nc = 0;
            for (DownloadLink dl : fp.getDownloadLinks()) {
                if (dl.isAvailabilityChecked()) {
                    if (dl.isAvailable()) {
                        ok++;
                    } else {
                        failedCount++;
                    }
                } else {
                    nc++;
                }
            }
            if (failedCount > 0) {
                value = JDLocale.LF("gui.linkgrabber.packageofflinepercent", "%s offline", JDUtilities.getPercent(failedCount, ok + nc + failedCount));
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

    public Painter getPainter() {
        // TODO Auto-generated method stub
        return painter;
    }

    public void setPainter(Painter painter) {
        this.painter = painter;

    }

}
