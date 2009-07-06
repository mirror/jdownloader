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
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.controlling.JDController;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.table.TableColumnExt;

public class TableRenderer extends DefaultTableRenderer {

    private static final long serialVersionUID = -3912572910439565199L;

    private static final String NULL_BYTE_PROGRESS = "0.00%(0 B/* MB)";

    private DecimalFormat c = new DecimalFormat("0.00");

    private Component co;

    private DownloadLink dLink;

    private FilePackage fp;

    private JDProgressBar progress;

    private DownloadTable table;

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

    private ImageIcon imgPriorityS;

    private ImageIcon imgPriority1;

    private ImageIcon imgPriority2;

    private ImageIcon imgPriority3;

    private String strTTPriorityS;

    private String strTTPriority1;

    private String strTTPriority2;

    private String strTTPriority3;

    private String strTTExtract;

    private String strTTStopMark;

    private String strTTFinished;

    private String strTTFailed;

    private ImageIcon imgFileFailed;

    private ImageIcon icon_fp_open_error;

    private ImageIcon icon_fp_closed_error;

    private StatusLabel statuspanel;

    private int counter;

    private static Color COL_PROGRESS_ERROR = new Color(0xCC3300);

    private static Color COL_PROGRESS_NORMAL = null;

    public TableRenderer(DownloadTable downloadTreeTable) {
        initIcons();
        initLocale();
        table = downloadTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        progress = new JDProgressBar();
        progress.setStringPainted(true);
        progress.setOpaque(true);
        COL_PROGRESS_NORMAL = progress.getForeground();
        statuspanel = new StatusLabel(new MigLayout("ins 0", "[]0[fill,grow,align right]"));
    }

