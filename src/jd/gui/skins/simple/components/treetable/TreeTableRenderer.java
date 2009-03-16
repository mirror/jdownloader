//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class TreeTableRenderer extends DefaultTableCellRenderer {
    private class MiniBar extends JLabel {

        private static final long serialVersionUID = -3508403269097752259L;
        private double p;

        public double getPercent() {
            return p;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (p != 0) {
                int n = 3;
                ((Graphics2D) g).setPaint(new GradientPaint(0, 0, ACTIVE_PROGRESS_COLOR, getWidth(), getHeight(), ACTIVE_PROGRESS_COLOR.darker()));
                g.fillRect(0, getHeight() - n, (int) (getWidth() * p), n);
            }
            super.paintComponent(g);
        }

        public void setPercent(double percent) {
            p = percent;
        }
    }

    private static Color ACTIVE_PROGRESS_COLOR;

    private static Color ACTIVE_PROGRESS_COLOR_FONT_A;

    private static Color ACTIVE_PROGRESS_COLOR_FONT_B;

    private static Color DONE_COLOR;

    private static Color DONE_COLOR_Package;

    private static Color DONE_COLOR_FONT_A;

    private static Color DONE_COLOR_FONT_B;

    private static Color ERROR_PROGRESS_COLOR;

    private static Color ERROR_PROGRESS_COLOR_FONT_A;

    private static Color ERROR_PROGRESS_COLOR_FONT_B;

    private static Color FONT_COLOR;

    private static Color INACTIVE_PROGRESS_COLOR;

    private static Color INACTIVE_PROGRESS_COLOR_FONT_A;

    private static Color INACTIVE_PROGRESS_COLOR_FONT_B;

    private static Color PACKAGE_PROGRESS_COLOR;

    private static Color PACKAGE_PROGRESS_COLOR_FONT_A;

    private static Color PACKAGE_PROGRESS_COLOR_FONT_B;

    private static final long serialVersionUID = -3912572910439565199L;

    private DecimalFormat c = new DecimalFormat("0.00");

    private Component co;

    private DownloadLink dLink;

    private FilePackage fp;

    private JLabel label;

    private MiniBar miniBar;

    private JDProgressBar progress;

    private DownloadTreeTable table;

    private TreeProgressBarUI ui;

    TreeTableRenderer(DownloadTreeTable downloadTreeTable) {

        FONT_COLOR = JDTheme.C("gui.color.downloadlist.font", "000000");

        PACKAGE_PROGRESS_COLOR = JDTheme.C("gui.color.downloadlist.package_progress", "94baff");
        ERROR_PROGRESS_COLOR = JDTheme.C("gui.color.downloadlist.progress_error", "FF0000");
        DONE_COLOR = JDTheme.C("gui.color.downloadlist.progress_done", "94baff");
        DONE_COLOR_Package = JDTheme.C("gui.color.downloadlist.row_package_done", "339933");
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

        table = downloadTreeTable;
        label = new JLabel();
        label.setOpaque(false);
        miniBar = new MiniBar();
        progress = new JDProgressBar();
        Dimension dim = progress.getPreferredSize();
        dim.width = Math.max(dim.width, 300);
        progress.setPreferredSize(dim);
        progress.setMinimumSize(dim);
//        if (JDUtilities.getJavaVersion() >= 1.6) {
//            ui = new TreeProgressBarUI();
//            ui.setSelectionForeground(Color.BLACK);
//            progress.setUI(ui);
//        }
        progress.setBorderPainted(false);
        progress.setStringPainted(true);
        progress.setOpaque(false);

    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        column = this.table.getColumn(column).getModelIndex();
        if (column == DownloadTreeTableModel.COL_STATUS && value instanceof FilePackage) {
            String label = "";
            fp = (FilePackage) value;
            if (fp.isFinished()) {
                label = JDLocale.L("gui.downloadlink.finished", "[finished]");
            } else if (fp.getTotalDownloadSpeed() > 0) {
                label = "[" + fp.getLinksInProgress() + "/" + fp.size() + "] ETA " + JDUtilities.formatSeconds(fp.getETA()) + " @ " + JDUtilities.formatKbReadable(fp.getTotalDownloadSpeed() / 1024) + "/s";
            } else if (fp.getLinksInProgress() > 0) {
                label = fp.getLinksInProgress() + "/" + fp.size() + " " + JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
            }
            miniBar.setText(label);
            miniBar.setPercent(fp.getPercent() / 100.0);
            return miniBar;
        } else if (column == DownloadTreeTableModel.COL_PROGRESS && value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            if (dLink.getPlugin() == null) {
                progress.setForeground(ERROR_PROGRESS_COLOR);
                if (ui != null) {
                    ui.setSelectionForeground(DONE_COLOR_FONT_A);
                    ui.setSelectionBackground(DONE_COLOR_FONT_B);
                }
                progress.setString("Plugin loading failed");
            } else if (!dLink.getPlugin().getWrapper().usePlugin()) {
                progress.setForeground(INACTIVE_PROGRESS_COLOR);
                if (ui != null) {
                    ui.setSelectionForeground(INACTIVE_PROGRESS_COLOR_FONT_A);
                    ui.setSelectionBackground(INACTIVE_PROGRESS_COLOR_FONT_B);
                }
                progress.setString(JDLocale.L("gui.downloadlink.plugindisabled", "[Plugin disabled]"));
                return progress;
            } else if (dLink.getPluginProgress() != null) {
                progress.setForeground(dLink.getPluginProgress().getColor());
                if (ui != null) {
                    ui.setSelectionForeground(DONE_COLOR_FONT_A);
                    ui.setSelectionBackground(DONE_COLOR_FONT_B);
                }
                progress.setString(dLink.getPluginProgress().getPercent() + "%");
                progress.setMaximum(dLink.getPluginProgress().getTotal());
                progress.setValue(dLink.getPluginProgress().getCurrent());
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && dLink.getPlugin().getRemainingHosterWaittime() > 0) || (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && dLink.getLinkStatus().getRemainingWaittime() > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                progress.setForeground(ERROR_PROGRESS_COLOR);
                if (ui != null) {
                    ui.setSelectionForeground(ERROR_PROGRESS_COLOR_FONT_A);
                    ui.setSelectionBackground(ERROR_PROGRESS_COLOR_FONT_B);
                }
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                progress.setString(c.format(10000 * progress.getPercentComplete() / 100.0) + "% (" + progress.getValue() / 1000 + "/" + progress.getMaximum() / 1000 + " sek)");
                return progress;
            } else if (dLink.getDownloadCurrent() > 0) {
                if (!dLink.getLinkStatus().isPluginActive()) {
                    if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        progress.setForeground(DONE_COLOR);
                        if (ui != null) {
                            ui.setSelectionForeground(DONE_COLOR_FONT_A);
                            ui.setSelectionBackground(DONE_COLOR_FONT_B);
                        }
                        progress.setString("- 100% -");
                    } else {
                        progress.setForeground(INACTIVE_PROGRESS_COLOR);
                        if (ui != null) {
                            ui.setSelectionForeground(INACTIVE_PROGRESS_COLOR_FONT_A);
                            ui.setSelectionBackground(INACTIVE_PROGRESS_COLOR_FONT_B);
                        }
                        progress.setString("");
                    }
                } else {
                    progress.setForeground(ACTIVE_PROGRESS_COLOR);
                    if (ui != null) {
                        ui.setSelectionForeground(ACTIVE_PROGRESS_COLOR_FONT_A);
                        ui.setSelectionBackground(ACTIVE_PROGRESS_COLOR_FONT_B);
                    }
                    if (dLink.getLinkStatus().hasStatus(LinkStatus.WAITING_USERIO)) {
                        progress.setString(SimpleGUI.WAITING_USER_IO);
                    } else {
                        progress.setString(c.format(dLink.getPercent() / 100.0) + "% (" + JDUtilities.formatBytesToMB(dLink.getDownloadCurrent()) + "/" + JDUtilities.formatBytesToMB(Math.max(1, dLink.getDownloadSize())) + ")");
                    }
                }
                progress.setMaximum(10000);
                progress.setValue(dLink.getPercent());
                return progress;
            }
            label.setText("");
            label.setIcon(null);
            return label;
        } else if (column == DownloadTreeTableModel.COL_PROGRESS && value instanceof FilePackage) {
            fp = (FilePackage) value;
            if (fp.isFinished()) {
                progress.setForeground(DONE_COLOR_Package);
                progress.setMaximum(100);
                progress.setValue(100);
                progress.setString("- 100% -");
            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setForeground(PACKAGE_PROGRESS_COLOR);
                if (ui != null) {
                    ui.setSelectionForeground(PACKAGE_PROGRESS_COLOR_FONT_A);
                    ui.setSelectionBackground(PACKAGE_PROGRESS_COLOR_FONT_B);
                }
                progress.setValue(fp.getTotalKBLoaded());
                progress.setString(c.format(fp.getPercent()) + "% (" + JDUtilities.formatKbReadable(progress.getValue()) + "/" + JDUtilities.formatKbReadable(Math.max(1, fp.getTotalEstimatedPackageSize())) + ")");
            }
            return progress;
        } else if (column == DownloadTreeTableModel.COL_HOSTER && value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            label.setText(dLink.getPlugin().getHost() + dLink.getPlugin().getSessionInfo());
            if (dLink.getPlugin().hasHosterIcon()) label.setIcon(dLink.getPlugin().getHosterIcon());
            return label;
        }

        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        co.setForeground(FONT_COLOR);
        co.setBackground(FONT_COLOR);
        return co;
    }
}