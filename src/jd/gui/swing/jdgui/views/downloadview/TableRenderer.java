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

package jd.gui.swing.jdgui.views.downloadview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableColumn;

import jd.controlling.JDController;
import jd.gui.swing.RowHighlighter;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;

public class TableRenderer extends DefaultTableRenderer {

    private static final long serialVersionUID = -3916572910439565199L;

    private static final String NULL_BYTE_PROGRESS = "0.00% (0 B/* MB)";



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

    private TableColumn col;

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

    private ImageIcon imgFileFailed;

    private ImageIcon icon_fp_open_error;

    private ImageIcon icon_fp_closed_error;

    private StatusLabel statuspanel;

    private int counter;
    private static Border ERROR_BORDER;
    private static Color COL_PROGRESS_ERROR = new Color(0xCC3300);

    private static Color COL_PROGRESS_NORMAL = null;

    public TableRenderer(DownloadTable downloadTreeTable) {
        initIcons();
        initLocale();
        table = downloadTreeTable;
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        highlighter = new ArrayList<RowHighlighter<?>>();
        progress = new JDProgressBar();
        progress.setStringPainted(false);
        progress.setOpaque(true);
        COL_PROGRESS_NORMAL = progress.getForeground();
        statuspanel = new StatusLabel();
        
        ERROR_BORDER=BorderFactory.createLineBorder(COL_PROGRESS_ERROR);
    }

    private ArrayList<RowHighlighter<?>> highlighter;

    private ImageIcon imgResume;

    private ImageIcon imgPremium;

    public void addHighlighter(RowHighlighter<?> rh) {
        highlighter.add(rh);
    }

    private void initIcons() {
        icon_fp_open = JDTheme.II("gui.images.package_opened_tree", 16, 16);
        icon_fp_open_error = JDTheme.II("gui.images.package_open_error_tree", 16, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_closed_tree", 16, 16);
        icon_fp_closed_error = JDTheme.II("gui.images.package_closed_error_tree", 16, 16);
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgResume = JDTheme.II("gui.images.resume", 16, 16);
        imgPremium = JDTheme.II("gui.images.premium", 16, 16);
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
    }

    // @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getTableModel().toModel(column);
        if (value instanceof FilePackage) {
            co = getFilePackageCell(table, value, isSelected, hasFocus, row, column);
            if (!((FilePackage) value).isEnabled()) {
                co.setEnabled(false);
                if (co instanceof JDProgressBar) {
                    ((JDProgressBar) co).setString("");
                }
            } else {
                co.setEnabled(true);
            }
        } else if (value instanceof DownloadLink) {
            co = getDownloadLinkCell(table, value, isSelected, hasFocus, row, column);
            if (!((DownloadLink) value).isEnabled()) {
                co.setEnabled(false);
                if (co instanceof JDProgressBar) {
                    ((JDProgressBar) co).setString("");
                }
            } else {
                co.setEnabled(true);
            }
        } else {
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            co.setEnabled(true);
        }

        if (co instanceof JDProgressBar) return co;
        if (!isSelected) {
            for (RowHighlighter<?> rh : highlighter) {
                if (rh.doHighlight(row)) {
                    ((JComponent) co).setBackground(rh.getColor());
                    break;
                }
            }
        } else {
            Color color = UIManager.getColor("TableHeader.background");
            if (color == null) color = co.getBackground();
            ((JComponent) co).setBackground(color.darker());

        }
        co.setSize(new Dimension(200, 30));
        return co;
    }

