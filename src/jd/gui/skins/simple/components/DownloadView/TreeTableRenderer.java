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

package jd.gui.skins.simple.components.DownloadView;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.controlling.JDController;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.table.TableColumnExt;

public class TreeTableRenderer extends DefaultTableRenderer {

    private static final long serialVersionUID = -3912572910439565199L;

    private DecimalFormat c = new DecimalFormat("0.00");

    private Component co;

    private DownloadLink dLink;

    private FilePackage fp;

    private JDProgressBar progress;

    private DownloadTreeTable table;

    private ImageIcon icon_fp_closed;

    private ImageIcon icon_fp_open;

    private String strPluginDisabled;

    private String strETA;

    private StringBuilder sb = new StringBuilder();

    private String strDownloadLinkActive;

    private String strPluginError;

    private String strSecondsAbrv;

    private TableColumnExt col;

    private Border leftGap;

    private Icon imgFinished;

    private ImageIcon imgFailed;

    private String strWaitIO;

    private Icon imgExtract;

    private ImageIcon imgStopMark;

    private ImageIcon imgPriority1;

    private ImageIcon imgPriority2;

    private ImageIcon imgPriority3;

    private String strTTPriority1;

    private String strTTPriority2;

    private String strTTPriority3;

    private String strTTExtract;

    private String strTTStopMark;

    private String strTTFinished;

    private String strTTFailed;

    // private String lblDisabled;

    private String lblTTDisabled;

    private ImageIcon imgFileFailed;

    private ImageIcon icon_fp_open_error;

    private ImageIcon icon_fp_closed_error;

    private static Color COL_PROGRESS_ERROR = new Color(0xCC3300);

    TreeTableRenderer(DownloadTreeTable downloadTreeTable) {
        initIcons();
        initLocale();
        table = downloadTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        progress = new JDProgressBar();
        progress.setStringPainted(true);
        progress.setOpaque(true);
    }

