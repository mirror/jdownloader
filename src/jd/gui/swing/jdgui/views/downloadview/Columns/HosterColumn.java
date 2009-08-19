package jd.gui.swing.jdgui.views.downloadview.Columns;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTable;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.views.downloadview.StatusLabel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class HosterColumn extends JDTableColumn {

    /**
     * 
     */

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.downloadview.TableRenderer.";

    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private FilePackage fp;
    private StatusLabel statuspanel;
    private int counter = 0;
    private ImageIcon imgResume;
    private ImageIcon imgPremium;
    private StringBuilder sb = new StringBuilder();
    private String strResume;
    private String strPremium;

    private String strLoadingFrom;

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
        statuspanel = new StatusLabel();
        imgResume = JDTheme.II("gui.images.resume", 16, 16);
        imgPremium = JDTheme.II("gui.images.premium", 16, 16);
        strResume = JDL.L(JDL_PREFIX + "resume", "Resumable download");
        strPremium = JDL.L(JDL_PREFIX + "premium", "Loading with Premium");
        strLoadingFrom = JDL.L(JDL_PREFIX + "loadingFrom", "Loading from") + " ";
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
            value = fp.getHoster();
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            statuspanel.setBackground(co.getBackground());
            statuspanel.setEnabled(co.isEnabled());
            statuspanel.setForeground(co.getForeground());
            statuspanel.setText(dLink.getLinkStatus().getStatusString());
            counter = 0;
            if (dLink.getPlugin() == null) {
                statuspanel.setText("plugin missing");
            } else {
                if (dLink.getPlugin().hasHosterIcon()) {
                    statuspanel.setText(dLink.getPlugin().getSessionInfo());
                    statuspanel.setIcon(-1, dLink.getPlugin().getHosterIcon(), strLoadingFrom + dLink.getPlugin().getHost());
                } else {
                    clearSB();
                    sb.append(dLink.getPlugin().getHost());
                    sb.append(dLink.getPlugin().getSessionInfo());
                    statuspanel.setText(sb.toString());
                }
            }
            if (dLink.getTransferStatus().usesPremium()) {
                statuspanel.setIcon(counter, imgPremium, strPremium);
                counter++;
            }
            if (dLink.getTransferStatus().supportsResume()) {
                statuspanel.setIcon(counter, imgResume, strResume);
                counter++;
            }
            statuspanel.clearIcons(counter);
            statuspanel.setBorder(null);
            co = statuspanel;
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

    private void clearSB() {
        sb.delete(0, sb.capacity());
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

}
