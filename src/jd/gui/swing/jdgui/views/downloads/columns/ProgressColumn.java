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

package jd.gui.swing.jdgui.views.downloads.columns;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jd.controlling.DownloadController;
import jd.controlling.proxy.ProxyController;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.JDProgressBarRender;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class ProgressColumn extends JDTableColumn {

    private static final long   serialVersionUID   = 2228210790952050305L;
    private DownloadLink        dLink;
    private JDProgressBarRender progress;
    private final String        strPluginDisabled;
    private final String        strPluginError;
    private final String        strUnknownFilesize;
    private final Color         COL_PROGRESS_ERROR = new Color(0xCC3300);
    private final Color         COL_PROGRESS_NORMAL;
    private Color               COL_PROGRESS       = null;
    private FilePackage         fp;
    private JRendererLabel      jlr;

    public ProgressColumn(String name, JDTableModel table) {
        super(name, table);
        progress = new JDProgressBarRender();
        progress.setStringPainted(true);
        progress.setOpaque(true);
        /* FIXME: wait for synthethica simple2d bugfix */
        // progress.setBorder(null);
        COL_PROGRESS_NORMAL = progress.getForeground();
        strPluginDisabled = JDL.L("gui.downloadlink.plugindisabled", "[Plugin disabled]");
        strPluginError = JDL.L("gui.treetable.error.plugin", "Plugin error");
        strUnknownFilesize = JDL.L("jd.gui.swing.jdgui.views.downloadview.Columns.ProgressColumn.unknownFilesize", "Unknown FileSize");
        jlr = new JRendererLabel();
        jlr.setBorder(null);
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
            if (fp.isFinished()) {
                progress.setMaximum(100);
                progress.setValue(100);
            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setValue(fp.getTotalKBLoaded());
            }
            progress.setString(fp.getFilePackageInfo().getProgressString());
            COL_PROGRESS = COL_PROGRESS_NORMAL;
            return progress;
        } else {
            dLink = (DownloadLink) value;
            if (dLink.getDefaultPlugin() == null) {
                jlr.setText(strPluginError);
                return jlr;
            } else if (!dLink.getDefaultPlugin().getWrapper().isEnabled() && !dLink.getLinkStatus().isPluginActive()) {
                jlr.setText(strPluginDisabled);
                return jlr;
            } else if (dLink.getPluginProgress() != null) {
                progress.setString(dLink.getPluginProgress().getPercent() + " %");
                progress.setMaximum(dLink.getPluginProgress().getTotal());
                progress.setValue(dLink.getPluginProgress().getCurrent());
                COL_PROGRESS = COL_PROGRESS_NORMAL;
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && ProxyController.getInstance().getRemainingIPBlockWaittime(dLink.getHost()) > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                COL_PROGRESS = COL_PROGRESS_ERROR;
                progress.setString(dLink.getDownloadLinkInfo().getFormattedWaittime());
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE) && ProxyController.getInstance().getRemainingTempUnavailWaittime(dLink.getHost()) > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                COL_PROGRESS = COL_PROGRESS_ERROR;
                progress.setString(dLink.getDownloadLinkInfo().getFormattedWaittime());
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                return progress;
            } else if (dLink.getLinkStatus().isFinished()) {
                progress.setMaximum(100);
                progress.setString(dLink.getDownloadLinkInfo().getFormattedSize());
                progress.setValue(100);
                COL_PROGRESS = COL_PROGRESS_NORMAL;
                return progress;
            } else if (dLink.getDownloadCurrent() > 0 || dLink.getDownloadSize() > 0) {
                progress.setMaximum(dLink.getDownloadSize());
                progress.setString(dLink.getDownloadLinkInfo().getProgressString());
                progress.setValue(dLink.getDownloadCurrent());
                COL_PROGRESS = COL_PROGRESS_NORMAL;
                return progress;
            }
        }
        jlr.setText(strUnknownFilesize);
        return jlr;
    }

    @Override
    public void handleSelected(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        /* customized handleSelected for ProgressBar */
        if (c instanceof JDProgressBarRender) {
            ((JDProgressBarRender) c).setForeground(COL_PROGRESS);
            /* check selected state */
            if (isSelected) {
                ((JDProgressBarRender) c).setBackground(JDTableColumn.background);
                return;
            } else {
                ((JDProgressBarRender) c).setBackground(JDTableColumn.background);
                /* check if we have to highlight an unselected cell */
                for (JDRowHighlighter high : table.getJDRowHighlighter()) {
                    if (high.doHighlight(value)) {
                        ((JDProgressBarRender) c).setBackground(high.getColor());
                        return;
                    }
                }
            }
        } else {
            super.handleSelected(c, table, value, isSelected, row, column);
        }
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        /*
         * DownloadView hat nur null(Header) oder ne ArrayList(FilePackage)
         */
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<FilePackage> packages = null;
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                packages = DownloadController.getInstance().getPackages();
                if (obj == null && packages.size() > 1) {
                    /* header, sortiere die packages nach namen */
                    Collections.sort(packages, new Comparator<FilePackage>() {
                        public int compare(FilePackage a, FilePackage b) {
                            FilePackage aa = a;
                            FilePackage bb = b;
                            if (sortingToggle) {
                                aa = b;
                                bb = a;
                            }
                            long ap = aa.getTotalEstimatedPackageSize() - aa.getTotalKBLoaded();
                            long bp = bb.getTotalEstimatedPackageSize() - bb.getTotalKBLoaded();
                            if (ap == bp) return 0;
                            return ap < bp ? -1 : 1;
                        }
                    });
                } else {
                    /*
                     * in obj stecken alle selektierten packages, sortiere die
                     * links nach namen
                     */
                    if (obj != null) packages = (ArrayList<FilePackage>) obj;
                    for (FilePackage fp : packages) {
                        Collections.sort(fp.getDownloadLinkList(), new Comparator<DownloadLink>() {
                            public int compare(DownloadLink a, DownloadLink b) {
                                DownloadLink aa = b;
                                DownloadLink bb = a;
                                if (sortingToggle) {
                                    aa = a;
                                    bb = b;
                                }
                                long ap = aa.getDownloadSize() - aa.getDownloadCurrent();
                                long bp = bb.getDownloadSize() - bb.getDownloadCurrent();
                                if (ap == bp) return 0;
                                return ap < bp ? -1 : 1;
                            }
                        });
                    }
                }
            }
        }
        /* inform DownloadController that structure changed */
        DownloadController.getInstance().fireStructureUpdate();
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

}