    private void initIcons() {
        icon_fp_open = JDTheme.II("gui.images.package_opened", 16, 16);
        icon_fp_open_error = JDTheme.II("gui.images.package_open_error", 16, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_closed", 16, 16);
        icon_fp_closed_error = JDTheme.II("gui.images.package_closed_error", 16, 16);
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
        imgExtract = JDTheme.II("gui.images.update_manager", 16, 16);
        imgStopMark = JDTheme.II("gui.images.stopmark", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        imgFileFailed = JDTheme.II("gui.images.offlinefile", 16, 16);
    }

    private void initLocale() {
        strPluginDisabled = JDLocale.L("gui.downloadlink.plugindisabled", "[Plugin disabled]");
        strDownloadLinkActive = JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
        strETA = JDLocale.L("gui.eta", "ETA");
        strPluginError = JDLocale.L("gui.treetable.error.plugin", "Plugin error");
        strSecondsAbrv = JDLocale.L("gui.treetable.seconds", "sec");
        strWaitIO = JDLocale.L("gui.linkgrabber.waitinguserio", "Waiting for user input");
        strTTPriority1 = JDLocale.L("gui.treetable.tooltip.priority1", "High Priority");
        strTTPriority2 = JDLocale.L("gui.treetable.tooltip.priority2", "Higher Priority");
        strTTPriority3 = JDLocale.L("gui.treetable.tooltip.priority3", "Highest Priority");

        strTTFailed = JDLocale.L("gui.treetable.tooltip.failed", "Download failed. See logs for details. Rightclick->reset to retry");
        strTTFinished = JDLocale.L("gui.treetable.tooltip.finished", "Download has finished successfully");
        strTTStopMark = JDLocale.L("gui.treetable.tooltip.stopmark", "Stopmark is set. After this Link/Package, no further downloads will start");
        strTTExtract = JDLocale.L("gui.treetable.tooltip.extract", "A post-download module(extracter,...) is running");
        lblTTDisabled = JDLocale.L("gui.treetable.tooltip.disabled", "This download(s) are disabled. Rightclick ->enable/reset to reactivate them.");

    }

    // @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getColumn(column).getModelIndex();

        if (value instanceof FilePackage) {
            co = getFilePackageCell(table, value, isSelected, hasFocus, row, column);
            if (!((FilePackage) value).isEnabled()) {
                co.setEnabled(false);
                ((JComponent) co).setToolTipText(lblTTDisabled);
                progress.setString("");

            } else {

                co.setEnabled(true);
            }
            return co;
        } else if (value instanceof DownloadLink) {
            co = getDownloadLinkCell(table, value, isSelected, hasFocus, row, column);
            if (!((DownloadLink) value).isEnabled()) {
                co.setEnabled(false);
                progress.setString("");

                ((JComponent) co).setToolTipText(lblTTDisabled);

            } else {

                co.setEnabled(true);
            }
            return co;
        } else {
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            co.setEnabled(true);
        }

        return co;

    }

    private Component getDownloadLinkCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        dLink = (DownloadLink) value;
        switch (column) {
        case DownloadTreeTableModel.COL_PART:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            clearSB();
            ((JComponent) co).setToolTipText(null);
            if (dLink.getLinkStatus().isFailed()) {
                ((JRendererLabel) co).setIcon(this.imgFileFailed);
            } else {
                ((JRendererLabel) co).setIcon(dLink.getIcon());
            }

            ((JRendererLabel) co).setText(dLink.getName());
            ((JRendererLabel) co).setBorder(leftGap);

            return co;
        case DownloadTreeTableModel.COL_HOSTER:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ((JComponent) co).setToolTipText(null);
            clearSB();
            ((JRendererLabel) co).setBorder(null);
            if (dLink.getPlugin() == null) {
                ((JRendererLabel) co).setText("plugin missing");
            } else {
                sb.append(dLink.getPlugin().getHost());
                sb.append(dLink.getPlugin().getSessionInfo());
                ((JRendererLabel) co).setText(sb.toString());
            }
            // ((JRendererLabel)
            // co).setIcon(JDImage.getScaledImageIcon(dLink.getPlugin
            // ().getHosterIcon(),16,16));
            return co;

        case DownloadTreeTableModel.COL_PROGRESS:

            if (dLink.getPlugin() == null) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JComponent) co).setToolTipText(null);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginError);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (!dLink.getPlugin().getWrapper().usePlugin()) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JComponent) co).setToolTipText(null);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginDisabled);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (dLink.getPluginProgress() != null) {
                col = this.table.getCols()[column];
                if (col.getWidth() < 40) {

                } else if (col.getWidth() < 170) {
                    progress.setString(dLink.getPluginProgress().getPercent() + "%");
                } else {
                    progress.setString(dLink.getPluginProgress().getPercent() + "%");
                }

                progress.setMaximum(dLink.getPluginProgress().getTotal());
                progress.setValue(dLink.getPluginProgress().getCurrent());
                progress.setToolTipText(null);
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && dLink.getPlugin().getRemainingHosterWaittime() > 0) || (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && dLink.getLinkStatus().getRemainingWaittime() > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                progress.setForeground(COL_PROGRESS_ERROR);
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                clearSB();
                col = this.table.getCols()[column];
                if (col.getWidth() < 60) {

                } else if (col.getWidth() < 170) {
                    sb.append(c.format(10000 * progress.getPercentComplete() / 100.0)).append('%');
                } else {
                    sb.append(c.format(10000 * progress.getPercentComplete() / 100.0)).append("% (").append(progress.getValue() / 1000).append('/').append(progress.getMaximum() / 1000).append(strSecondsAbrv).append(')');
                }
                progress.setString(sb.toString());
                progress.setToolTipText(null);
                return progress;
            } else if (dLink.getDownloadCurrent() > 0) {
                if (!dLink.getLinkStatus().isPluginActive()) {
                    if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        clearSB();
                        col = this.table.getCols()[column];
                        if (col.getWidth() < 40) {

                        } else if (col.getWidth() < 170) {
                            sb.append("100%");
                        } else {
                            sb.append("100% (").append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize()))).append(')');
                        }
                        progress.setString(sb.toString());

                    } else {
                        clearSB();
                        col = this.table.getCols()[column];
                        if (col.getWidth() < 60) {

                        } else if (col.getWidth() < 170) {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                        } else {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append("% (").append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize()))).append(')');
                        }
                        progress.setString(sb.toString());
                    }
                } else {

                    if (dLink.getLinkStatus().hasStatus(LinkStatus.WAITING_USERIO)) {
                        progress.setString(strWaitIO);
                    } else {
                        clearSB();
                        col = this.table.getCols()[column];
                        if (col.getWidth() < 60) {

                        } else if (col.getWidth() < 170) {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                        } else {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append("% (").append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize()))).append(')');
                        }
                        progress.setString(sb.toString());
                    }
                }
                progress.setMaximum(10000);
                progress.setToolTipText(null);
                progress.setValue(dLink.getPercent());
                return progress;
            }
            progress.setMaximum(10000);
            progress.setValue(0);
            if (dLink.getDownloadSize() > 1) {
                clearSB();
                col = this.table.getCols()[column];
                if (col.getWidth() < 60) {

                } else if (col.getWidth() < 170) {
                    sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                } else {
                    sb.append(c.format(dLink.getPercent() / 100.0)).append("% (").append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize()))).append(')');
                }
                progress.setString(sb.toString());
            } else {
                progress.setString("");
            }
            progress.setToolTipText(null);
            return progress;

            // co = super.getTableCellRendererComponent(table, value,
            // isSelected, hasFocus, row, column);
            // ((JRendererLabel) co).setIcon(null);
            // ((JRendererLabel) co).setText("");
            // ((JRendererLabel) co).setBorder(null);
            // return co;
        case DownloadTreeTableModel.COL_STATUS_ICON:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText("");
            ((JRendererLabel) co).setToolTipText(null);
            if (dLink.getPluginProgress() != null && dLink.getPluginProgress().getPercent() > 0.0 && dLink.getPluginProgress().getPercent() < 100.0) {
                ((JRendererLabel) co).setIcon(imgExtract);
                ((JRendererLabel) co).setToolTipText(strTTExtract);
            } else if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                ((JRendererLabel) co).setIcon(imgStopMark);
                ((JRendererLabel) co).setToolTipText(strTTStopMark);
            } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                ((JRendererLabel) co).setIcon(imgFinished);
                ((JRendererLabel) co).setToolTipText(strTTFinished);
            } else if (dLink.getLinkStatus().isFailed()) {
                ((JRendererLabel) co).setIcon(imgFailed);
                ((JRendererLabel) co).setToolTipText(strTTFailed);
            } else {

                switch (dLink.getPriority()) {
                case 0:
                default:
                    break;
                case 1:
                    ((JRendererLabel) co).setIcon(imgPriority1);
                    ((JRendererLabel) co).setToolTipText(strTTPriority1);
                    break;
                case 2:
                    ((JRendererLabel) co).setIcon(imgPriority2);
                    ((JRendererLabel) co).setToolTipText(strTTPriority2);
                    break;
                case 3:
                    ((JRendererLabel) co).setIcon(imgPriority3);
                    ((JRendererLabel) co).setToolTipText(strTTPriority3);
                    break;
                }
            }

            ((JRendererLabel) co).setBorder(null);
            // ((JRendererLabel) co).setToolTipText("Blabla Leberkäs");
            return co;

        case DownloadTreeTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setToolTipText(null);
            ((JRendererLabel) co).setIcon(null);
            if (dLink.getPluginProgress() != null && dLink.getPluginProgress().getPercent() > 0.0 && dLink.getPluginProgress().getPercent() < 100.0) {

                ((JRendererLabel) co).setText(dLink.getLinkStatus().getStatusString());
            } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {

                ((JRendererLabel) co).setText(dLink.getLinkStatus().getStatusString());
            } else if (dLink.getLinkStatus().isFailed()) {

                ((JRendererLabel) co).setText(dLink.getLinkStatus().getStatusString());

            } else {
                ((JRendererLabel) co).setText(dLink.getLinkStatus().getStatusString());

            }

            ((JRendererLabel) co).setBorder(null);

            return co;

        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        ((JRendererLabel) co).setToolTipText(null);
        return co;
    }

    private Component getFilePackageCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        fp = (FilePackage) value;
        switch (column) {
        case DownloadTreeTableModel.COL_PART:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName());
            if (fp.getLinksFailed() > 0) {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(DownloadTreeTable.PROPERTY_EXPANDED, false) ? icon_fp_closed_error : icon_fp_open_error);

            } else {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(DownloadTreeTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);

            }

            ((JRendererLabel) co).setBorder(null);
            ((JComponent) co).setToolTipText(null);
            return co;

        case DownloadTreeTableModel.COL_HOSTER:
            value = fp.getHoster();

            break;

        case DownloadTreeTableModel.COL_PROGRESS:

            if (fp.isFinished()) {
                progress.setMaximum(100);
                progress.setValue(100);
                clearSB();
                col = this.table.getCols()[column];
                if (col.getWidth() < 40) {

                } else if (col.getWidth() < 170) {
                    sb.append("100%");
                } else {
                    sb.append("100% (").append(Formatter.formatReadable(Math.max(0, fp.getTotalEstimatedPackageSize()))).append(')');
                }
                progress.setString(sb.toString());
            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setValue(fp.getTotalKBLoaded());
                clearSB();
                col = this.table.getCols()[column];
                if (col.getWidth() < 40) {

                } else if (col.getWidth() < 170) {
                    sb.append(c.format(fp.getPercent())).append('%');
                } else {
                    sb.append(c.format(fp.getPercent())).append("% (").append(Formatter.formatReadable(progress.getRealValue())).append('/').append(Formatter.formatReadable(Math.max(0, progress.getRealMax()))).append(')');
                }
                progress.setString(sb.toString());
            }
            progress.setToolTipText(null);
            return progress;
        case DownloadTreeTableModel.COL_STATUS_ICON:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText("");
            ((JRendererLabel) co).setIcon(null);
            ((JComponent) co).setToolTipText(null);
            if (fp.isFinished()) {
                ((JRendererLabel) co).setIcon(this.imgFinished);
            } else if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                ((JRendererLabel) co).setIcon(imgStopMark);
                ((JRendererLabel) co).setToolTipText(strTTStopMark);

            } else if (fp.getTotalDownloadSpeed() > 0) {

            } else if (fp.getLinksInProgress() > 0) {

            } else {

            }

            ((JRendererLabel) co).setBorder(null);
            return co;

        case DownloadTreeTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JComponent) co).setToolTipText(null);
            if (fp.isFinished()) {
                ((JRendererLabel) co).setText("");
            } else if (fp.getTotalDownloadSpeed() > 0) {
                clearSB();
                sb.append('[').append(fp.getLinksInProgress()).append('/').append(fp.size()).append("] ");
                sb.append(strETA).append(' ').append(Formatter.formatSeconds(fp.getETA())).append(" @ ").append(Formatter.formatReadable(fp.getTotalDownloadSpeed())).append("/s");
                ((JRendererLabel) co).setText(sb.toString());
            } else if (fp.getLinksInProgress() > 0) {
                clearSB();
                sb.append(fp.getLinksInProgress()).append('/').append(fp.size()).append(' ').append(strDownloadLinkActive);
                ((JRendererLabel) co).setText(sb.toString());
            } else {
                ((JRendererLabel) co).setText("");
            }
            ((JRendererLabel) co).setBorder(null);
            return co;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        ((JComponent) co).setToolTipText(null);
        return co;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity() - 1);
    }
}