    private Component getDownloadLinkCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        dLink = (DownloadLink) value;
        switch (column) {
        case DownloadJTableModel.COL_PART:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            clearSB();
            if (dLink.getLinkStatus().isFailed()) {
                ((JRendererLabel) co).setIcon(imgFileFailed);
            } else {
                ((JRendererLabel) co).setIcon(dLink.getIcon());
            }

            ((JRendererLabel) co).setText(dLink.getName());
            ((JRendererLabel) co).setBorder(leftGap);

            return co;
        case DownloadJTableModel.COL_HOSTER:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            statuspanel.setBackground(co.getBackground());
            statuspanel.setForeground(co.getForeground());
            statuspanel.setText(dLink.getLinkStatus().getStatusString());

            counter = 0;
            if (dLink.getPlugin() == null) {
                statuspanel.setText("plugin missing");
            } else {
                if (dLink.getPlugin().hasHosterIcon()) {
                    statuspanel.setText(dLink.getPlugin().getSessionInfo());
                    statuspanel.setIcon(-1, dLink.getPlugin().getHosterIcon());
                } else {
                    clearSB();
                    sb.append(dLink.getPlugin().getHost());
                    sb.append(dLink.getPlugin().getSessionInfo());
                    statuspanel.setText(sb.toString());
                }
            }
            if (dLink.getTransferStatus().usesPremium()) {
                statuspanel.setIcon(counter, imgPremium);
                counter++;
            }
            if (dLink.getTransferStatus().supportsResume()) {
                statuspanel.setIcon(counter, imgResume);
                counter++;
            }
            statuspanel.clearIcons(counter);
            statuspanel.setBorder(null);
            return statuspanel;
        case DownloadJTableModel.COL_PROGRESS:
            progress.setBorder(null);
            if (dLink.getPlugin() == null) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginError);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (!dLink.getPlugin().getWrapper().usePlugin()) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginDisabled);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (dLink.getPluginProgress() != null) {
                col = this.table.getColumn(column);
                // if (col.getWidth() < 40) {
                //
                // } else if (col.getWidth() < 170) {
                // progress.setString(dLink.getPluginProgress().getPercent() +
                // "%");
                // } else {
                // progress.setString(dLink.getPluginProgress().getPercent() +
                // "%");
                // }

                progress.setMaximum(dLink.getPluginProgress().getTotal());
                progress.setValue(dLink.getPluginProgress().getCurrent());
                // progress.setToolTipText(null);
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && dLink.getPlugin().getRemainingHosterWaittime() > 0) || (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && dLink.getLinkStatus().getRemainingWaittime() > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                progress.setForeground(COL_PROGRESS_ERROR);
                progress.setBorder(ERROR_BORDER);
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                clearSB();
                col = this.table.getColumn(column);
                if (col.getWidth() < 100) {
                    sb.append(c.format(10000 * progress.getPercentComplete() / 100.0)).append('%');
                } else {
                    sb.append(progress.getRealValue() / 1000).append('/').append(progress.getRealMax() / 1000).append(strSecondsAbrv);
                }
                // progress.setString(sb.toString());
                return progress;
            } else if (dLink.getDownloadCurrent() > 0) {
                if (!dLink.getLinkStatus().isPluginActive()) {
                    if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        clearSB();
                        col = this.table.getColumn(column);
                        if (col.getWidth() < 100) {
                            sb.append("100%");
                        } else {
                            sb.append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                        }
                        // progress.setString(sb.toString());

                    } else {
                        clearSB();
                        col = this.table.getColumn(column);
                        if (col.getWidth() < 100) {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                        } else {
                            sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                        }
                        // progress.setString(sb.toString());
                    }
                } else {

                    if (dLink.getLinkStatus().hasStatus(LinkStatus.WAITING_USERIO)) {
                        // progress.setString(strWaitIO);
                    } else {
                        clearSB();
                        col = this.table.getColumn(column);
                        if (col.getWidth() < 100) {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                        } else {
                            sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                        }
                        // progress.setString(sb.toString());
                    }
                }
                progress.setMaximum(10000);
                progress.setValue(dLink.getPercent());
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            }
            progress.setMaximum(10000);
            progress.setValue(0);
            if (dLink.getDownloadSize() > 1) {
                clearSB();
                col = this.table.getColumn(column);
                if (col.getWidth() < 100) {
                    sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                } else {
                    sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                }
                // progress.setString(sb.toString());
            } else {
                // progress.setString(NULL_BYTE_PROGRESS);
            }
            progress.setForeground(COL_PROGRESS_NORMAL);
            return progress;

        case DownloadJTableModel.COL_STATUS_DETAILS:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            clearSB();
            ((JRendererLabel) co).setBorder(null);

            if (dLink.getPlugin() == null) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginError);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (!dLink.getPlugin().getWrapper().usePlugin()) {
                co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginDisabled);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (dLink.getPluginProgress() != null) {

                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(dLink.getPluginProgress().getPercent() + "%");
                ((JRendererLabel) co).setBorder(null);
                return co;

            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && dLink.getPlugin().getRemainingHosterWaittime() > 0) || (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && dLink.getLinkStatus().getRemainingWaittime() > 0)) {
                clearSB();
                col = this.table.getColumn(column);
//                sb.append(Formatter.formatSeconds(dLink.getLinkStatus().getRemainingWaittime() / 1000));
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText("");
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (dLink.getDownloadCurrent() > 0) {
                if (!dLink.getLinkStatus().isPluginActive()) {
                    if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        clearSB();
                        col = this.table.getColumn(column);
                        if (col.getWidth() < 100) {
                            sb.append("100%");
                        } else {
                            sb.append("100% (").append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize()))).append(')');
                        }
                        ((JRendererLabel) co).setIcon(null);
                        ((JRendererLabel) co).setText(sb.toString());
                        ((JRendererLabel) co).setBorder(null);
                    } else {
                        clearSB();
                        col = this.table.getColumn(column);
                        if (col.getWidth() < 100) {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                        } else {
                            sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                        }
                        ((JRendererLabel) co).setIcon(null);
                        ((JRendererLabel) co).setText(sb.toString());
                        ((JRendererLabel) co).setBorder(null);
                    }
                } else {

                    if (dLink.getLinkStatus().hasStatus(LinkStatus.WAITING_USERIO)) {

                        ((JRendererLabel) co).setIcon(null);
                        ((JRendererLabel) co).setText(strWaitIO);
                        ((JRendererLabel) co).setBorder(null);
                    } else {
                        clearSB();
                        col = this.table.getColumn(column);
                        if (col.getWidth() < 100) {
                            sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                        } else {
                            sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                        }

                        ((JRendererLabel) co).setIcon(null);
                        ((JRendererLabel) co).setText(sb.toString());
                        ((JRendererLabel) co).setBorder(null);
                    }
                }

                return co;
            }

            if (dLink.getDownloadSize() > 1) {
                clearSB();
                col = this.table.getColumn(column);
                if (col.getWidth() < 100) {
                    sb.append(c.format(dLink.getPercent() / 100.0)).append('%');
                } else {
                    sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                }
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(sb.toString());
                ((JRendererLabel) co).setBorder(null);
            } else {

                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(NULL_BYTE_PROGRESS);
                ((JRendererLabel) co).setBorder(null);

            }

            return co;
        case DownloadJTableModel.COL_STATUS:

            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            statuspanel.setBackground(co.getBackground());
            statuspanel.setForeground(co.getForeground());
            // if (dLink.getPluginProgress() != null &&
            // dLink.getPluginProgress().getPercent() > 0.0 &&
            // dLink.getPluginProgress().getPercent() < 100.0) {
            // statuspanel.setText(dLink.getLinkStatus().getStatusString());
            // } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED))
            // {
            // statuspanel.setText(dLink.getLinkStatus().getStatusString());
            // } else if (dLink.getLinkStatus().isFailed()) {
            // statuspanel.setText(dLink.getLinkStatus().getStatusString());
            // } else {
            statuspanel.setText(dLink.getLinkStatus().getStatusString());
            // }

            counter = 0;

            if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                statuspanel.setIcon(counter, imgStopMark);
                counter++;
            }

            if (dLink.getLinkStatus().getStatusIcon() != null) {
                statuspanel.setIcon(counter, dLink.getLinkStatus().getStatusIcon());
                counter++;
            } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                statuspanel.setIcon(counter, imgFinished);
                counter++;
            } else if (dLink.getLinkStatus().isFailed()) {
                statuspanel.setIcon(counter, imgFailed);
                counter++;
            }

            if (counter <= StatusLabel.ICONCOUNT && dLink.getPluginProgress() != null && dLink.getPluginProgress().getPercent() > 0.0 && dLink.getPluginProgress().getPercent() < 100.0) {
                statuspanel.setIcon(counter, imgExtract);
                counter++;
            }

            if (counter <= StatusLabel.ICONCOUNT) {
                switch (dLink.getPriority()) {
                case 0:
                default:
                    break;
                case -1:
                    statuspanel.setIcon(counter, imgPriorityS);
                    counter++;
                    break;
                case 1:
                    statuspanel.setIcon(counter, imgPriority1);
                    counter++;
                    break;
                case 2:
                    statuspanel.setIcon(counter, imgPriority2);
                    counter++;
                    break;
                case 3:
                    statuspanel.setIcon(counter, imgPriority3);
                    counter++;
                    break;
                }
            }

            statuspanel.clearIcons(counter);
            statuspanel.setBorder(null);
            return statuspanel;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private Component getFilePackageCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        fp = (FilePackage) value;
        switch (column) {
        case DownloadJTableModel.COL_PART:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName() + " [" + fp.size() + "]");
            if (fp.getLinksFailed() > 0) {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false) ? icon_fp_closed_error : icon_fp_open_error);
            } else {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
            }

            ((JRendererLabel) co).setBorder(null);
            return co;
        case DownloadJTableModel.COL_HOSTER:
            value = fp.getHoster();
            break;
        case DownloadJTableModel.COL_PROGRESS:
            progress.setBorder(null);
            if (fp.isFinished()) {
                progress.setMaximum(100);
                progress.setValue(100);
                clearSB();
                col = this.table.getColumn(column);
                if (col.getWidth() < 100) {
                    sb.append("100%");
                } else {
                    sb.append("100% (").append(Formatter.formatReadable(Math.max(0, fp.getTotalEstimatedPackageSize()))).append(')');
                }
                progress.setString(sb.toString());
            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setValue(fp.getTotalKBLoaded());
                clearSB();
                col = this.table.getColumn(column);
                if (col.getWidth() < 100) {
                    sb.append(c.format(fp.getPercent())).append('%');
                } else {
                    sb.append(Formatter.formatReadable(progress.getRealValue())).append('/').append(Formatter.formatReadable(Math.max(0, progress.getRealMax())));
                }
                progress.setString(sb.toString());
            }
            progress.setForeground(COL_PROGRESS_NORMAL);
            return progress;
        case DownloadJTableModel.COL_STATUS_DETAILS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (fp.isFinished()) {
                clearSB();
                col = this.table.getColumn(column);
                if (col.getWidth() < 100) {
                    sb.append("100%");
                } else {
                    sb.append("100% (").append(Formatter.formatReadable(Math.max(0, fp.getTotalEstimatedPackageSize()))).append(')');
                }
                ((JRendererLabel) co).setText(sb.toString());
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else {
                clearSB();
                col = this.table.getColumn(column);
                if (col.getWidth() < 100) {
                    sb.append(c.format(fp.getPercent())).append('%');
                } else {
                    sb.append(Formatter.formatReadable(fp.getTotalKBLoaded())).append('/').append(Formatter.formatReadable(Math.max(0, fp.getTotalEstimatedPackageSize())));
                }
                ((JRendererLabel) co).setText(sb.toString());
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setBorder(null);
                return co;
            }
        case DownloadJTableModel.COL_STATUS:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            statuspanel.setBackground(co.getBackground());
            statuspanel.setForeground(co.getForeground());
            if (fp.isFinished()) {
                statuspanel.setText("");
            } else if (fp.getTotalDownloadSpeed() > 0) {
                clearSB();
                sb.append('[').append(fp.getLinksInProgress()).append('/').append(fp.size()).append("] ");
                sb.append(strETA).append(' ').append(Formatter.formatSeconds(fp.getETA())).append(" @ ").append(Formatter.formatReadable(fp.getTotalDownloadSpeed())).append("/s");
                statuspanel.setText(sb.toString());
            } else if (fp.getLinksInProgress() > 0) {
                clearSB();
                sb.append(fp.getLinksInProgress()).append('/').append(fp.size()).append(' ').append(strDownloadLinkActive);
                statuspanel.setText(sb.toString());
            } else {
                statuspanel.setText("");
            }
            counter = 0;
            if (fp.isFinished()) {
                statuspanel.setIcon(counter, imgFinished);
                counter++;
            } else if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                statuspanel.setIcon(counter, imgStopMark);
                counter++;
            } else if (fp.getTotalDownloadSpeed() > 0) {

            } else if (fp.getLinksInProgress() > 0) {

            } else {

            }
            statuspanel.clearIcons(counter);
            statuspanel.setBorder(null);
            return statuspanel;
        }
        co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
        return co;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity());
    }
}