    private void initIcons() {
        icon_fp_open = JDTheme.II("gui.images.package_opened_tree", 16, 16);
        icon_fp_open_error = JDTheme.II("gui.images.package_open_error_tree", 16, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_closed_tree", 16, 16);
        icon_fp_closed_error = JDTheme.II("gui.images.package_closed_error_tree", 16, 16);
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
        imgExtract = JDTheme.II("gui.images.update_manager", 16, 16);
        imgStopMark = JDTheme.II("gui.images.stopmark", 16, 16);
        imgPriorityS = JDTheme.II("gui.images.priority-1", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        imgFileFailed = JDTheme.II("gui.images.offlinefile", 16, 16);
    }

    private void initLocale() {
        strPluginDisabled = JDL.L("gui.downloadlink.plugindisabled", "[Plugin disabled]");
        strDownloadLinkActive = JDL.L("gui.treetable.packagestatus.links_active", "aktiv");
        strETA = JDL.L("gui.eta", "ETA");
        strPluginError = JDL.L("gui.treetable.error.plugin", "Plugin error");
        strSecondsAbrv = JDL.L("gui.treetable.seconds", "sec");
        strWaitIO = JDL.L("gui.linkgrabber.waitinguserio", "Waiting for user input");
        strTTPriorityS = JDL.L("gui.treetable.tooltip.priority-1", "Low Priority");
        strTTPriority1 = JDL.L("gui.treetable.tooltip.priority1", "High Priority");
        strTTPriority2 = JDL.L("gui.treetable.tooltip.priority2", "Higher Priority");
        strTTPriority3 = JDL.L("gui.treetable.tooltip.priority3", "Highest Priority");
        strTTFailed = JDL.L("gui.treetable.tooltip.failed", "Download failed. See logs for details. Rightclick->reset to retry");
        strTTFinished = JDL.L("gui.treetable.tooltip.finished", "Download has finished successfully");
        strTTStopMark = JDL.L("gui.treetable.tooltip.stopmark", "Stopmark is set. After this Link/Package, no further downloads will start");
        strTTExtract = JDL.L("gui.treetable.tooltip.extract", "A post-download module(extracter,...) is running");
    }

    // @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getColumn(column).getModelIndex();

        if (value instanceof FilePackage) {
            co = getFilePackageCell(table, value, isSelected, hasFocus, row, column);
            if (!((FilePackage) value).isEnabled()) {
                co.setEnabled(false);
                // ((JComponent) co).setToolTipText(lblTTDisabled);
                progress.setString("");

            } else {

                co.setEnabled(true);
            }

            return co;
        } else if (value instanceof DownloadLink) {
            co = getDownloadLinkCell(table, value, isSelected, hasFocus, row, column);
            if (!((DownloadLink) value).isEnabled()) {
                co.setEnabled(false);
                if (co instanceof JDProgressBar) {
                    ((JDProgressBar) co).setString("");
                }

                // ((JComponent) co).setToolTipText(lblTTDisabled);

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
        case DownloadJTableModel.COL_PART:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            clearSB();
            // ((JComponent) co).setToolTipText(null);
            if (dLink.getLinkStatus().isFailed()) {
                ((JRendererLabel) co).setIcon(this.imgFileFailed);
            } else {
                ((JRendererLabel) co).setIcon(dLink.getIcon());
            }

            ((JRendererLabel) co).setText(dLink.getName());
            // ((JRendererLabel) co).setToolTipText(dLink.getName() + " - " +
            // dLink.getFileInfomationString());
            ((JRendererLabel) co).setBorder(leftGap);

            return co;
        case DownloadJTableModel.COL_HOSTER:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            clearSB();
            ((JRendererLabel) co).setBorder(null);
            if (dLink.getPlugin() == null) {
                // ((JComponent) co).setToolTipText(null);
                ((JRendererLabel) co).setText("plugin missing");
            } else {
                if (dLink.getPlugin().hasHosterIcon()) {
                    sb.append(dLink.getPlugin().getHost());
                    sb.append(dLink.getPlugin().getSessionInfo());
                    ((JRendererLabel) co).setText(dLink.getPlugin().getSessionInfo());
                    ((JRendererLabel) co).setIcon(dLink.getPlugin().getHosterIcon());
                    // ((JComponent) co).setToolTipText(sb.toString());
                } else {
                    sb.append(dLink.getPlugin().getHost());
                    sb.append(dLink.getPlugin().getSessionInfo());
                    ((JRendererLabel) co).setText(sb.toString());
                    // ((JComponent) co).setToolTipText(sb.toString());
                }

            }
            // ((JRendererLabel)
            // co).setIcon(JDImage.getScaledImageIcon(dLink.getPlugin
            // ().getHosterIcon(),16,16));
            return co;

        case DownloadJTableModel.COL_PROGRESS:

            if (dLink.getPlugin() == null) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // ((JComponent) co).setToolTipText(null);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginError);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (!dLink.getPlugin().getWrapper().usePlugin()) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // ((JComponent) co).setToolTipText(null);
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
                // progress.setToolTipText(null);
                progress.setForeground(COL_PROGRESS_NORMAL);
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
                    sb.append(c.format(10000 * progress.getPercentComplete() / 100.0)).append("% (").append(progress.getRealValue() / 1000).append('/').append(progress.getRealMax() / 1000).append(strSecondsAbrv).append(')');
                }
                progress.setString(sb.toString());
                // progress.setToolTipText(null);
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
                // progress.setToolTipText(null);
                progress.setValue(dLink.getPercent());
                progress.setForeground(COL_PROGRESS_NORMAL);
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
                progress.setString(NULL_BYTE_PROGRESS);
            }
            // progress.setToolTipText(null);
            progress.setForeground(COL_PROGRESS_NORMAL);
            return progress;

        case DownloadJTableModel.COL_STATUS:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            this.statuspanel.setBackground(co.getBackground());
            statuspanel.setPainter(((JRendererLabel) co).getPainter());

            if (dLink.getPluginProgress() != null && dLink.getPluginProgress().getPercent() > 0.0 && dLink.getPluginProgress().getPercent() < 100.0) {
                statuspanel.left.setText(dLink.getLinkStatus().getStatusString());
            } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                statuspanel.left.setText(dLink.getLinkStatus().getStatusString());
            } else if (dLink.getLinkStatus().isFailed()) {
                statuspanel.left.setText(dLink.getLinkStatus().getStatusString());
            } else {
                statuspanel.left.setText(dLink.getLinkStatus().getStatusString());
            }

            counter = 0;
            this.clearSB();

            if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                statuspanel.rights[counter].setIcon(imgStopMark);
                if (counter > 0) sb.append(" | ");
                sb.append(strTTStopMark);

                counter++;
            }

            if (dLink.getLinkStatus().getStatusIcon() != null) {
                statuspanel.rights[counter].setIcon(dLink.getLinkStatus().getStatusIcon());
                if (counter > 0) sb.append(" | ");

                counter++;
            } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                statuspanel.rights[counter].setIcon(imgFinished);
                if (counter > 0) sb.append(" | ");
                sb.append(strTTFinished);

