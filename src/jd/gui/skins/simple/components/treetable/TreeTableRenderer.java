package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
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

    private DownloadTreeTable table;

    private DownloadLink dLink;

    private FilePackage fp;

    private TreeProgressBarUI ui;

    private DecimalFormat c = new DecimalFormat("0.00");

    private Component co;

    private MiniBar miniBar;

    TreeTableRenderer(DownloadTreeTable downloadTreeTable) {

        FONT_COLOR = JDTheme.C("gui.color.downloadlist.font", "000000");    

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

        this.table = downloadTreeTable;
        this.label = new JLabel();
        this.label.setOpaque(false);
        this.miniBar = new MiniBar();
        this.progress = new JProgressBar();
        if (JDUtilities.getJavaVersion() >= 1.6) {
            ui = new TreeProgressBarUI();
            progress.setUI(ui);
            ui.setSelectionForeground(Color.BLACK);
        }

        progress.setBorderPainted(false);
        progress.setOpaque(false);

    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        column = this.table.getColumn(column).getModelIndex();
        if (column == DownloadTreeTableModel.COL_STATUS && value instanceof FilePackage) {
            String label = "";
            FilePackage filePackage = (FilePackage) value;
            if (filePackage.getLinksInProgress() > 0) label = filePackage.getLinksInProgress() + "/" + filePackage.size() + " " + JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
            if (filePackage.getTotalDownloadSpeed() > 0) label = "[" + filePackage.getLinksInProgress() + "/" + filePackage.size() + "] " + "ETA " + JDUtilities.formatSeconds(filePackage.getETA()) + " @ " + JDUtilities.formatKbReadable(filePackage.getTotalDownloadSpeed() / 1024) + "/s";
            double pv;
            if (filePackage.getLinksFinished() == 0)
                pv = 1;
            else
                pv = 1.0 - (filePackage.getLinksFinished() / ((double) filePackage.size()));
            miniBar.setText(label);
            miniBar.setPercent(1.0 - pv);
            return miniBar;
        } else if (column == DownloadTreeTableModel.COL_PROGRESS && value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            if (dLink.getRemainingWaittime() == 0 && (int) dLink.getDownloadCurrent() > 0) {

                if (!dLink.isInProgress()) {
                    progress.setString("");
                    if (dLink.getStatus() == DownloadLink.STATUS_DONE) {
                        progress.setForeground(DONE_COLOR);
                        if (ui != null) ui.setSelectionForeground(DONE_COLOR_FONT_A);
                        if (ui != null) ui.setSelectionBackground(DONE_COLOR_FONT_B);
                        progress.setString("- 100 % -");
                    } else {
                        progress.setForeground(INACTIVE_PROGRESS_COLOR);
                        if (ui != null) ui.setSelectionForeground(INACTIVE_PROGRESS_COLOR_FONT_A);
                        if (ui != null) ui.setSelectionBackground(INACTIVE_PROGRESS_COLOR_FONT_B);
                    }
                    progress.setMaximum(Math.max(1, (int) dLink.getDownloadMax()));
                    progress.setStringPainted(true);

                    progress.setValue((int) dLink.getDownloadCurrent());

                } else {
                    progress.setMaximum(Math.max(1, (int) dLink.getDownloadMax()));
                    progress.setStringPainted(true);
                    progress.setForeground(ACTIVE_PROGRESS_COLOR);
                    if (ui != null) ui.setSelectionForeground(ACTIVE_PROGRESS_COLOR_FONT_A);
                    if (ui != null) ui.setSelectionBackground(ACTIVE_PROGRESS_COLOR_FONT_B);
                    progress.setValue((int) dLink.getDownloadCurrent());
                    progress.setString(c.format(10000 * progress.getPercentComplete() / 100.0) + "% (" + JDUtilities.formatBytesToMB(progress.getValue()) + "/" + JDUtilities.formatBytesToMB(progress.getMaximum()) + ")");

                }

                return progress;
            } else if (dLink.getRemainingWaittime() > 0 && dLink.getWaitTime() >= dLink.getRemainingWaittime()) {
                progress.setMaximum(dLink.getWaitTime());
                progress.setForeground(ERROR_PROGRESS_COLOR);
                if (ui != null) ui.setSelectionForeground(ERROR_PROGRESS_COLOR_FONT_A);
                if (ui != null) ui.setSelectionBackground(ERROR_PROGRESS_COLOR_FONT_B);
                progress.setStringPainted(true);
                progress.setValue((int) dLink.getRemainingWaittime());
                progress.setString(c.format(10000 * progress.getPercentComplete() / 100.0) + "% (" + progress.getValue() / 1000 + "/" + progress.getMaximum() / 1000 + " sek)");
                return progress;
            }
            label.setText("");
            return label;
        } else if (column == DownloadTreeTableModel.COL_PROGRESS && value instanceof FilePackage) {
            fp = (FilePackage) value;
            if (fp.getPercent() == 0.0) {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setStringPainted(true);
                progress.setForeground(PACKAGE_PROGRESS_COLOR);
                if (ui != null) ui.setSelectionForeground(PACKAGE_PROGRESS_COLOR_FONT_A);
                if (ui != null) ui.setSelectionBackground(PACKAGE_PROGRESS_COLOR_FONT_B);

                progress.setValue(fp.getTotalKBLoaded());
                progress.setString("- 0% -");

            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setStringPainted(true);
                progress.setForeground(PACKAGE_PROGRESS_COLOR);
                if (ui != null) ui.setSelectionForeground(PACKAGE_PROGRESS_COLOR_FONT_A);
                if (ui != null) ui.setSelectionBackground(PACKAGE_PROGRESS_COLOR_FONT_B);

                progress.setValue(fp.getTotalKBLoaded());
                progress.setString(c.format(fp.getPercent()) + "% (" + JDUtilities.formatKbReadable(progress.getValue()) + "/" + JDUtilities.formatKbReadable(Math.max(1, fp.getTotalEstimatedPackageSize())) + ")");
            }
            return progress;
        }

        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        co.setForeground(FONT_COLOR);
        co.setBackground(FONT_COLOR);

        return co;

    }

    private class MiniBar extends JLabel {

        private static final long serialVersionUID = -3508403269097752259L;
        private double p;

        @Override
        protected void paintComponent(Graphics g) {
            if (p != 0) {
                int n = 3;
                int start = getHeight() - n;
                ((Graphics2D) g).setPaint(new GradientPaint(0, 0, ACTIVE_PROGRESS_COLOR, getWidth(), getHeight(), ACTIVE_PROGRESS_COLOR.darker()));
                g.fillRect(0, start, (int) (getWidth() * p), n);
            }
            super.paintComponent(g);
        }

        public void setPercent(double percent) {
            p = percent;
        }

        public double getPercent() {
            return p;
        }
    }
}