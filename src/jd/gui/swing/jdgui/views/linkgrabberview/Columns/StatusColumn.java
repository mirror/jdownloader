package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTable;

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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        return true;
    }

}