                counter++;
            } else if (dLink.getLinkStatus().isFailed()) {
                statuspanel.rights[counter].setIcon(imgFailed);
                if (counter > 0) sb.append(" | ");
                sb.append(strTTFailed);

                counter++;
            }
            if (counter <= StatusLabel.ICONCOUNT && dLink.getPluginProgress() != null && dLink.getPluginProgress().getPercent() > 0.0 && dLink.getPluginProgress().getPercent() < 100.0) {
                statuspanel.rights[counter].setIcon(imgExtract);
                if (counter > 0) sb.append(" | ");
                sb.append(strTTExtract);

                counter++;
            }
            if (counter <= StatusLabel.ICONCOUNT) {
                switch (dLink.getPriority()) {
                case 0:
                default:
                    break;
                case -1:
                    statuspanel.rights[counter].setIcon(imgPriorityS);
                    if (counter > 0) sb.append(" | ");
                    sb.append(strTTPriorityS);

                    counter++;
                    break;
                case 1:
                    statuspanel.rights[counter].setIcon(imgPriority1);
                    if (counter > 0) sb.append(" | ");
                    sb.append(strTTPriority1);

                    counter++;
                    break;
                case 2:
                    statuspanel.rights[counter].setIcon(imgPriority2);
                    if (counter > 0) sb.append(" | ");
                    sb.append(strTTPriority2);

                    counter++;
                    break;
                case 3:
                    statuspanel.rights[counter].setIcon(imgPriority3);
                    if (counter > 0) sb.append(" | ");
                    sb.append(strTTPriority3);

                    counter++;
                    break;
                }
            }
            // statuspanel.setToolTipText(sb.toString());
            statuspanel.clearIcons(counter);
            statuspanel.setBorder(null);
            // statuspanel.left.setText("HHH");
            // statuspanel.right.setIcon(imgPriority3);
            // statuspanel.righter.setIcon(this.imgFinished);
            return statuspanel;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        // ((JRendererLabel) co).setToolTipText(null);
        return co;
    }

    private Component getFilePackageCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        fp = (FilePackage) value;
        switch (column) {
        case DownloadJTableModel.COL_PART:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName());
            if (fp.getLinksFailed() > 0) {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false) ? icon_fp_closed_error : icon_fp_open_error);

            } else {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);

            }

            ((JRendererLabel) co).setBorder(null);
            // ((JComponent) co).setToolTipText(null);
            return co;

        case DownloadJTableModel.COL_HOSTER:
            value = fp.getHoster();

            break;

        case DownloadJTableModel.COL_PROGRESS:

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
            // progress.setToolTipText(null);
            progress.setForeground(COL_PROGRESS_NORMAL);
            return progress;

        case DownloadJTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            this.statuspanel.setBackground(co.getBackground());
            statuspanel.setPainter(((JRendererLabel) co).getPainter());
            if (fp.isFinished()) {
                statuspanel.left.setText("");
            } else if (fp.getTotalDownloadSpeed() > 0) {
                clearSB();
                sb.append('[').append(fp.getLinksInProgress()).append('/').append(fp.size()).append("] ");
                sb.append(strETA).append(' ').append(Formatter.formatSeconds(fp.getETA())).append(" @ ").append(Formatter.formatReadable(fp.getTotalDownloadSpeed())).append("/s");
                statuspanel.left.setText(sb.toString());
            } else if (fp.getLinksInProgress() > 0) {
                clearSB();
                sb.append(fp.getLinksInProgress()).append('/').append(fp.size()).append(' ').append(strDownloadLinkActive);
                statuspanel.left.setText(sb.toString());
            } else {
                statuspanel.left.setText("");
            }

            counter = 0;
            clearSB();
            if (fp.isFinished()) {
                statuspanel.rights[counter].setIcon(this.imgFinished);
                counter++;
            } else if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                statuspanel.rights[counter].setIcon(imgStopMark);
                if (counter > 0) sb.append(" | ");

                counter++;
                sb.append(strTTStopMark);

            } else if (fp.getTotalDownloadSpeed() > 0) {

            } else if (fp.getLinksInProgress() > 0) {

            } else {

            }
            statuspanel.clearIcons(counter);
            // statuspanel.setToolTipText(sb.toString());
            statuspanel.setBorder(null);
            return statuspanel;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        // ((JComponent) co).setToolTipText(null);
        return co;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity());
    }
}