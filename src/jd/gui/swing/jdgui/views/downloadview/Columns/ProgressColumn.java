package jd.gui.swing.jdgui.views.downloadview.Columns;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.views.downloadview.JDProgressBar;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class ProgressColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private JDProgressBar progress;

    private String strPluginDisabled;

    private String strPluginError;
    private Border ERROR_BORDER;
    private Color COL_PROGRESS_ERROR = new Color(0xCC3300);
    private StringBuilder sb = new StringBuilder();
    private Color COL_PROGRESS_NORMAL = null;
    private FilePackage fp;

    public ProgressColumn(String name, JDTableModel table) {
        super(name, table);
        progress = new JDProgressBar();
        progress.setStringPainted(true);
        progress.setOpaque(true);
        COL_PROGRESS_NORMAL = progress.getForeground();
        strPluginDisabled = JDL.L("gui.downloadlink.plugindisabled", "[Plugin disabled]");
        strPluginError = JDL.L("gui.treetable.error.plugin", "Plugin error");
        ERROR_BORDER = BorderFactory.createLineBorder(COL_PROGRESS_ERROR);
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
        if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            progress.setBorder(null);
            progress.setString(null);
            if (fp.isFinished()) {
                progress.setMaximum(100);
                progress.setValue(100);
            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setValue(fp.getTotalKBLoaded());
            }
            progress.setForeground(COL_PROGRESS_NORMAL);
            return progress;
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            progress.setBorder(null);
            if (dLink.getPlugin() == null) {
                co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginError);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (!dLink.getPlugin().getWrapper().usePlugin()) {
                co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginDisabled);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (dLink.getPluginProgress() != null) {
                progress.setMaximum(dLink.getPluginProgress().getTotal());
                progress.setValue(dLink.getPluginProgress().getCurrent());
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && dLink.getPlugin().getRemainingHosterWaittime() > 0) || (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && dLink.getLinkStatus().getRemainingWaittime() > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                progress.setForeground(COL_PROGRESS_ERROR);
                progress.setBorder(ERROR_BORDER);
                progress.setString(Formatter.formatSeconds(dLink.getLinkStatus().getRemainingWaittime() / 1000));
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                return progress;
            } else if (dLink.getLinkStatus().isFinished()) {
                clearSB();
                sb.append((Formatter.formatReadable(Math.max(0, dLink.getDownloadSize()))));
                progress.setMaximum(100);
                progress.setString(sb.toString());
                progress.setValue(100);
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            } else if (dLink.getDownloadCurrent() > 0) {
                clearSB();
                sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                progress.setMaximum(dLink.getDownloadSize());
                progress.setString(sb.toString());
                progress.setValue(dLink.getDownloadCurrent());
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            } else if (dLink.getDownloadSize() > 0) {
                clearSB();
                sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                progress.setMaximum(dLink.getDownloadSize());
                progress.setString(sb.toString());
                progress.setValue(dLink.getDownloadCurrent());
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            }
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(null);
            ((JRendererLabel) co).setText("Unknown FileSize");
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

    @Override
    public boolean isSortable(Object obj) {
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
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity());
    }

}
