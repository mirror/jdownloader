//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.downloadview.Columns;

import java.awt.Component;

import javax.swing.ImageIcon;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.StatusLabel;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class StatusColumn extends JDTableColumn {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.downloadview.TableRenderer.";
    private static final long serialVersionUID = 2228210790952050305L;
    private DownloadLink dLink;
    private StatusLabel statuspanel;
    private int counter = 0;
    private ImageIcon imgFinished;
    private ImageIcon imgFailed;
    private ImageIcon imgExtract;
    private ImageIcon imgPriorityS;
    private ImageIcon imgPriority2;
    private ImageIcon imgPriority1;
    private ImageIcon imgStopMark;
    private ImageIcon imgPriority3;
    private String strStopMark;
    private String strFinished;
    private String strFailed;
    private String strPriorityS;
    private String strPriority1;
    private String strPriority2;
    private String strPriority3;
    private String strExtract;
    private FilePackage fp;
    private StringBuilder sb = new StringBuilder();
    private String strDownloadLinkActive;
    private String strETA;

    public StatusColumn(String name, JDTableModel table) {
        super(name, table);
        statuspanel = new StatusLabel();
        statuspanel.setBorder(null);
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
        imgExtract = JDTheme.II("gui.images.update_manager", 16, 16);
        imgStopMark = JDTheme.II("gui.images.stopmark", 16, 16);
        imgPriorityS = JDTheme.II("gui.images.priority-1", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        strStopMark = JDL.L(JDL_PREFIX + "stopmark", "Stopmark is set");
        strFinished = JDL.L(JDL_PREFIX + "finished", "Download finished");
        strFailed = JDL.L(JDL_PREFIX + "failed", "Download failed");
        strExtract = JDL.L(JDL_PREFIX + "extract", "Extracting");
        strPriorityS = JDL.L("gui.treetable.tooltip.priority-1", "Low Priority");
        strPriority1 = JDL.L("gui.treetable.tooltip.priority1", "High Priority");
        strPriority2 = JDL.L("gui.treetable.tooltip.priority2", "Higher Priority");
        strPriority3 = JDL.L("gui.treetable.tooltip.priority3", "Highest Priority");
        strDownloadLinkActive = JDL.L("gui.treetable.packagestatus.links_active", "aktiv");
        strETA = JDL.L("gui.eta", "ETA");
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            counter = 0;
            if (fp.isFinished()) {
                statuspanel.setIcon(counter, imgFinished, null, strFinished);
                counter++;
            } else if (DownloadWatchDog.getInstance().isStopMark(value)) {
                statuspanel.setIcon(counter, imgStopMark, null, strStopMark);
                counter++;
            }
            clearSB();
            if (fp.getTotalDownloadSpeed() > 0) {
                sb.append('[').append(fp.getLinksInProgress()).append('/').append(fp.size()).append("] ");
                sb.append(strETA).append(' ').append(Formatter.formatSeconds(fp.getETA())).append(" @ ").append(Formatter.formatReadable(fp.getTotalDownloadSpeed())).append("/s");
            } else if (fp.getLinksInProgress() > 0) {
                sb.append(fp.getLinksInProgress()).append('/').append(fp.size()).append(' ').append(strDownloadLinkActive);
            }
            statuspanel.setText(sb.toString(), null);
            statuspanel.clearIcons(counter);
        } else {
            dLink = (DownloadLink) value;
            counter = 0;
            if (DownloadWatchDog.getInstance().isStopMark(value)) {
                statuspanel.setIcon(counter, imgStopMark, null, strStopMark);
                counter++;
            }
            if (dLink.getLinkStatus().getStatusIcon() != null) {
                statuspanel.setIcon(counter, dLink.getLinkStatus().getStatusIcon(), null, dLink.getLinkStatus().getStatusText());
                counter++;
            } else if (dLink.getLinkStatus().isFinished()) {
                statuspanel.setIcon(counter, imgFinished, null, strFinished);
                counter++;
            } else if (dLink.getLinkStatus().isFailed() || (dLink.isAvailabilityStatusChecked() && !dLink.isAvailable())) {
                statuspanel.setIcon(counter, imgFailed, null, strFailed);
                counter++;
            }
            if (counter <= StatusLabel.ICONCOUNT && dLink.getPluginProgress() != null && dLink.getPluginProgress().getPercent() > 0.0 && dLink.getPluginProgress().getPercent() < 100.0) {
                statuspanel.setIcon(counter, imgExtract, null, strExtract);
                counter++;
            }
            if (counter <= StatusLabel.ICONCOUNT) {
                switch (dLink.getPriority()) {
                case 0:
                default:
                    break;
                case -1:
                    statuspanel.setIcon(counter, imgPriorityS, null, strPriorityS);
                    counter++;
                    break;
                case 1:
                    statuspanel.setIcon(counter, imgPriority1, null, strPriority1);
                    counter++;
                    break;
                case 2:
                    statuspanel.setIcon(counter, imgPriority2, null, strPriority2);
                    counter++;
                    break;
                case 3:
                    statuspanel.setIcon(counter, imgPriority3, null, strPriority3);
                    counter++;
                    break;
                }
            }
            if (counter <= StatusLabel.ICONCOUNT && dLink.hasCustomIcon()) {
                statuspanel.setIcon(counter, dLink.getCustomIcon(), null, dLink.getCustomIconText());
                counter++;
            }
            statuspanel.setText(dLink.getLinkStatus().getStatusString(), null);
            statuspanel.clearIcons(counter);
        }
        return statuspanel;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
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
