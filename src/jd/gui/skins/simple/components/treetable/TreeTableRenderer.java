package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreePath;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class TreeTableRenderer extends DefaultTableCellRenderer {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3912572910439565199L;

    private JLabel label;

    private JProgressBar progress;
    private static Color PACKAGE_PROGRESS_COLOR;

    private static Color ERROR_PROGRESS_COLOR;

    private static Color DONE_COLOR;

    private static Color INACTIVE_PROGRESS_COLOR;

    private static Color ACTIVE_PROGRESS_COLOR;
    private static Color PACKAGE_PROGRESS_COLOR_FONT_A;

    private static Color ERROR_PROGRESS_COLOR_FONT_A;

    private static Color DONE_COLOR_FONT_A;

    private static Color INACTIVE_PROGRESS_COLOR_FONT_A;

    private static Color ACTIVE_PROGRESS_COLOR_FONT_A;

    private static Color PACKAGE_PROGRESS_COLOR_FONT_B;

    private static Color ERROR_PROGRESS_COLOR_FONT_B;

    private static Color DONE_COLOR_FONT_B;

    private static Color INACTIVE_PROGRESS_COLOR_FONT_B;

    private static Color ACTIVE_PROGRESS_COLOR_FONT_B;

    private static Color FONT_COLOR;

    // private static Color FONT_ODD_ROW_COLOR;
    //
    // private static Color FONT_SELECTED_ROW_COLOR;
    //
    // private static Color FONT_PACKAGE_ROW_COLOR;
    //
    // private static Color FONT_COLOR_DONE;
    //
    // private static Color FONT_COLOR_DISABLED;
    // private static Color FONT_COLOR_ERROR;

    private DownloadLink dLink;

    private FilePackage fp;
    private TreeProgressBarUI ui;
    private DecimalFormat c = new DecimalFormat("0.00");

    private Component co;

    private DownloadTreeTable treeTable;

    private TreePath path;

    private DownloadLink link;

    private Double version;;

    TreeTableRenderer(DownloadTreeTable downloadTreeTable) {

        this.treeTable = downloadTreeTable;
        FONT_COLOR = JDTheme.C("gui.color.downloadlist.font", "000000");

        //
        // FONT_ODD_ROW_COLOR = JDTheme.C("gui.color.downloadlist.font_row_b",
        // "000000");
        //
        // FONT_SELECTED_ROW_COLOR =
        // JDTheme.C("gui.color.downloadlist.font_row_selected", "ff");
        //
        // FONT_PACKAGE_ROW_COLOR =
        // JDTheme.C("gui.color.downloadlist.font_row_package", "ffff");
        //
        // FONT_COLOR_DONE = JDTheme.C("gui.color.downloadlist.font_row_done",
        // "000000");
        //
        // FONT_COLOR_ERROR = JDTheme.C("gui.color.downloadlist.font_row_error",
        // "000000");
        // FONT_COLOR_DISABLED=
        // JDTheme.C("gui.color.downloadlist.font_row_disabled", "999999");
        //   
        //
        // FONT_COLOR_WAIT = JDTheme.C("gui.color.downloadlist.font_row_wait",
        // "000000");
        //
        //       

        PACKAGE_PROGRESS_COLOR = JDTheme.C("gui.color.downloadlist.package_progress", "94baff");
        ERROR_PROGRESS_COLOR = JDTheme.C("gui.color.downloadlist.progress_error", "FF0000");
        DONE_COLOR = JDTheme.C("gui.color.downloadlist.progress_done", "94baff");
        INACTIVE_PROGRESS_COLOR = JDTheme.C("gui.color.downloadlist.progress_inactive", "AAAAAA");
        ACTIVE_PROGRESS_COLOR = JDTheme.C("gui.color.downloadlist.progress_active", "94baff");

        PACKAGE_PROGRESS_COLOR_FONT_A = JDTheme.C("gui.color.downloadlist.package_progress_font_a", "000000");
        ERROR_PROGRESS_COLOR_FONT_A = JDTheme.C("gui.color.downloadlist.progress_error_font_a", "000000");
        DONE_COLOR_FONT_A = JDTheme.C("gui.color.downloadlist.progress_done_font_a", "000000");
        INACTIVE_PROGRESS_COLOR_FONT_A = JDTheme.C("gui.color.downloadlist.progress_inactive_font_a", "000000");
        ACTIVE_PROGRESS_COLOR_FONT_A = JDTheme.C("gui.color.downloadlist.progress_active_font_a", "000000");

        PACKAGE_PROGRESS_COLOR_FONT_B = JDTheme.C("gui.color.downloadlist.package_progress_font_b", "555555");
        ERROR_PROGRESS_COLOR_FONT_B = JDTheme.C("gui.color.downloadlist.progress_error_font_b", "555555");
        DONE_COLOR_FONT_B = JDTheme.C("gui.color.downloadlist.progress_done_font_b", "555555");
        INACTIVE_PROGRESS_COLOR_FONT_B = JDTheme.C("gui.color.downloadlist.progress_inactive_font_b", "555555");
        ACTIVE_PROGRESS_COLOR_FONT_B = JDTheme.C("gui.color.downloadlist.progress_active_font_b", "555555");

        this.label = new JLabel();
        this.label.setOpaque(false);

        this.progress = new JProgressBar();
this.version=JDUtilities.getJavaVersion();
JDUtilities.getLogger().info("Version "+version);
if(version>=1.6){
        ui = new TreeProgressBarUI();
        progress.setUI(ui);
        ui.setSelectionForeground(Color.BLACK);
}

        progress.setBorderPainted(false);
        progress.setOpaque(false);

    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // if (column == DownloadTreeTableModel.COL_PROGRESS) {
        if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            if (dLink.getRemainingWaittime() == 0 && (int) dLink.getDownloadCurrent() > 0) {

                if (!dLink.isInProgress()) {
                    progress.setString("");
                    if (dLink.getStatus() == DownloadLink.STATUS_DONE) {
                        progress.setForeground(DONE_COLOR);
                        if(ui!=null)ui.setSelectionForeground(DONE_COLOR_FONT_A);
                        if(ui!=null) ui.setSelectionBackground(DONE_COLOR_FONT_B);
                        progress.setString("- 100 % -");
                    } else {
                        progress.setForeground(INACTIVE_PROGRESS_COLOR);
                        if(ui!=null) ui.setSelectionForeground(INACTIVE_PROGRESS_COLOR_FONT_A);
                        if(ui!=null)  ui.setSelectionBackground(INACTIVE_PROGRESS_COLOR_FONT_B);
                    }
                    progress.setMaximum(Math.max(1, (int) dLink.getDownloadMax()));
                    progress.setStringPainted(true);

                    progress.setValue((int) dLink.getDownloadCurrent());

                } else {
                    progress.setMaximum(Math.max(1, (int) dLink.getDownloadMax()));
                    progress.setStringPainted(true);
                    progress.setForeground(ACTIVE_PROGRESS_COLOR);
                    if(ui!=null) ui.setSelectionForeground(ACTIVE_PROGRESS_COLOR_FONT_A);
                    if(ui!=null) ui.setSelectionBackground(ACTIVE_PROGRESS_COLOR_FONT_B);
                    progress.setValue((int) dLink.getDownloadCurrent());
                    progress.setString(c.format(10000 * progress.getPercentComplete() / 100.0) + "% (" + JDUtilities.formatBytesToMB(progress.getValue()) + "/" + JDUtilities.formatBytesToMB(progress.getMaximum()) + ")");

                }

                return progress;
            } else if (dLink.getRemainingWaittime() > 0 && dLink.getWaitTime() >= dLink.getRemainingWaittime()) {
                progress.setMaximum(dLink.getWaitTime());
                progress.setForeground(ERROR_PROGRESS_COLOR);
                if(ui!=null)    ui.setSelectionForeground(ERROR_PROGRESS_COLOR_FONT_A);
                if(ui!=null)   ui.setSelectionBackground(ERROR_PROGRESS_COLOR_FONT_B);
                progress.setStringPainted(true);
                progress.setValue((int) dLink.getRemainingWaittime());
                progress.setString(c.format(10000 * progress.getPercentComplete() / 100.0) + "% (" + progress.getValue() / 1000 + "/" + progress.getMaximum() / 1000 + " sek)");
                return progress;
            }
            label.setText("");
            return label;
        } else if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            if (fp.getPercent() == 0.0) {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setStringPainted(true);
                progress.setForeground(PACKAGE_PROGRESS_COLOR);
                if(ui!=null)   ui.setSelectionForeground(PACKAGE_PROGRESS_COLOR_FONT_A);
                if(ui!=null)  ui.setSelectionBackground(PACKAGE_PROGRESS_COLOR_FONT_B);

                progress.setValue(fp.getTotalKBLoaded());
                progress.setString("- 0% -");

            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setStringPainted(true);
                progress.setForeground(PACKAGE_PROGRESS_COLOR);
             if(ui!=null)   ui.setSelectionForeground(PACKAGE_PROGRESS_COLOR_FONT_A);
             if(ui!=null)  ui.setSelectionBackground(PACKAGE_PROGRESS_COLOR_FONT_B);

                progress.setValue(fp.getTotalKBLoaded());
                progress.setString(c.format(fp.getPercent()) + "% (" + JDUtilities.formatKbReadable(progress.getValue()) + "/" + JDUtilities.formatKbReadable(Math.max(1, fp.getTotalEstimatedPackageSize())) + ")");
            }
            return progress;
        }

        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        co.setForeground(FONT_COLOR);
        co.setBackground(FONT_COLOR);

        // if (treeTable.isRowSelected(row)) {
        // co.setForeground(FONT_SELECTED_ROW_COLOR);
        // return co;
        // }
        //
        // path = treeTable.getPathForRow(row);
        // if (path == null) return super.getTableCellRendererComponent(table,
        // value, isSelected, hasFocus, row, column);
        //
        // if (path.getLastPathComponent() instanceof DownloadLink) {
        // link = (DownloadLink) path.getLastPathComponent();
        // if (link.getStatus() == DownloadLink.STATUS_DONE) {
        // co.setForeground(FONT_COLOR_DONE);
        //               
        // } else if (link.getRemainingWaittime() > 0 && link.getWaitTime() >=
        // link.getRemainingWaittime()) {
        // co.setForeground(FONT_COLOR_WAIT);
        //                
        // } else if(link.isInProgress() ||
        // link.getStatus()==DownloadLink.STATUS_TODO){
        // if (row % 2 == 0)
        // co.setForeground(FONT_EVEN_ROW_COLOR);
        // else
        // co.setForeground(FONT_ODD_ROW_COLOR);
        // } else if(link.isEnabled()){
        //                
        // co.setForeground(FONT_COLOR_DISABLED);
        // }else{
        // co.setForeground(FONT_COLOR_ERROR);
        // }
        // JDUtilities.getLogger().info(link+" : "+co.getForeground()+"");
        // } else if (path.getLastPathComponent() instanceof FilePackage) {
        // co.setForeground(FONT_PACKAGE_ROW_COLOR);
        // }

        return co;

    }

}